/** 后端统一返回结构，对应 com.mori.dashboard.common.Result。 */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}
