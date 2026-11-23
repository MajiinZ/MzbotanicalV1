export interface StoreUser {
  uid: string;
  email: string;
  displayName: string;
  photoURL: string | null;
  emailVerified: boolean;
}

export interface Product {
  id: string;
  title: string;
  description: string;
  priceCents: number;
  thumbnail: string;
  category: string;
  packSize: number | null;
  stockQuantity: number;
  isNew: boolean;
  isPopular: boolean;
  isDiscounted: boolean;
  provenance?: {name: string; url: string};
}

export interface CartLine {
  id: string;
  productId: string;
  amountOfSeeds: number | null;
  quantity: number;
}

export interface Profile {
  firstName: string;
  lastName: string;
  email: string;
  city: string;
  state: string;
  postalCode: string;
  address: string;
  phoneNumber: string;
}

export interface OrderItem {
  productId: string;
  title: string;
  quantity: number;
  packSize: number | null;
  unitPriceCents: number | null;
}

export interface StoreOrder {
  id: string;
  createdAt: number | null;
  status: string;
  paymentStatus: string;
  totalCents: number | null;
  items: OrderItem[];
  trackingNumber: string;
  carrier: string;
  legacy: boolean;
}

export interface PublicBusiness {
  businessName: string;
  supportEmail: string;
  privacyPolicyUrl: string;
  termsUrl: string;
  refundPolicyUrl: string;
}

export interface StoreState {
  authReady: boolean;
  user: StoreUser | null;
  products: Product[];
  cart: CartLine[];
  orders: StoreOrder[];
  profile: Profile | null;
  deletionStatus: string | null;
  business: PublicBusiness | null;
  loading: boolean;
  error: string | null;
  testReceipt?: MockReceipt | null;
}

export type ProfileUpdate = Pick<Profile, 'firstName' | 'lastName' | 'city' | 'state' | 'postalCode' | 'address' | 'phoneNumber'>;
import type {MockReceipt} from './mock-checkout';
