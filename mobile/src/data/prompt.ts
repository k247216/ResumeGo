/**
 * 提示词片段点一下就在文本框里另起一行写好抬头，用户接着往下填即可。
 * 心得与版本备注两个书写窗共用同一套拼接规则，避免一处换行一处不换行。
 */
export function appendPromptLine(draft: string, label: string, max: number): string {
  const head = draft.trimEnd()
  const next = `${head ? `${head}\n` : ''}${label}：`
  // 按码点截：textarea 的 maxlength 也按码点计，切在代理对中间会留下半个 emoji。
  return [...next].length > max ? [...next].slice(0, max).join('') : next
}
