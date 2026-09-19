import { escapeHtml } from './noteHtml'
import type { QuestionCat } from '../types/project'

/**
 * 面经解析：把从贴吧/牛客/脉脉复制来的原始文本，清洗排版成可读的纸面文章。
 * 只做保守的清洗——宁可少删不可误删，用户贴什么内容都是他的数据。
 */

export interface ParsedInterview {
  /** 排版后的正文（自产 HTML，所有文本已经过 escapeHtml，无外部标签混入）。 */
  html: string
  /** 识别出的轮次段数（一面/二面/hr面…），没有分段标记时为 1。 */
  rounds: number
  /** 识别出的问题（以 ？/? 结尾的行，去重保序）。 */
  questions: string[]
  /** 与 questions 平行的题型分类（启发式）：八股 / 场景 / 手撕算法 / 项目拷打。 */
  cats: QuestionCat[]
}

/**
 * 帖子站点的水词：推广前缀类（下载App…/关注公众号…）只看行首就足够判定，
 * 整行短词（回复/顶/收藏）才要求全行精确匹配。
 */
const JUNK_LINE = /^(?:点击|下载|打开)[^\n]{0,20}(?:App|APP|app|客户端|链接)|^(?:关注|扫码)[^\n]{0,16}(?:公众号|二维码)|^本帖最后|^编辑于|^发表于|^\d+楼$|^收藏$|^回复$|^顶$/

/** 轮次标题：一面/二面/hr面/终面/笔试，允许「1.」「【】」「#」等常见装饰。 */
const ROUND_HEAD = /^[\s#*\-·]*(?:【|\[)?\s*((?:[一二三四五六]|1|2|3|4|5|6)\s*面|hr\s*面|HR\s*面|终面|笔试|三面|二面|一面)(?:\s*(?:电话|视频|现场|线上|线下|牛客|视频面))?\s*(?:】|\])?\s*:?\s*$/

/** 中文标点收尾的判定：合并断行时，句尾才算一段结束。 */
const SENTENCE_END = /[。！？!?…：:」』）)」"]$/

/**
 * 题型分类规则（按序命中即止）：手撕最明确（写代码/算法），其次是项目拷打
 * （问「你的项目」），再次是场景题（假设性排查/设计），剩下的默认归八股——
 * 搬运面经里的问题绝大多数是概念/原理/对比类，兜底方向别错。
 */
const CAT_RULES: Array<[QuestionCat, RegExp]> = [
  ['coding', /手撕|手写|写一?(?:个|道|段|下)|实现一?(?:个|道|下)|算法题|反转|链表|二叉树|动态规划|\bDP\b|leetcode|LC\s?\d|LRU|快排|堆排|top\s?k|two\s?sum|滑动窗口|双指针/i],
  ['project', /项目(?:里|中|上|经历)?|简历(?:上|里|中)?(?:的|这个)|实习(?:期间|经历)?|你负责|你做的|你参与|你主导|你的(?:角色|贡献|难点|收获)|难点是什么|亮点|碰到(?:的|过)(?:什么|最大)(?:难|问题)/],
  ['scene', /场景题|线上(?:问题|环境|事故|故障)|生产环境|排查|定位问题|如果|假如|万一|怎么处理|如何解决|怎么办|怎么优化|如何排查|设计方案|设计一?个|高并发|内存溢出|OOM|慢查询|cpu\s?飙高|服务雪崩|缓存(?:击穿|穿透|雪崩)/i],
]

export function classifyQuestion(q: string): QuestionCat {
  for (const [cat, re] of CAT_RULES) if (re.test(q)) return cat
  return 'rote'
}

function isJunk(line: string): boolean {
  return JUNK_LINE.test(line.trim())
}

/**
 * 合并被帖子排版打断的段落：上一行足够长且没以句读收尾、下一行又不是
 * 轮次标题/短行时，视为同一句话的断行，拼回来。
 */
function mergeWrapped(lines: string[]): string[] {
  const out: string[] = []
  for (const line of lines) {
    const prev = out[out.length - 1]
    if (
      prev && prev.length >= 12 && !SENTENCE_END.test(prev) &&
      line.length >= 4 && !ROUND_HEAD.test(line) && !/^[•\-\d]+[.、)）]/.test(line)
    ) {
      out[out.length - 1] = prev + line
    } else {
      out.push(line)
    }
  }
  return out
}

