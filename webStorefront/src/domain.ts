import type {CartLine, Product, Profile, PublicBusiness, StoreOrder} from './types';

export const MAX_QUANTITY = 10;
export class StoreValidationError extends Error {}
export const validId = (value: unknown): value is string => typeof value === 'string' && /^[A-Za-z0-9_-]{1,128}$/.test(value);
const object = (value: unknown): Record<string, unknown> => value !== null && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {};
const text = (value: unknown, maximum = 5000): string => typeof value === 'string' ? value.slice(0, maximum) : '';
const integer = (value: unknown, minimum: number, maximum = Number.MAX_SAFE_INTEGER): value is number => Number.isSafeInteger(value) && Number(value) >= minimum && Number(value) <= maximum;

export function moneyCents(value: unknown): number | null {
  if (typeof value !== 'number' || !Number.isFinite(value) || value < 0 || value > 100000000) return null;
  const cents = Math.round(value * 100);
  return Math.abs(value * 100 - cents) < 0.00001 && Number.isSafeInteger(cents) ? cents : null;
}

export function safeHttpsUrl(value: unknown): string {
  if (typeof value !== 'string' || value.length > 4096) return '';
  try { const url = new URL(value); return url.protocol === 'https:' && !url.username && !url.password ? url.href : ''; }
  catch { return ''; }
}

export function normalizeProduct(id: string, value: unknown): Product | null {
  const data = object(value), priceCents = moneyCents(data.price);
  if (!validId(id) || data.isPublished !== true || !text(data.title, 200).trim() || priceCents === null || priceCents <= 0 || priceCents > 10000000 ||
      !integer(data.stockQuantity, 0, 1000000000)) return null;
  const pack = typeof data.amountOfSeeds === 'string' && /^\d+$/.test(data.amountOfSeeds) ? Number(data.amountOfSeeds) : data.amountOfSeeds;
  return {id, title: text(data.title, 200), description: text(data.description), priceCents,
    thumbnail: safeHttpsUrl(data.thumbnail), category: text(data.category, 80),
    packSize: integer(pack, 1, 10000) ? pack : null, stockQuantity: data.stockQuantity,
    isNew: data.isNew === true, isPopular: data.isPopular === true, isDiscounted: data.isDiscounted === true};
}

export function parseCart(value: unknown): CartLine[] {
  if (value === undefined || value === null) return [];
  if (!Array.isArray(value) || value.length > 100) throw new StoreValidationError('Your saved cart needs review. Please contact support.');
  const seen = new Set<string>();
  return value.map(item => {
    const data = object(item), pack = data.amountOfSeeds;
    if (!validId(data.id) || !validId(data.productId) || seen.has(data.id) || !integer(data.quantity, 1, MAX_QUANTITY) ||
        pack !== undefined && pack !== null && !integer(pack, 1, 10000)) {
      throw new StoreValidationError('Your saved cart needs review. Please contact support.');
    }
    seen.add(data.id);
    return {id: data.id, productId: data.productId, amountOfSeeds: typeof pack === 'number' ? pack : null, quantity: data.quantity};
  });
}

export function changeCart(cart: CartLine[], product: Product | null, lineId: string | null, productId: string, quantity: number, newLineId: string): CartLine[] {
  if (!integer(quantity, lineId ? 0 : 1, MAX_QUANTITY)) throw new StoreValidationError('Choose between 1 and 10 packs.');
  if (lineId && !cart.some(line => line.id === lineId)) throw new StoreValidationError('This cart item was removed. Please refresh your cart.');
  if (lineId && quantity === 0) return cart.filter(line => line.id !== lineId);
  if (!product || product.id !== productId || product.packSize === null) throw new StoreValidationError('This product is no longer available.');
  if (lineId && cart.find(line => line.id === lineId)?.amountOfSeeds !== product.packSize) {
    throw new StoreValidationError('This pack size has changed. Remove the saved selection and add it again.');
  }
  const existing = lineId ? cart.find(line => line.id === lineId) : cart.find(line => line.productId === productId && line.amountOfSeeds === product.packSize);
  const nextQuantity = lineId ? quantity : (existing?.quantity ?? 0) + quantity;
  if (nextQuantity > MAX_QUANTITY) throw new StoreValidationError('Your cart already contains this product. Maximum 10 packs per selection.');
  if (!existing && cart.length >= 100) throw new StoreValidationError('Your cart is full. Remove an item before adding another.');
  const next = existing ? cart.map(line => line.id === existing.id ? {...line, quantity: nextQuantity} : line) :
    [...cart, {id: newLineId, productId, amountOfSeeds: product.packSize, quantity: nextQuantity}];
  if (next.filter(line => line.productId === productId).reduce((total, line) => total + line.quantity, 0) > product.stockQuantity) {
    throw new StoreValidationError('That quantity is no longer in stock. Choose fewer packs.');
  }
  return next;
}

export function normalizeProfile(value: unknown, verifiedEmail: string): Profile {
  const data = object(value), phone = object(data.phoneNumber);
  return {firstName: text(data.firstName, 50), lastName: text(data.lastName, 50), email: verifiedEmail,
    city: text(data.city, 80), state: text(data.state, 2), postalCode: text(data.postalCode ?? data.zip, 10),
    address: text(data.address, 150), phoneNumber: text(phone.number, 15)};
}

export function normalizeOrder(id: string, value: unknown, uid: string): StoreOrder | null {
  const data = object(value);
  if (!validId(id) || data.customerId !== uid) return null;
  const legacy = data.createdAt == null && data.status == null;
  const status = text(data.status, 80).toUpperCase(), payment = text(data.paymentStatus, 80).toUpperCase();
  const knownStatuses = ['AWAITING_PAYMENT', 'PROCESSING', 'CONFIRMED', 'PACKING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'EXPIRED', 'REFUNDED'];
  const knownPayments = ['PAID', 'UNPAID', 'PENDING', 'REFUNDED', 'UNVERIFIED'];
  return {id, legacy, createdAt: !legacy && integer(data.createdAt, 1, 8640000000000000) ? data.createdAt : null,
    status: legacy ? 'LEGACY' : knownStatuses.includes(status) ? status : 'UNVERIFIED',
    paymentStatus: legacy ? 'UNVERIFIED' : knownPayments.includes(payment) ? payment : 'UNVERIFIED',
    totalCents: moneyCents(data.totalAmount), trackingNumber: legacy ? '' : text(data.trackingNumber, 150), carrier: legacy ? '' : text(data.carrier, 80),
    items: Array.isArray(data.items) ? data.items.slice(0, 100).flatMap(item => {
      const line = object(item);
      if (!validId(line.productId) || !integer(line.quantity, 1, 1000000)) return [];
      return [{productId: line.productId, title: legacy ? `Product ${line.productId}` : text(line.title, 200) || `Product ${line.productId}`,
        quantity: line.quantity, packSize: integer(line.amountOfSeeds, 1, 10000) ? line.amountOfSeeds : null,
        unitPriceCents: legacy ? null : moneyCents(line.unitPrice)}];
    }) : []};
}

export function normalizeBusiness(value: unknown): PublicBusiness | null {
  const data = object(value);
  if (data.approved !== true) return null;
  const support = text(data.supportEmail, 254);
  return {businessName: text(data.businessName, 150), supportEmail: /^[^\s@<>]+@[^\s@<>]+\.[^\s@<>]+$/.test(support) ? support : '',
    privacyPolicyUrl: safeHttpsUrl(data.privacyPolicyUrl), termsUrl: safeHttpsUrl(data.termsUrl), refundPolicyUrl: safeHttpsUrl(data.refundPolicyUrl)};
}
