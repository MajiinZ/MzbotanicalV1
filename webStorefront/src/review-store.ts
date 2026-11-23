// Build-time review adapter. `npm run build` uses only the real Firebase adapter.
import {changeCart, normalizeProduct} from './domain';
import {mockSummary} from './mock-checkout';
import type {ProfileUpdate, StoreState, StoreUser} from './types';

const user: StoreUser = {uid: 'review-only', email: 'review@example.invalid', displayName: 'Sample Shopper', photoURL: null, emailVerified: true};
const products = [
  normalizeProduct('sample-hybrid', {title: 'Sample Hybrid', category: 'Hybrid', price: 24, amountOfSeeds: 6, stockQuantity: 12, isPublished: true, isNew: true,
    description: 'A sample listing for reviewing the storefront. Not offered for sale.',
    provenance: {name: 'Example breeder (sample link)', url: 'https://example.com/', approved: true}})!,
  normalizeProduct('sample-indica', {title: 'Sample Indica', category: 'Indica', price: 18, amountOfSeeds: 3, stockQuantity: 0, isPublished: true})!,
  normalizeProduct('sample-sativa', {title: 'Sample Sativa', category: 'Sativa', price: 36, amountOfSeeds: 12, stockQuantity: 8, isPublished: true})!,
];
const blank = {firstName:'Sample', lastName:'Shopper', address:'', city:'', state:'', postalCode:'', phoneNumber:''};
const listeners = new Set<(state: StoreState) => void>();
let state: StoreState = {authReady:true, user:null, products, cart:[], orders:[], profile:null, deletionStatus:null, business:null, loading:false, error:null};
let nextLine = 0, nextReceipt = 0;
function emit(fields: Partial<StoreState>): void {
  state = {...state, ...fields};
  listeners.forEach(listener => listener(structuredClone(state)));
}
export function subscribeStore(listener: (state: StoreState) => void): () => void {
  listeners.add(listener); listener(structuredClone(state));
  return () => {listeners.delete(listener);};
}
export async function signIn(): Promise<void> {
  emit({user, profile:{...blank,email:user.email}, orders:[{id:'sample-order',legacy:false,createdAt:1700000000000,
    status:'DELIVERED',paymentStatus:'PAID',totalCents:2400,trackingNumber:'',carrier:'',
    items:[{productId:'sample-hybrid',title:'Sample Hybrid',quantity:1,packSize:6,unitPriceCents:2400}]}]});
}
export async function signOut(): Promise<void> {
  emit({user:null, cart:[], orders:[], profile:null, deletionStatus:null, testReceipt:null});
}
export async function addToCart(id: string, quantity: number): Promise<void> {
  if (!state.user) throw new Error('Use the simulated sign-in first.');
  emit({cart:changeCart(state.cart, products.find(item=>item.id === id) ?? null, null, id, quantity, `line-${++nextLine}`), testReceipt:null});
}
export async function placeTestOrder(): Promise<void> {
  if (!state.user || state.deletionStatus === 'PROCESSING' || state.deletionStatus === 'COMPLETED') throw new Error('The test cart is unavailable.');
  const summary = mockSummary(state.cart, products);
  if (!summary) throw new Error('Review your cart before placing a test order.');
  emit({cart:[], testReceipt:{...summary, id:`TEST-${String(++nextReceipt).padStart(4, '0')}`}});
}
export async function setCartQuantity(lineId: string, quantity: number): Promise<void> {
  if (!state.user) throw new Error('Use the simulated sign-in first.');
  const id = state.cart.find(item=>item.id === lineId)?.productId ?? '';
  emit({cart:changeCart(state.cart, products.find(item=>item.id === id) ?? null, lineId, id, quantity, '')});
}
export async function requestAccountDeletion(): Promise<void> {
  emit({deletionStatus:'REQUESTED'});
}
export async function updateProfile(fields: ProfileUpdate): Promise<void> {
  if (!state.user) throw new Error('Use the simulated sign-in first.');
  emit({profile:{...fields,email:user.email}});
}