type Sec = { head: string; paras: string[] }

/** 轮次标题里的「流程起点」：再次出现它且当前篇已有内容，就视为新的一篇面经。
 * 只认 一面/1面——笔试/二面可以是单篇内的合法顺序，不能当切分点。 */
const FLOW_START = /^一\s*面$|^1\s*面$/

function buildSections(merged: string[]): Sec[] {
  const sections: Sec[] = []
  for (const line of merged) {
    const m = ROUND_HEAD.exec(line)
    if (m) {
      const head = /^(hr|HR)/.test(m[1]) ? 'HR面' : m[1]
      sections.push({ head, paras: [] })
      continue
    }
    const sec = sections[sections.length - 1]
    if (sec) sec.paras.push(line)
    else sections.push({ head: '', paras: [line] })
  }
  if (sections.length === 1 && sections[0].head === '') sections[0].head = '正文'
  return sections
}

function parseSections(sections: Sec[]): ParsedInterview {
  if (!sections.length) return { html: '', rounds: 0, questions: [], cats: [] }

  // 问题抽取：以 ？/? 结尾的行；列表页和战前速览用。去重按「去问号」的键——
  // 全角半角转写是搬运面经时的常见噪声，不算新问题。
  const questions: string[] = []
  const cats: QuestionCat[] = []
  const seen = new Set<string>()
  for (const sec of sections) {
    for (const line of [...(sec.head ? [sec.head] : []), ...sec.paras]) {
      if (!/[？?]$/.test(line) || line.length < 6) continue
      const key = line.replace(/[?？\s]+$/, '')
      if (seen.has(key)) continue
      seen.add(key)
      questions.push(line)
      cats.push(classifyQuestion(line))
    }
  }

  const secHtml = sections.map((sec) => {
    const head = sec.head ? `<h3>${escapeHtml(sec.head)}</h3>` : ''
    const paras = sec.paras.map((p) => `<p>${escapeHtml(p)}</p>`).join('')
    return `<section class="iv-sec">${head}${paras}</section>`
  }).join('')
  return { html: secHtml, rounds: sections.filter((s) => s.head !== '正文').length || 1, questions, cats }
}

/**
 * 一次粘贴多篇面经的切分：再次出现「一面 / 1面 / 笔试」这类流程起点、
 * 且当前篇已有内容时，就认为是新的一篇——用户整页复制多个公司的面经也能逐篇建档。
 */
export function parseInterviewMany(raw: string): ParsedInterview[] {
  const lines = (raw ?? '')
    .replace(/\r\n?/g, '\n')
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => l.length > 0 && !isJunk(l))
  const merged = mergeWrapped(lines)
  const sections = buildSections(merged)

  const interviews: Sec[][] = []
  let current: Sec[] = []
  let sawRounds = false
  for (const sec of sections) {
    if (sec.head && FLOW_START.test(sec.head) && sawRounds && current.length) {
      interviews.push(current)
      current = []
    }
    if (sec.head) sawRounds = true
    current.push(sec)
  }
  if (current.length) interviews.push(current)
  // 只有一篇（或没有任何轮次标题）时，整体作为一篇返回
  if (interviews.length <= 1) return [parseSections(sections)]
  return interviews.map(parseSections)
}

export function parseInterview(raw: string): ParsedInterview {
  return parseInterviewMany(raw)[0] ?? { html: '', rounds: 0, questions: [], cats: [] }
}
