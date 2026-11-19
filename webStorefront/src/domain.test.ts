import test from 'node:test';
import assert from 'node:assert/strict';
import {changeCart, moneyCents, normalizeBusiness, normalizeOrder, normalizeProduct, normalizeProfile, parseCart, safeHttpsUrl} from './domain';

const fixture = {title: 'A real published pack', price: 29.99, description: 'Description', thumbnail: 'https://example.com/photo.png',
  category: 'Hybrid', amountOfSeeds: '6', stockQuantity: 8, isPublished: true, isNew: true};
const product = normalizeProduct('sku', fixture)!;

test('catalog exposes only valid published products and preserves cents, pack size and stock without invented discounts', () => {
  assert.equal(product.priceCents, 2999); assert.equal(product.packSize, 6); assert.equal(product.stockQuantity, 8);
  assert.equal(product.isDiscounted, false);
  for (const invalid of [{isPublished: false}, {price: 0}, {price: 10.999}, {stockQuantity: -1}, {stockQuantity: 1.5}, {title: ''}, {price: Infinity}, {price: 100000.01}]) {
    assert.equal(normalizeProduct('sku', {...fixture, ...invalid}), null);
  }
  assert.equal(normalizeProduct('sku', {...fixture, amountOfSeeds: 'unknown'})?.packSize, null);
});

test('money conversion uses integer cents and does not silently round invalid historical precision', () => {
  assert.equal(moneyCents(0.1 + 0.2), 30); assert.equal(moneyCents(0), 0); assert.equal(moneyCents(29.99), 2999);
  for (const value of [29.995, -1, NaN, Infinity, '30', null]) assert.equal(moneyCents(value), null);
});

test('unsafe URL schemes and embedded credentials cannot become images or policy links', () => {
  for (const value of ['javascript:alert(1)', 'data:text/html,unsafe', 'http://example.com', 'https://name:secret@example.com', '//example.com', null]) {
    assert.equal(safeHttpsUrl(value), '');
  }
  assert.equal(safeHttpsUrl('https://example.com/photo.png'), 'https://example.com/photo.png');
});

test('saved cart identity, quantity and pack size are validated before any write, avoiding silent record loss', () => {
  const valid = {id: 'line', productId: 'sku', amountOfSeeds: 6, quantity: 2};
  assert.deepEqual(parseCart([valid]), [valid]); assert.deepEqual(parseCart(undefined), []);
  for (const value of [[{...valid, quantity: 0}], [{...valid, quantity: 11}], [{...valid, quantity: 1.5}],
    [{...valid, productId: '../other'}], [{...valid, amountOfSeeds: '6'}], [valid, valid], 'bad']) {
    assert.throws(() => parseCart(value));
  }
});

test('cart edits merge identical packs while retaining IDs and enforce current stock across all saved lines', () => {
  const cart = parseCart([{id: 'existing', productId: 'sku', amountOfSeeds: 6, quantity: 2}]);
  const merged = changeCart(cart, product, null, 'sku', 3, 'unused');
  assert.deepEqual(merged, [{...cart[0], quantity: 5}]);
  assert.equal(cart[0].quantity, 2, 'The saved source snapshot is immutable.');
  assert.throws(() => changeCart(cart, product, null, 'sku', 7, 'new'), /stock/);
  assert.throws(() => changeCart(cart, {...product, stockQuantity: 100}, null, 'sku', 9, 'new'), /Maximum 10/);
  const duplicateProducts = [...cart, {...cart[0], id: 'other', amountOfSeeds: 3, quantity: 5}];
  assert.throws(() => changeCart(duplicateProducts, product, 'existing', 'sku', 4, 'unused'), /stock/);
});

test('unpublished or changed packs cannot be added, but a removed catalog item can still be removed from the cart', () => {
  const cart = parseCart([{id: 'line', productId: 'sku', amountOfSeeds: 6, quantity: 2}]);
  assert.throws(() => changeCart(cart, null, null, 'sku', 1, 'new'), /no longer available/);
  assert.throws(() => changeCart(cart, {...product, packSize: 12}, 'line', 'sku', 1, 'unused'), /pack size has changed/);
  assert.deepEqual(changeCart(cart, null, 'line', 'sku', 0, 'unused'), []);
  assert.throws(() => changeCart(cart, product, 'missing', 'sku', 1, 'unused'), /removed/);
});

test('legacy orders retain only actual data and never acquire invented payment, dates or item prices', () => {
  const order = normalizeOrder('order', {customerId: 'owner', totalAmount: 40, items: [{productId: 'sku', quantity: 2, amountOfSeeds: 6}]}, 'owner')!;
  assert.equal(order.legacy, true); assert.equal(order.status, 'LEGACY'); assert.equal(order.paymentStatus, 'UNVERIFIED');
  assert.equal(order.createdAt, null); assert.equal(order.totalCents, 4000); assert.equal(order.items[0].unitPriceCents, null);
  assert.equal(normalizeOrder('order', {customerId: 'someone-else'}, 'owner'), null);
});

test('current order records use only verified enum values and preserve missing money or dates as unknown', () => {
  const order = normalizeOrder('order', {customerId: 'owner', createdAt: 1000, status: 'SHIPPED', paymentStatus: 'PAID',
    items: [{productId: 'sku', title: 'Stored title', quantity: 1, unitPrice: 29.99}]}, 'owner')!;
  assert.equal(order.legacy, false); assert.equal(order.createdAt, 1000); assert.equal(order.status, 'SHIPPED'); assert.equal(order.totalCents, null);
  assert.equal(order.items[0].unitPriceCents, 2999);
  const unknown = normalizeOrder('order', {customerId: 'owner', status: '<bad>', paymentStatus: 'assume paid'}, 'owner')!;
  assert.equal(unknown.status, 'UNVERIFIED'); assert.equal(unknown.paymentStatus, 'UNVERIFIED');
});

test('business links require an approved public document, and profile email comes from the authenticated identity', () => {
  assert.equal(normalizeBusiness({approved: false, privacyPolicyUrl: 'https://example.com'}), null);
  const business = normalizeBusiness({approved: true, supportEmail: 'support@example.com', privacyPolicyUrl: 'javascript:bad', termsUrl: 'https://example.com/terms'})!;
  assert.equal(business.privacyPolicyUrl, ''); assert.equal(business.termsUrl, 'https://example.com/terms');
  assert.equal(normalizeProfile({email: 'spoof@example.com'}, 'real@example.com').email, 'real@example.com');
});
