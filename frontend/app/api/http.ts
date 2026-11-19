import type { ApiResult } from '~/types/api'

export type QueryParams = Record<string, string | number | null | undefined>

/** 接口错误：status 为 HTTP 状态码，0 表示网络不可达。 */
export class ApiError extends Error {
  constructor(message: string, public readonly status: number) {
    super(message)
    this.name = 'ApiError'
  }
}

function toSearch(query?: QueryParams): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== null && value !== undefined && value !== '') params.set(key, String(value))
  }
  const search = params.toString()
  return search ? `?${search}` : ''
}

/**
 * 最底层的 HTTP 客户端：拼接 URL、发送请求、解包后端统一返回结构 Result<T>。
 */
export function createHttpClient(baseURL: string) {
  const base = baseURL.replace(/\/$/, '')

  function url(path: string, query?: QueryParams): string {
    return `${base}${path}${toSearch(query)}`
  }

  async function get<T>(path: string, query?: QueryParams, signal?: AbortSignal): Promise<T> {
    let response: Response
    try {
      response = await fetch(url(path, query), {
        headers: { Accept: 'application/json' },
        signal,
      })
    } catch (error) {
      if ((error as Error).name === 'AbortError') throw error
      throw new ApiError('无法连接后端服务，请确认 Spring Boot 已启动。', 0)
    }

    const payload = (await response.json().catch(() => null)) as ApiResult<T> | null
    if (!response.ok || !payload || payload.code !== 200) {
      throw new ApiError(payload?.message || '数据加载失败，请稍后重试。', response.status)
    }
    return payload.data
  }

  return { baseURL: base, url, get }
}

export type HttpClient = ReturnType<typeof createHttpClient>
