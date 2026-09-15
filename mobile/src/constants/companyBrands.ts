// 本地公司标识。
//
// 三档优先级（全部离线、零网络）：
//   1) 单色字形（brands-glyph/*.svg，simple-icons 风格）——衬在品牌色底上，最干净；
//   2) 遗留字标 PNG（brands-auto/*.png，真实 logo 但小尺寸下是整条字标）——放在白盘上；
//   3) 哈希回退色 + 首字母——任何没录入的公司也能得到一个稳定、好看的颜色块。
//
// 匹配前先做公司名归一化：去掉「有限公司 / 集团 / 科技 / （深圳）」等后缀与括号，
// 这样「腾讯科技（深圳）有限公司」也能命中「腾讯」的字形。
import type { CompanyMark } from './companyBrands.types'
import { GLYPH_REGISTRY } from './glyphRegistry'

// 单色字形：构建期打包（src/assets/brands-glyph），?raw 拿到 SVG 文本以便统一染色。
const glyphModules = import.meta.glob<string>('../assets/brands-glyph/*.svg', {
  eager: true,
  query: '?raw',
  import: 'default',
}) as Record<string, string>

// 遗留字标 PNG（真实 logo），构建期打包，运行时零网络。
const pngModules = import.meta.glob<string>('../assets/brands-auto/*.png', {
  eager: true,
  query: '?url',
  import: 'default',
}) as Record<string, string>

function pngAuto(file: string): string | undefined {
  return pngModules[`../assets/brands-auto/${file}.png`]
}

/** favicon 字标：ascii 键 → 公司名关键词。每一条都必须能在 brands-auto/ 里找到同名文件。 */
export const AUTO_ICONS: Array<{ file: string; keys: string[] }> = [
  { file: 'tencent', keys: ['腾讯', '微信', '微众银行', 'QQ', 'qq', 'Tencent'] },
  { file: 'bytedance', keys: ['字节跳动', '抖音', '今日头条', 'TikTok'] },
  { file: 'meituan', keys: ['美团', '大众点评'] },
  { file: 'netease', keys: ['网易', '网易云音乐', 'NetEase'] },
  { file: 'jd', keys: ['京东', '京东物流', 'JD', 'Jd'] },
  { file: 'pinduoduo', keys: ['拼多多', 'PDD', 'Pinduoduo'] },
  { file: 'didi', keys: ['滴滴', '快的', 'DiDi', 'Didi'] },
  { file: 'alibaba', keys: ['阿里', '阿里巴巴', '淘宝', '天猫', 'Alibaba'] },
  { file: 'honor', keys: ['荣耀', 'Honor'] },
  { file: 'dahua', keys: ['大华', 'Dahua'] },
  { file: 'ctrip', keys: ['携程', 'Trip.com', 'Ctrip'] },
  { file: 'vipshop', keys: ['唯品会', 'Vipshop'] },
  { file: 'zhihu', keys: ['知乎', 'Zhihu'] },
  { file: 'huya', keys: ['虎牙', 'Huya'] },
  { file: 'douyu', keys: ['斗鱼', 'Douyu'] },
  { file: 'dewu', keys: ['得物', 'DeWu', 'Dewu'] },
  { file: 'sfexpress', keys: ['顺丰', '顺丰速运', 'SF Express', 'SF'] },
  { file: 'pingan', keys: ['平安', '中国平安', 'Ping An', 'PingAn'] },
  { file: 'cmbchina', keys: ['招商银行', '招行', 'CMB'] },
  { file: 'ccb', keys: ['建设银行', 'CCB'] },
  { file: 'icbc', keys: ['工商银行', '工行', 'ICBC'] },
  { file: 'abc', keys: ['农业银行', '农行', 'ABC'] },
  { file: 'youdao', keys: ['有道', 'Youdao'] },
  { file: 'lilith', keys: ['莉莉丝', 'Lilith'] },
  { file: 'haier', keys: ['海尔', 'Haier'] },
  { file: 'midea', keys: ['美的', 'Midea'] },
  { file: 'yonyou', keys: ['用友', 'Yonyou'] },
  { file: 'chinaunicom', keys: ['中国联通', '联通', 'China Unicom', 'Unicom'] },
  { file: 'chinalife', keys: ['中国人寿', '国寿', 'China Life', 'ChinaLife'] },
  { file: 'xdf', keys: ['新东方', 'XDF'] },
  { file: 'tal', keys: ['好未来', '学而思', 'TAL'] },
  { file: 'geely', keys: ['吉利', 'Geely'] },
  { file: 'saic', keys: ['上汽', '上汽集团', 'SAIC'] },
  { file: 'crrc', keys: ['中车', 'CRRC'] },
  { file: 'sgcc', keys: ['国家电网', '国网', 'State Grid', 'SGCC'] },
  { file: 'sinopec', keys: ['中石化', '中国石化', 'Sinopec'] },
  { file: 'haidilao', keys: ['海底捞', 'Haidilao'] },
  { file: 'luckin', keys: ['瑞幸', '瑞幸咖啡', 'Luckin'] },
  { file: 'anta', keys: ['安踏', 'Anta'] },
  { file: 'lining', keys: ['李宁', 'Li-Ning', 'Lining'] },
  { file: 'myhexin', keys: ['同花顺', 'Hexin', 'MyHexin'] },
  { file: 'eastmoney', keys: ['东方财富', 'East Money', 'Eastmoney'] },
  { file: 'mybank', keys: ['网商银行', 'MyBank'] },
  { file: 'shein', keys: ['SHEIN', '希音', 'Shein'] },
  { file: 'xunlei', keys: ['迅雷', 'Xunlei'] },
  { file: 'sohu', keys: ['搜狐', 'Sohu'] },
  { file: 'sina', keys: ['新浪', 'Sina'] },
]

