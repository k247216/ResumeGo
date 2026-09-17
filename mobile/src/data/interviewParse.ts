import { escapeHtml } from './noteHtml'

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

export function parseInterview(raw: string): ParsedInterview {
  const lines = (raw ?? '')
    .replace(/\r\n?/g, '\n')
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => l.length > 0 && !isJunk(l))
  const merged = mergeWrapped(lines)

  // 按轮次标题切段；没有任何标题就整体作一段「正文」。
  type Sec = { head: string; paras: string[] }
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
  if (!sections.length) return { html: '', rounds: 0, questions: [] }

  // 问题抽取：以 ？/? 结尾的行；列表页和战前速览用。去重按「去问号」的键——
  // 全角半角转写是搬运面经时的常见噪声，不算新问题。
  const questions: string[] = []
  const seen = new Set<string>()
  for (const line of merged) {
    if (/[？?]$/.test(line) && line.length >= 6) {
      const key = line.replace(/[?？\s]+$/, '')
      if (seen.has(key)) continue
      seen.add(key)
      questions.push(line)
    }
  }

  const secHtml = sections.map((sec) => {
    const head = sec.head ? `<h3>${escapeHtml(sec.head)}</h3>` : ''
    const paras = sec.paras.map((p) => `<p>${escapeHtml(p)}</p>`).join('')
    return `<section class="iv-sec">${head}${paras}</section>`
  }).join('')
  return { html: secHtml, rounds: sections.filter((s) => s.head !== '正文').length || 1, questions }
}
