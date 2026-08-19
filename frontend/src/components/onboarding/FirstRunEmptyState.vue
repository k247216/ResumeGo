<template>
  <section class="first-run-state" aria-labelledby="first-run-title">
    <div class="first-run-state__copy">
      <span class="first-run-state__eyebrow">ResumeGo · 本地优先</span>
      <h1 id="first-run-title">从一个真实目标开始</h1>
      <p>
        ResumeGo 不会预置任何个人简历、岗位或面试记录。
        先创建你的第一份目标岗位与简历，之后的建议和面试都会围绕真实内容展开。
      </p>

      <button
        class="first-run-state__primary"
        data-test="create-first-workspace"
        type="button"
        @click="$emit('create')"
      >
        开始创建
      </button>
    </div>

    <ol class="first-run-state__steps" aria-label="首次使用步骤">
      <li>
        <span>01</span>
        <div>
          <strong>添加目标岗位</strong>
          <small>粘贴一份你真正准备投递的 JD。</small>
        </div>
      </li>
      <li>
        <span>02</span>
        <div>
          <strong>整理简历与证据</strong>
          <small>只使用你确认过的经历、职责和结果。</small>
        </div>
      </li>
      <li>
        <span>03</span>
        <div>
          <strong>开始针对性迭代</strong>
          <small>审查建议、保存版本，再进入面试准备。</small>
        </div>
      </li>
    </ol>
  </section>
</template>

<script setup lang="ts">
defineEmits<{
  create: []
}>()
</script>

<style scoped>
.first-run-state {
  width: min(1080px, 100%);
  min-height: min(680px, calc(100vh - 80px));
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(320px, 0.95fr);
  gap: 72px;
  align-items: center;
  margin: 0 auto;
  padding: 64px 48px;
  box-sizing: border-box;
}

.first-run-state__copy {
  display: grid;
  justify-items: start;
}

.first-run-state__eyebrow {
  color: #047857;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.first-run-state h1 {
  max-width: 620px;
  margin: 20px 0 18px;
  color: #0f172a;
  font-size: clamp(42px, 5vw, 68px);
  line-height: 1.04;
  letter-spacing: -0.05em;
}

.first-run-state p {
  max-width: 620px;
  margin: 0;
  color: #526076;
  font-size: 17px;
  line-height: 1.85;
}

.first-run-state__primary {
  min-width: 156px;
  height: 48px;
  margin-top: 34px;
  border: 0;
  border-radius: 14px;
  background: #10b981;
  color: #ffffff;
  cursor: pointer;
  font-size: 15px;
  font-weight: 800;
  box-shadow: 0 16px 32px rgba(16, 185, 129, 0.24);
}

.first-run-state__primary:hover {
  background: #059669;
  transform: translateY(-1px);
}

.first-run-state__primary:focus-visible {
  outline: 3px solid rgba(16, 185, 129, 0.28);
  outline-offset: 4px;
}

.first-run-state__steps {
  display: grid;
  gap: 14px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.first-run-state__steps li {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  padding: 20px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.06);
}

.first-run-state__steps span {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 14px;
  background: #ecfdf5;
  color: #047857;
  font-size: 13px;
  font-weight: 900;
}

.first-run-state__steps div {
  display: grid;
  gap: 5px;
}

.first-run-state__steps strong {
  color: #172033;
  font-size: 15px;
}

.first-run-state__steps small {
  color: #68758a;
  font-size: 13px;
  line-height: 1.55;
}

@media (max-width: 820px) {
  .first-run-state {
    grid-template-columns: 1fr;
    gap: 40px;
    padding: 48px 24px;
  }
}
</style>
