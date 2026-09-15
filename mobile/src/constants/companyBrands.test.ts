import { describe, expect, it } from 'vitest'
import { AUTO_ICONS, PENDING_AUTO_ICONS, autoIconFileAvailable, companyMark, normalizeCompanyName } from './companyBrands'

describe('companyBrands 资源一致性', () => {
  it('AUTO_ICONS 的每一条都能找到对应图片', () => {
    const missing = AUTO_ICONS.filter((entry) => !autoIconFileAvailable(entry.file)).map((entry) => entry.file)
    expect(missing, `以下映射没有对应图片，请补图或移入 PENDING_AUTO_ICONS：${missing.join(', ')}`).toEqual([])
  })

  it('PENDING_AUTO_ICONS 的每一条都确实还没有图片', () => {
    const resolved = PENDING_AUTO_ICONS.filter((entry) => autoIconFileAvailable(entry.file)).map((entry) => entry.file)
    expect(resolved, `以下公司已经补上图片，应移入 AUTO_ICONS：${resolved.join(', ')}`).toEqual([])
  })

  it('两个清单不重复登记同一个文件', () => {
    const active = new Set(AUTO_ICONS.map((entry) => entry.file))
    const duplicated = PENDING_AUTO_ICONS.filter((entry) => active.has(entry.file)).map((entry) => entry.file)
    expect(duplicated).toEqual([])
  })
})

describe('companyMark', () => {
  it('单色字形公司返回字形 + 品牌色', () => {
    const mark = companyMark('谷歌')
    expect(mark.glyph).toBeTruthy()
    expect(mark.color).toBe('#4285F4')
    expect(mark.icon).toBeUndefined()
  })

  it('字形公司的别名也能命中', () => {
    expect(companyMark('字节跳动').glyph).toBeTruthy()
    expect(companyMark('抖音').glyph).toBeTruthy()
    expect(companyMark('Google').glyph).toBeTruthy()
  })

  it('只有字标 PNG 的公司返回 icon、不返回字形', () => {
    // 腾讯在 simple-icons 已下架，走的是遗留字标 PNG，没有单色字形
    const mark = companyMark('腾讯')
    expect(mark.icon).toBeTruthy()
    expect(mark.glyph).toBeUndefined()
  })

  it('归一化让带后缀的公司名也能命中', () => {
    expect(companyMark('北京字节跳动科技有限公司').glyph).toBeTruthy()
    expect(companyMark('腾讯科技（深圳）有限公司').icon).toBeTruthy()
    expect(companyMark('北京网易研发中心').icon).toBeTruthy()
    const ali = companyMark('  阿里巴巴集团  ')
    expect(ali.glyph ?? ali.icon).toBeTruthy()
  })

  it('未收录的公司按名字哈希取回退色，同一个名字始终一致', () => {
    const first = companyMark('某不知名创业公司')
    const second = companyMark('某不知名创业公司')
    expect(first.glyph).toBeUndefined()
    expect(first.icon).toBeUndefined()
    expect(first.color).toBe(second.color)
    expect(first.color).toMatch(/^#[0-9A-F]{6}$/i)
  })

  it('空输入不崩溃，返回占位标识', () => {
    for (const input of [undefined, null, '', '   ']) {
      const mark = companyMark(input)
      expect(mark.letter).toBe('？')
      expect(mark.glyph).toBeUndefined()
      expect(mark.icon).toBeUndefined()
    }
  })

  it('前后空格不影响结果，命中的是同一条映射', () => {
    expect(companyMark('  谷歌  ').glyph).toBe(companyMark('谷歌').glyph)
  })

  it('重复调用命中缓存，不会因为大小写或空格产生第二份对象', () => {
    expect(companyMark('美团')).toBe(companyMark('美团'))
  })

  it('深色芯片的品牌拿到白色字形，浅色芯片拿到深色字形', () => {
    // 华为暗红 / adidas 纯黑：曾经因为染反了出现「深底深字形」几乎不可见
    expect(companyMark('华为').lightText).toBe(true)
    expect(companyMark('华为').fg).toBeUndefined()
    expect(companyMark('adidas').lightText).toBe(true)
    expect(companyMark('美团').lightText).toBe(false)
  })

  it('整体填充型 App 图标（美团）必须强制白色前景，不走明暗规则', () => {
    const mark = companyMark('美团')
    expect(mark.fg).toBe('#ffffff')
  })
})

describe('normalizeCompanyName', () => {
  it('去掉括号与常见后缀', () => {
    expect(normalizeCompanyName('腾讯科技（深圳）有限公司')).toBe('腾讯')
    expect(normalizeCompanyName('北京字节跳动科技有限公司')).toBe('字节跳动')
    expect(normalizeCompanyName('浙江大学')).toBe('浙江大学') // 不以「公司」结尾，不误伤
    expect(normalizeCompanyName('科大讯飞股份有限公司')).toBe('科大讯飞')
  })
})
