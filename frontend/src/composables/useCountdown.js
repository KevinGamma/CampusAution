import { ref, computed, onMounted, onUnmounted } from 'vue'

/**
 * Reactive HH:MM:SS countdown to a dynamic end time.
 *
 * @param {() => string | Date | null | undefined} getEndTime
 *   Getter called on every tick — pass a lambda so it re-reads reactive state.
 *   e.g.  useCountdown(() => props.auction.endTime)
 *         useCountdown(() => auction.value?.endTime)
 */
export function useCountdown(getEndTime) {
  const remaining = ref(0)
  let timer = null

  const tick = () => {
    const end = getEndTime()
    remaining.value = end ? new Date(end) - Date.now() : 0
  }

  onMounted(() => {
    tick()
    timer = setInterval(tick, 1000)
  })

  onUnmounted(() => clearInterval(timer))

  const isUrgent = computed(() => remaining.value > 0 && remaining.value < 60_000)

  const formatted = computed(() => {
    const ms = remaining.value
    if (ms <= 0) return 'Ended'
    const totalSec = Math.floor(ms / 1000)
    const h = Math.floor(totalSec / 3600)
    const m = Math.floor((totalSec % 3600) / 60)
    const s = totalSec % 60
    return [h, m, s].map(n => String(n).padStart(2, '0')).join(':')
  })

  return { remaining, isUrgent, formatted }
}
