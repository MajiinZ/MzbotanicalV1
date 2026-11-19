import {initializeApp} from 'firebase/app';
import {getAuth, GoogleAuthProvider, onAuthStateChanged, signInWithPopup, signOut as firebaseSignOut, type User} from 'firebase/auth';
import {collection, doc, getFirestore, limit, onSnapshot, query, runTransaction, where, type DocumentData, type Transaction, type Unsubscribe} from 'firebase/firestore';
import firebaseConfig from './firebase-config.json';
import {changeCart, normalizeBusiness, normalizeOrder, normalizeProduct, normalizeProfile, parseCart, safeHttpsUrl, StoreValidationError, validId} from './domain';
import type {ProfileUpdate, StoreState, StoreUser} from './types';

const app = initializeApp(firebaseConfig);
const auth = getAuth(app), db = getFirestore(app);
const listeners = new Set<(state: StoreState) => void>();
let state: StoreState = {authReady: false, user: null, products: [], cart: [], orders: [], profile: null, deletionStatus: null, business: null, loading: true, error: null};
let generation = 0, catalogLoading = true, profileLoading = false;
let pendingSignOuts = 0;
let accountRequestReadable = true;
let privateSubscriptions: Unsubscribe[] = [], protectedSubscriptions: Unsubscribe[] = [];

