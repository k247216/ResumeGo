// @vitest-environment happy-dom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import KnowledgeDeleteDialog from './KnowledgeDeleteDialog.vue'
import type { KnowledgeDeletionImpact } from '../../types/knowledge'

const impact: KnowledgeDeletionImpact = {
  title: '笔记甲', hasSource: true, hasContent: true, hasCategory: true, hasTags: true,
  confirmationToken: 'tok123', expiresAt: '2026-08-22T12:00:00',
}

function mountDlg(props: Record<string, unknown> = {}) {
  return mount(KnowledgeDeleteDialog, {
    props: { impact: null, loading: false, deleting: false, error: '', ...props },
  })
}

describe('KnowledgeDeleteDialog', () => {
  it('shows the real deletion impact summary without paths', () => {
    const wrapper = mountDlg({ impact })
    const text = wrapper.get('[data-test="delete-impact"]').text()
    expect(text).toContain('已提取正文')
    expect(text).toContain('受管原文件')
    expect(text).toContain('分类关联')
    expect(text).toContain('标签关联')
    expect(wrapper.text()).not.toContain('tok123')
  })

  it('requires typed confirmation before emitting the token', async () => {
    const wrapper = mountDlg({ impact })
    const confirm = wrapper.get('[data-test="delete-confirm"]')
    expect((confirm.element as HTMLButtonElement).disabled).toBe(true)

    await wrapper.get('[data-test="delete-confirm-input"]').setValue('删除')
    expect((confirm.element as HTMLButtonElement).disabled).toBe(false)
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('confirm')).toEqual([['tok123']])
  })

  it('does not emit for wrong confirmation text', async () => {
    const wrapper = mountDlg({ impact })
    await wrapper.get('[data-test="delete-confirm-input"]').setValue('删除哦')
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('confirm')).toBeUndefined()
  })

  it('shows loading and error states', () => {
    expect(mountDlg({ loading: true }).find('[data-test="delete-impact-loading"]').exists()).toBe(true)
    expect(mountDlg({ error: '删除失败' }).get('[data-test="delete-dialog-error"]').text()).toContain('删除失败')
  })
})
