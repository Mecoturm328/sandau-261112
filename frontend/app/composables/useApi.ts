import { useRuntimeConfig } from '#imports'
import { createDashboardApi } from '~/api/dashboard'
import { createHttpClient } from '~/api/http'

/**
 * 在组件 setup 中获取 API 实例（读取 runtimeConfig.public.apiBase）。
 */
export function useApi() {
  const config = useRuntimeConfig()
  const http = createHttpClient(String(config.public.apiBase))
  return {
    apiBase: http.baseURL,
    dashboard: createDashboardApi(http),
  }
}