function emit(change: Partial<StoreState> = {}) {
  state = {...state, ...change, loading: catalogLoading || profileLoading};
  for (const listener of listeners) listener(structuredClone(state));
}
const stopProtected = () => { protectedSubscriptions.forEach(unsubscribe => unsubscribe()); protectedSubscriptions = []; };
function clearPrivate() {
  privateSubscriptions.forEach(unsubscribe => unsubscribe()); privateSubscriptions = [];
  stopProtected();
  accountRequestReadable = true;
  state = {...state, cart: [], orders: [], profile: null, deletionStatus: null, business: null, error: null};
}
const active = (uid: string, epoch: number) => generation === epoch && auth.currentUser?.uid === uid && state.user?.uid === uid;
function assertActive(uid: string, epoch: number) {
  if (!active(uid, epoch)) throw new StoreValidationError('Your account changed. Please reopen this page and try again.');
}
async function guardedTransaction<T>(uid: string, epoch: number, action: (transaction: Transaction) => Promise<T>): Promise<T> {
  let expired = false;
  const check = () => {
    assertActive(uid, epoch);
    if (expired) throw new StoreValidationError('The save could not be confirmed. Refresh this page before trying again.');
  };
  let timer: ReturnType<typeof setTimeout>;
  const deadline = new Promise<never>((_, reject) => {
    timer = setTimeout(() => {
      expired = true;
      reject(new StoreValidationError('The save could not be confirmed. Refresh this page before trying again.'));
    }, 30000);
  });
  try {
    return await Promise.race([runTransaction(db, async transaction => {
      check();
      const result = await action(transaction);
      // Queued transaction writes cannot commit if the session/deadline changed
      // while a document read was pending. An already submitted commit cannot
      // be cancelled; the timeout deliberately says unconfirmed, not failed.
      check();
      return result;
    }, {maxAttempts: 3}), deadline]);
  } finally { clearTimeout(timer!); }
}
const blocked = (status: string | null) => status === 'PROCESSING' || status === 'COMPLETED';
function requestStatus(value: DocumentData | undefined, uid: string): string | null {
  if (!value) return null;
  if (value.customerId !== uid || !['REQUESTED', 'PROCESSING', 'COMPLETED'].includes(value.status)) {
    throw new StoreValidationError('Unable to confirm your account request status. Please contact support.');
  }
  return value.status;
}
function authenticated(): {user: User; epoch: number} {
  const user = auth.currentUser;
  if (!user || state.user?.uid !== user.uid) throw new StoreValidationError('Please sign in to continue.');
  if (!user.emailVerified) throw new StoreValidationError('Please sign in with a verified Google email.');
  if (!accountRequestReadable) throw new StoreValidationError('Unable to confirm your account status. Refresh this page before making changes.');
  if (blocked(state.deletionStatus)) throw new StoreValidationError('Your account-deletion request is being processed. Changes are unavailable.');
  return {user, epoch: generation};
}
function friendly(error: unknown, fallback: string): Error {
  if (error instanceof StoreValidationError) return error;
  const code = error && typeof error === 'object' && 'code' in error ? String(error.code) : '';
  if (['auth/popup-closed-by-user', 'auth/cancelled-popup-request'].includes(code)) return new Error('Sign-in was cancelled. You can try again.');
  if (code === 'auth/popup-blocked') return new Error('Your browser blocked sign-in. Allow the Google sign-in window and try again.');
  if (code === 'auth/unauthorized-domain') return new Error('Sign-in is not available at this address yet. Please contact the store.');
  if (['unavailable', 'auth/network-request-failed'].includes(code)) return new Error('Unable to connect. Check your internet connection and try again.');
  return new Error(fallback);
}
async function operation<T>(fallback: string, task: () => Promise<T>): Promise<T> {
  try { emit({error: null}); return await task(); }
  catch (error) { throw friendly(error, fallback); }
}
function userView(user: User): StoreUser {
  return {uid: user.uid, email: user.email || '', displayName: (user.displayName || '').slice(0, 100),
    photoURL: safeHttpsUrl(user.photoURL) || null, emailVerified: user.emailVerified};
}
async function readFence(transaction: Transaction, uid: string, epoch: number): Promise<void> {
  const deletion = await transaction.get(doc(db, 'account_deletion_request', uid));
  assertActive(uid, epoch);
  if (blocked(requestStatus(deletion.exists() ? deletion.data() : undefined, uid))) {
    throw new StoreValidationError('Your account-deletion request is being processed. Changes are unavailable.');
  }
}
async function ensureCustomer(user: User, epoch: number) {
  const reference = doc(db, 'customer', user.uid);
  await guardedTransaction(user.uid, epoch, async transaction => {
    await readFence(transaction, user.uid, epoch);
    const existing = await transaction.get(reference);
    assertActive(user.uid, epoch);
    if (!existing.exists()) {
      const names = (user.displayName || '').trim().split(/\s+/);
      transaction.set(reference, {id: user.uid, firstName: (names[0] || '').slice(0, 50), lastName: names.slice(1).join(' ').slice(0, 50),
        email: user.email || '', city: null, state: null, postalCode: null, zip: null, address: null, phoneNumber: null,
        cart: [], isAdmin: false, dateOfBirth: '', gender: '', profileImageUrl: null});
    } else if (existing.data().id !== user.uid) throw new StoreValidationError('Your saved profile needs review. Please contact support.');
  });
}
function startProtectedSubscriptions(user: User, epoch: number) {
  const current = () => active(user.uid, epoch) && accountRequestReadable && !blocked(state.deletionStatus);
  protectedSubscriptions.push(onSnapshot(doc(db, 'customer', user.uid), snapshot => {
    if (!current()) return;
    profileLoading = false;
    try {
      if (!snapshot.exists() || snapshot.data().id !== user.uid) throw new StoreValidationError('Your profile is unavailable. Please sign out and contact support.');
      emit({profile: normalizeProfile(snapshot.data(), user.email || ''), cart: parseCart(snapshot.data().cart)});
    } catch (error) { emit({profile: null, cart: [], error: friendly(error, 'Unable to load your saved profile.').message}); }
  }, () => { if (current()) { profileLoading = false; emit({profile: null, cart: [], error: 'Unable to load your saved profile. Please try again.'}); } }));
  protectedSubscriptions.push(onSnapshot(query(collection(db, 'order'), where('customerId', '==', user.uid), limit(100)), snapshot => {
    if (!current()) return;
    emit({orders: snapshot.docs.flatMap(document => {
      const order = normalizeOrder(document.id, document.data(), user.uid); return order ? [order] : [];
    }).sort((a, b) => (b.createdAt ?? 0) - (a.createdAt ?? 0))});
  }, () => { if (current()) emit({orders: [], error: 'Unable to load your order history. Please try again.'}); }));
  protectedSubscriptions.push(onSnapshot(doc(db, 'store_config', 'public'), snapshot => {
    if (current()) emit({business: snapshot.exists() ? normalizeBusiness(snapshot.data()) : null});
  }, () => { if (current()) emit({business: null}); }));
}

