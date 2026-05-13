<template>
  <div class="crop-box" ref="cropBox" @mousedown="startDrag" @wheel.prevent="onWheel" @click="onClick">
    <img ref="imgEl" :src="imageUrl" :style="imgStyle" draggable="false" />
    <div class="crop-overlay"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = defineProps<{ imageFile?: File }>()
const emit = defineEmits<{ (e: 'preview', dataUrl: string): void; (e: 'change'): void }>()

const imgEl = ref<HTMLImageElement>()
const cropBox = ref<HTMLDivElement>()
const imageUrl = ref('')
const scale = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)
const dragging = ref(false)
const dragStart = { x: 0, y: 0, ox: 0, oy: 0 }
let wasDragged = false
let previewTimer: any

const natW = ref(0)
const natH = ref(0)

const boxSize = computed(() => cropBox.value?.offsetWidth || 320)
const cover = computed(() => {
  if (!natW.value || !natH.value) return 1
  return Math.max(boxSize.value / natW.value, boxSize.value / natH.value)
})
const dispW = computed(() => natW.value * cover.value * scale.value)
const dispH = computed(() => natH.value * cover.value * scale.value)

const imgStyle = computed(() => ({
  width: `${dispW.value}px`,
  height: `${dispH.value}px`,
  left: `${-(dispW.value - boxSize.value) / 2 + offsetX.value}px`,
  top: `${-(dispH.value - boxSize.value) / 2 + offsetY.value}px`,
  cursor: dragging.value ? 'grabbing' : 'grab',
}))

watch(() => props.imageFile, (file) => {
  if (file) {
    const url = URL.createObjectURL(file)
    const loader = new Image()
    loader.onload = () => {
      if (imageUrl.value) URL.revokeObjectURL(imageUrl.value)
      natW.value = loader.naturalWidth
      natH.value = loader.naturalHeight
      imageUrl.value = url
      scale.value = 1
      offsetX.value = 0
      offsetY.value = 0
    }
    loader.src = url
  }
}, { immediate: true })

function getMaxOffset(): { maxX: number; maxY: number } {
  return {
    maxX: Math.max(0, (dispW.value - boxSize.value) / 2),
    maxY: Math.max(0, (dispH.value - boxSize.value) / 2),
  }
}

function clamp() {
  const { maxX, maxY } = getMaxOffset()
  offsetX.value = Math.max(-maxX, Math.min(maxX, offsetX.value))
  offsetY.value = Math.max(-maxY, Math.min(maxY, offsetY.value))
}

function onWheel(e: WheelEvent) {
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  const ns = Math.max(1, Math.min(3, scale.value + delta))
  const ratio = ns / scale.value
  offsetX.value *= ratio
  offsetY.value *= ratio
  scale.value = ns
  clamp()
  schedulePreview()
}

function onClick() {
  if (!wasDragged) emit('change')
  wasDragged = false
}

function startDrag(e: MouseEvent) {
  dragging.value = true
  wasDragged = false
  dragStart.x = e.clientX
  dragStart.y = e.clientY
  dragStart.ox = offsetX.value
  dragStart.oy = offsetY.value
  const { maxX, maxY } = getMaxOffset()
  const onMove = (ev: MouseEvent) => {
    if (!dragging.value) return
    wasDragged = true
    offsetX.value = dragStart.ox + (ev.clientX - dragStart.x) * (maxX / Math.max(1, maxX))
    offsetY.value = dragStart.oy + (ev.clientY - dragStart.y) * (maxY / Math.max(1, maxY))
    clamp()
    schedulePreview()
  }
  const onUp = () => {
    dragging.value = false
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    schedulePreview()
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

function schedulePreview() {
  clearTimeout(previewTimer)
  previewTimer = setTimeout(() => {
    if (!imageUrl.value || !imgEl.value || !cropBox.value) return
    const dataUrl = cropCanvas()
    if (dataUrl) emit('preview', dataUrl)
  }, 100)
}

function cropCanvas(): string | null {
  if (!imgEl.value || !cropBox.value) return null
  const img = imgEl.value
  const bs = boxSize.value
  const canvas = document.createElement('canvas')
  canvas.width = 300; canvas.height = 300
  const ctx = canvas.getContext('2d')!
  const cv = cover.value
  const dW = natW.value * cv * scale.value
  const dH = natH.value * cv * scale.value
  const cx = (dW - bs) / 2 - offsetX.value
  const cy = (dH - bs) / 2 - offsetY.value
  const sx = (cx / dW) * natW.value; const sy = (cy / dH) * natH.value
  const sw = (bs / dW) * natW.value; const sh = (bs / dH) * natH.value
  ctx.drawImage(img, sx, sy, sw, sh, 0, 0, 300, 300)
  return canvas.toDataURL('image/jpeg', 0.85)
}

</script>

<style scoped>
.crop-box { width: 320px; height: 320px; overflow: hidden; position: relative; border: 1px solid #e0e0e0; user-select: none; }
.crop-box img { position: absolute; object-fit: cover; }
.crop-overlay { position: absolute; inset: 0; box-shadow: 0 0 0 9999px rgba(0,0,0,0.4); pointer-events: none; }
</style>
