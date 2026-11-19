const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const numberFormatter = new Intl.NumberFormat('zh-CN')
const decimalFormatter = new Intl.NumberFormat('zh-CN', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
})
const timeFormatter = new Intl.DateTimeFormat('zh-CN', {
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

export function pad(value: number | string): string {
  return String(value).padStart(2, '0')
}

export function formatNumber(value: number): string {
  return numberFormatter.format(Math.round(Number(value) || 0))
}

export function formatDecimal(value: number): string {
  return decimalFormatter.format(Number(value) || 0)
}

export function formatMoney(value: number): string {
  const number = Number(value) || 0
  const digits = Number.isInteger(number) ? 0 : 2
  return `¥${number.toLocaleString('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: 2,
  })}`
}

export function formatCompactMoney(value: number): string {
  const number = Number(value) || 0
  if (number >= 10000) {
    const digits = number >= 100000 ? 0 : 1
    return `¥${(number / 10000).toFixed(digits)}万`
  }
  if (number >= 1000) return `¥${(number / 1000).toFixed(1)}k`
  return `¥${formatNumber(number)}`
}

export function formatAxisMoney(value: number): string {
  const number = Number(value) || 0
  if (number >= 10000) {
    return `¥${(number / 10000).toFixed(number % 10000 === 0 ? 0 : 1)}万`
  }
  if (number >= 1000) {
    return `¥${(number / 1000).toFixed(number % 1000 === 0 ? 0 : 1)}k`
  }
  return `¥${Math.round(number)}`
}

export function parseIsoDate(value: string): Date | null {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(String(value || ''))) return null
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(Date.UTC(year!, month! - 1, day!))
  if (
    date.getUTCFullYear() !== year
    || date.getUTCMonth() !== month! - 1
    || date.getUTCDate() !== day
  ) return null
  return date
}

export function formatIsoDate(date: Date): string {
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}`
}

export function addDays(value: string, days: number): string {
  const date = parseIsoDate(value)
  if (!date) return value
  date.setUTCDate(date.getUTCDate() + days)
  return formatIsoDate(date)
}

export function daysBetween(start: string, end: string): number {
  const startDate = parseIsoDate(start)
  const endDate = parseIsoDate(end)
  if (!startDate || !endDate) return 0
  return Math.round((endDate.getTime() - startDate.getTime()) / 86400000)
}

export function dateParts(value: string) {
  const [year, month, day] = String(value).split('-')
  return { year, month, day }
}

export function formatRange(start: string, end: string): string {
  const first = dateParts(start)
  const last = dateParts(end)
  if (first.year === last.year) {
    return `${first.year}.${first.month}.${first.day} — ${last.month}.${last.day}`
  }
  return `${first.year}.${first.month}.${first.day} — ${last.year}.${last.month}.${last.day}`
}

export function formatMonthDay(value: string): string {
  const parts = dateParts(value)
  return `${parts.month}.${parts.day}`
}

export function formatChineseDate(value: string): string {
  const date = parseIsoDate(value)
  if (!date) return value
  return `${date.getUTCMonth() + 1}月${date.getUTCDate()}日 · ${WEEKDAYS[date.getUTCDay()]}`
}

export function formatOrderStamp(value: string): string {
  if (!value) return ''
  const [datePart, timePart = ''] = String(value).split(' ')
  return `${datePart!.slice(5).replace('-', '.')} ${timePart.slice(0, 5)}`
}

export function formatGeneratedAt(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  return timeFormatter.format(date)
}

export function safeColor(value: string | undefined, fallback = '#8ba58e'): string {
  return /^#[0-9a-f]{6}$/i.test(String(value || '')) ? value! : fallback
}

export function formatChange(value: number | null | undefined, unit = '%'): string {
  if (value === null || value === undefined) return '—'
  const sign = value > 0 ? '+' : ''
  return `${sign}${formatDecimal(value)}${unit}`
}

export function changeClass(value: number | null | undefined): string {
  if (value === null || value === undefined || Math.abs(value) <= 0.05) return 'is-neutral'
  return value > 0 ? 'is-up' : 'is-down'
}

export function changeLabel(value: number | null | undefined, unit = '%'): string {
  if (value === null || value === undefined) return '上期暂无数据'
  if (Math.abs(value) <= 0.05) return '与上期持平'
  const direction = value > 0 ? '增长' : '下降'
  return `较上期${direction} ${formatDecimal(Math.abs(value))}${unit}`
}