// A published-only query remains safe before/after sign-in, including owner sessions.
onSnapshot(query(collection(db, 'product'), where('isPublished', '==', true), limit(100)), snapshot => {
  catalogLoading = false;
  emit({products: snapshot.docs.flatMap(document => {
    const product = normalizeProduct(document.id, document.data()); return product ? [product] : [];
  }).sort((a, b) => Number(b.isNew) - Number(a.isNew) || a.title.localeCompare(b.title))});
}, () => { catalogLoading = false; emit({products: [], error: 'The catalog could not load. Please check your connection and try again.'}); });

onAuthStateChanged(auth, user => {
  // Ignore queued callbacks for a superseded identity, including an old signed-
  // in callback that arrives while explicit sign-out is still being completed.
  if (user?.uid !== auth.currentUser?.uid || pendingSignOuts > 0 && user !== null) return;
  const epoch = ++generation;
  clearPrivate();
  profileLoading = !!user;
  emit({authReady: true, user: user ? userView(user) : null});
  if (!user) return;
  privateSubscriptions.push(onSnapshot(doc(db, 'account_deletion_request', user.uid), snapshot => {
    if (!active(user.uid, epoch)) return;
    try {
      const status = requestStatus(snapshot.exists() ? snapshot.data() : undefined, user.uid);
      accountRequestReadable = true;
      if (blocked(status)) {
        stopProtected(); profileLoading = false;
        emit({deletionStatus: status, cart: [], orders: [], profile: null, business: null});
      } else emit({deletionStatus: status});
    } catch (error) {
      accountRequestReadable = false;
      stopProtected(); profileLoading = false;
      emit({cart: [], orders: [], profile: null, business: null, error: friendly(error, 'Unable to load your account request.').message});
    }
  }, () => { if (active(user.uid, epoch)) { accountRequestReadable = false; stopProtected(); profileLoading = false; emit({cart: [], orders: [], profile: null, business: null, error: 'Unable to load your account request. Please try again.'}); } }));
  void ensureCustomer(user, epoch).then(() => {
    if (active(user.uid, epoch) && accountRequestReadable && !blocked(state.deletionStatus)) startProtectedSubscriptions(user, epoch);
  }).catch(error => {
    if (active(user.uid, epoch)) { profileLoading = false; emit({error: friendly(error, 'Unable to open your account. Please try again.').message}); }
  });
}, () => { ++generation; clearPrivate(); profileLoading = false; emit({authReady: true, user: null, error: 'Unable to restore sign-in. Please sign in again.'}); });

export function subscribeStore(listener: (state: StoreState) => void): () => void {
  listeners.add(listener); listener(structuredClone(state)); return () => listeners.delete(listener);
}
export async function signIn(): Promise<void> {
  return operation('Unable to sign in. Please try again.', async () => {
    const provider = new GoogleAuthProvider(); provider.setCustomParameters({prompt: 'select_account'});
    await signInWithPopup(auth, provider);
  });
}
export async function signOut(): Promise<void> {
  pendingSignOuts++;
  ++generation; clearPrivate(); profileLoading = false; emit({user: null});
  try { await operation('Unable to sign out. Please try again.', () => firebaseSignOut(auth)); }
  finally { pendingSignOuts--; }
}

