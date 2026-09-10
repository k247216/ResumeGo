export interface TimelineEventLike {
  startTime: string
}

function startOfDay(value: Date): Date {
  const day = new Date(value)
  day.setHours(0, 0, 0, 0)
  return day
}

/** Generate consecutive local calendar days for the schedule timeline. */
export function timelineDates(anchor: Date = new Date(), count = 7): Date[] {
  const first = startOfDay(anchor)
  return Array.from({ length: Math.max(0, count) }, (_, index) => {
    const day = new Date(first)
    day.setDate(first.getDate() + index)
    return day
  })
}

/** Keep schedule grouping in local time, matching what the user sees in the UI. */
export function eventsOnDate<T extends TimelineEventLike>(events: T[], day: Date): T[] {
  const target = startOfDay(day).getTime()
  return events.filter((event) => {
    const value = new Date(event.startTime)
    return !Number.isNaN(value.getTime()) && startOfDay(value).getTime() === target
  })
}
