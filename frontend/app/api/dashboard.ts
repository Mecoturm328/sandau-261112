import type { DashboardData, DashboardQuery, HealthData } from '~/types/dashboard'
import type { HttpClient, QueryParams } from './http'

function toParams(query: DashboardQuery): QueryParams {
  return { start: query.start, end: query.end, store: query.store }
}

/**
 * 看板相关接口，对应后端 HealthController / DashboardController / OrderController。
 */
export function createDashboardApi(http: HttpClient) {
  return {
    /** GET /api/health */
    getHealth(signal?: AbortSignal) {
      return http.get<HealthData>('/health', undefined, signal)
    },

    /** GET /api/dashboard */
    getDashboard(query: DashboardQuery, signal?: AbortSignal) {
      return http.get<DashboardData>('/dashboard', toParams(query), signal)
    },

    /** GET /api/orders/export —— 返回下载地址，由浏览器直接下载 CSV。 */
    getExportUrl(query: DashboardQuery) {
      return http.url('/orders/export', toParams(query))
    },
  }
}

export type DashboardApi = ReturnType<typeof createDashboardApi>
