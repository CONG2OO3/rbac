export const DEFAULT_THEME_COLOR = '#176cff'

export const THEME_COLOR_PRESETS = [
  { name: '经典蓝', color: '#176cff' },
  { name: '紫罗兰', color: '#7546df' },
  { name: '翡翠绿', color: '#078763' },
  { name: '暖橙色', color: '#b95a17' },
  { name: '玫瑰红', color: '#c13d70' },
]

export function normalizeThemeColor(value) {
  return /^#[0-9a-f]{6}$/i.test(value || '') ? value.toLowerCase() : null
}

export function getThemeColor() {
  return normalizeThemeColor(localStorage.getItem('theme_color')) || DEFAULT_THEME_COLOR
}

function contrastColor(color) {
  const components = [1, 3, 5].map((index) => {
    const value = Number.parseInt(color.slice(index, index + 2), 16) / 255
    return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4
  })
  const luminance = components[0] * 0.2126 + components[1] * 0.7152 + components[2] * 0.0722
  return luminance > 0.179 ? '#101827' : '#ffffff'
}

export function applyThemeColor(value) {
  const color = normalizeThemeColor(value) || DEFAULT_THEME_COLOR
  document.documentElement.style.setProperty('--blue', color)
  document.documentElement.style.setProperty('--accent-contrast', contrastColor(color))
  localStorage.setItem('theme_color', color)
  return color
}
