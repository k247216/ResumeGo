<template>
  <section class="settings-view">
    <p class="settings-eyebrow">本地设置</p>
    <h1>ResumeGo 运行在你的设备上</h1>
    <p>当前阶段使用本地工作区服务。简历、岗位与求职目标不会因为打开设置页面而上传。</p>
    <div class="settings-card">
      <strong>本地数据</strong>
      <span>已启用</span>
      <small>桌面打包阶段将补充数据目录、备份和模型密钥管理。</small>
    </div>
  </section>
</template>

<style scoped>
.settings-view{max-width:760px;padding:56px 48px}.settings-eyebrow{color:#168866;font-size:12px;font-weight:700;letter-spacing:.12em}.settings-view h1{margin:8px 0 12px;font-size:30px}.settings-view>p{color:#687586;line-height:1.7}.settings-card{display:grid;grid-template-columns:1fr auto;gap:6px;margin-top:28px;padding:20px;border:1px solid #dfe5e9;border-radius:14px;background:white}.settings-card span{color:#168866;font-weight:700}.settings-card small{grid-column:1/-1;color:#7b8794}
</style>
