// 本地公司标识映射：真实 SVG 图标在构建期打包（src/assets/brands），运行时不请求任何网络。
// 未收录的公司回退为品牌色字母标识，颜色按公司名哈希保持稳定。
import type { CompanyMark } from './companyBrands.types'

const svgModules = import.meta.glob<string>('../assets/brands/*.svg', {
  eager: true,
  query: '?url',
  import: 'default',
}) as Record<string, string>

// 官网 favicon 官方图标（构建期抓取打包，运行时零网络请求）
const pngModules = import.meta.glob<string>('../assets/brands-auto/*.png', {
  eager: true,
  query: '?url',
  import: 'default',
}) as Record<string, string>

function svg(name: string): string | undefined {
  return svgModules[`../assets/brands/${name}.svg`]
}

function png(key: string): string | undefined {
  return pngModules[`../assets/brands-auto/${key}.png`]
}

// brands-auto 目录内也可能有 SVG（如 simple-icons 补充抓取）
const autoSvgModules = import.meta.glob<string>('../assets/brands-auto/*.svg', {
  eager: true,
  query: '?url',
  import: 'default',
}) as Record<string, string>

function pngAuto(key: string): string | undefined {
  return png(key) ?? autoSvgModules[`../assets/brands-auto/${key}.svg`]
}

/** favicon 图标：ascii 键 → 公司名关键词 */
const AUTO_ICONS: Array<{ file: string; keys: string[] }> = [
  { file: 'tencent', keys: ['腾讯'] },
  { file: 'bytedance', keys: ['字节跳动'] },
  { file: 'meituan', keys: ['美团'] },
  { file: 'netease', keys: ['网易'] },
  { file: 'jd', keys: ['京东'] },
  { file: 'pinduoduo', keys: ['拼多多'] },
  { file: 'didi', keys: ['滴滴'] },
  { file: 'alibaba', keys: ['阿里'] },
  { file: 'honor', keys: ['荣耀'] },
  { file: 'lenovo', keys: ['联想'] },
  { file: 'zte', keys: ['中兴'] },
  { file: 'dahua', keys: ['大华'] },
  { file: 'ctrip', keys: ['携程'] },
  { file: 'vipshop', keys: ['唯品会'] },
  { file: 'zhihu', keys: ['知乎'] },
  { file: 'huya', keys: ['虎牙'] },
  { file: 'douyu', keys: ['斗鱼'] },
  { file: 'dewu', keys: ['得物'] },
  { file: 'sfexpress', keys: ['顺丰'] },
  { file: 'pingan', keys: ['平安'] },
  { file: 'cmbchina', keys: ['招商银行'] },
  { file: 'ccb', keys: ['建设银行'] },
  { file: 'icbc', keys: ['工商银行'] },
  { file: 'abc', keys: ['农业银行'] },
  { file: 'cicc', keys: ['中金'] },
  { file: 'youdao', keys: ['有道'] },
  { file: 'trip', keys: ['Trip.com'] },
  { file: 'lilith', keys: ['莉莉丝'] },
  { file: 'transsion', keys: ['传音'] },
  { file: 'goertek', keys: ['歌尔'] },
  { file: 'haier', keys: ['海尔'] },
  { file: 'midea', keys: ['美的'] },
  { file: 'gree', keys: ['格力'] },
  { file: 'wps', keys: ['金山办公', 'WPS'] },
  { file: 'yonyou', keys: ['用友'] },
  { file: 'cmcc', keys: ['中国移动'] },
  { file: 'chinatelecom', keys: ['中国电信'] },
  { file: 'chinaunicom', keys: ['中国联通'] },
  { file: 'citics', keys: ['中信证券'] },
  { file: 'chinalife', keys: ['中国人寿'] },
  { file: 'xdf', keys: ['新东方'] },
  { file: 'tal', keys: ['好未来'] },
  { file: 'geely', keys: ['吉利'] },
  { file: 'gwm', keys: ['长城汽车'] },
  { file: 'saic', keys: ['上汽'] },
  { file: 'crrc', keys: ['中车'] },
  { file: 'sgcc', keys: ['国家电网'] },
  { file: 'cnpc', keys: ['中石油'] },
  { file: 'sinopec', keys: ['中石化'] },
  { file: 'cdfg', keys: ['中免'] },
  { file: 'haidilao', keys: ['海底捞'] },
  { file: 'luckin', keys: ['瑞幸'] },
  { file: 'popmart', keys: ['泡泡玛特'] },
  { file: 'anta', keys: ['安踏'] },
  { file: 'lining', keys: ['李宁'] },
  { file: 'bosideng', keys: ['波司登'] },
  { file: 'myhexin', keys: ['同花顺'] },
  { file: 'eastmoney', keys: ['东方财富'] },
  { file: 'mybank', keys: ['网商银行'] },
  { file: 'zhuanzhuan', keys: ['转转'] },
  { file: 'tuhu', keys: ['途虎'] },
  { file: 'cmcm', keys: ['猎豹'] },
  { file: 'xunlei', keys: ['迅雷'] },
  { file: 'sohu', keys: ['搜狐'] },
  { file: 'sina', keys: ['新浪'] },
  { file: 'dingtalk', keys: ['钉钉'] },
]

