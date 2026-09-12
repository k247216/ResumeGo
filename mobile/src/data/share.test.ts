import { describe, expect, it } from 'vitest'
import { isShareCancelled, shareErrorMessage, shareFileName, shareTargetName } from './share'

describe('shareFileName', () => {
  it('保留中文名与扩展名，收件人看到的仍是原文件名', () => {
    expect(shareFileName('张正坤-后端.pdf', 'application/pdf')).toBe('张正坤-后端.pdf')
  })

  it('清掉文件系统非法字符但不吞掉扩展名，连续非法字符合并成一个下划线', () => {
    expect(shareFileName('a/b:c*?.pdf', 'application/pdf')).toBe('a_b_c_.pdf')
  })

  it('扩展名缺失时按 mime 补上，否则插件拿不到类型会退化成通配', () => {
    expect(shareFileName('resume', 'application/pdf')).toBe('resume.pdf')
    expect(shareFileName('resume', 'text/markdown')).toBe('resume.md')
    expect(shareFileName('resume', 'image/jpeg')).toBe('resume.jpg')
    expect(shareFileName('面试-42', 'text/calendar;charset=utf-8')).toBe('面试-42.ics')
  })

  it('mime 带参数或不认识时不影响已有扩展名', () => {
    expect(shareFileName('x.md', 'text/markdown; charset=utf-8')).toBe('x.md')
    expect(shareFileName('x', 'application/octet-stream')).toBe('x')
  })
})

describe('分享结果播报', () => {
  it('用户在面板里按返回不算失败', () => {
    expect(isShareCancelled(new Error('Share canceled'))).toBe(true)
    expect(isShareCancelled(new Error('FILE_NOTCREATED'))).toBe(false)
  })

  it('只把已知收件端翻译成人话，未知包名不硬编', () => {
    expect(shareTargetName('com.tencent.mm')).toBe('微信')
    expect(shareTargetName('com.tencent.mobileqq')).toBe('QQ')
    expect(shareTargetName('com.some.other')).toBeNull()
    expect(shareTargetName('')).toBeNull()
  })

  it('写盘失败要说成用户能行动的原因', () => {
    expect(shareErrorMessage(new Error('FILE_NOTCREATED'))).toBe('本机空间不足或写入失败，未能生成分享文件')
    expect(shareErrorMessage(new Error('bad uri'))).toBe('分享未完成：bad uri')
  })
})
