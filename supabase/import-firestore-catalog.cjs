'use strict';
// Prints SQL that upserts the published Firestore catalog into Supabase `public.products`.
// Uses only the public Firestore REST API (published products are world-readable under
// firestore.rules); no Firebase Admin credentials are needed.
//   node supabase/import-firestore-catalog.cjs > /tmp/catalog.sql
// Then run the SQL in the Supabase SQL editor (or `supabase db execute`).
const config = require('../webStorefront/src/firebase-config.json');
const url = `https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/(default)/documents:runQuery?key=${config.apiKey}`;
const value = field => field == null ? null : 'stringValue' in field ? field.stringValue : 'integerValue' in field ? Number(field.integerValue)
  : 'doubleValue' in field ? field.doubleValue : 'booleanValue' in field ? field.booleanValue
  : 'arrayValue' in field ? (field.arrayValue.values || []).map(value) : null;
const q = v => v == null ? 'null' : typeof v === 'number' || typeof v === 'boolean' ? String(v)
  : Array.isArray(v) ? (v.length ? `array[${v.map(q).join(',')}]::text[]` : 'null') : `'${String(v).replaceAll("'", "''")}'`;
async function main() {
  const response = await fetch(url, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({structuredQuery: {
    from: [{collectionId: 'product'}], where: {fieldFilter: {field: {fieldPath: 'isPublished'}, op: 'EQUAL', value: {booleanValue: true}}}, limit: 1000}})});
  if (!response.ok) throw Error(`Firestore returned ${response.status}`);
  const rows = (await response.json()).filter(row => row.document).map(({document}) => {
    const f = Object.fromEntries(Object.entries(document.fields).map(([key, field]) => [key, value(field)]));
    const seeds = /^\d+$/.test(String(f.amountOfSeeds ?? '')) ? Number(f.amountOfSeeds) : null;
    return `(${[document.name.split('/').pop(), f.title, f.description ?? '', f.price ?? 0, f.thumbnail ?? '', f.category ?? '', f.strains ?? null, seeds,
      f.isPopular === true, f.isNew === true, f.isDiscounted === true, f.stockQuantity ?? 0, f.isPublished === true, f.createdAt ?? null].map(q).join(', ')})`;
  });
  if (!rows.length) { console.log('-- No published products found.'); return; }
  console.log(`insert into public.products (id, title, description, price, thumbnail, category, strains, amount_of_seeds,
  is_popular, is_new, is_discounted, stock_quantity, is_published, created_at) values
${rows.join(',\n')}
on conflict (id) do update set title = excluded.title, description = excluded.description, price = excluded.price,
  thumbnail = excluded.thumbnail, category = excluded.category, strains = excluded.strains, amount_of_seeds = excluded.amount_of_seeds,
  is_popular = excluded.is_popular, is_new = excluded.is_new, is_discounted = excluded.is_discounted,
  stock_quantity = excluded.stock_quantity, is_published = excluded.is_published, created_at = excluded.created_at, updated_at = now();`);
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
