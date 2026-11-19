import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter, useRuntimeConfig } from '#imports'
import type {
  DashboardData,
  DashboardFilters,
  RangeInfo,
  Store,
  ToastState,
} from '~/types/dashboard'
import {
  addDays,
  daysBetween,
  formatGeneratedAt,
  formatMoney,
  formatMonthDay,
  formatRange,
  parseIsoDate,
} from '~/utils/format'

const PRESETS = [7, 30, 90] as const

function detectPreset(range: RangeInfo): number | null {
  if (range.end !== range.maxDate) return null
  for (const days of PRESETS) {
    const expectedStart = addDays(range.maxDate, -(days - 1))
    const boundedStart = expectedStart < range.minDate ? range.minDate : expectedStart
    if (range.start === boundedStart) return days
  }
  return null
}

export function useDashboard() {
  const config = useRuntimeConfig()
  const route = useRoute()
  const router = useRouter()
  const apiBase = String(config.public.apiBase).replace(/\/$/, '')

  const data = ref<DashboardData | null>(null)
  const loading = ref(false)
  const errorMessage = ref('')
  const dateInputs = reactive({ start: '', end: '' })
  const filters = reactive<DashboardFilters>({
    start: '',
    end: '',
    storeId: '',
    preset: null,
  })
  const toast = reactive<ToastState>({
    visible: false,
    message: '',
    type: 'info',
  })

  let requestController: AbortController | null = null
  let toastTimer: ReturnType<typeof setTimeout> | null = null
  let retriedDefault = false

  const bounds = computed(() => data.value?.range ?? null)
  const stores = computed<Store[]>(() => data.value?.stores ?? [])
  const selectedStore = computed<Store | null>(() => {
    if (!data.value || !filters.storeId) return null
    return data.value.stores.find((store) => String(store.id) === filters.storeId) ?? null
  })
  const heroRange = computed(() => (
    data.value ? formatRange(data.value.range.start, data.value.range.end) : '—'
  ))
  const heroDays = computed(() => {
    if (!data.value) return '正在读取数据'
    const scope = selectedStore.value?.name ?? `${data.value.stores.length} 家门店`
    return `共 ${data.value.range.days} 天 · ${scope}`
  })
  const updatedLabel = computed(() => {
    if (!data.value) return errorMessage.value ? '数据连接失败' : '正在同步数据…'
    return `数据更新 ${formatGeneratedAt(data.value.meta.generatedAt)} · 截至 ${formatMonthDay(data.value.range.maxDate)}`
  })
  const dailyAverage = computed(() => {
    if (!data.value) return '—'
    return formatMoney(data.value.kpis.revenue / Math.max(1, data.value.range.days))
  })
  const storeScope = computed(() => selectedStore.value?.name ?? '全部门店')
  const isInitialLoading = computed(() => loading.value && !data.value)
  const exportDisabled = computed(() => !data.value || data.value.kpis.orders === 0)

  function showToast(message: string, type: 'info' | 'error' = 'info') {
    if (toastTimer) clearTimeout(toastTimer)
    toast.message = message
    toast.type = type
    toast.visible = true
    toastTimer = setTimeout(() => {
      toast.visible = false
    }, type === 'error' ? 4600 : 3200)
  }

  function dismissToast() {
    toast.visible = false
    if (toastTimer) {
      clearTimeout(toastTimer)
      toastTimer = null
    }
  }

  function hydrateFromRoute() {
    const start = String(route.query.start || '')
    const end = String(route.query.end || '')
    const store = String(route.query.store || '')
    if (start && end && parseIsoDate(start) && parseIsoDate(end) && start <= end) {
      filters.start = start
      filters.end = end
    }
    if (store && /^\d+$/.test(store)) {
      filters.storeId = store
    }
  }

  function buildQuery(): string {
    const params = new URLSearchParams()
    if (filters.start && filters.end) {
      params.set('start', filters.start)
      params.set('end', filters.end)
    }
    if (filters.storeId) params.set('store', filters.storeId)
    const query = params.toString()
    return query ? `?${query}` : ''
  }

  function syncUrl() {
    if (!data.value) return
    const query: Record<string, string> = {
      ...(route.query as Record<string, string>),
      start: data.value.range.start,
      end: data.value.range.end,
    }
    if (filters.storeId) query.store = filters.storeId
    else delete query.store
    router.replace({ query })
  }

  function applyPayload(payload: DashboardData) {
    data.value = payload
    filters.start = payload.range.start
    filters.end = payload.range.end
    filters.storeId = payload.storeId ? String(payload.storeId) : ''
    filters.preset = detectPreset(payload.range)
    dateInputs.start = payload.range.start
    dateInputs.end = payload.range.end
    errorMessage.value = ''
    syncUrl()
  }

  async function loadDashboard() {
    requestController?.abort()
    const controller = new AbortController()
    requestController = controller
    loading.value = true
    errorMessage.value = ''

    try {
      const response = await fetch(`${apiBase}/dashboard${buildQuery()}`, {
        headers: { Accept: 'application/json' },
        signal: controller.signal,
      })
      const payload = await response.json().catch(() => ({}))
      if (!response.ok) {
        const error = new Error(payload.error || '数据加载失败，请稍后重试。') as Error & { status?: number }
        error.status = response.status
        throw error
      }
      if (controller.signal.aborted) return

      applyPayload(payload as DashboardData)
      retriedDefault = false
    } catch (error) {
      if ((error as Error).name === 'AbortError') return

      const typedError = error as Error & { status?: number }
      if (!data.value && !retriedDefault && typedError.status === 400) {
        retriedDefault = true
        filters.start = ''
        filters.end = ''
        filters.storeId = ''
        filters.preset = null
        showToast('筛选条件无效，已载入默认区间。', 'error')
        await loadDashboard()
        return
      }

      errorMessage.value = !typedError.status
        ? '无法连接后端服务，请确认 Spring Boot 已启动。'
        : (typedError.message || '数据加载失败，请稍后重试。')
      showToast(errorMessage.value, 'error')
    } finally {
      if (requestController === controller) {
        requestController = null
        loading.value = false
      }
    }
  }

  async function applyPreset(days: number) {
    if (!data.value) return
    const range = data.value.range
    const rawStart = addDays(range.maxDate, -(days - 1))
    filters.start = rawStart < range.minDate ? range.minDate : rawStart
    filters.end = range.maxDate
    filters.preset = days
    await loadDashboard()
  }

  async function applyCustomRange() {
    if (!data.value) return
    const start = dateInputs.start
    const end = dateInputs.end
    if (!parseIsoDate(start) || !parseIsoDate(end)) {
      showToast('请选择完整的开始与结束日期。', 'error')
      return
    }
    if (start > end) {
      showToast('开始日期不能晚于结束日期。', 'error')
      return
    }
    if (daysBetween(start, end) + 1 > 366) {
      showToast('单次查询最多支持 366 天。', 'error')
      return
    }
    const range = data.value.range
    if (start < range.minDate || end > range.maxDate) {
      showToast(`请选择 ${range.minDate} 至 ${range.maxDate} 之间的日期。`, 'error')
      return
    }
    filters.start = start
    filters.end = end
    filters.preset = null
    await loadDashboard()
  }

  async function changeStore(storeId: string) {
    if (!data.value) return
    filters.storeId = storeId
    await loadDashboard()
  }

  function exportCsv() {
    if (!data.value || exportDisabled.value) return
    const params = new URLSearchParams({
      start: filters.start,
      end: filters.end,
    })
    if (filters.storeId) params.set('store', filters.storeId)

    const link = document.createElement('a')
    link.href = `${apiBase}/orders/export?${params.toString()}`
    link.download = ''
    document.body.appendChild(link)
    link.click()
    link.remove()
    showToast('正在准备 CSV 文件，请留意浏览器下载。')
  }

  function resetDateInputs() {
    dateInputs.start = filters.start
    dateInputs.end = filters.end
  }

  onMounted(() => {
    hydrateFromRoute()
    void loadDashboard()
  })

  onBeforeUnmount(() => {
    requestController?.abort()
    if (toastTimer) clearTimeout(toastTimer)
  })

  return {
    apiBase,
    data,
    loading,
    errorMessage,
    dateInputs,
    filters,
    toast,
    bounds,
    stores,
    selectedStore,
    heroRange,
    heroDays,
    updatedLabel,
    dailyAverage,
    storeScope,
    isInitialLoading,
    exportDisabled,
    showToast,
    dismissToast,
    loadDashboard,
    applyPreset,
    applyCustomRange,
    changeStore,
    exportCsv,
    resetDateInputs,
  }
}