// 公司名关键词 → 打包图标 + 品牌色
interface BrandEntry {
  keys: string[]
  color: string
  icon?: string
}

const BRANDS: BrandEntry[] = [
  { keys: ['腾讯'], color: '#0052D9' },
  { keys: ['字节跳动', '抖音', 'TikTok'], color: '#00C8D2' },
  { keys: ['阿里云'], color: '#FF6A00', icon: svg('alibabacloud') },
  { keys: ['阿里'], color: '#FF6A00' },
  { keys: ['蚂蚁'], color: '#1677FF' },
  { keys: ['美团'], color: '#FFC300' },
  { keys: ['百度'], color: '#2932E1', icon: svg('baidu') },
  { keys: ['网易'], color: '#D43C33' },
  { keys: ['京东'], color: '#E1251B' },
  { keys: ['华为'], color: '#CF0A2C', icon: svg('huawei') },
  { keys: ['小米'], color: '#FF6900', icon: svg('xiaomi') },
  { keys: ['拼多多'], color: '#E22E1F' },
  { keys: ['快手'], color: '#FF4906', icon: svg('kuaishou') },
  { keys: ['哔哩哔哩', 'B站', 'bilibili'], color: '#FB7299', icon: svg('bilibili') },
  { keys: ['小红书'], color: '#FF2442', icon: svg('xiaohongshu') },
  { keys: ['米哈游'], color: '#33CCFF', icon: svg('mihoyo') },
  { keys: ['滴滴'], color: '#FF7E33' },
  { keys: ['大疆', 'DJI'], color: '#141414', icon: svg('dji') },
  { keys: ['OPPO'], color: '#2E7CEF', icon: svg('oppo') },
  { keys: ['vivo'], color: '#415FFF', icon: svg('vivo') },
  { keys: ['英伟达', 'NVIDIA'], color: '#76B900', icon: svg('nvidia') },
  { keys: ['谷歌', 'Google'], color: '#4285F4', icon: svg('google') },
]

const FALLBACK_COLORS = ['#4C6FFF', '#00B42A', '#14C9C9', '#722ED1', '#3491FA', '#F77234', '#F53F3F', '#00B8A9']

export function companyMark(companyName?: string | null): CompanyMark {
  const name = (companyName ?? '').trim()
  const letter = name ? name[0].toUpperCase() : '？'
  if (!name) return { letter, color: '#98A2B3', lightText: true }
  const brand = BRANDS.find((entry) => entry.keys.some((key) => name.includes(key)))
  const auto = !brand?.icon
    ? AUTO_ICONS.find((entry) => entry.keys.some((key) => name.includes(key)))
    : undefined
  const icon = brand?.icon ?? (auto ? pngAuto(auto.file) : undefined)
  const color = brand?.color ?? '#4C6FFF'
  if (brand || icon) {
    return {
      letter,
      color,
      lightText: !isLightColor(color),
      icon,
      iconColor: icon && !brand?.icon ? color : undefined,
    }
  }
  let hash = 0
  for (const ch of name) hash = (hash * 31 + ch.codePointAt(0)!) >>> 0
  const fallback = FALLBACK_COLORS[hash % FALLBACK_COLORS.length]
  return { letter, color: fallback, lightText: true }
}

function isLightColor(hex: string): boolean {
  const value = hex.replace('#', '')
  if (value.length !== 6) return false
  const r = parseInt(value.slice(0, 2), 16)
  const g = parseInt(value.slice(2, 4), 16)
  const b = parseInt(value.slice(4, 6), 16)
  return (r * 299 + g * 587 + b * 114) / 1000 > 160
}
