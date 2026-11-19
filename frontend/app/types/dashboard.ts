export interface Store {
  id: number
  name: string
  shortName: string
  address: string
  accent: string
}

export interface PreviousPeriod {
  start: string
  end: string
}

export interface RangeInfo {
  start: string
  end: string
  days: number
  minDate: string
  maxDate: string
  previous: PreviousPeriod
}

export interface KpiChange {
  revenue: number | null
  orders: number | null
  avgTicket: number | null
  memberShare: number | null
}

export interface Kpis {
  revenue: number
  orders: number
  items: number
  avgTicket: number
  memberShare: number
  memberOrders: number
  previous: {
    revenue: number
    orders: number
    avgTicket: number
    memberShare: number
  }
  change: KpiChange
}

export interface TrendPoint {
  date: string
  revenue: number
  orders: number
  memberOrders: number
}

export interface CategorySlice {
  name: string
  revenue: number
  quantity: number
  share: number
}

export interface HourlyPoint {
  hour: number
  label: string
  orders: number
  revenue: number
}

export interface TopProduct {
  name: string
  category: string
  quantity: number
  revenue: number
}

export interface StorePerformance {
  id: number
  name: string
  shortName: string
  address: string
  accent: string
  orders: number
  revenue: number
  avgTicket: number
}

export interface RecentOrder {
  orderNo: string
  orderedAt: string
  store: string
  accent: string
  total: number
  itemCount: number
  payment: string
  isMember: boolean
  items: string
}

export interface DashboardData {
  meta: {
    generatedAt: string
    source: string
    totalOrdersAllTime: number
    totalRevenueAllTime: number
  }
  range: RangeInfo
  storeId: number | null
  stores: Store[]
  kpis: Kpis
  trend: TrendPoint[]
  categories: CategorySlice[]
  hourly: HourlyPoint[]
  topProducts: TopProduct[]
  storePerformance: StorePerformance[]
  recentOrders: RecentOrder[]
}

export interface DashboardFilters {
  start: string
  end: string
  storeId: string
  preset: number | null
}

export interface ToastState {
  visible: boolean
  message: string
  type: 'info' | 'error'
}

/** 看板查询参数，对应后端 DashboardQueryDTO。 */
export interface DashboardQuery {
  start?: string
  end?: string
  store?: string
}

export interface HealthData {
  status: string
  database: string
  orders: number
  dataRange: PreviousPeriod
}
