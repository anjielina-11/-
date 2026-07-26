export interface ForecastRecord {
  recordedAt: string
}

const localDateKey = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const selectSevenDayForecast = <T extends ForecastRecord>(records: T[], today = new Date()): T[] => {
  const allowedDates = new Set<string>()
  for (let index = 0; index < 7; index++) {
    const date = new Date(today)
    date.setHours(0, 0, 0, 0)
    date.setDate(date.getDate() + index)
    allowedDates.add(localDateKey(date))
  }

  const latestByDate = new Map<string, T>()
  records.forEach(record => {
    const date = record.recordedAt.slice(0, 10)
    if (!allowedDates.has(date)) return
    const current = latestByDate.get(date)
    if (!current || current.recordedAt.localeCompare(record.recordedAt) <= 0) latestByDate.set(date, record)
  })

  return [...latestByDate.values()]
    .sort((left, right) => left.recordedAt.localeCompare(right.recordedAt))
    .slice(0, 7)
}
