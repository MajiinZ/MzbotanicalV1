-- MZ Botanicals storefront schema for Supabase.
-- Mirrors the Firestore collections and firestore.rules: published catalog is public,
-- customers own their profile/cart/orders/deletion request, and one verified owner
-- email administers the catalog. Clients never write orders.

-- ---------------------------------------------------------------- admin identity
create table public.app_admins (
  email text primary key check (email = lower(email))
);
alter table public.app_admins enable row level security;
revoke all on public.app_admins from anon, authenticated;
insert into public.app_admins (email) values ('zakfalkenbach92@gmail.com');

-- True only for a signed-in user whose confirmed account email is listed in app_admins.
-- Profile fields never grant admin rights.
create or replace function public.is_admin()
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from auth.users u join public.app_admins a on a.email = lower(u.email)
    where u.id = (select auth.uid()) and u.email_confirmed_at is not null
  );
$$;
-- anon may call it (it returns false) because public catalog policies reference it.
revoke all on function public.is_admin() from public;
grant execute on function public.is_admin() to anon, authenticated;

-- ---------------------------------------------------------------- products
create table public.products (
  id text primary key check (id ~ '^[A-Za-z0-9_-]{1,128}$'),
  title text not null check (length(title) between 1 and 200),
  description text not null default '' check (length(description) <= 5000),
  price numeric(10,2) not null default 0 check (price >= 0 and price <= 100000),
  thumbnail text not null default '' check (thumbnail = '' or thumbnail like 'https://%'),
  category text not null default '' check (category in ('', 'Indica', 'Sativa', 'Hybrid')),
  strains text[],
  amount_of_seeds integer check (amount_of_seeds is null or amount_of_seeds between 1 and 10000),
  is_popular boolean not null default false,
  is_new boolean not null default false,
  is_discounted boolean not null default false,
  stock_quantity integer not null default 0 check (stock_quantity between 0 and 1000000000),
  is_published boolean not null default false,
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
  updated_at timestamptz not null default now(),
  -- Same publishing rule as Product.inventoryValidationError() in the Kotlin app.
  constraint publishable check (not is_published or (
    description <> '' and price > 0 and thumbnail like 'https://%' and category <> '' and amount_of_seeds > 0))
);
create index products_published_idx on public.products (is_published, created_at desc);
alter table public.products enable row level security;
create policy "Published products are public" on public.products for select to anon, authenticated
  using (is_published or (select public.is_admin()));
create policy "Admin inserts products" on public.products for insert to authenticated with check ((select public.is_admin()));
create policy "Admin updates products" on public.products for update to authenticated using ((select public.is_admin())) with check ((select public.is_admin()));
create policy "Admin deletes products" on public.products for delete to authenticated using ((select public.is_admin()));

-- ---------------------------------------------------------------- account deletion requests
create table public.account_deletion_requests (
  customer_id uuid primary key default auth.uid() references auth.users(id) on delete cascade,
  status text not null default 'REQUESTED' check (status in ('REQUESTED', 'PROCESSING', 'COMPLETED')),
  requested_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);
alter table public.account_deletion_requests enable row level security;
create policy "Read own deletion request" on public.account_deletion_requests for select to authenticated
  using ((select auth.uid()) = customer_id or (select public.is_admin()));
create policy "Create own deletion request" on public.account_deletion_requests for insert to authenticated
  with check ((select auth.uid()) = customer_id and status = 'REQUESTED');
create policy "Admin updates deletion requests" on public.account_deletion_requests for update to authenticated
  using ((select public.is_admin())) with check ((select public.is_admin()));

-- True while the caller's deletion request is being processed or is complete; blocks changes.
create or replace function public.account_locked()
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (select 1 from public.account_deletion_requests
    where customer_id = (select auth.uid()) and status in ('PROCESSING', 'COMPLETED'));
$$;
revoke all on function public.account_locked() from public, anon;
grant execute on function public.account_locked() to authenticated;

-- ---------------------------------------------------------------- customers
create table public.customers (
  id uuid primary key references auth.users(id) on delete cascade,
  first_name text not null default '' check (length(first_name) <= 50),
  last_name text not null default '' check (length(last_name) <= 50),
  email text not null default '',
  address text check (address is null or length(address) <= 150),
  city text check (city is null or length(city) <= 80),
  state text check (state is null or state = '' or state ~ '^[A-Z]{2}$'),
  postal_code text check (postal_code is null or postal_code = '' or postal_code ~ '^\d{5}(-\d{4})?$'),
  phone_dial_code integer check (phone_dial_code is null or phone_dial_code between 1 and 999),
  phone_number text check (phone_number is null or phone_number ~ '^\d{10,15}$'),
  date_of_birth text not null default '',
  gender text not null default '',
  profile_image_url text check (profile_image_url is null or profile_image_url like 'https://%'),
  created_at timestamptz not null default now()
);
alter table public.customers enable row level security;
create policy "Read own customer profile" on public.customers for select to authenticated
  using ((select auth.uid()) = id or (select public.is_admin()));
