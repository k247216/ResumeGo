import { describe, expect, it } from 'vitest'
import { parseInterview, parseInterviewMany } from './interviewParse'

describe('面经解析', () => {
  it('按轮次标题分段，段落转义后进 HTML', () => {
    const raw = ['一面', '自我介绍，然后问了操作系统。', '二面', '聊项目深挖。'].join('\n')
    const r = parseInterview(raw)
    expect(r.rounds).toBe(2)
    expect(r.html).toContain('<h3>一面</h3>')
    expect(r.html).toContain('<h3>二面</h3>')
    expect(r.html).toContain('<p>自我介绍，然后问了操作系统。</p>')
  })

  it('hr 面/HR面/笔试都算轮次标题', () => {
    const raw = ['一面', 'x', 'hr面', 'y', '笔试', 'z'].join('\n')
    expect(parseInterview(raw).rounds).toBe(3)
  })

  it('被帖子断行的长句自动拼回一段', () => {
    const raw = ['一面', '面试官先让我做了自我介绍，然后从项目切入，重点问了消息队列的', '幂等设计怎么保证不丢不重。'].join('\n')
    const r = parseInterview(raw)
    expect(r.html).toContain('幂等设计怎么保证不丢不重。')
    expect(r.html).not.toContain('</p><p>幂等')
  })

  it('问号行进问题清单并去重', () => {
    const raw = ['一面', '讲讲 HashMap 的底层实现？', '线程安全的集合有哪些？', '讲讲 HashMap 的底层实现?'].join('\n')
    const r = parseInterview(raw)
    expect(r.questions).toHaveLength(2)
    expect(r.questions[0]).toContain('？')
  })

  it('问题按启发式分类：手撕/项目/场景/八股，cats 与 questions 平行', () => {
    const raw = [
      '一面',
      '手写一个 LRU 缓存？',
      '你简历上这个项目最难的地方在哪？',
      '如果线上出现 OOM 你怎么排查？',
      '讲讲 HashMap 的底层实现？',
    ].join('\n')
    const r = parseInterview(raw)
    expect(r.cats).toEqual(['coding', 'project', 'scene', 'rote'])
    expect(r.cats).toHaveLength(r.questions.length)
  })

  it('没有手撕/项目/场景特征的问题默认归八股', () => {
    const r = parseInterview('线程安全的集合有哪些？')
    expect(r.cats).toEqual(['rote'])
  })

  it('一次粘贴多篇面经：再次出现「一面/笔试」就切分成两篇，各自独立解析', () => {
    const raw = [
      '一面', '字节问了很多基础。', '讲讲 HashMap 的底层实现？', '二面', '字节二面聊设计。',
      '一面', '腾讯主要手撕。', '手写一个 LRU 缓存？',
    ].join('\n')
    const list = parseInterviewMany(raw)
    expect(list).toHaveLength(2)
    expect(list[0].rounds).toBe(2)
    expect(list[0].questions).toEqual(['讲讲 HashMap 的底层实现？'])
    expect(list[1].rounds).toBe(1)
    expect(list[1].questions).toEqual(['手写一个 LRU 缓存？'])
    expect(list[1].cats).toEqual(['coding'])
  })

  it('单篇面经（含二面三面）不会被误切', () => {
    const raw = ['一面', 'x？', '二面', 'y？', '三面', 'z？'].join('\n')
    expect(parseInterviewMany(raw)).toHaveLength(1)
  })

  it('parseInterview 保留单篇语义（多篇时取第一篇）', () => {
    const raw = ['一面', '介绍一下你的项目经验？', '一面', '讲讲你的实习经历？'].join('\n')
    expect(parseInterview(raw).questions).toEqual(['介绍一下你的项目经验？'])
  })

  it('站点水词行被整行剔除，正文不动', () => {
    const raw = ['一面', '下载App查看更多', '关注公众号领资料', '真问题是这个。', '回复'].join('\n')
    const r = parseInterview(raw)
    expect(r.html).not.toContain('下载App')
    expect(r.html).not.toContain('关注公众号')
    expect(r.html).not.toContain('>回复<')
    expect(r.html).toContain('真问题是这个。')
  })

  it('粘贴的内容先转义：任何 HTML 标签都不会存活', () => {
    const raw = ['一面', '<script>alert(1)</script>', '<img src=x onerror=alert(1)>'].join('\n')
    const r = parseInterview(raw)
    expect(r.html).not.toContain('<script>')
    expect(r.html).not.toContain('<img')
    expect(r.html).toContain('&lt;script&gt;')
  })

  it('没有轮次标记时整体归入「正文」一段', () => {
    const r = parseInterview('直接就是一段面经内容。')
    expect(r.rounds).toBe(1)
    expect(r.html).toContain('<h3>正文</h3>')
  })

  it('空输入返回空结构', () => {
    expect(parseInterview('')).toEqual({ html: '', rounds: 0, questions: [], cats: [] })
  })
})