/**
 * 已登记关键词、但 brands-auto/ 里还没有对应图片的公司（不会生效，仅供测试发现死配置）。
 */
export const PENDING_AUTO_ICONS: Array<{ file: string; keys: string[] }> = [
  { file: 'lenovo', keys: ['联想'] },
  { file: 'zte', keys: ['中兴'] },
  { file: 'cicc', keys: ['中金'] },
  { file: 'trip', keys: ['Trip.com'] },
  { file: 'transsion', keys: ['传音'] },
  { file: 'goertek', keys: ['歌尔'] },
  { file: 'gree', keys: ['格力'] },
  { file: 'wps', keys: ['金山办公', 'WPS'] },
  { file: 'cmcc', keys: ['中国移动'] },
  { file: 'chinatelecom', keys: ['中国电信'] },
  { file: 'citics', keys: ['中信证券'] },
  { file: 'gwm', keys: ['长城汽车'] },
  { file: 'cnpc', keys: ['中石油'] },
  { file: 'cdfg', keys: ['中免'] },
  { file: 'popmart', keys: ['泡泡玛特'] },
  { file: 'bosideng', keys: ['波司登'] },
  { file: 'zhuanzhuan', keys: ['转转'] },
  { file: 'tuhu', keys: ['途虎'] },
  { file: 'cmcm', keys: ['猎豹'] },
  { file: 'dingtalk', keys: ['钉钉'] },
]

/** 供测试核对资源是否真实存在。 */
export function autoIconFileAvailable(file: string): boolean {
  return pngAuto(file) !== undefined
}

const FALLBACK_COLORS = ['#4C6FFF', '#00B42A', '#14C9C9', '#722ED1', '#3491FA', '#F77234', '#F53F3F', '#00B8A9']

/**
 * 公司名归一化：去掉括号与常见后缀，让「腾讯科技（深圳）有限公司」收敛成「腾讯」。
 * 只剥结尾的后缀；「科大讯飞」里的「科技」在开头不会被误删。
 */
const STRIP_SUFFIXES = [
  '股份有限公司', '有限责任公司', '有限公司', '集团有限公司', '集团',
  '科技有限公司', '网络科技有限公司', '信息技术有限公司', '信息科技有限公司',
  '技术有限公司', '软件有限公司', '系统工程有限公司',
  '研发中心', '研究中心', '研究院', '研究所', '实验室',
  '控股', '股份', '科技', '网络', '信息', '技术', '计算机', '电子', '数据', '智能', '公司',
  ' Co., Ltd.', ' Co.,Ltd.', ' Inc.', ' Limited', ' LLC', ' Corp.', ' Corporation',
  ' Group', ' Technologies', ' Technology', ' Holdings', ' PLC', ' plc', ' S.A.', ' GmbH', ' SE',
]

