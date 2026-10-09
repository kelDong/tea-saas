/**
 * C 端接口类型（对齐后端 VO 契约）
 *
 * 口径：雪花 ID（19 位）后端超 JS 安全整数序列化为字符串（JacksonConfig.SafeLongSerializer），
 * 故 id 型字段一律 string；金额/数量等小整数 number。
 */

export interface WxLoginVO {
  memberId: string;
  token: string;
  isNew: boolean;
  nickname: string;
  avatar: string;
}

export interface MemberVO {
  id: string;
  nickname: string;
  avatar: string;
  phone: string | null;
  levelName: string | null;
  totalPoints: number;
  availablePoints: number;
  walletBalance: number;
  createTime: string;
}

export interface WalletVO {
  balance: number;
  totalRecharge: number;
  totalConsume: number;
}

export interface StoreNearbyVO {
  id: string;
  storeName: string;
  address: string;
  longitude: number;
  latitude: number;
  openTime: string;
  closeTime: string;
  distanceMeters: number;
}

export interface SpecOption {
  id: string;
  optionName: string;
  extraPrice: number;
}

export interface SpecNode {
  id: string;
  specName: string;
  isRequired: number;
  options: SpecOption[];
}

export interface AppCategoryVO {
  id: string;
  name: string;
}

export interface AppProductVO {
  productId: string;
  listingId: string;
  productName: string;
  categoryId: string;
  categoryName: string;
  salePrice: number;
  image: string | null;
  description: string | null;
  specs: SpecNode[] | null;
}

export interface OrderItemDTO {
  listingId: string;
  qty: number;
  specOptionIds: string[];
}

export interface AppOrderPreviewDTO {
  storeId: string;
  items: OrderItemDTO[];
  couponId?: string;
  pointsUsed?: number;
}

export interface AppOrderCreateDTO extends AppOrderPreviewDTO {
  payChannel: number; // 1微信 / 2储值
}

export interface OrderPreviewVO {
  totalAmount: number;
  discountAmount: number;
  couponAmount: number;
  pointsUsed: number;
  pointsAmount: number;
  payAmount: number;
  items: { productName: string; specDesc: string; price: number; qty: number; amount: number }[];
}

export interface OrderCreateVO {
  id: string;
  orderNo: string;
  takeCode: string;
  payAmount: number;
}

/** 订单列表裸实体（GET /api/app/v1/orders，无商品摘要——契约限制） */
export interface OrdOrderLite {
  id: string;
  orderNo: string;
  storeId: string;
  status: number;
  totalAmount: number;
  discountAmount: number;
  payAmount: number;
  payChannel: number | null;
  takeCode: string | null;
  createTime: string;
  payTime: string | null;
  cancelReason: string | null;
}

export interface OrderDetailVO {
  id: string;
  orderNo: string;
  storeId: string;
  orderSource: number;
  status: number;
  totalAmount: number;
  discountAmount: number;
  payAmount: number;
  payChannel: number | null;
  takeCode: string | null;
  payTime: string | null;
  finishTime: string | null;
  cancelReason: string | null;
  remark: string | null;
  createTime: string;
  items: { productId: string; listingId: string; productName: string; specDesc: string; price: number; qty: number; amount?: number }[];
}

export interface ReorderItem {
  productId: string;
  listingId: string;
  productName: string;
  specDesc: string | null;
  qty: number;
  salePrice?: number;
  skipReason?: string;
}

export interface ReorderVO {
  storeId: string;
  items: ReorderItem[];
  skipped: ReorderItem[];
}

export interface PaymentVO {
  paymentNo: string;
  orderId: string;
  channel: number;
  amount: number;
  status: number;
  payParams?: Record<string, string>;
  codeUrl?: string;
}

export interface CouponVO {
  id: string;
  templateId: string;
  name: string;
  type: number; // 1满减 / 2折扣
  threshold: number;
  amount: number;
  discount: number | null;
  status: number; // 1未用 / 2已用 / 3过期
  scopeType: number; // 1全场 / 2指定商品
  validStart: string;
  validEnd: string;
}

export interface PointsLog {
  id: string;
  changePoints: number;
  beforePoints: number;
  afterPoints: number;
  bizType: number; // 1得/2抵扣/3回补/4过期
  bizNo: string;
  createTime: string;
}

export interface RechargeRecord {
  id: string;
  amount: number;
  bonus: number;
  balanceAfter: number;
  bizNo: string;
  createTime: string;
}

/** 统一响应（工程规范 4.5） */
export interface Result<T> {
  code: number;
  message: string;
  data: T;
  traceId?: string;
}

/** 统一分页（字段是 list 不是 records） */
export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}
