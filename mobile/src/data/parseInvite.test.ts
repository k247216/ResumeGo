import { describe, expect, it } from 'vitest'
import { parseInviteText } from './parseInvite'

// 固定「现在」，解析结果才能断言；只造 Date 不碰定时器。
const NOW = new Date(2026, 8, 15, 20, 0, 0) // 2026-09-15 20:00 周二

function iso(text: string, companies: string[] = []): string | null {
  return parseInviteText(text, NOW, companies).startTime
}

describe('粘贴邀请解析', () => {
  it('典型面试短信：公司、类型、轮次、月日、下午时间全部命中', () => {
    const r = parseInviteText(
      '【腾讯】您好，邀请您参加后端开发工程师岗位的一面。时间：9月18日（周五）下午2:30，请提前15分钟到场。',
      NOW, [],
    )
    expect(r.company).toBe('腾讯')
    expect(r.eventType).toBe('interview')
    expect(r.roundLabel).toBe('第1面')
    expect(r.dateKnown).toBe(true)
    expect(r.timeKnown).toBe(true)
    const d = new Date(r.startTime!)
    expect(d.getFullYear()).toBe(2026)
    expect(d.getMonth()).toBe(8)
    expect(d.getDate()).toBe(18)
    expect(d.getHours()).toBe(14)
    expect(d.getMinutes()).toBe(30)
  })

  it('已有目标名优先于【】括号当公司名（返回完整目标名）', () => {
    const r = parseInviteText('【字节的确认函】面试时间：9月20日 10:00', NOW, ['腾讯 · 后端开发', '字节跳动 · 服务端'])
    expect(r.company).toBe('字节跳动 · 服务端')
  })

  it('笔试通知识别为 exam 类型', () => {
    const r = parseInviteText('华为2026届优招笔试通知：9月25日 19:00 在线进行，请提前调试设备。', NOW, [])
    expect(r.eventType).toBe('exam')
    expect(new Date(r.startTime!).getHours()).toBe(19)
  })

  it('只写日期没写时间：给上午 10 点默认值并如实标记 timeKnown=false', () => {
    const r = parseInviteText('邀请您于10月9日参加面试。', NOW, [])
    expect(r.dateKnown).toBe(true)
    expect(r.timeKnown).toBe(false)
    expect(new Date(r.startTime!).getHours()).toBe(10)
  })

  it('相对日：明天下午三点', () => {
    const d = new Date(iso('明天下午三点来面试')!)
    expect(d.getDate()).toBe(16)
    expect(d.getHours()).toBe(15)
  })

  it('下周X：周二说「下周一」落到 9 月 21 日', () => {
    const d = new Date(iso('下周一上午10点复试')!)
    expect(d.getMonth()).toBe(8)
    expect(d.getDate()).toBe(21)
    expect(d.getHours()).toBe(10)
  })

  it('月日只写数字：跨年滚到明年，别存进过去', () => {
    // 现在 9 月，文案写 3 月 1 日 → 应滚到 2027 年
    const d = new Date(iso('面试时间：3月1日 14:00')!)
    expect(d.getFullYear()).toBe(2027)
    expect(d.getMonth()).toBe(2)
  })

  it('显式年份不回滚', () => {
    const d = new Date(iso('面试时间：2027年3月1日 14:00')!)
    expect(d.getFullYear()).toBe(2027)
  })

  it('没写上下午的小时数按惯例当下午（2:30 → 14:30）', () => {
    const d = new Date(iso('面试时间：9月18日 2:30')!)
    expect(d.getHours()).toBe(14)
  })

  it('两点半这类口语写法也能解析', () => {
    const d = new Date(iso('明天两点半面试')!)
    expect(d.getHours()).toBe(14)
    expect(d.getMinutes()).toBe(30)
  })

  it('什么都认不出时如实返回 null，不编造时间', () => {
    const r = parseInviteText('你好，最近方便聊一聊吗？', NOW, [])
    expect(r.startTime).toBeNull()
    expect(r.dateKnown).toBe(false)
    expect(r.eventType).toBe('interview')
  })

  it('轮次识别：二面与 HR 面', () => {
    expect(parseInviteText('二面邀请：9月18日 10:00', NOW, []).roundLabel).toBe('第2面')
    expect(parseInviteText('HR面时间：9月18日 10:00', NOW, []).roundLabel).toBe('HR面')
  })
})