create policy "Update own customer profile" on public.customers for update to authenticated
  using ((select auth.uid()) = id and not (select public.account_locked()))
  with check ((select auth.uid()) = id);
create policy "Admin deletes customers" on public.customers for delete to authenticated using ((select public.is_admin()));
-- Customers may change only these columns (same list as firestore.rules); id and email are fixed.
revoke insert, update on public.customers from anon, authenticated;
grant update (first_name, last_name, address, city, state, postal_code, phone_dial_code, phone_number,
  date_of_birth, gender, profile_image_url) on public.customers to authenticated;

-- Every new auth user gets a customer row (replaces the client-side ensureCustomer()).
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = '' as $$
declare
  full_name text := trim(coalesce(new.raw_user_meta_data ->> 'full_name', new.raw_user_meta_data ->> 'name', ''));
begin
  insert into public.customers (id, email, first_name, last_name)
  values (new.id, coalesce(new.email, ''),
    left(split_part(full_name, ' ', 1), 50),
    left(trim(substr(full_name, length(split_part(full_name, ' ', 1)) + 1)), 50))
  on conflict (id) do nothing;
  return new;
end;
$$;
revoke all on function public.handle_new_user() from public, anon, authenticated;
create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------- cart
create table public.cart_items (
  id text primary key default replace(gen_random_uuid()::text, '-', '') check (id ~ '^[A-Za-z0-9_-]{1,128}$'),
  customer_id uuid not null default auth.uid() references public.customers(id) on delete cascade,
  product_id text not null references public.products(id) on delete cascade,
  amount_of_seeds integer check (amount_of_seeds is null or amount_of_seeds between 1 and 10000),
  quantity integer not null check (quantity between 1 and 10),
  created_at timestamptz not null default now(),
  unique (customer_id, product_id, amount_of_seeds)
);
create index cart_items_customer_idx on public.cart_items (customer_id);
create index cart_items_product_idx on public.cart_items (product_id);
alter table public.cart_items enable row level security;
create policy "Read own cart" on public.cart_items for select to authenticated
  using ((select auth.uid()) = customer_id or (select public.is_admin()));
create policy "Remove own cart lines" on public.cart_items for delete to authenticated
  using ((select auth.uid()) = customer_id and not (select public.account_locked()));
-- Adds and quantity changes go through change_cart(), which checks stock and pack size.
revoke insert, update on public.cart_items from anon, authenticated;

-- Atomic cart edit with the same rules as domain.ts changeCart():
--   add: p_product_id set, p_line_id null, quantity 1..10 (merged into an existing line)
--   set: p_line_id set, quantity 0..10 (0 removes the line)
create or replace function public.change_cart(p_product_id text, p_line_id text, p_quantity integer)
returns void language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  line public.cart_items;
  product public.products;
  next_quantity integer;
  product_total integer;
