// Shapes of the Kalamet API (see docs/api.md). Money is Rial; times are ISO-8601 UTC strings.

export type Page<T> = { items: T[]; page: number; size: number; totalItems: number; totalPages: number };

export type Problem = {
  status: number;
  code: string;
  detail: string;
  errors?: Record<string, string>;
  retryAfterSeconds?: number;
};

// --- Users ---
export type Role = "CUSTOMER" | "ADMIN";

export type Profile = {
  id: number;
  mobile: string;
  email: string | null;
  firstName: string | null;
  lastName: string | null;
  role: Role;
  profileComplete: boolean;
  createdAt: string;
};

export type OtpResponse = { mobile: string; expiresInSeconds: number; resendInSeconds: number; demoCode: string | null };
export type SignInResult = { newUser: boolean; user: Profile };

export type Province = { id: number; name: string };

export type Address = {
  id: number;
  recipientName: string;
  recipientMobile: string;
  province: Province;
  city: string;
  addressLine: string;
  plaque: string;
  unit: string | null;
  postalCode: string;
  isDefault: boolean;
};

export type AddressInput = {
  recipientName: string;
  recipientMobile: string;
  provinceId: number | null;
  city: string;
  addressLine: string;
  plaque: string;
  unit: string;
  postalCode: string;
  makeDefault: boolean;
};

export type AdminUser = {
  id: number;
  mobile: string;
  email: string | null;
  firstName: string | null;
  lastName: string | null;
  role: Role;
  active: boolean;
  createdAt: string;
};

// --- Catalog ---
export type CategoryNode = { id: number; name: string; slug: string; imageUrl: string | null; children: CategoryNode[] };
export type CategoryRef = { id: number; name: string; slug: string };
export type CategoryDetail = CategoryRef & { imageUrl: string | null; breadcrumbs: CategoryRef[]; children: CategoryRef[] };
export type Brand = { id: number; name: string; nameEn: string | null; slug: string; logoUrl: string | null };

export type ProductSummary = {
  id: number;
  slug: string;
  name: string;
  nameEn: string | null;
  brandName: string | null;
  imageUrl: string | null;
  price: number;
  originalPrice: number | null;
  discountPercent: number;
  offerEndsAt: string | null;
  inStock: boolean;
  rating: number | null;
  reviewCount: number;
};

export type ProductImage = { id: number; url: string; altText: string | null; variantId: number | null };

export type Variant = {
  id: number;
  sku: string;
  attributes: Record<string, string>;
  price: number;
  originalPrice: number | null;
  discountPercent: number;
  offerEndsAt: string | null;
  inStock: boolean;
  remaining: number | null;
};

export type RatingSummary = {
  average: number | null;
  count: number;
  recommendedPercent: number | null;
  distribution: Record<string, number>;
};

export type ProductDetail = {
  id: number;
  slug: string;
  name: string;
  nameEn: string | null;
  description: string | null;
  brand: Brand | null;
  breadcrumbs: CategoryRef[];
  images: ProductImage[];
  options: { key: string; values: string[] }[];
  variants: Variant[];
  specGroups: { name: string; specs: { name: string; value: string }[] }[];
  rating: RatingSummary;
};

export type ProductSort = "newest" | "cheapest" | "most-expensive" | "biggest-discount" | "top-rated" | "bestselling";

// --- Cart ---
export type LineIssue = "UNAVAILABLE" | "OUT_OF_STOCK" | "INSUFFICIENT_STOCK";

export type CartLine = {
  variantId: number;
  sku: string;
  productSlug: string;
  productName: string;
  imageUrl: string | null;
  attributes: Record<string, string>;
  quantity: number;
  unitPrice: number;
  originalPrice: number | null;
  discountPercent: number;
  offerEndsAt: string | null;
  lineTotal: number;
  maxQuantity: number;
  issue: LineIssue | null;
};

export type Cart = {
  lines: CartLine[];
  itemCount: number;
  itemsTotal: number;
  discountTotal: number;
  shippingFee: number;
  freeShippingRemaining: number;
  payable: number;
  hasIssues: boolean;
};

// --- Orders ---
export type OrderStatus = "PENDING_PAYMENT" | "PAID" | "SHIPPED" | "DELIVERED" | "CANCELLED" | "REFUNDED";
export type PaymentGateway = "ZARINPAL" | "MOCK";
export type PaymentStatus = "PENDING" | "SUCCEEDED" | "FAILED" | "REFUNDED";

export type OrderLine = {
  variantId: number;
  productSlug: string;
  productName: string;
  sku: string;
  attributes: Record<string, string>;
  imageUrl: string | null;
  unitPrice: number;
  originalPrice: number | null;
  quantity: number;
  lineTotal: number;
};

export type Payment = {
  id: number;
  gateway: PaymentGateway;
  status: PaymentStatus;
  amount: number;
  refId: string | null;
  cardPan: string | null;
  failureReason: string | null;
  paidAt: string | null;
  createdAt: string;
};

export type Customer = { id: number; mobile: string; name: string };

export type Order = {
  orderNumber: string;
  status: OrderStatus;
  createdAt: string;
  payableUntil: string | null;
  items: OrderLine[];
  itemsTotal: number;
  discountTotal: number;
  shippingFee: number;
  total: number;
  shippingAddress: {
    recipientName: string;
    recipientMobile: string;
    province: string;
    city: string;
    addressLine: string;
    plaque: string;
    unit: string | null;
    postalCode: string;
  };
  payments: Payment[];
  customer: Customer | null;
};

export type OrderSummary = {
  orderNumber: string;
  status: OrderStatus;
  total: number;
  itemCount: number;
  imageUrls: string[];
  createdAt: string;
  customer: Customer | null;
};

export type PayResponse = { orderNumber: string; gateway: PaymentGateway; authority: string; paymentUrl: string };

// --- Reviews ---
export type ReviewStatus = "PENDING" | "APPROVED" | "REJECTED";
export type Review = {
  id: number;
  authorName: string;
  rating: number;
  title: string | null;
  comment: string | null;
  recommended: boolean | null;
  verifiedPurchase: boolean;
  createdAt: string;
};
export type ProductRef = { id: number; name: string; slug: string };
export type MyReview = Omit<Review, "authorName"> & { product: ProductRef; status: ReviewStatus };
export type AdminReview = MyReview & { userId: number; userMobile: string; authorName: string };

// --- Admin catalog ---
export type AdminVariant = {
  id: number;
  sku: string;
  attributes: Record<string, string>;
  price: number;
  compareAtPrice: number | null;
  discountEndsAt: string | null;
  stock: number;
  active: boolean;
  version: number;
};
export type Spec = { groupName: string | null; name: string; value: string };
export type AdminProductSummary = {
  id: number;
  name: string;
  slug: string;
  categoryName: string;
  brandName: string | null;
  active: boolean;
  createdAt: string;
};
export type AdminProduct = {
  id: number;
  category: CategoryRef;
  brand: Brand | null;
  name: string;
  nameEn: string | null;
  slug: string;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  variants: AdminVariant[];
  specs: Spec[];
  images: ProductImage[];
};
export type LowStock = { variantId: number; sku: string; productId: number; productName: string; stock: number };
