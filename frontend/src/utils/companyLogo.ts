import type { JobDescription, SourceMeta } from '../types/job'

interface LocalBrandRule {
  key: string
  pattern: RegExp
  label: string
  colors: [string, string]
  motif: 'bolt' | 'ring' | 'grid' | 'arc' | 'dot' | 'shield' | 'stack'
}

export interface CompanyBrand {
  name: string
  initials: string
  logoUrl: string | null
  gradient: string
  source: 'explicit' | 'local-brand' | 'fallback'
}

const localBrandRules: LocalBrandRule[] = [
  { key: 'bytedance', pattern: /字节|bytedance|抖音|douyin|tiktok/i, label: 'byte', colors: ['#111827', '#2563eb'], motif: 'bolt' },
  { key: 'alibaba', pattern: /阿里|alibaba|淘宝|天猫|蚂蚁|ant group|aliyun/i, label: '阿里', colors: ['#ff6a00', '#f97316'], motif: 'arc' },
  { key: 'tencent', pattern: /腾讯|tencent|微信|wechat|qq/i, label: '腾讯', colors: ['#0052d9', '#38bdf8'], motif: 'ring' },
  { key: 'baidu', pattern: /百度|baidu/i, label: '百度', colors: ['#2932e1', '#4f46e5'], motif: 'dot' },
  { key: 'meituan', pattern: /美团|meituan/i, label: '美团', colors: ['#ffd100', '#f59e0b'], motif: 'arc' },
  { key: 'jd', pattern: /京东|jd\.com|jingdong/i, label: 'JD', colors: ['#dc2626', '#ef4444'], motif: 'shield' },
  { key: 'kuaishou', pattern: /快手|kuaishou/i, label: '快手', colors: ['#ff4906', '#fb923c'], motif: 'grid' },
  { key: 'huawei', pattern: /华为|huawei/i, label: '华为', colors: ['#be123c', '#ef4444'], motif: 'ring' },
  { key: 'didi', pattern: /滴滴|didi/i, label: 'DiDi', colors: ['#ff7a00', '#fb923c'], motif: 'arc' },
  { key: 'pdd', pattern: /拼多多|pinduoduo|pdd/i, label: 'PDD', colors: ['#dc2626', '#f43f5e'], motif: 'grid' },
  { key: 'xiaomi', pattern: /小米|xiaomi/i, label: 'MI', colors: ['#ff6900', '#fb923c'], motif: 'stack' },
  { key: 'netease', pattern: /网易|netease|youdao/i, label: '网易', colors: ['#dc2626', '#f87171'], motif: 'ring' },
  { key: 'bilibili', pattern: /哔哩|bilibili|b站/i, label: 'bili', colors: ['#00aeec', '#38bdf8'], motif: 'grid' },
  { key: 'microsoft', pattern: /微软|microsoft/i, label: 'MS', colors: ['#0f766e', '#2563eb'], motif: 'grid' },
  { key: 'google', pattern: /谷歌|google/i, label: 'G', colors: ['#2563eb', '#ef4444'], motif: 'dot' },
  { key: 'amazon', pattern: /亚马逊|amazon/i, label: 'amz', colors: ['#111827', '#f59e0b'], motif: 'arc' },
  { key: 'apple', pattern: /苹果|apple/i, label: 'Apple', colors: ['#111827', '#64748b'], motif: 'ring' },
  { key: 'sohu', pattern: /搜狐|sohu/i, label: 'SOHU', colors: ['#0f172a', '#334155'], motif: 'stack' },
  { key: 'sina', pattern: /新浪|sina|微博|weibo/i, label: '新浪', colors: ['#f59e0b', '#ef4444'], motif: 'dot' },
  { key: 'pingan', pattern: /平安|ping.?an/i, label: '平安', colors: ['#f97316', '#ea580c'], motif: 'shield' },
  { key: 'dahua', pattern: /大华|dahua/i, label: '大华', colors: ['#2563eb', '#60a5fa'], motif: 'shield' },
  { key: 'hikvision', pattern: /海康|hikvision/i, label: '海康', colors: ['#dc2626', '#fb7185'], motif: 'shield' },
  { key: 'megvii', pattern: /旷视|megvii/i, label: '旷视', colors: ['#111827', '#64748b'], motif: 'dot' },
  { key: 'cloudwalk', pattern: /云从|cloudwalk/i, label: '云从', colors: ['#0f766e', '#14b8a6'], motif: 'arc' },
  { key: 'sensetime', pattern: /商汤|sensetime/i, label: '商汤', colors: ['#1e3a8a', '#6366f1'], motif: 'ring' },
  { key: 'ctrip', pattern: /携程|ctrip|trip\.com/i, label: '携程', colors: ['#2563eb', '#38bdf8'], motif: 'arc' },
  { key: 'qunar', pattern: /去哪儿|qunar/i, label: '去哪', colors: ['#06b6d4', '#22d3ee'], motif: 'arc' },
  { key: 'lenovo', pattern: /联想|lenovo/i, label: 'Lenovo', colors: ['#dc2626', '#ef4444'], motif: 'stack' },
  { key: 'oppo', pattern: /oppo/i, label: 'OPPO', colors: ['#16a34a', '#22c55e'], motif: 'ring' },
  { key: 'vivo', pattern: /vivo/i, label: 'vivo', colors: ['#2563eb', '#60a5fa'], motif: 'ring' },
  { key: 'honor', pattern: /荣耀|honor/i, label: 'HONOR', colors: ['#0f172a', '#475569'], motif: 'stack' },
  { key: 'iflytek', pattern: /科大讯飞|讯飞|iflytek/i, label: '讯飞', colors: ['#2563eb', '#3b82f6'], motif: 'dot' },
  { key: 'kingsoft', pattern: /金山|kingsoft/i, label: '金山', colors: ['#0f766e', '#10b981'], motif: 'shield' },
  { key: 'qihu360', pattern: /奇虎|360|qihu/i, label: '360', colors: ['#16a34a', '#84cc16'], motif: 'ring' },
  { key: 'ke', pattern: /贝壳|ke\.com|lianjia|链家/i, label: '贝壳', colors: ['#10b981', '#14b8a6'], motif: 'arc' },
  { key: 'ths', pattern: /同花顺|10jqka/i, label: '同花', colors: ['#dc2626', '#f97316'], motif: 'grid' },
  { key: 'yonyou', pattern: /用友|yonyou/i, label: '用友', colors: ['#2563eb', '#4f46e5'], motif: 'stack' },
  { key: 'sangfor', pattern: /深信服|sangfor/i, label: '深信', colors: ['#dc2626', '#ef4444'], motif: 'shield' },
  { key: 'nsfocus', pattern: /绿盟|nsfocus/i, label: '绿盟', colors: ['#16a34a', '#22c55e'], motif: 'shield' },
  { key: 'cmb', pattern: /招银|招商银行|cmb/i, label: '招银', colors: ['#dc2626', '#ef4444'], motif: 'shield' },
  { key: 'webank', pattern: /微众|webank/i, label: '微众', colors: ['#2563eb', '#60a5fa'], motif: 'dot' },
  { key: 'shopee', pattern: /shopee|虾皮/i, label: 'S', colors: ['#f97316', '#fb923c'], motif: 'arc' },
]

