import './style.css';
import {subscribeStore, signIn, signOut, addToCart, setCartQuantity, requestAccountDeletion, updateProfile} from './firebase-store';
import type {StoreState, Product, StoreOrder, ProfileUpdate, CartLine} from './types';

const app = document.querySelector<HTMLDivElement>('#app')!;
const money = (cents: number) => new Intl.NumberFormat('en-US', {style: 'currency', currency: 'USD'}).format(cents / 100);
const html = (value: unknown) => String(value ?? '').replace(/[&<>"']/g, character => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[character]!));
const icons: Record<string, string> = {
  leaf: '<path d="M20 4C10 3 3 8 5 15c2 6 12 6 15-11Z"/><path d="m4 21 10-11M10 14l-1-5m4 2 5 1"/>',
  home: '<path d="m3 11 9-8 9 8M5 10v11h5v-7h4v7h5V10"/>',
  explore: '<circle cx="12" cy="12" r="9"/><path d="m16 8-2 6-6 2 2-6 6-2Z"/>',
  cart: '<path d="M3 3h2l3 12h10l3-9H6M9 20h.01M18 20h.01"/><circle cx="9" cy="20" r="1"/><circle cx="18" cy="20" r="1"/>',
  account: '<circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/>',
  search: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
  arrow: '<path d="M4 12h16m-6-6 6 6-6 6"/>',
  close: '<path d="m6 6 12 12M6 18 18 6"/>',
  menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
  bag: '<path d="M5 7h14l2 14H3L5 7Z"/><path d="M8 8V6a4 4 0 0 1 8 0v2"/>',
  check: '<path d="m5 12 4 4L20 5"/>',
  lock: '<rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V6a4 4 0 0 1 8 0v4"/>',
  logout: '<path d="M10 4H4v16h6m5-13 5 5-5 5m-8-5h13"/>',
  minus: '<path d="M5 12h14"/>',
  plus: '<path d="M5 12h14M12 5v14"/>',
  back: '<path d="M20 12H4m6-6-6 6 6 6"/>',
};
const icon = (name: string, className = '') => `<svg class="icon ${className}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${icons[name] ?? icons.leaf}</svg>`;
function imageUrl(value: string | null | undefined): string | null {
  if (!value) return null;
  try { const url = new URL(value, location.origin); return url.protocol === 'https:' || url.origin === location.origin ? url.href : null; }
  catch { return null; }
}
function externalLink(url: string | undefined, label: string): string {
  const safe = imageUrl(url); return safe?.startsWith('https:') ? `<a href="${html(safe)}" target="_blank" rel="noopener noreferrer">${html(label)}</a>` : '';
}
function storageGet(key: string, session = false): string | null { try { return (session ? sessionStorage : localStorage).getItem(key); } catch { return null; } }
function storageSet(key: string, value: string, session = false): void { try { (session ? sessionStorage : localStorage).setItem(key, value); } catch { /* The declaration remains valid for this page. */ } }
function adultDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const [year, month, day] = value.split('-').map(Number), date = new Date(year, month - 1, day), now = new Date();
  if (year < 1900 || date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day || date > now) return false;
  const age = now.getFullYear() - year - (now.getMonth() < month - 1 || now.getMonth() === month - 1 && now.getDate() < day ? 1 : 0);
  return age >= 18;
}
const deniedDate = storageGet('mz-web-age-denied');
const ui = {route: 'home', productId: '', search: '', category: 'All', busy: '', toast: '', toastError: false,
  modal: '' as '' | 'age' | 'delete' | 'menu' | 'image', agePassed: storageGet('mz-web-age-verified', true) === 'yes',
  ageDenied: Boolean(deniedDate && !adultDate(deniedDate)), ageError: '', ageAction: 'browse', ageDestination: 'explore', ageProductId: '', birthDate: '', quantity: 1,
  profileDraft: {} as Partial<ProfileUpdate>, profileDirty: false, profileUid: '', offline: !navigator.onLine};
let store: StoreState = {authReady: false, user: null, products: [], cart: [], orders: [], profile: null,
  deletionStatus: null, business: null, loading: true, error: null};
let toastTimer: ReturnType<typeof setTimeout> | undefined;
const locked = () => store.deletionStatus === 'PROCESSING' || store.deletionStatus === 'COMPLETED';
const count = () => store.cart.reduce((sum, line) => sum + line.quantity, 0);
const productById = (id: string) => store.products.find(product => product.id === id);
const disabled = (condition = false) => ui.busy || condition ? 'disabled' : '';

function notice(message: string, error = false): void {
  ui.toast = message; ui.toastError = error; clearTimeout(toastTimer); render();
  toastTimer = setTimeout(() => {ui.toast = ''; render();}, 6500);
}
async function perform(operation: () => Promise<void>, success = ''): Promise<void> {
  if (ui.busy) return;
  if (ui.offline) {notice('You appear to be offline. Connect to the internet and try again.', true); return;}
  ui.busy = 'Saving'; render();
  try {await operation(); if (success) notice(success);}
  catch (error) {notice(error instanceof Error ? error.message : 'That could not be completed. Please try again.', true);}
  finally {ui.busy = ''; render();}
}
function beginSignIn(): void {
  if (!ui.agePassed) {ui.ageAction = 'signin'; ui.modal = 'age'; render(); return;}
  void perform(signIn, 'You are signed in.');
}
function navigate(route: string, productId = ''): void {
  if (route !== 'home' && !ui.agePassed) {ui.ageAction = 'browse'; ui.ageDestination = route; ui.ageProductId = productId; ui.modal = 'age'; render(); return;}
  ui.modal = ''; ui.quantity = 1;
  const next = route === 'product' ? `#/product/${encodeURIComponent(productId)}` : route === 'home' ? '#/' : `#/${route}`;
  if (location.hash === next) {readRoute(); render();} else location.hash = next;
  window.scrollTo({top: 0, behavior: 'instant'});
}
function readRoute(): void {
  const parts = location.hash.replace(/^#\/?/, '').split('/');
  ui.route = ['explore', 'cart', 'orders', 'account', 'product'].includes(parts[0]) ? parts[0] : 'home';
  try {ui.productId = ui.route === 'product' ? decodeURIComponent(parts[1] || '') : '';}
  catch {ui.route = 'home'; ui.productId = '';}
  document.title = `${({home: 'The collection', explore: 'Explore', cart: 'Your cart', orders: 'Your orders', account: 'Your account', product: productById(ui.productId)?.title || 'Product details'} as Record<string,string>)[ui.route]} — MZ Botanicals`;
}

function header(): string {
  const nav = [['home','Home'],['explore','Explore'],['orders','Orders'],['account','Account']];
  return `<header class="site-header"><div class="header-inner"><a href="#/" data-route="home" class="wordmark" aria-label="MZ Botanicals home"><img src="/brand/mz-brand.png" width="44" height="44" alt=""/><span>MZ<span class="brand-word">BOTANICALS</span></span></a><nav class="desktop-nav" aria-label="Main navigation">${nav.map(([route,label]) => `<a href="#/${route === 'home' ? '' : route}" data-route="${route}" ${ui.route === route ? 'aria-current="page"' : ''}>${label}</a>`).join('')}</nav><div class="header-actions"><button type="button" class="cart-button" data-route="cart" aria-label="Cart, ${count()} packs">${icon('cart')}<span class="cart-label">Cart</span><span class="cart-count">${count()}</span></button>${!store.user ? `<button type="button" class="button small signin-header" data-action="signin" ${disabled(!store.authReady)}>Sign in</button>` : ''}<button class="icon-button mobile-menu" data-action="menu" aria-label="Open navigation">${icon('menu')}</button></div></div></header>`;
}
function bottomNav(): string {
  return `<nav class="mobile-nav" aria-label="Mobile navigation">${[['home','Home','home'],['explore','Explore','explore'],['cart','Cart','cart'],['account','Account','account']].map(([route,label,glyph]) => `<a href="#/${route === 'home' ? '' : route}" data-route="${route}" ${ui.route === route ? 'aria-current="page"' : ''}>${icon(glyph)}<span>${label}${route === 'cart' && count() ? ` <span class="mobile-count">${count()}</span>` : ''}</span></a>`).join('')}</nav>`;
}
function footer(): string {
  const business = store.business;
  const support = business?.supportEmail && /^[^\s@<>]+@[^\s@<>]+\.[^\s@<>]+$/.test(business.supportEmail) ? `<a href="mailto:${html(business.supportEmail)}">Contact us</a>` : '';
  return `<footer class="site-footer"><div><span class="footer-brand">MZ BOTANICALS</span><p>A collection worth exploring.</p></div><div class="footer-links">${externalLink(business?.privacyPolicyUrl, 'Privacy')}${externalLink(business?.termsUrl, 'Terms')}${externalLink(business?.refundPolicyUrl, 'Returns')}${support}<a href="#/account" data-route="account">Your account</a></div><span class="footer-note">For adults 18 and older.</span></footer>`;
}
function hero(): string {
  return `<section class="hero"><div class="hero-copy"><span class="eyebrow light"><span class="tiny-line"></span> THE MZ BOTANICALS COLLECTION</span><h1>Good things<br/>start with<br/><em>great genetics.</em></h1><p>Explore the collection, get to know each variety, and find your next favorite.</p><div class="hero-actions"><button class="button cream" data-route="explore">Explore the collection ${icon('arrow')}</button><span class="hero-detail">Rooted in possibility.</span></div></div><div class="hero-art" aria-hidden="true"><img src="/brand/mz-welcome.png" alt=""/><span class="art-caption">MZ / BOTANICALS</span><span class="art-index">01</span></div></section>`;
}
function home(): string {
  const arrivals = store.products.filter(product => product.isNew).slice(0,4);
  return `${hero()}<section class="intro-strip"><span>${icon('leaf')} Explore your varieties</span><span>${icon('bag')} Seed packs, clearly detailed</span><span>${icon('account')} Your collection, your account</span></section>
    <section class="section"><div class="section-heading"><div><span class="eyebrow">FIND YOUR NEXT FAVORITE</span><h2>The collection</h2></div><button class="text-button" data-route="explore">Explore all ${icon('arrow')}</button></div><div class="category-grid">${[['Indica','A collection with character.','01'],['Sativa','A different perspective.','02'],['Hybrid','Explore the possibilities.','03']].map(([category,copy,index]) => `<button class="category-tile ${category.toLowerCase()}" data-category-entry="${category}"><span class="tile-index">${index}</span>${icon('leaf','category-leaf')}<span class="tile-bottom"><span><strong>${category}</strong><span>${copy}</span></span>${icon('arrow')}</span></button>`).join('')}</div></section>
    ${ui.agePassed ? `<section class="section"><div class="section-heading"><div><span class="eyebrow">TAKE A CLOSER LOOK</span><h2>${arrivals.length ? 'New arrivals' : 'Available now'}</h2></div><button class="text-button" data-route="explore">View collection ${icon('arrow')}</button></div>${catalogContent(arrivals.length ? arrivals : store.products.slice(0,4))}</section>` : `<section class="welcome-note"><div class="seal">${icon('leaf')}</div><div><span class="eyebrow">A LITTLE MORE TO DISCOVER</span><h2>Make room for something new.</h2><p>Take a closer look at the varieties in our collection. Find pack details, availability, and a place to save your favorites.</p></div><button class="button" data-route="explore">Step inside ${icon('arrow')}</button></section>`}`;
}
function catalogContent(products: Product[]): string {
  if (store.loading && !store.products.length) return `<div class="loading-grid" aria-label="Loading products" aria-busy="true">${[1,2,3,4].map(() => '<div class="skeleton-card"><div></div><span></span><span></span></div>').join('')}</div>`;
  if (!products.length) return emptyState('leaf', store.error ? 'The collection could not be loaded.' : 'A little space for something new.', store.error ? 'Please refresh the page or try again in a moment.' : ui.search || ui.category !== 'All' ? 'Try another search or explore a different category.' : 'There are no published products to show right now. Please check back soon.', store.error ? 'Refresh page' : '', store.error ? 'refresh' : '');
  return `<div class="product-grid">${products.map(productCard).join('')}</div>`;
}
function productPhoto(product: Product, className = ''): string {
  const url = imageUrl(product.thumbnail);
  return `<div class="product-photo ${className}"><span class="photo-fallback">${icon('leaf')}</span>${url ? `<img src="${html(url)}" alt="${html(product.title)}" loading="lazy" referrerpolicy="no-referrer"/>` : ''}</div>`;
}
function productCard(product: Product): string {
  return `<article class="product-card"><button class="product-open" data-product="${html(product.id)}" aria-label="View ${html(product.title)}">${productPhoto(product)}${product.isNew ? '<span class="product-badge">New arrival</span>' : ''}<span class="product-card-body"><span class="eyebrow">${html(product.category || 'Collection')}</span><h3>${html(product.title)}</h3><span class="product-meta">${product.packSize ? `${product.packSize} seeds per pack` : 'See pack details'}<span>${product.stockQuantity > 0 ? 'In stock' : 'Out of stock'}</span></span><span class="product-card-end"><strong>${money(product.priceCents)}</strong><span class="round-arrow">${icon('arrow')}</span></span></span></button></article>`;
}
function explore(): string {
  const filtered = store.products.filter(product => (ui.category === 'All' || product.category.toLowerCase() === ui.category.toLowerCase()) && `${product.title} ${product.category} ${product.description}`.toLowerCase().includes(ui.search.toLowerCase()));
  return `<section class="page-heading"><span class="eyebrow">THE MZ BOTANICALS COLLECTION</span><h1>Find your next favorite.</h1><p>A closer look at every variety, one pack at a time.</p></section><section class="section catalog-section"><div class="catalog-tools"><div class="filters" role="group" aria-label="Filter by category">${['All','Indica','Sativa','Hybrid'].map(category => `<button data-filter="${category}" class="filter ${ui.category === category ? 'selected' : ''}" aria-pressed="${ui.category === category}">${category}</button>`).join('')}</div><label class="search-box" for="product-search">${icon('search')}<span class="sr-only">Search the collection</span><input id="product-search" type="search" placeholder="Search varieties…" value="${html(ui.search)}" autocomplete="off"/></label></div><div class="results-line"><span>${filtered.length} ${filtered.length === 1 ? 'variety' : 'varieties'}</span>${ui.search || ui.category !== 'All' ? '<button class="text-button" data-action="clear-filters">Clear filters</button>' : '<span>Discover the details.</span>'}</div>${catalogContent(filtered)}</section>`;
}
function details(): string {
  const product = productById(ui.productId);
  if (!product) return `<section class="section">${emptyState('leaf', store.loading ? 'Opening this variety…' : 'This variety is unavailable.', 'Explore the collection to see what is currently available.', 'Back to collection', 'explore')}</section>`;
  const max = Math.max(1,Math.min(10,product.stockQuantity)); ui.quantity = Math.min(max, Math.max(1, ui.quantity));
  return `<section class="section detail-section"><button class="text-button breadcrumb" data-route="explore">${icon('back')} Back to collection</button><div class="product-detail"><button class="detail-image" data-action="image" aria-label="Enlarge ${html(product.title)} image">${productPhoto(product)}<span class="image-hint">Take a closer look ${icon('plus')}</span></button><div class="detail-copy"><span class="eyebrow">${html(product.category || 'MZ Botanicals collection')}</span><h1>${html(product.title)}</h1><div class="availability"><span class="availability-dot ${product.stockQuantity ? '' : 'soldout'}"></span>${product.stockQuantity ? 'In stock' : 'Currently out of stock'}${product.isNew ? '<span class="pill">New arrival</span>' : ''}</div><p class="description">${html(product.description)}</p><div class="selection-card"><span class="eyebrow">MAKE IT YOURS</span><div class="pack-row"><div><span class="field-label">Pack size</span><strong>${product.packSize ? `${product.packSize} seeds` : 'Not specified'}</strong></div><div><span class="field-label">Price per pack</span><strong>${money(product.priceCents)}</strong></div></div><div class="purchase-row"><label class="quantity-field" for="product-quantity"><span class="field-label">Quantity · packs</span><input id="product-quantity" type="number" min="1" max="${max}" value="${ui.quantity}" inputmode="numeric" ${disabled(!product.stockQuantity || locked())}/></label><div class="selection-total"><span class="field-label">Item subtotal</span><strong>${money(product.priceCents * ui.quantity)}</strong></div></div><button class="button wide" data-action="add-to-cart" ${disabled(!product.stockQuantity || !product.packSize || locked())}>${icon('bag')}${store.user ? 'Add to cart' : 'Sign in to add to cart'}<span>${money(product.priceCents * ui.quantity)}</span></button><p class="fine-print">Online checkout is currently unavailable. You can save your selection to your cart.</p></div></div></div></section>`;
}
function emptyState(glyph: string, title: string, text: string, button = '', action = ''): string {
  return `<div class="empty-state"><span class="empty-icon">${icon(glyph)}</span><h2>${html(title)}</h2><p>${html(text)}</p>${button ? `<button class="button" ${['signin','refresh'].includes(action) ? `data-action="${action}"` : `data-route="${action}"`}>${html(button)} ${icon('arrow')}</button>` : ''}</div>`;
}
function signInPanel(title: string, text: string): string {return `<section class="section">${emptyState('lock', title, text, 'Continue with Google', 'signin')}</section>`;}
function cartSelectionMatches(line: CartLine, product: Product | undefined): product is Product {
  return Boolean(product && product.packSize !== null && line.amountOfSeeds !== null && line.amountOfSeeds === product.packSize);
}
function cart(): string {
  if (!store.user) return signInPanel('A place for your next favorites.', 'Sign in to save your cart and pick up where you left off.');
  if (locked()) return deletionNotice();
  const subtotal = store.cart.reduce((sum,line) => {const product = productById(line.productId); return sum + (cartSelectionMatches(line,product) ? product.priceCents * line.quantity : 0);}, 0);
  const hasChangedSelection = store.cart.some(line => !cartSelectionMatches(line,productById(line.productId)));
  return `<section class="page-heading"><span class="eyebrow">YOUR COLLECTION, IN THE MAKING</span><h1>Your cart.</h1><p>${count()} ${count() === 1 ? 'pack' : 'packs'} saved for a closer look.</p></section><section class="section cart-layout">${store.cart.length ? `<div class="cart-items">${store.cart.map(line => {
    const product = productById(line.productId), selectionMatches = cartSelectionMatches(line,product);
    const max = Math.min(10,product?.stockQuantity ?? 0);
    return `<article class="cart-item">${product ? `<button class="cart-image" data-product="${html(product.id)}" aria-label="View ${html(product.title)}">${productPhoto(product)}</button>` : `<div class="cart-image unavailable-photo">${icon('leaf')}</div>`}<div class="cart-item-copy"><span class="eyebrow">${html(product?.category || 'Unavailable item')}</span><h2>${html(product?.title || 'This product is no longer available')}</h2><p>${line.amountOfSeeds ? `${line.amountOfSeeds} seeds per pack` : 'Pack details unavailable'}</p><div class="cart-controls"><div class="stepper" aria-label="Quantity"><button data-cart-id="${html(line.id)}" data-cart-quantity="${Math.max(0,line.quantity-1)}" aria-label="Remove one pack" ${disabled(!selectionMatches)}>${icon('minus')}</button><span aria-label="${line.quantity} packs">${line.quantity}</span><button data-cart-id="${html(line.id)}" data-cart-quantity="${line.quantity+1}" aria-label="Add one pack" ${disabled(!selectionMatches || line.quantity >= max)}>${icon('plus')}</button></div><button class="text-button remove" data-cart-id="${html(line.id)}" data-cart-quantity="0" ${disabled()}>Remove</button></div>${product && !selectionMatches ? '<p class="inline-error">Pack size changed or is unavailable. Remove this selection and choose a current pack from the collection.</p>' : selectionMatches && line.quantity > product.stockQuantity ? '<p class="inline-error">Availability changed. Please reduce the quantity.</p>' : ''}</div><strong class="cart-line-total">${selectionMatches ? money(product.priceCents*line.quantity) : '—'}</strong></article>`;
  }).join('')}</div><aside class="cart-summary"><span class="eyebrow">A LITTLE CLOSER</span><h2>Your selection</h2><dl><div><dt>Item subtotal</dt><dd>${hasChangedSelection ? 'Review changed or unavailable items' : money(subtotal)}</dd></div><div><dt>Shipping &amp; tax</dt><dd>Not calculated</dd></div></dl><div class="checkout-notice">${icon('lock')}<div><strong>Checkout is being prepared.</strong><p>Your cart is saved. Online purchases are currently unavailable.</p></div></div><button class="button wide" disabled>Checkout unavailable</button><button class="text-button" data-route="explore">Continue exploring ${icon('arrow')}</button></aside>` : `<div class="cart-empty">${emptyState('bag','Something good belongs here.','Explore the collection and add your favorite packs to your cart.','Explore the collection','explore')}</div>`}</section>`;
}
function orderCard(order: StoreOrder): string {
  const date = order.createdAt ? new Intl.DateTimeFormat('en-US',{dateStyle:'medium'}).format(new Date(order.createdAt)) : 'Date unavailable';
  return `<article class="order-card"><div class="order-heading"><div><span class="eyebrow">ORDER ${html(order.id)}</span><h2>${html(date)}</h2></div><span class="status-pill ${order.legacy ? 'muted' : ''}">${order.legacy ? 'Legacy / unverified' : html(order.status.replaceAll('_',' ').toLowerCase())}</span></div>${order.legacy ? '<p class="order-note">This historical record has unverified payment and fulfillment details.</p>' : `<p class="order-note">Payment: ${html(order.paymentStatus.replaceAll('_',' ').toLowerCase())}${order.paymentStatus !== 'PAID' ? ' · Payment has not been confirmed.' : ''}</p>`}<ul class="order-items">${order.items.map(item => `<li><span>${html(item.title)}${item.packSize ? `<small>${item.packSize} seeds per pack</small>` : ''}</span><span>${item.quantity} ${item.quantity === 1 ? 'pack' : 'packs'}</span>${item.unitPriceCents !== null ? `<span>${money(item.unitPriceCents * item.quantity)}</span>` : '<span>Price unavailable</span>'}</li>`).join('')}</ul>${order.trackingNumber ? `<p class="order-note">Tracking: ${html(order.carrier)} ${html(order.trackingNumber)}</p>` : ''}<div class="order-total"><span>${order.legacy ? 'Recorded total' : 'Order total'}</span><strong>${order.totalCents !== null ? money(order.totalCents) : 'Unavailable'}</strong></div></article>`;
}
function orders(): string {
  if (!store.user) return signInPanel('Your orders, all in one place.', 'Sign in to view your saved order history and available status updates.');
  if (locked()) return deletionNotice();
  return `<section class="page-heading"><span class="eyebrow">KEEP TRACK OF THE DETAILS</span><h1>Your orders.</h1><p>Order history and updates for your account.</p></section><section class="section order-list">${store.orders.length ? store.orders.map(orderCard).join('') : emptyState('bag',store.loading ? 'Opening your order history…' : 'Your story starts here.',store.loading ? 'Just a moment while your account loads.' : 'There are no orders to show for this account.','Explore the collection','explore')}</section>`;
}
function deletionNotice(): string {return `<section class="section">${emptyState('lock',store.deletionStatus === 'COMPLETED' ? 'Account deletion processed.' : 'Account deletion is in progress.','Your account is restricted while the store handles your request under its retention policy. You can sign out below.')}<div class="center"><button class="button secondary" data-action="signout" ${disabled()}>Sign out ${icon('logout')}</button></div></section>`;}
function profileField(name: keyof ProfileUpdate, label: string, autocomplete: string, options = ''): string {
  return `<label class="form-field" for="profile-${name}"><span>${label}</span><input id="profile-${name}" name="${name}" value="${html(ui.profileDraft[name] || '')}" autocomplete="${autocomplete}" ${options}/></label>`;
}
function account(): string {
  if (!store.user) return signInPanel('A space of your own.', 'Sign in with Google to manage your profile, save your cart, and view your orders.');
  if (locked()) return deletionNotice();
  const avatar = imageUrl(store.user.photoURL);
  return `<section class="page-heading"><span class="eyebrow">WELCOME TO YOUR SPACE</span><h1>Your account.</h1></section><section class="section account-layout"><aside class="identity-card">${avatar ? `<img class="avatar" src="${html(avatar)}" referrerpolicy="no-referrer" alt=""/>` : `<span class="avatar avatar-icon">${icon('account')}</span>`}<h2>${html(store.user.displayName || 'Welcome back')}</h2><p>${html(store.user.email)}</p><a class="identity-link" href="#/orders" data-route="orders">Your orders ${icon('arrow')}</a><a class="identity-link" href="#/cart" data-route="cart">Your saved cart <span>${count()}</span></a><button class="text-button" data-action="signout" ${disabled()}>${icon('logout')} Sign out</button></aside><div class="account-content"><section class="profile-card"><div class="section-heading compact"><div><span class="eyebrow">THE EVERYDAY DETAILS</span><h2>Your profile</h2></div></div>${store.profile ? `<form id="profile-form"><div class="form-grid">${profileField('firstName','First name','given-name','maxlength="50"')}${profileField('lastName','Last name','family-name','maxlength="50"')}${profileField('address','Street address','street-address','maxlength="150"')}${profileField('city','City','address-level2','maxlength="80"')}${profileField('state','State','address-level1','maxlength="2" placeholder="CA"')}${profileField('postalCode','ZIP code','postal-code','maxlength="10" inputmode="numeric"')}${profileField('phoneNumber','Phone number','tel','type="tel" maxlength="25"')}</div><div class="form-end"><span>Keep your details up to date.</span><button type="submit" class="button" ${disabled()}>${ui.busy ? 'Saving…' : 'Save changes'} ${icon('check')}</button></div></form>` : '<p>Your profile is loading. If it does not appear, sign out and sign in again.</p>'}</section><section class="privacy-card"><span class="eyebrow">ACCOUNT &amp; PRIVACY</span><h2>Stay in control.</h2>${store.deletionStatus ? `<div class="request-status"><strong>Request status: ${html(store.deletionStatus)}</strong><p>Your account-deletion request has been recorded. The store will review it under its retention policy.</p></div>` : '<p>You can request account and data deletion. The store reviews each request before processing it.</p>'}<button class="text-button danger" data-action="delete-dialog" ${disabled(Boolean(store.deletionStatus))}>Request account deletion ${icon('arrow')}</button></section></div></section>`;
}
function modal(): string {
  if (!ui.modal) return '';
  let contents = '';
  if (ui.modal === 'menu') contents = `<span class="eyebrow">YOUR WAY AROUND</span><h2 id="modal-title">Explore MZ Botanicals</h2><nav class="drawer-nav" aria-label="All pages">${[['home','Home'],['explore','Explore the collection'],['cart','Your cart'],['orders','Your orders'],['account','Your account']].map(([route,label])=>`<button data-route="${route}">${label} ${icon('arrow')}</button>`).join('')}</nav>`;
  if (ui.modal === 'delete') contents = `<span class="eyebrow">ACCOUNT &amp; PRIVACY</span><h2 id="modal-title">Request account deletion?</h2><p>This sends an account and data deletion request to the store for review. It does not immediately delete your account, cancel orders, or issue refunds.</p><p>Some records may be retained under the store’s approved policy.</p><div class="dialog-actions"><button class="button secondary" data-action="close-modal">Keep account</button><button class="button danger-button" data-action="confirm-deletion" ${disabled()}>Send deletion request</button></div>`;
  if (ui.modal === 'age') contents = `<span class="eyebrow">BEFORE YOU STEP INSIDE</span><h2 id="modal-title">${ui.ageDenied ? 'Access restricted.' : 'Welcome to the collection.'}</h2><p>${ui.ageDenied ? 'You must be 18 or older to access MZ Botanicals. Sign-in is unavailable on this browser until you turn 18.' : 'You must be at least 18 to continue. Please enter your date of birth.'}</p>${!ui.ageDenied ? `<form id="age-form"><label class="form-field" for="birth-date"><span>Date of birth</span><input id="birth-date" name="birthDate" type="date" value="${html(ui.birthDate)}" min="1900-01-01" max="${new Date().getFullYear()}-${String(new Date().getMonth()+1).padStart(2,'0')}-${String(new Date().getDate()).padStart(2,'0')}" required autocomplete="bday"/></label>${ui.ageError ? `<p class="inline-error" role="alert">${html(ui.ageError)}</p>` : ''}<button type="submit" class="button wide">Verify age &amp; continue ${icon('arrow')}</button><p class="fine-print">Your birth date is not sent to the store during this check.</p></form>` : ''}`;
  if (ui.modal === 'image') {const product = productById(ui.productId); contents = product ? `<h2 id="modal-title" class="sr-only">${html(product.title)} image</h2>${productPhoto(product,'enlarged')}` : '';}
  return `<dialog id="store-dialog" class="store-dialog ${ui.modal === 'image' ? 'image-dialog' : ''}" aria-labelledby="modal-title"><button class="icon-button dialog-close" data-action="close-modal" aria-label="Close dialog">${icon('close')}</button>${contents}</dialog>`;
}
function render(): void {
  const active = document.activeElement as HTMLInputElement | null;
  const focusId = active?.id, selection = active?.selectionStart, selectionEnd = active?.selectionEnd;
  const visibleRoute = !ui.agePassed && ui.route !== 'home' ? 'home' : ui.route;
  const content = ({home, explore, product: details, cart, orders, account} as Record<string,()=>string>)[visibleRoute]();
  app.innerHTML = `${header()}${ui.offline ? '<div class="connection-bar" role="status">You are offline. Connect to save changes.</div>' : ''}${store.error ? `<div class="error-bar" role="alert">${html(store.error)}<button class="text-button" data-action="refresh">Refresh page</button></div>` : ''}<main id="main-content" tabindex="-1">${content}</main>${footer()}${bottomNav()}${ui.toast ? `<div class="toast ${ui.toastError ? 'error' : ''}" role="${ui.toastError ? 'alert' : 'status'}">${icon(ui.toastError ? 'close' : 'check')}<span>${html(ui.toast)}</span><button class="icon-button" data-action="close-toast" aria-label="Dismiss notification">${icon('close')}</button></div>` : ''}${modal()}`;
  app.querySelectorAll('img').forEach(image => image.addEventListener('error', () => image.classList.add('image-failed'), {once:true}));
  const dialog = document.querySelector<HTMLDialogElement>('#store-dialog');
  if (dialog) {dialog.showModal(); dialog.addEventListener('cancel', event => {event.preventDefault(); ui.modal = ''; render();});}
  if (focusId) {const next = document.getElementById(focusId) as HTMLInputElement | null; if (next) {next.focus({preventScroll:true}); try {if (selection !== null && selection !== undefined) next.setSelectionRange(selection,selectionEnd ?? selection);} catch { /* Date/number inputs do not support text selection. */ }}}
}

app.addEventListener('click', event => {
  const target = (event.target as Element).closest<HTMLElement>('button,a'); if (!target) return;
  if (target instanceof HTMLButtonElement && target.disabled) return;
  if (target.dataset.route) {event.preventDefault(); navigate(target.dataset.route); return;}
  if (target.dataset.product) {event.preventDefault(); if (!ui.agePassed) {ui.ageAction = 'browse'; ui.modal = 'age'; render();} else navigate('product', target.dataset.product); return;}
  if (target.dataset.categoryEntry) {ui.category = target.dataset.categoryEntry; ui.search = ''; navigate('explore'); return;}
  if (target.dataset.filter) {ui.category = target.dataset.filter; render(); return;}
  if (target.dataset.cartId) {const quantity = Number(target.dataset.cartQuantity); void perform(()=>setCartQuantity(target.dataset.cartId!, quantity), quantity === 0 ? 'Item removed from your cart.' : 'Cart updated.'); return;}
  switch (target.dataset.action) {
    case 'signin': beginSignIn(); break;
    case 'signout': void perform(async()=>{await signOut(); ui.profileDirty = false; navigate('home');},'You are signed out.'); break;
    case 'refresh': location.reload(); break;
    case 'menu': ui.modal = 'menu'; render(); break;
    case 'close-modal': ui.modal = ''; ui.ageError = ''; render(); break;
    case 'close-toast': ui.toast = ''; render(); break;
    case 'clear-filters': ui.search = ''; ui.category = 'All'; render(); break;
    case 'image': ui.modal = 'image'; render(); break;
    case 'delete-dialog': ui.modal = 'delete'; render(); break;
    case 'confirm-deletion': void perform(async()=>{await requestAccountDeletion(); ui.modal = '';},'Your deletion request has been recorded for review.'); break;
    case 'add-to-cart': if (!store.user) beginSignIn(); else void perform(()=>addToCart(ui.productId,ui.quantity),'Added to your cart.'); break;
  }
});
app.addEventListener('input', event => {
  const input = event.target as HTMLInputElement;
  if (input.id === 'birth-date') ui.birthDate = input.value;
  if (input.id === 'product-search') {ui.search = input.value; render();}
  if (input.id === 'product-quantity') {const product = productById(ui.productId); ui.quantity = Math.min(Math.max(1,Number(input.value)||1), Math.min(10,product?.stockQuantity||1)); render();}
  if (input.form?.id === 'profile-form') {ui.profileDraft[input.name as keyof ProfileUpdate] = input.value; ui.profileDirty = true;}
});
app.addEventListener('submit', event => {
  const form = event.target as HTMLFormElement; event.preventDefault();
  if (form.id === 'age-form') {
    const birthDate = String(new FormData(form).get('birthDate') || '');
    if (!/^\d{4}-\d{2}-\d{2}$/.test(birthDate) || birthDate > String(form.querySelector<HTMLInputElement>('input')?.max)) {ui.ageError = 'Enter a valid birth date in the past.'; render(); return;}
    if (!adultDate(birthDate)) {storageSet('mz-web-age-denied',birthDate); ui.ageDenied = true; render(); return;}
    ui.agePassed = true; ui.ageError = ''; storageSet('mz-web-age-verified','yes',true); ui.modal = '';
    if (ui.ageAction === 'signin') beginSignIn(); else navigate(ui.ageDestination,ui.ageProductId);
  }
  if (form.id === 'profile-form') {
    const fields = Object.fromEntries(new FormData(form)) as unknown as ProfileUpdate;
    void perform(async()=>{await updateProfile(fields); ui.profileDirty = false;},'Your profile has been saved.');
  }
});
window.addEventListener('hashchange',()=>{ui.modal = ''; readRoute(); render();});
window.addEventListener('online',()=>{ui.offline = false; render();});
window.addEventListener('offline',()=>{ui.offline = true; render();});
readRoute();
subscribeStore(next => {
  const changedUser = (next.user?.uid || '') !== ui.profileUid;
  store = next;
  if (changedUser) {ui.profileUid = next.user?.uid || ''; ui.profileDraft = {}; ui.profileDirty = false; if (ui.modal === 'delete') ui.modal = '';}
  if (next.profile && !ui.profileDirty) ui.profileDraft = {...next.profile};
  readRoute(); render();
});
