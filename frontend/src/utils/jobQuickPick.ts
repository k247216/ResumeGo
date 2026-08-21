/**
 * 目标岗位录入的快捷选择数据：纯本地静态列表，点击即填入输入框。
 * 公司名单与 utils/companyLogo.ts 的品牌规则对齐，保证填入后能解析出品牌图标。
 */

export const POPULAR_COMPANIES = [
  '腾讯',
  '字节跳动',
  '阿里巴巴',
  '美团',
  '京东',
  '百度',
  '华为',
  '拼多多',
  '小米',
  '网易',
  '哔哩哔哩',
  '快手',
  '滴滴',
  '携程',
  'Shopee',
] as const

export const POPULAR_JOB_TITLES = [
  'Java 后端开发',
  '前端开发',
  '算法工程师',
  '测试开发',
  '产品经理',
  '数据分析师',
  '客户端开发',
  '后端开发',
] as const

/** 每个热门公司的别名，用于输入英文/品牌词时也能匹配到中文名。 */
const COMPANY_KEYWORDS: Record<string, string> = {
  腾讯: 'tencent wechat 微信 qq',
  字节跳动: '字节 bytedance 抖音 douyin tiktok',
  阿里巴巴: '阿里 alibaba 淘宝 天猫 ant 支付宝',
  美团: 'meituan',
  京东: 'jd jingdong',
  百度: 'baidu',
  华为: 'huawei',
  拼多多: 'pinduoduo pdd',
  小米: 'xiaomi',
  网易: 'netease youdao',
  哔哩哔哩: 'bilibili b站',
  快手: 'kuaishou',
  滴滴: 'didi',
  携程: 'ctrip trip.com',
  Shopee: '虾皮',
}

/** 按关键词过滤热门公司；空关键词返回完整列表。 */
export function filterCompanies(keyword: string): string[] {
  const key = keyword.trim().toLowerCase()
  if (!key) return [...POPULAR_COMPANIES]
  return POPULAR_COMPANIES.filter((name) => {
    const haystack = `${name} ${COMPANY_KEYWORDS[name] ?? ''}`.toLowerCase()
    return haystack.includes(key)
  })
}

/** 按关键词过滤热门岗位；空关键词返回完整列表。 */
export function filterJobTitles(keyword: string): string[] {
  const key = keyword.trim().toLowerCase()
  if (!key) return [...POPULAR_JOB_TITLES]
  return POPULAR_JOB_TITLES.filter((title) => title.toLowerCase().includes(key))
}