export function resolveCompanyLogoUrl(job: JobDescription): string | null {
  return getCompanyBrand(job).logoUrl
}

export function getCompanyBrand(job?: JobDescription | null): CompanyBrand {
  const name = job?.companyName?.trim() || job?.jobTitle?.trim() || '目标岗位'
  const searchText = buildBrandSearchText(job, name)

  const localBrand = localBrandRules.find((rule) => rule.pattern.test(searchText))
  if (localBrand) {
    return {
      name,
      initials: localBrand.label,
      logoUrl: localBrandSvg(localBrand),
      gradient: gradientFromColors(localBrand.colors),
      source: 'local-brand',
    }
  }

  const explicitLogo = job ? explicitLogoUrl(job.sourceMeta) : ''
  if (explicitLogo) {
    return {
      name,
      initials: companyInitials(name),
      logoUrl: explicitLogo,
      gradient: fallbackGradient(name),
      source: 'explicit',
    }
  }

  return {
    name,
    initials: companyInitials(name),
    logoUrl: null,
    gradient: fallbackGradient(name),
    source: 'fallback',
  }
}

function buildBrandSearchText(job: JobDescription | null | undefined, name: string) {
  const meta = job?.sourceMeta
  return [
    name,
    job?.jobTitle,
    optionalMetaString(meta, 'companyDomain'),
    optionalMetaString(meta, 'companyWebsite'),
    optionalMetaString(meta, 'domain'),
    optionalMetaString(meta, 'website'),
    optionalMetaString(meta, 'sourceUrl'),
    optionalMetaString(meta, 'platform'),
  ].filter(Boolean).join(' ')
}

export function hideBrokenCompanyLogo(event: Event) {
  const image = event.target
  if (image instanceof HTMLImageElement) {
    image.style.display = 'none'
  }
}

function explicitLogoUrl(meta: SourceMeta | null | undefined) {
  const explicitLogo = firstNonEmpty(
    optionalMetaString(meta, 'logoUrl'),
    optionalMetaString(meta, 'companyLogo'),
    optionalMetaString(meta, 'companyLogoUrl'),
    optionalMetaString(meta, 'iconUrl'),
    optionalMetaString(meta, 'logo'),
  )
  if (isSafeLogoUrl(explicitLogo)) return explicitLogo
  return ''
}

