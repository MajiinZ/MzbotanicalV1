export const catalogCategories = ['All', 'Indica', 'Sativa', 'Hybrid'] as const;
export type Category = typeof catalogCategories[number];
export type CatalogFilters = {category: Category; search: string};
export type Route = 'home' | 'explore' | 'cart' | 'orders' | 'account' | 'product' | 'merchants' | 'test-checkout';
const validRoutes: Route[] = ['explore', 'cart', 'orders', 'account', 'product', 'merchants', 'test-checkout'];
export const cleanSearch = (value: string) => value.slice(0, 100);
export const catalogCategory = (value: string | null): Category =>
  catalogCategories.find(category => category.toLowerCase() === value?.toLowerCase()) ?? 'All';
export const requiresAge = (route: Route) => route !== 'home' && route !== 'merchants';

export function storefrontHash(route: Route, productId = '', filters?: CatalogFilters): string {
  const path = route === 'home' ? '/' : route === 'product' ? `/product/${encodeURIComponent(productId)}` : `/${route}`;
  const params = new URLSearchParams();
  if (filters && (route === 'explore' || route === 'product')) {
    if (filters.category !== 'All') params.set('category', filters.category.toLowerCase());
    if (filters.search.trim()) params.set('q', cleanSearch(filters.search));
  }
  return `#${path}${params.size ? '?' + params.toString() : ''}`;
}

export function parseStorefrontHash(hash: string): {route: Route; productId: string; filters: CatalogFilters} {
  const separator = hash.indexOf('?');
  const parts = (separator < 0 ? hash : hash.slice(0, separator)).replace(/^#\/?/, '').split('/');
  let route = validRoutes.find(value => value === parts[0]) ?? 'home';
  let productId = '';
  if (route === 'product') {
    try { productId = decodeURIComponent(parts[1] ?? ''); }
    catch { route = 'home'; }
  }
  const params = new URLSearchParams(separator < 0 ? '' : hash.slice(separator + 1));
  const useFilters = route === 'explore' || route === 'product';
  return {route, productId, filters: {category: catalogCategory(useFilters ? params.get('category') : null), search: useFilters ? cleanSearch(params.get('q') ?? '') : ''}};
}