begin
  if uid is null then raise exception 'Please sign in to continue.' using errcode = '28000'; end if;
  if public.account_locked() then
    raise exception 'Your account-deletion request is being processed. Changes are unavailable.' using errcode = 'P0001';
  end if;
  if p_line_id is not null then
    select * into line from public.cart_items where id = p_line_id and customer_id = uid for update;
    if not found then raise exception 'This cart item was removed. Please refresh your cart.' using errcode = 'P0001'; end if;
    if p_quantity is null or p_quantity < 0 or p_quantity > 10 then raise exception 'Choose between 1 and 10 packs.' using errcode = 'P0001'; end if;
    if p_quantity = 0 then delete from public.cart_items where id = line.id; return; end if;
    p_product_id := line.product_id;
  elsif p_quantity is null or p_quantity < 1 or p_quantity > 10 then
    raise exception 'Choose between 1 and 10 packs.' using errcode = 'P0001';
  end if;

  select * into product from public.products where id = p_product_id and is_published for share;
  if not found or product.amount_of_seeds is null then raise exception 'This product is no longer available.' using errcode = 'P0001'; end if;

  if p_line_id is not null then
    if line.amount_of_seeds is distinct from product.amount_of_seeds then
      raise exception 'This pack size has changed. Remove the saved selection and add it again.' using errcode = 'P0001';
    end if;
    next_quantity := p_quantity;
  else
    select * into line from public.cart_items
      where customer_id = uid and product_id = p_product_id and amount_of_seeds = product.amount_of_seeds for update;
    next_quantity := coalesce(line.quantity, 0) + p_quantity;
    if next_quantity > 10 then raise exception 'Your cart already contains this product. Maximum 10 packs per selection.' using errcode = 'P0001'; end if;
    if line.id is null and (select count(*) from public.cart_items where customer_id = uid) >= 100 then
      raise exception 'Your cart is full. Remove an item before adding another.' using errcode = 'P0001';
    end if;
  end if;

  select coalesce(sum(quantity), 0) into product_total from public.cart_items
    where customer_id = uid and product_id = p_product_id and id is distinct from line.id;
  if product_total + next_quantity > product.stock_quantity then
    raise exception 'That quantity is no longer in stock. Choose fewer packs.' using errcode = 'P0001';
  end if;

  if line.id is null then
    insert into public.cart_items (customer_id, product_id, amount_of_seeds, quantity)
    values (uid, p_product_id, product.amount_of_seeds, next_quantity);
  else
    update public.cart_items set quantity = next_quantity where id = line.id;
  end if;
end;
$$;
revoke all on function public.change_cart(text, text, integer) from public, anon;
grant execute on function public.change_cart(text, text, integer) to authenticated;

-- ---------------------------------------------------------------- orders (written by trusted backend only)
create table public.orders (
  id text primary key check (id ~ '^[A-Za-z0-9_-]{1,128}$'),
  customer_id uuid not null references auth.users(id) on delete cascade,
  status text not null default 'AWAITING_PAYMENT' check (status in
    ('AWAITING_PAYMENT','PROCESSING','CONFIRMED','PACKING','SHIPPED','DELIVERED','CANCELLED','EXPIRED','REFUNDED')),
  payment_status text not null default 'UNPAID' check (payment_status in ('PAID','UNPAID','PENDING','REFUNDED','UNVERIFIED')),
  total_amount numeric(10,2) check (total_amount is null or total_amount >= 0),
  -- [{productId, title, quantity, amountOfSeeds, unitPrice}]
  items jsonb not null default '[]'::jsonb check (jsonb_typeof(items) = 'array'),
  tracking_number text not null default '',
  carrier text not null default '',
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);
create index orders_customer_idx on public.orders (customer_id, created_at desc);
alter table public.orders enable row level security;
create policy "Read own orders" on public.orders for select to authenticated
  using ((select auth.uid()) = customer_id or (select public.is_admin()));
create policy "Admin updates orders" on public.orders for update to authenticated
  using ((select public.is_admin())) with check ((select public.is_admin()));
revoke insert, delete on public.orders from anon, authenticated;

-- ---------------------------------------------------------------- public business details
create table public.store_config (
  id text primary key default 'public' check (id = 'public'),
  approved boolean not null default false,
  business_name text not null default '' check (length(business_name) <= 150),
  support_email text not null default '' check (length(support_email) <= 254),
  privacy_policy_url text not null default '',
  terms_url text not null default '',
  refund_policy_url text not null default ''
);
alter table public.store_config enable row level security;
create policy "Approved store details are public" on public.store_config for select to anon, authenticated
  using (approved or (select public.is_admin()));
create policy "Admin edits store details" on public.store_config for all to authenticated
  using ((select public.is_admin())) with check ((select public.is_admin()));

-- ---------------------------------------------------------------- storage
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types) values
  ('product-images', 'product-images', true, 5242880, array['image/jpeg','image/png','image/webp']),
  ('profile-images', 'profile-images', true, 5242880, array['image/jpeg','image/png','image/webp']);

create policy "Admin manages product images" on storage.objects for all to authenticated
  using (bucket_id = 'product-images' and (select public.is_admin()))
  with check (bucket_id = 'product-images' and (select public.is_admin()));
-- Profile photos live at profile-images/<user id>/<file>, like Firebase profile_images/<uid>/<id>.
create policy "Customers upload own profile images" on storage.objects for insert to authenticated
  with check (bucket_id = 'profile-images' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "Customers replace own profile images" on storage.objects for update to authenticated
  using (bucket_id = 'profile-images' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "Customers delete own profile images" on storage.objects for delete to authenticated
  using (bucket_id = 'profile-images' and (storage.foldername(name))[1] = (select auth.uid())::text);