function isSafeLogoUrl(value: string) {
  if (!value) return false
  return /^https?:\/\//i.test(value) || /^data:image\//i.test(value)
}

function optionalMetaString(meta: SourceMeta | null | undefined, key: string) {
  if (!meta) return ''
  const value = (meta as Record<string, unknown>)[key]
  return typeof value === 'string' ? value.trim() : ''
}

function firstNonEmpty(...values: Array<string | null | undefined>) {
  return values.find((value) => Boolean(value?.trim()))?.trim() || ''
}

function companyInitials(name: string) {
  const cleaned = name
    .replace(/[（(].*?[）)]/g, '')
    .replace(/有限公司|有限责任公司|股份|集团|科技|信息|技术|网络|软件|公司/g, '')
    .trim()
  if (!cleaned) return 'JD'
  const ascii = cleaned.match(/[A-Za-z0-9]/g)
  if (ascii?.length) return ascii.slice(0, 3).join('').toUpperCase()
  return Array.from(cleaned).slice(0, 2).join('')
}

function fallbackGradient(name: string) {
  const palettes: Array<[string, string]> = [
    ['#0f766e', '#14b8a6'],
    ['#1d4ed8', '#60a5fa'],
    ['#7c3aed', '#c084fc'],
    ['#db2777', '#fb7185'],
    ['#ea580c', '#fbbf24'],
    ['#0f172a', '#475569'],
  ]
  return gradientFromColors(palettes[hashName(name) % palettes.length])
}

function gradientFromColors(colors: [string, string]) {
  return `linear-gradient(135deg, ${colors[0]}, ${colors[1]})`
}

function hashName(name: string) {
  return Array.from(name || 'JD').reduce((sum, char) => sum + char.charCodeAt(0), 0)
}

function localBrandSvg(rule: LocalBrandRule) {
  const { label, colors, motif } = rule
  const fontSize = label.length <= 2 ? 34 : label.length <= 4 ? 24 : 18
  const motifMarkup = brandMotif(motif)
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 96 96">
      <defs>
        <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="${colors[0]}"/>
          <stop offset="100%" stop-color="${colors[1]}"/>
        </linearGradient>
      </defs>
      <rect width="96" height="96" rx="24" fill="url(#g)"/>
      <circle cx="76" cy="18" r="18" fill="rgba(255,255,255,0.16)"/>
      <circle cx="15" cy="82" r="22" fill="rgba(255,255,255,0.10)"/>
      ${motifMarkup}
      <text x="48" y="53" text-anchor="middle" dominant-baseline="middle"
        font-family="Inter, PingFang SC, Microsoft YaHei, Arial, sans-serif"
        font-size="${fontSize}" font-weight="800" fill="#fff">${escapeSvgText(label)}</text>
    </svg>
  `
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

function brandMotif(motif: LocalBrandRule['motif']) {
  if (motif === 'bolt') {
    return '<path d="M58 13 36 48h15L40 83l24-42H49l9-28Z" fill="rgba(255,255,255,0.18)"/>'
  }
  if (motif === 'ring') {
    return '<circle cx="48" cy="48" r="29" fill="none" stroke="rgba(255,255,255,0.22)" stroke-width="8"/><circle cx="48" cy="48" r="15" fill="rgba(255,255,255,0.10)"/>'
  }
  if (motif === 'grid') {
    return '<g fill="rgba(255,255,255,0.18)"><rect x="18" y="18" width="16" height="16" rx="5"/><rect x="62" y="18" width="16" height="16" rx="5"/><rect x="18" y="62" width="16" height="16" rx="5"/><rect x="62" y="62" width="16" height="16" rx="5"/></g>'
  }
  if (motif === 'arc') {
    return '<path d="M18 59c15 17 45 19 62-4" fill="none" stroke="rgba(255,255,255,0.24)" stroke-width="8" stroke-linecap="round"/><path d="M24 37c13-13 35-15 50-2" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="6" stroke-linecap="round"/>'
  }
  if (motif === 'shield') {
    return '<path d="M48 13 73 24v20c0 18-10 31-25 39-15-8-25-21-25-39V24l25-11Z" fill="rgba(255,255,255,0.16)"/>'
  }
  if (motif === 'stack') {
    return '<g fill="none" stroke="rgba(255,255,255,0.22)" stroke-width="7" stroke-linecap="round"><path d="M23 30h50"/><path d="M23 48h50"/><path d="M23 66h50"/></g>'
  }
  return '<g fill="rgba(255,255,255,0.22)"><circle cx="28" cy="48" r="8"/><circle cx="48" cy="48" r="8"/><circle cx="68" cy="48" r="8"/></g>'
}

function escapeSvgText(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}