// 公司名里常见的「城市/省份前缀」，匹配后剥掉，让「北京字节跳动」收敛成「字节跳动」。
// 只收录确实常作前缀的省级/直辖市级行政区；「浙江」「江苏」等刻意不收，避免误伤「浙江大学」这类。
const CITY_PREFIXES = [
  '北京', '上海', '深圳', '广州', '天津', '重庆', '杭州', '南京', '成都', '武汉',
  '苏州', '厦门', '长沙', '西安', '青岛', '福州', '济南', '合肥', '郑州', '宁波',
  '东莞', '佛山', '无锡', '昆明', '南昌', '沈阳', '大连', '常州', '珠海', '中山',
]

export function normalizeCompanyName(raw: string): string {
  let s = (raw ?? '').trim()
  if (!s) return ''
  // 先去掉括号及内部内容（「（深圳）」「(Shanghai)」）
  s = s.replace(/[（(][^）)]*[）)]/g, '').trim()
  // 剥掉城市/省份前缀（只剥一次，且不能把名字剥空）
  for (const city of CITY_PREFIXES) {
    if (s.startsWith(city) && s.length > city.length) {
      s = s.slice(city.length).trim()
      break
    }
  }
  let changed = true
  while (changed) {
    changed = false
    for (const suf of STRIP_SUFFIXES) {
      if (s.endsWith(suf)) {
        s = s.slice(0, s.length - suf.length).trim()
        changed = true
        break
      }
    }
  }
  return s
}

/** 把 simple-icons 的 SVG 统一成「可染色」形态：固定 viewBox，fill 交给 currentColor。 */
function glyphSvg(slug: string): string | undefined {
  const raw = glyphModules[`../assets/brands-glyph/${slug}.svg`]
  if (!raw) return undefined
  return raw.replace(
    /<svg[^>]*>/,
    '<svg viewBox="0 0 24 24" fill="currentColor" xmlns="http://www.w3.org/2000/svg">',
  )
}

/**
 * 结果缓存。便签墙、时间轴、计划列表的模板里每张卡片会调用 markOf() 三到四次，
 * 而一次 resolveMark 最坏要遍历近 200 条映射做 includes 比较。公司名是稳定输入，适合缓存。
 */
const markCache = new Map<string, CompanyMark>()

export function companyMark(companyName?: string | null): CompanyMark {
  const name = (companyName ?? '').trim()
  const cached = markCache.get(name)
  if (cached) return cached
  const mark = resolveMark(name)
  if (markCache.size >= 512) markCache.clear()
  markCache.set(name, mark)
  return mark
}

function hits(mark: string, keys: string[]): boolean {
  return keys.some((k) => mark.includes(k))
}

function resolveMark(name: string): CompanyMark {
  const letter = name ? name[0].toUpperCase() : '？'
  if (!name) return { letter, color: '#98A2B3', lightText: true }

  const norm = normalizeCompanyName(name)
  const match = (keys: string[]) => hits(name, keys) || hits(norm, keys)

  // 1) 单色字形：衬在品牌色底上，最干净
  const glyph = GLYPH_REGISTRY.find((entry) => match(entry.keys))
  if (glyph) {
    const svg = glyphSvg(glyph.slug)
    if (svg) return { letter, color: glyph.color, lightText: !isLightColor(glyph.color), glyph: svg, fg: glyph.fg }
  }

  // 2) 遗留字标 PNG：真实 logo，但小尺寸下是整条字标，放在白盘上
  const auto = AUTO_ICONS.find((entry) => match(entry.keys))
  if (auto) {
    const icon = pngAuto(auto.file)
    if (icon) return { letter, color: '#4C6FFF', lightText: true, icon }
  }

  // 3) 哈希回退色 + 首字母：任何公司都能稳定得到一个颜色块
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
