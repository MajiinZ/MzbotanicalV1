import type {CartLine, Product} from './types';

export function mockSummary(cart: readonly CartLine[], products: readonly Product[]) {
  if (!cart.length) return null;
  const quantities = new Map<string, number>();
  const items = [];
  let subtotal = 0;
  for (const line of cart) {
    const product = products.find(item => item.id === line.productId);
    if (!product || product.packSize === null || product.packSize !== line.amountOfSeeds ||
        !Number.isInteger(line.quantity) || line.quantity < 1 || line.quantity > 10) return null;
    const quantity = (quantities.get(product.id) ?? 0) + line.quantity;
    if (quantity > Math.min(10, product.stockQuantity)) return null;
    quantities.set(product.id, quantity);
    const total = product.priceCents * line.quantity;
    subtotal += total;
    items.push({title: product.title, packSize: product.packSize, quantity: line.quantity, total});
  }
  return {items, subtotal};
}

export type MockReceipt = NonNullable<ReturnType<typeof mockSummary>> & {id: string};