async function editCart(productId: string | null, lineId: string | null, quantity: number): Promise<void> {
  const {user, epoch} = authenticated(), reference = doc(db, 'customer', user.uid);
  await guardedTransaction(user.uid, epoch, async transaction => {
    await readFence(transaction, user.uid, epoch);
    const customer = await transaction.get(reference);
    assertActive(user.uid, epoch);
    if (!customer.exists() || customer.data().id !== user.uid) throw new StoreValidationError('Your saved profile is unavailable. Please sign in again.');
    const cart = parseCart(customer.data().cart), targetId = productId ?? cart.find(line => line.id === lineId)?.productId;
    if (!targetId || !validId(targetId)) throw new StoreValidationError('This cart item was removed. Please refresh your cart.');
    const productSnapshot = lineId && quantity === 0 ? null : await transaction.get(doc(db, 'product', targetId));
    assertActive(user.uid, epoch);
    const product = productSnapshot?.exists() ? normalizeProduct(productSnapshot.id, productSnapshot.data()) : null;
    const changed = changeCart(cart, product, lineId, targetId, quantity, crypto.randomUUID().replaceAll('-', ''));
    assertActive(user.uid, epoch);
    transaction.update(reference, {cart: changed});
  });
  assertActive(user.uid, epoch);
}
export async function addToCart(productId: string, quantity: number): Promise<void> {
  return operation('Unable to save your cart. Please try again.', () => editCart(productId, null, quantity));
}
export async function setCartQuantity(lineId: string, quantity: number): Promise<void> {
  return operation('Unable to update your cart. Please try again.', () => editCart(null, lineId, quantity));
}

export async function requestAccountDeletion(): Promise<void> {
  return operation('Unable to submit your deletion request. Please try again.', async () => {
    const {user, epoch} = authenticated(), reference = doc(db, 'account_deletion_request', user.uid);
    await guardedTransaction(user.uid, epoch, async transaction => {
      const existing = await transaction.get(reference); assertActive(user.uid, epoch);
      if (existing.exists()) { requestStatus(existing.data(), user.uid); return; }
      transaction.set(reference, {customerId: user.uid, status: 'REQUESTED', requestedAtEpochMillis: Date.now()});
    });
    assertActive(user.uid, epoch);
  });
}

export async function updateProfile(update: ProfileUpdate): Promise<void> {
  return operation('Unable to save your profile. Please try again.', async () => {
    const {user, epoch} = authenticated(), reference = doc(db, 'customer', user.uid);
    const fields = {firstName: update.firstName.trim(), lastName: update.lastName.trim(), city: update.city.trim(), state: update.state.trim().toUpperCase(),
      postalCode: update.postalCode.trim(), address: update.address.trim(), phoneNumber: update.phoneNumber.replace(/[\s()+-]/g, '')};
    if (fields.firstName.length > 50 || fields.lastName.length > 50 || fields.city.length > 80 || fields.address.length > 150 ||
        fields.state && !/^[A-Z]{2}$/.test(fields.state) || fields.postalCode && !/^\d{5}(-\d{4})?$/.test(fields.postalCode) ||
        fields.phoneNumber && !/^\d{10,15}$/.test(fields.phoneNumber)) throw new StoreValidationError('Review your name, address, state, ZIP code and phone number.');
    await guardedTransaction(user.uid, epoch, async transaction => {
      await readFence(transaction, user.uid, epoch);
      const existing = await transaction.get(reference); assertActive(user.uid, epoch);
      if (!existing.exists() || existing.data().id !== user.uid) throw new StoreValidationError('Your saved profile is unavailable. Please sign in again.');
      const existingPhone = existing.data().phoneNumber;
      const dialCode = Number.isInteger(existingPhone?.dialCode) && existingPhone.dialCode > 0 ? existingPhone.dialCode : 1;
      transaction.update(reference, {...fields, phoneNumber: fields.phoneNumber ? {dialCode, number: fields.phoneNumber} : null});
    });
    assertActive(user.uid, epoch);
  });
}
