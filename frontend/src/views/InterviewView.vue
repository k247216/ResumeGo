<template>
  <div class="interview-page">
    <div v-if="fromWorkspace" class="workspace-return-bar">
      <button type="button" :disabled="!canReturnToWorkspace" @click="returnToEditor">← {{ workspaceReturnLabel }}</button>
      <span>{{ canReturnToWorkspace ? '模拟面试将作为当前简历的改进输入' : '请先完成本次多轮面试，再回到简历工作台' }}</span>
    </div>

    <!-- ========== 面试大厅（未选中活跃会话时显示） ========== -->
    <template v-if="!activeSessionId">
      <section class="interview-lobby-hero">
        <div class="lobby-hero-copy">
          <p class="section-kicker">{{ fromTarget ? 'Target Practice' : 'Interview Practice' }}</p>
          <h1>{{ fromTarget ? '用当前目标验证这版简历' : '把这版简历放进一次完整多轮面试里验证' }}</h1>
          <p>
            {{ fromTarget
              ? '岗位和简历版本已经由求职目标锁定。你只需选择本轮面试官与考察重点，面试记录会留在这组真实材料下。'
              : '绑定简历版本与目标岗位，选择多位面试官依次追问；过程、评分与复盘都会沉淀为下一版简历的优化依据。' }}
          </p>
          <div class="lobby-hero-actions">
            <button class="interview-start-button" type="button" @click="focusCreatePanel">
              新建模拟面试
              <el-icon><ArrowRight /></el-icon>
            </button>
            <button
              class="lobby-ghost-button"
              type="button"
              :disabled="growthLoading"
              @click="loadGrowthData"
            >
              <el-icon v-if="growthLoading" class="is-loading"><Loading /></el-icon>
              <el-icon v-else><Trophy /></el-icon>
              查看成长趋势
            </button>
          </div>
        </div>
        <div class="lobby-hero-panel">
          <div class="lobby-orbit">
            <span>简历</span>
            <span>岗位</span>
            <span>面试</span>
            <div class="zhida-brand-mark" aria-label="职达">
              <img :src="zhidaInterviewBrand" alt="职达 AI 简历求职助手" />
            </div>
          </div>
          <div class="lobby-stat-grid">
            <div>
              <span>面试档案</span>
              <strong>{{ visibleInterviewRecords.length }}</strong>
            </div>
            <div>
              <span>完成复盘</span>
              <strong>{{ completedSessionCount }}</strong>
            </div>
            <div>
              <span>待继续</span>
              <strong>{{ inProgressSessionCount }}</strong>
            </div>
          </div>
        </div>
      </section>

      <el-alert
        v-if="errorMessage"
        class="interview-preview-note"
        :title="errorMessage"
        type="error"
        show-icon
        closable
        @close="errorMessage = ''"
      />

      <div v-if="actionLoading" class="interview-loading-bar">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>{{ actionStage }}</span>
        <span class="elapsed-time">{{ formatElapsedTime }}</span>
      </div>

      <div class="lobby-shell">
        <section ref="createPanelRef" class="interview-context-card lobby-create-card">
          <div class="context-heading">
            <span class="context-step">01</span>
            <div>
              <h2>新建一次面试演练</h2>
              <p>把简历版本、目标岗位和面试官队列固定下来，形成一组可连续复盘的面试计划。</p>
            </div>
          </div>

          <div class="lobby-selected-strip">
            <div>
              <span>简历版本</span>
              <strong>{{ selectedResumeLabel }}</strong>
            </div>
            <div>
              <span>目标岗位</span>
              <strong>{{ selectedJobLabel }}</strong>
            </div>
            <div>
              <span>面试官</span>
              <strong>{{ selectedPersonaSummary }}</strong>
            </div>
          </div>

          <div v-if="workspaceContextLocked" class="bound-context-card">
            <div class="bound-context-main">
              <CompanyAvatar v-if="selectedJobEntity" :job="selectedJobEntity" size="lg" />
              <div v-else class="bound-context-fallback">职</div>
              <div>
                <span class="bound-context-label">{{ fromTarget ? '已绑定当前求职目标' : '已绑定当前工作台' }}</span>
                <strong>{{ selectedJobLabel }}</strong>
                <p>{{ selectedResumeLabel }}</p>
              </div>
            </div>
            <button type="button" class="bound-context-action" @click="returnToEditor">
              返回工作台更换
            </button>
          </div>

          <div v-else class="context-selectors">
            <label class="context-field">
              <span>简历版本</span>
              <el-select
                v-model="selectedVersionId"
                placeholder="选择简历版本"
                :loading="loadingOptions"
              >
                <el-option v-for="item in resumeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </label>
            <label class="context-field">
              <span>目标岗位</span>
              <el-select
                v-model="selectedJobId"
                placeholder="选择目标岗位"
                :loading="loadingOptions"
                filterable
              >
                <el-option v-for="item in jobOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </label>
          </div>

          <label class="context-field question-count-slider">
            <span>题目数量</span>
            <div class="question-slider-shell">
              <span class="slider-limit">3</span>
              <el-slider v-model="questionCount" :min="3" :max="10" :step="1" show-stops />
              <span class="slider-limit">10</span>
              <strong class="slider-value">{{ questionCount }} 道</strong>
            </div>
          </label>

          <!-- 面试官角色卡选择 -->
          <div class="persona-section">
            <div class="persona-section-head">
              <div>
                <h3>选择本轮面试官</h3>
                <p>默认展示常用面试官；展开后可查看更多角色并调整本次多轮顺序。</p>
              </div>
              <div class="section-head-actions">
                <button class="soft-toggle-btn" type="button" @click="personaPanelExpanded = !personaPanelExpanded">
                  {{ personaPanelExpanded ? '收起' : '展开全部' }}
                </button>
                <button class="add-persona-btn compact" type="button" @click="showPersonaDialog = true">
                  <el-icon><Plus /></el-icon> 自定义
                </button>
              </div>
            </div>
            <div class="persona-cards" :class="{ condensed: !personaPanelExpanded }">
              <div
                v-for="p in personas"
                :key="p.id"
                class="persona-card"
                :class="{ selected: selectedPersonaIds.includes(p.id), primary: selectedPersonaId === p.id }"
                @click="togglePersonaPlan(p.id)"
              >
                <div class="persona-card-top">
                  <div class="persona-avatar" :class="'avatar-' + (p.avatar || 'general')">{{ p.name.charAt(0) }}</div>
                  <span v-if="personaPlanIndex(p.id)" class="persona-order-badge">{{ personaPlanIndex(p.id) }}</span>
                  <el-icon v-if="selectedPersonaIds.includes(p.id)" class="persona-check"><CircleCheck /></el-icon>
                  <button
                    v-if="p.type === 'custom'"
                    type="button"
                    class="persona-delete-btn"
                    title="删除自定义角色"
                    @click.stop="handleDeletePersona(p)"
                  >
                    <el-icon><Close /></el-icon>
                  </button>
                </div>
                <div class="persona-info">
                  <span class="persona-name">{{ p.name }}</span>
                  <span class="persona-title">{{ p.title }}</span>
                  <span class="persona-style">{{ p.style }}</span>
                </div>
              </div>
            </div>
            <p v-if="!personaPanelExpanded" class="persona-condensed-hint">
              已露出常用角色；需要更多面试官或查看完整描述时，可展开全部。
            </p>
          </div>

          <div class="lobby-plan-preview">
            <div
              v-for="(persona, index) in selectedPersonaQueue"
              :key="persona.id"
              class="plan-step active"
            >
              <span>{{ index + 1 }}</span>
              <div>
                <strong>{{ persona.name }}</strong>
                <small>{{ persona.title || '模拟面试官' }}</small>
              </div>
              <div class="plan-step-actions">
                <button type="button" :disabled="index === 0" @click.stop="movePersonaInPlan(persona.id, -1)">↑</button>
                <button type="button" :disabled="index === selectedPersonaQueue.length - 1" @click.stop="movePersonaInPlan(persona.id, 1)">↓</button>
                <button type="button" @click.stop="removePersonaFromPlan(persona.id)">×</button>
              </div>
            </div>
            <div v-if="selectedPersonaQueue.length === 0" class="plan-step empty">
              <span>1</span>
              <div>
                <strong>选择面试官</strong>
                <small>先选择至少一位面试官，本阶段会启动队列第一位。</small>
              </div>
            </div>
            <div class="plan-step muted">
              <span>+</span>
              <div>
                <strong>{{ selectedPersonaQueue.length > 1 ? '已形成多面试官计划' : '可添加多位面试官' }}</strong>
                <small>当前会启动第 1 位；完成后在总结卡片中进入下一位，不自动控制状态机。</small>
              </div>
            </div>
          </div>

          <div class="lobby-context-extra" :class="{ collapsed: !extraPanelExpanded }">
            <div class="context-extra-head">
              <div>
                <h3>重点考察方向</h3>
                <p>标记本次练习最想验证的能力方向，便于结束后统一复盘。</p>
              </div>
              <button class="soft-toggle-btn" type="button" @click="extraPanelExpanded = !extraPanelExpanded">
                {{ extraPanelExpanded ? '收起' : '展开' }}
              </button>
            </div>
            <div v-if="!extraPanelExpanded" class="extra-collapsed-summary">
              {{ selectedFocusSummary }}
            </div>
            <div v-show="extraPanelExpanded" class="focus-chip-list">
              <button
                v-for="tag in focusTagOptions"
                :key="tag"
                type="button"
                class="focus-chip"
                :class="{ active: selectedFocusTags.includes(tag) }"
                @click="toggleFocusTag(tag)"
              >
                {{ tag }}
              </button>
            </div>
            <label v-show="extraPanelExpanded" class="supplement-field">
              <span>补充信息</span>
              <el-input
                v-model="interviewSupplement"
                type="textarea"
                :rows="3"
                maxlength="300"
                show-word-limit
                placeholder="例如：希望重点追问分布式项目、实习贡献边界、岗位动机等。"
              />
            </label>
          </div>

          <div class="context-start-row">
            <button
              class="interview-start-button"
              type="button"
              :disabled="!canCreateSession || actionLoading"
              @click="handleCreateAndStart"
            >
              {{ startInterviewButtonLabel }}
              <el-icon v-if="actionLoading" class="is-loading"><Loading /></el-icon>
              <el-icon v-else><ArrowRight /></el-icon>
            </button>
            <span class="start-hint">当前会启动队列第 1 位面试官；回答评价、单轮总结和综合复盘能力保持不变。</span>
          </div>
        </section>

        <aside class="lobby-side">
          <section class="recent-interview-card">
            <div class="recent-card-head">
              <span>Quick Access</span>
              <strong>最近面试</strong>
            </div>
            <div v-if="recentInterviewRecords.length" class="recent-record-list">
              <article
                v-for="record in recentInterviewRecords"
                :key="record.id"
                class="recent-record"
                @click="openInterviewRecord(record)"
              >
                <div>
                  <strong>{{ record.title }}</strong>
                  <span>{{ record.completedCount }}/{{ record.totalCount }} 轮 · {{ record.resumeLabel }}</span>
                </div>
                <button type="button">{{ record.isCompleted ? '查看' : '继续' }}</button>
              </article>
            </div>
            <p v-else class="recent-empty">创建一次面试后，这里会出现快捷入口。</p>
          </section>

          <!-- 历史会话列表 -->
          <section class="history-sessions-section side-history-section">
            <div class="history-head">
              <div>
                <p class="section-kicker">History</p>
                <h2>面试记录</h2>
                <p>以一次面试计划为单位管理历史。</p>
              </div>
            </div>

            <div v-if="visibleInterviewRecords.length > 0" class="history-filter-tabs">
              <button
                v-for="tab in historyFilterTabs"
                :key="tab.key"
                class="filter-tab"
                :class="{ active: historyFilter === tab.key }"
                @click="historyFilter = tab.key"
              >
                {{ tab.label }} ({{ tab.count }})
              </button>
            </div>

            <div v-if="visibleInterviewRecords.length === 0" class="history-empty-card">
              <el-icon><VideoPlay /></el-icon>
              <strong>还没有面试记录</strong>
              <span>先从左侧创建一次面试。</span>
            </div>

            <div v-else class="history-record-grid">
              <div
                v-for="record in filteredInterviewRecords"
                :key="record.id"
                class="history-record-card"
              >
                <div class="history-card-top">
                  <span class="hsc-status" :class="recordStatusClass(record)">
                    {{ recordStatusText(record) }}
                  </span>
                </div>
                <div class="hsc-main" @click="openInterviewRecord(record)">
                  <CompanyAvatar v-if="record.job" class="hsc-company-avatar" :job="record.job" size="md" />
                  <div v-else class="hsc-avatar">{{ record.latestSession.personaName?.charAt(0) || '面' }}</div>
                  <div class="hsc-info">
                    <span class="hsc-name">{{ record.title }}</span>
                    <span class="hsc-title">{{ record.resumeLabel }} · {{ record.subtitle }}</span>
                  </div>
                </div>
                <div class="hsc-progress-line">
                  <span>轮次进度</span>
                  <strong>{{ record.completedCount }}/{{ record.totalCount }} 轮</strong>
                </div>
                <div class="record-round-list">
                  <span
                    v-for="(session, index) in record.sessions"
                    :key="session.sessionId"
                    :class="roundStatusClass(session)"
                  >
                    {{ index + 1 }}. {{ session.personaName || '面试官' }}
                  </span>
                </div>
                <el-progress
                  :percentage="recordProgress(record)"
                  :show-text="false"
                  :stroke-width="6"
                  :color="record.isCompleted ? '#10b981' : '#101a33'"
                />
                <div class="record-actions">
                  <button class="hsc-open-button" type="button" @click="openInterviewRecord(record)">
                    {{ record.isCompleted ? '查看复盘' : '继续面试' }}
                    <el-icon><ArrowRight /></el-icon>
                  </button>
                  <button class="hsc-delete-button" type="button" @click.stop="deleteInterviewRecord(record)">
                    <el-icon><Delete /></el-icon>
                  </button>
                </div>
              </div>
            </div>
          </section>
        </aside>
      </div>
    </template>

    <!-- 自定义人设弹窗 -->
    <el-dialog v-model="showPersonaDialog" title="创建自定义面试官" width="420px">
      <el-form :model="customPersonaForm" label-position="top">
        <el-form-item label="姓名" required>
          <el-input v-model="customPersonaForm.name" maxlength="20" placeholder="如：张总监" />
        </el-form-item>
        <el-form-item label="职位" required>
          <el-input v-model="customPersonaForm.title" maxlength="50" placeholder="如：资深后端架构师" />
        </el-form-item>
        <el-form-item label="风格描述" required>
          <el-input
            v-model="customPersonaForm.style"
            type="textarea"
            :rows="3"
            maxlength="200"
            placeholder="如：严谨深入，注重系统设计能力，擅长追问技术细节"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPersonaDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!customPersonaFormValid" @click="handleCreatePersona">创建</el-button>
      </template>
    </el-dialog>

    <!-- ========== 成长趋势弹窗 ========== -->
    <el-dialog v-model="showGrowthDialog" title="成长趋势" width="800px" :close-on-click-modal="false">
      <div v-if="growthReport" class="growth-content">
        <!-- 顶部信息 -->
        <div class="growth-header">
          <div class="growth-header-info">
            <span class="growth-header-label">当前岗位</span>
            <strong>{{ growthReport.jobTitle }}</strong>
            <small v-if="growthReport.companyName">{{ growthReport.companyName }}</small>
          </div>
          <div class="growth-header-info">
            <span class="growth-header-label">当前简历</span>
            <strong>{{ selectedResumeLabel }}</strong>
          </div>
        </div>

        <!-- 版本时间线 -->
        <div class="growth-timeline" v-if="growthSnapshots.length > 1">
          <div class="growth-timeline-line">
            <div
              v-for="(snap, idx) in growthSnapshots"
              :key="snap.resumeVersionId"
              class="growth-timeline-dot"
              :class="{ active: idx === growthSnapshots.length - 1 }"
            >
              <span class="growth-timeline-dot-inner"></span>
              <span class="growth-timeline-label">{{ snap.versionLabel.split('·')[0]?.trim() || snap.versionLabel }}</span>
            </div>
          </div>
        </div>

        <!-- 折线图 -->
        <div class="growth-chart" v-if="growthSnapshots.length > 1">
          <h4 class="growth-section-title">四维能力变化趋势</h4>
          <svg :viewBox="'0 0 600 280'" class="growth-line-chart" preserveAspectRatio="xMidYMid meet">
            <!-- Y轴网格线 -->
            <line v-for="y in 5" :key="'grid-' + y" :x1="50" :y1="40 + (y - 1) * 50" :x2="580" :y2="40 + (y - 1) * 50" stroke="#e5e7eb" stroke-width="1" />
            <!-- Y轴标签 -->
            <text v-for="y in 5" :key="'ylabel-' + y" :x="42" :y="44 + (y - 1) * 50" text-anchor="end" font-size="10" fill="#94a3b8">{{ 10 - (y - 1) * 2.5 }}</text>
            <!-- X轴标签 -->
            <text
              v-for="(snap, idx) in growthSnapshots"
              :key="'xlabel-' + idx"
              :x="50 + (idx / Math.max(growthSnapshots.length - 1, 1)) * 530"
              :y="272"
              text-anchor="middle"
              font-size="10"
              fill="#64748b"
            >{{ snap.versionLabel.split('·')[0]?.trim() || snap.versionLabel }}</text>

            <!-- 折线 -->
            <template v-for="(dim, dimIdx) in dimNames" :key="dim.key">
              <polyline
                :points="growthSnapshots.map((snap, idx) => {
                  const x = 50 + (idx / Math.max(growthSnapshots.length - 1, 1)) * 530
                  const y = 240 - ((snap.dimensions[dim.key as keyof GrowthDimensions] as number) / 10) * 200
                  return x + ',' + y
                }).join(' ')"
                :stroke="dim.color"
                :stroke-width="dimIdx === 0 ? 2.5 : 2"
                fill="none"
                stroke-linejoin="round"
                stroke-linecap="round"
              />
              <!-- 数据点 -->
              <circle
                v-for="(snap, idx) in growthSnapshots"
                :key="dim.key + '-' + idx"
                :cx="50 + (idx / Math.max(growthSnapshots.length - 1, 1)) * 530"
                :cy="240 - ((snap.dimensions[dim.key as keyof GrowthDimensions] as number) / 10) * 200"
                :r="dimIdx === 0 ? 4 : 3"
                :fill="dim.color"
                stroke="white"
                stroke-width="1.5"
              />
            </template>
          </svg>

          <!-- 图例 -->
          <div class="growth-legend">
            <span v-for="dim in dimNames" :key="dim.key" class="growth-legend-item">
              <span class="growth-legend-dot" :style="{ background: dim.color }"></span>
              {{ dim.label }}
            </span>
          </div>
        </div>

        <!-- 变化摘要 -->
        <div class="growth-changes" v-if="growthSnapshots.length > 1">
          <h4 class="growth-section-title">变化摘要</h4>
          <div class="growth-changes-grid">
            <div v-for="dim in dimNames" :key="dim.key" class="growth-change-item">
              <span class="growth-change-label">{{ dim.label }}</span>
              <span
                class="growth-change-value"
                :class="{
                  'growth-change-positive': (growthChanges[dim.key as keyof GrowthDimensions] as number) > 0,
                  'growth-change-negative': (growthChanges[dim.key as keyof GrowthDimensions] as number) < 0,
                }"
              >
                {{ (growthChanges[dim.key as keyof GrowthDimensions] as number) > 0 ? '+' : '' }}{{ (growthChanges[dim.key as keyof GrowthDimensions] as number).toFixed(1) }}
              </span>
            </div>
          </div>
        </div>

        <!-- 版本卡片 -->
        <div class="growth-snapshots">
          <h4 class="growth-section-title">版本详情</h4>
          <div v-for="snap in growthSnapshots" :key="snap.resumeVersionId" class="growth-snapshot-card">
            <div class="growth-snapshot-header">
              <strong>{{ snap.versionLabel.split('·')[0]?.trim() || snap.versionLabel }}</strong>
              <span class="growth-snapshot-badge">代表面试：Plan #{{ snap.representativePlanId }}</span>
              <span class="growth-snapshot-badge">该版本共面试 {{ snap.interviewCount }} 次</span>
            </div>
            <div class="growth-snapshot-scores">
              <div v-for="dim in dimNames" :key="dim.key" class="growth-snapshot-score">
                <span class="growth-snapshot-score-label">{{ dim.label }}</span>
                <span class="growth-snapshot-score-bar-wrapper">
                  <span
                    class="growth-snapshot-score-bar"
                    :style="{ width: ((snap.dimensions[dim.key as keyof GrowthDimensions] as number) / 10) * 100 + '%', background: dim.color }"
                  ></span>
                </span>
                <span class="growth-snapshot-score-value">{{ (snap.dimensions[dim.key as keyof GrowthDimensions] as number).toFixed(1) }}</span>
              </div>
            </div>
            <div v-if="snap.summary" class="growth-snapshot-summary">
              <span>总结：</span>{{ snap.summary }}
            </div>
          </div>
        </div>

        <!-- 单次提示 -->
        <div v-if="growthSnapshots.length <= 1" class="growth-single-hint">
          <el-icon><Trophy /></el-icon>
          <p>仅有 1 个版本数据</p>
          <small>完成更多面试后可见成长趋势对比</small>
        </div>
      </div>
      <div v-else-if="growthLoading" class="growth-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在加载成长数据，请稍候...</span>
      </div>
      <template #footer>
        <el-button @click="showGrowthDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ========== 单次多轮面试复盘弹窗 ========== -->
    <el-dialog
      v-model="showPlanReviewDialog"
      title="整次面试复盘"
      width="780px"
      class="plan-review-dialog"
      :close-on-click-modal="false"
    >
      <div v-if="activePlanReviewSummary" class="plan-review-dialog-content">
        <div class="plan-review-dialog-hero">
          <div>
            <span class="chat-plan-kicker">Interview Review</span>
            <h3>{{ activePlanReviewSummary.plan.jobLabel }}</h3>
            <p>{{ activePlanReviewSummary.plan.resumeLabel }} · {{ activePlanReviewSummary.completedRounds }}/{{ activePlanReviewSummary.totalRounds }} 轮已完成</p>
          </div>
          <div v-if="activePlanReviewSummary.overall" class="plan-review-dialog-score">
            <strong>{{ activePlanReviewSummary.overall.displayAverage }}</strong>
            <span>/10</span>
          </div>
        </div>

        <div v-if="activePlanReviewSummary.overall" class="plan-review-dialog-metrics">
          <div
            v-for="dim in activePlanReviewSummary.overall.dimensions"
            :key="dim.key"
            class="plan-review-dialog-metric"
          >
            <span>{{ dim.label }}</span>
            <strong>{{ dim.value.toFixed(1) }}</strong>
            <div><i :style="{ width: `${dim.value * 10}%`, background: dim.color }"></i></div>
          </div>
        </div>

        <div class="round-review-grid dialog-round-review-grid">
          <article
            v-for="round in activePlanReviewSummary.rounds"
            :key="round.sessionId"
            class="round-review-card"
            :class="{ completed: round.completed }"
          >
            <span>第 {{ round.order }} 轮</span>
            <strong>{{ round.personaName }}</strong>
            <small>{{ round.personaTitle }}</small>
            <div v-if="round.summary" class="round-review-score">
              <b>{{ round.summary.displayAverage }}</b>
              <em>/10</em>
              <p>薄弱点：{{ round.summary.weakest.label }}</p>
            </div>
            <p v-else class="round-review-pending">{{ round.completed ? '评分加载中' : '待完成' }}</p>
          </article>
        </div>

        <div v-if="activePlanReviewSummary.cachedSummary" class="plan-review-ai-summary">
          <h4>整次总结</h4>
          <p>{{ activePlanReviewSummary.cachedSummary.overallSummary }}</p>
          <div class="plan-review-list" v-if="activePlanReviewSummary.cachedSummary.crossStrengths.length">
            <span>稳定优势</span>
            <ul>
              <li v-for="item in activePlanReviewSummary.cachedSummary.crossStrengths" :key="item">{{ item }}</li>
            </ul>
          </div>
          <div class="plan-review-list" v-if="activePlanReviewSummary.cachedSummary.crossWeaknesses.length">
            <span>共性薄弱点</span>
            <ul>
              <li v-for="item in activePlanReviewSummary.cachedSummary.crossWeaknesses" :key="item">{{ item }}</li>
            </ul>
          </div>
          <div class="plan-review-list" v-if="activePlanReviewSummary.cachedSummary.suggestions.length">
            <span>训练方向</span>
            <ul>
              <li v-for="item in activePlanReviewSummary.cachedSummary.suggestions" :key="item">{{ item }}</li>
            </ul>
          </div>
        </div>
        <div v-else class="plan-review-empty-summary">
          <el-icon><Trophy /></el-icon>
          <strong>整次总结正在准备中</strong>
          <span>完成全部面试官后，可以生成跨轮次复盘。</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showPlanReviewDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ========== 聊天界面（有活跃会话时显示） ========== -->
    <section v-if="activeSession" class="interview-chat-layout">
      <!-- 窄侧边栏 -->
      <aside class="chat-sidebar">
        <button
          class="sidebar-back-btn"
          type="button"
          @click="backToPersona"
          title="返回面试大厅"
        >
          <el-icon><ArrowLeft /></el-icon>
        </button>

        <div class="sidebar-persona-card">
          <div class="sidebar-persona-avatar" :class="'avatar-' + (activeSessionPersona?.avatar || 'general')">
            {{ activeSession?.personaName?.charAt(0) || '面' }}
          </div>
          <span>当前面试官</span>
          <strong>{{ activeSession?.personaName || '面试官' }}</strong>
          <small>{{ activeSession?.personaTitle || '模拟面试官' }}</small>
          <p>{{ activePersonaStyle }}</p>
        </div>

        <!-- 活跃会话的题目进度 -->
        <div class="sidebar-question-card">
          <span>{{ activeSession.completed ? '本轮已完成' : '当前题目' }}</span>
          <strong>{{ activeSession.completed ? '完成' : `第 ${activeSession.currentQuestionIndex} 题` }}</strong>
          <small>{{ activeSession.currentQuestionIndex }} / {{ activeSession.totalQuestions }}</small>
        </div>
        <div class="sidebar-dots">
          <span
            v-for="step in (activeSession?.totalQuestions || 3)"
            :key="step"
            class="sidebar-dot"
            :class="{
              active: !activeSession.completed && step === activeSession.currentQuestionIndex && activeState.viewingHistoryIndex === null,
              completed: isQuestionStepCompleted(step),
              viewing: step === activeState.viewingHistoryIndex
            }"
            :title="isQuestionStepCompleted(step) ? '第' + step + '题已完成' : ''"
          >
            {{ step }}
          </span>
        </div>
        <div v-if="activeInterviewPlan && activePlanSessions.length > 1" class="sidebar-round-card">
          <span>{{ activeReviewMode ? '复盘轮次' : '本次轮次' }}</span>
          <button
            v-for="(session, index) in activePlanSessions"
            :key="session.sessionId"
            type="button"
            class="round-switch-button"
            :class="{
              active: session.sessionId === activeSessionId,
              completed: sessionCompleted(session),
              failed: sessionFailed(session),
              cancelled: sessionCancelled(session),
            }"
            :disabled="actionLoading"
            @click="switchToSession(session.sessionId)"
          >
            <i>{{ index + 1 }}</i>
            <strong>{{ session.personaName || '面试官' }}</strong>
            <small>{{ roundStatusText(session) }}</small>
          </button>
        </div>
      </aside>

      <!-- 聊天框 -->
      <div class="chat-main">
        <div v-if="activeInterviewPlan" class="chat-plan-header">
          <div>
            <span class="chat-plan-kicker">Interview Plan</span>
            <strong>{{ activeInterviewPlan.jobLabel }}</strong>
            <p>{{ activeInterviewPlan.resumeLabel }} · {{ activePlanStepLabel }} · {{ activeInterviewPlan.questionCount }} 道题</p>
          </div>
          <div class="chat-plan-status">
            <strong>{{ activePlanCompletionLabel }}</strong>
            <span>{{ activeReviewMode ? '复盘模式：可切换轮次查看完整对话' : (nextPlannedPersona ? `下一位：${nextPlannedPersona.name}` : '计划内面试官已到末尾') }}</span>
          </div>
          <div class="chat-plan-tags">
            <span v-for="tag in activeInterviewPlan.focusTags" :key="tag">{{ tag }}</span>
          </div>
          <CompanyProfileSignal
            v-if="hasCompanyProfile"
            class="chat-company-focus"
            :profile="selectedCompanyProfile"
            variant="inline"
            label="Interview Signal"
          />
          <div class="chat-plan-steps">
            <span
              v-for="(name, index) in activeInterviewPlan.personaNames"
              :key="`${activeInterviewPlan.planId}-${name}-${index}`"
              :class="{
                done: index < activePlanCompletedSessionIds.length,
                current: index === activeInterviewPlan.currentPersonaIndex,
              }"
            >
              <i>{{ index + 1 }}</i>
              {{ name }}
            </span>
          </div>
          <p v-if="activeInterviewPlan.supplement" class="chat-plan-note">
            {{ activeInterviewPlan.supplement }}
          </p>
        </div>
        <div
          v-if="activePlanReviewSummary && (activeReviewMode || activePlanFinished)"
          class="plan-review-panel"
        >
          <div class="plan-review-head">
            <div>
              <span class="chat-plan-kicker">Review Mode</span>
              <strong>正在复看本次面试对话</strong>
            </div>
            <div v-if="activePlanReviewSummary.overall" class="plan-review-score">
              <span>{{ activePlanReviewSummary.overall.displayAverage }}</span>
              <small>/10</small>
            </div>
          </div>

          <div v-if="activePlanReviewSummary.overall" class="plan-review-insight">
            <span>最高维度：{{ activePlanReviewSummary.overall.strongest.label }}</span>
            <span>最低维度：{{ activePlanReviewSummary.overall.weakest.label }}</span>
          </div>

          <button
            v-if="activePlanReviewSummary.cachedSummary || canSummarizeActivePlan"
            class="plan-review-generate"
            type="button"
            :disabled="multiSummaryLoading"
            @click="openPlanReviewDialog"
          >
            <el-icon v-if="multiSummaryLoading" class="is-loading"><Loading /></el-icon>
            <el-icon v-else><Trophy /></el-icon>
            {{
              multiSummaryLoading
                ? '正在生成整次复盘...'
                : activePlanReviewSummary.cachedSummary
                  ? '查看整次复盘'
                  : '生成并查看整次复盘'
            }}
          </button>
        </div>
        <div class="chat-messages" ref="chatMessagesRef">
          <!-- 聊天消息列表 -->
          <div
            v-for="(msg, idx) in chatMessages"
            :key="idx"
            class="chat-message"
            :class="msg.role"
          >
            <!-- 面试官消息 -->
            <template v-if="msg.role === 'interviewer'">
              <div class="msg-avatar" :class="'avatar-' + (activeSessionPersona?.avatar || 'general')">
                {{ activeSessionPersona?.name?.charAt(0) || '面' }}
              </div>
              <div class="msg-bubble interviewer-bubble">
                <div class="bubble-header">
                  <span class="bubble-name">{{ activeSession?.personaName || '面试官' }}</span>
                  <span class="bubble-question-num">第 {{ msg.questionIndex }} / {{ activeSession?.totalQuestions }} 题</span>
                </div>
                <div class="bubble-text">{{ msg.text }}</div>
              </div>
            </template>

            <!-- 用户消息 -->
            <template v-else-if="msg.role === 'user'">
              <div class="msg-bubble user-bubble">
                <div class="bubble-text">{{ msg.text }}</div>
              </div>
              <div class="msg-avatar user-avatar">我</div>
            </template>

            <!-- 发送中加载指示器 -->
            <template v-else-if="msg.role === 'sending'">
              <div class="msg-bubble user-bubble">
                <div class="bubble-text">{{ msg.text }}</div>
              </div>
              <div class="msg-avatar user-avatar">我</div>
              <div class="sending-indicator">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>{{ formatElapsedTime }}</span>
              </div>
            </template>

            <!-- 评价卡片 -->
            <template v-else-if="msg.role === 'evaluation' && msg.evaluation">
              <div class="evaluation-inline">
                <div class="eval-header">
                  <el-icon><Trophy /></el-icon>
                  <span>本题评价</span>
                </div>
                <div v-if="msg.evaluation.score" class="eval-overall-card">
                  <span>本题综合表现</span>
                  <strong>{{ questionEvaluationAverage(msg.evaluation.score) }}<small>/10</small></strong>
                  <p>{{ questionEvaluationCopy(msg.evaluation.score) }}</p>
                </div>
                <div v-if="msg.evaluation.score" class="eval-score-row">
                  <div class="eval-score-item">
                    <span>清晰度</span>
                    <el-progress :percentage="msg.evaluation.score.clarity * 10" :show-text="false" :stroke-width="5" color="#10b981" />
                    <strong>{{ msg.evaluation.score.clarity }}/10</strong>
                  </div>
                  <div class="eval-score-item">
                    <span>相关性</span>
                    <el-progress :percentage="msg.evaluation.score.relevance * 10" :show-text="false" :stroke-width="5" color="#047857" />
                    <strong>{{ msg.evaluation.score.relevance }}/10</strong>
                  </div>
                  <div class="eval-score-item">
                    <span>深度</span>
                    <el-progress :percentage="msg.evaluation.score.depth * 10" :show-text="false" :stroke-width="5" color="#101a33" />
                    <strong>{{ msg.evaluation.score.depth }}/10</strong>
                  </div>
                  <div class="eval-score-item">
                    <span>准确度</span>
                    <el-progress :percentage="msg.evaluation.score.accuracy * 10" :show-text="false" :stroke-width="5" color="#f59e0b" />
                    <strong>{{ msg.evaluation.score.accuracy }}/10</strong>
                  </div>
                </div>
                <div v-if="msg.evaluation.strengths?.length" class="eval-section">
                  <h4><el-icon><CircleCheck /></el-icon> 亮点</h4>
                  <ul><li v-for="s in msg.evaluation.strengths" :key="s">{{ s }}</li></ul>
                </div>
                <div v-if="msg.evaluation.weaknesses?.length" class="eval-section">
                  <h4><el-icon><Warning /></el-icon> 可加强</h4>
                  <ul><li v-for="w in msg.evaluation.weaknesses" :key="w">{{ w }}</li></ul>
                </div>
                <div v-if="msg.evaluation.suggestions?.length" class="eval-section">
                  <h4>建议</h4>
                  <p>{{ msg.evaluation.suggestions.join('；') }}</p>
                </div>
                <div v-if="msg.evaluation.referenceAnswer" class="eval-section ref-answer">
                  <h4><el-icon><Trophy /></el-icon> 参考回答</h4>
                  <p>{{ msg.evaluation.referenceAnswer }}</p>
                </div>
              </div>
            </template>

            <!-- 总结卡片 -->
            <template v-else-if="msg.role === 'summary'">
              <div class="summary-inline">
                <div class="summary-header">
                  <el-icon><Trophy /></el-icon>
                  <h3>练习总结</h3>
                </div>
                <p class="summary-desc">{{ summaryDescription }}</p>
                <div v-if="summaryStrengths.length" class="summary-block">
                  <h4>本次亮点</h4>
                  <span v-for="s in summaryStrengths" :key="s"><el-icon><CircleCheck /></el-icon>{{ s }}</span>
                </div>
                <div v-if="summarySuggestions.length" class="summary-block">
                  <h4>下一步建议</h4>
                  <span v-for="s in summarySuggestions" :key="s"><el-icon><ArrowRight /></el-icon>{{ s }}</span>
                </div>
                <div v-if="activeState.perQuestionScores.length > 0" class="summary-scores">
                  <h4>本轮评分画像</h4>
                  <div v-if="activeRoundScoreSummary" class="round-score-overview">
                    <div class="round-score-main">
                      <span>{{ activeRoundScoreSummary.displayAverage }}</span>
                      <small>/10</small>
                    </div>
                    <div class="round-score-copy">
                      <strong>薄弱维度：{{ activeRoundScoreSummary.weakest.label }}</strong>
                      <p>{{ trainingHintForDimension(activeRoundScoreSummary.weakest.key) }}</p>
                    </div>
                  </div>
                  <div class="summary-score-cards">
                    <div v-for="score in activeState.perQuestionScores" :key="score.questionIndex" class="summary-score-card">
                      <span class="sq-label">第 {{ score.questionIndex }} 题</span>
                      <div class="sq-dims">
                        <span>清晰度 {{ score.clarity }}</span>
                        <span>相关性 {{ score.relevance }}</span>
                        <span>深度 {{ score.depth }}</span>
                        <span>准确度 {{ score.accuracy }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-if="activeReviewMode" class="summary-actions-row">
                  <span class="review-inline-hint">可点击上方按钮查看整次复盘，也可以切换左侧轮次查看完整对话。</span>
                </div>
                <div v-else class="summary-actions-row">
                  <button
                    v-if="nextPlannedPersona"
                    class="interview-secondary-button"
                    type="button"
                    :disabled="actionLoading"
                    @click="startNextPlannedPersona"
                  >
                    进入下一位：{{ nextPlannedPersona.name }}
                    <el-icon v-if="actionLoading" class="is-loading"><Loading /></el-icon>
                    <el-icon v-else><ArrowRight /></el-icon>
                  </button>
                  <button
                    v-if="canReturnToWorkspace"
                    class="interview-primary-button"
                    type="button"
                    @click="goToOptimization"
                  >
                    回到简历优化
                    <el-icon><ArrowRight /></el-icon>
                  </button>
                </div>
              </div>
            </template>
          </div>

          <!-- 重试卡片 -->
          <div v-if="!activeReviewMode && activeState.retryable" class="retry-card">
            <div class="retry-message">
              <el-icon><Warning /></el-icon>
              <span>AI 评价暂时不可用，你可以重试提交</span>
            </div>
            <div v-if="activeState.lastSubmitAnswer" class="retry-answer-preview">
              <span class="retry-answer-label">已提交的回答：</span>
              <p>{{ activeState.lastSubmitAnswer }}</p>
            </div>
            <button class="interview-primary-button retry-button" type="button" :disabled="actionLoading" @click="retrySubmitAnswer">
              重试评价
              <el-icon v-if="actionLoading" class="is-loading"><Loading /></el-icon>
              <el-icon v-else><RefreshRight /></el-icon>
            </button>
          </div>
        </div>

        <!-- 输入栏 -->
        <div v-if="activeReviewMode" class="review-mode-bar">
          <el-icon><Trophy /></el-icon>
          <span>复盘模式：只能查看本次面试各轮对话与总结，回答、重试和继续面试操作已关闭。</span>
        </div>
        <div v-else class="chat-input-bar">
          <div class="voice-row">
            <el-tooltip
              :content="speechSupported ? '点击开始语音输入，再次点击结束' : '当前浏览器不支持语音识别'"
              placement="top"
            >
              <button
                type="button"
                class="voice-button"
                :class="{ listening: isListening }"
                :disabled="!speechSupported || !canSubmitAnswer || actionLoading"
                @click="toggleVoiceInput"
              >
                <el-icon :class="{ 'is-pulsing': isListening }"><Microphone /></el-icon>
              </button>
            </el-tooltip>
            <span v-if="isListening" class="voice-hint">正在聆听...</span>
          </div>
          <el-input
            v-model="activeState.answerDraft"
            type="textarea"
            :rows="2"
            maxlength="1200"
            show-word-limit
            :disabled="!canSubmitAnswer || actionLoading"
            placeholder="输入你的回答..."
            @keydown.enter="handleEnterKey"
            class="chat-textarea"
          />
          <button
            class="chat-send-btn"
            type="button"
            :disabled="!canSubmitAnswer || !activeState.answerDraft.trim() || actionLoading"
            @click="handleSubmitAnswer"
          >
            <el-icon v-if="actionLoading" class="is-loading"><Loading /></el-icon>
            <span v-else>发送</span>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ArrowRight,
  CircleCheck,
  Close,
  Delete,
  Loading,
  Microphone,
  Plus,
  RefreshRight,
  Trophy,
  VideoPlay,
  Warning,
} from '@element-plus/icons-vue'
import { listJobDescriptions, resolveCompanyProfile } from '../api/job'
import zhidaInterviewBrand from '../assets/zhida-interview-brand.png'
import CompanyAvatar from '../components/CompanyAvatar.vue'
import CompanyProfileSignal from '../components/CompanyProfileSignal.vue'
import {
  createInterviewPlan,
  deleteInterviewPlan,
  generateInterviewPlanSummary,
  getInterviewStatus,
  getSessionHistory,
  listInterviewerPersonas,
  createInterviewerPersona,
  deleteInterviewerPersona,
  listMyInterviewPlans,
  listMyInterviews,
  startInterview,
  submitInterviewAnswer,
  getInterviewGrowthReport,
} from '../api/interview'
import { getResumeVersion, getResumeVersions, listResumes } from '../api/resume'
import type { CompanyProfile, JobDescription } from '../types/job'
import type {
  EvaluationSummary,
  GrowthDimensions,
  GrowthReport,
  InterviewPlanResponse,
  InterviewPlanRound,
  InterviewerPersona,
  InterviewStatusResponse,
  MultiSessionSummaryResponse,
  PerQuestionScore,
  ScoreDetail,
  SessionHistoryItem,
} from '../types/interview'
import type { Resume, ResumeVersion } from '../types/resume'
import {
  getWorkspaceSelectedJobId,
  getWorkspaceSelectedResumeId,
  markReturnToEditor,
  setWorkspaceSelectedJobId,
} from '../utils/workspaceContext'
import { buildResumeEditorLocation } from '../utils/editorRoute'
import { filterTargetInterviewRecords } from '../utils/interviewContext'

interface ChatMessage {
  role: 'interviewer' | 'user' | 'sending' | 'evaluation' | 'summary'
  text?: string
  questionIndex?: number
  evaluation?: EvaluationSummary | null
}

interface SessionState {
  history: SessionHistoryItem[]
  answerDraft: string
  pendingAnswer: string
  retryable: boolean
  lastSubmitAnswer: string
  perQuestionScores: PerQuestionScore[]
  viewingHistoryIndex: number | null
}

interface LocalInterviewPlan {
  planId: string
  sessionId: number
  resumeVersionId: number
  resumeLabel: string
  jobDescriptionId: number
  jobLabel: string
  personaIds: number[]
  personaNames: string[]
  currentPersonaIndex: number
  questionCount: number
  focusTags: string[]
  supplement: string
  summary: MultiSessionSummaryResponse | null
  summaryGeneratedAt?: string | null
  createdAt: string
}

interface InterviewRecord {
  id: string
  title: string
  subtitle: string
  sessions: InterviewStatusResponse[]
  latestSession: InterviewStatusResponse
  job: JobDescription | null
  resumeLabel: string
  completedCount: number
  totalCount: number
  isCompleted: boolean
  isInProgress: boolean
  jobDescriptionId: number | null
  resumeVersionId: number | null
}

type ScoreDimensionKey = keyof GrowthDimensions

interface ScoreDimensionSummary {
  key: ScoreDimensionKey
  label: string
  value: number
  color: string
}

interface ScoreSummary {
  average: number
  displayAverage: string
  dimensions: ScoreDimensionSummary[]
  strongest: ScoreDimensionSummary
  weakest: ScoreDimensionSummary
}

interface RoundReviewSummary {
  sessionId: number
  personaName: string
  personaTitle: string
  order: number
  completed: boolean
  questionCount: number
  summary: ScoreSummary | null
}

function createSessionState(): SessionState {
  return {
    history: [],
    answerDraft: '',
    pendingAnswer: '',
    retryable: false,
    lastSubmitAnswer: '',
    perQuestionScores: [],
    viewingHistoryIndex: null,
  }
}

const route = useRoute()
const router = useRouter()
const fromEditor = computed(() => route.query.from === 'editor')
const fromTarget = computed(() => route.query.from === 'target')
const fromWorkspace = computed(() => fromEditor.value || fromTarget.value)
const targetId = computed(() => positiveQueryId(route.query.targetId))
const workspaceReturnLabel = computed(() => fromTarget.value ? '返回求职目标工作台' : '返回当前简历工作台')
const loadingOptions = ref(false)
const actionLoading = ref(false)
const activeReviewMode = ref(false)
const errorMessage = ref('')
const resumes = ref<Resume[]>([])
const versions = ref<ResumeVersion[]>([])
const jobs = ref<JobDescription[]>([])
const selectedCompanyProfile = ref<CompanyProfile | null>(null)
const selectedVersionId = ref<number | null>(null)
const selectedJobId = ref<number | null>(null)
const questionCount = ref(5)
const focusTagOptions = ['项目深挖', '技术基础', '系统设计', '岗位匹配', '表达结构', '职业动机']
const selectedFocusTags = ref<string[]>(['项目深挖', '岗位匹配'])
const interviewSupplement = ref('')
const personas = ref<InterviewerPersona[]>([])
const selectedPersonaId = ref<number | null>(null)
const selectedPersonaIds = ref<number[]>([])
const personaPanelExpanded = ref(false)
const extraPanelExpanded = ref(false)
const showPersonaDialog = ref(false)
const customPersonaForm = ref({ name: '', title: '', style: '' })

// 跨会话总结
const multiSummaryLoading = ref(false)
const deletedSessionIds = ref(new Set<number>())
const deletedSessionStorageKey = 'resumego:deletedInterviewSessionIds'

// 成长趋势
const showGrowthDialog = ref(false)
const growthLoading = ref(false)
const growthReport = ref<GrowthReport | null>(null)
const showPlanReviewDialog = ref(false)

// 当前简历 ID（由 selectedVersionId 推导）
const currentResumeId = computed(() => {
  const version = versions.value.find((v) => v.id === selectedVersionId.value)
  return version?.resumeId ?? null
})

// 维度名称
const dimNames = [
  { key: 'clarity', label: '表达清晰度', color: '#3b82f6' },
  { key: 'relevance', label: '岗位相关性', color: '#10b981' },
  { key: 'depth', label: '技术深度', color: '#f59e0b' },
  { key: 'accuracy', label: '回答准确性', color: '#8b5cf6' },
]

// 成长趋势快照（模板中类型收窄用）
const growthSnapshots = computed(() => growthReport.value?.snapshots ?? [])
const growthChanges = computed(() => growthReport.value?.changes ?? { clarity: 0, relevance: 0, depth: 0, accuracy: 0 })

// 历史会话筛选
const historyFilter = ref<'all' | 'completed' | 'inProgress'>('all')
const historyFilterTabs = computed(() => {
  const completed = visibleInterviewRecords.value.filter((record) => record.isCompleted).length
  const inProgress = visibleInterviewRecords.value.filter((record) => record.isInProgress).length
  return [
    { key: 'all' as const, label: '全部', count: visibleInterviewRecords.value.length },
    { key: 'completed' as const, label: '已完成', count: completed },
    { key: 'inProgress' as const, label: '进行中', count: inProgress },
  ]
})
const filteredInterviewRecords = computed(() => {
  if (historyFilter.value === 'completed') return visibleInterviewRecords.value.filter((record) => record.isCompleted)
  if (historyFilter.value === 'inProgress') return visibleInterviewRecords.value.filter((record) => record.isInProgress)
  return visibleInterviewRecords.value
})

// 多会话状态
const sessions = ref<InterviewStatusResponse[]>([])
const activeSessionId = ref<number | null>(null)
const sessionStates = ref<Record<number, SessionState>>({})
const localInterviewPlans = ref<Record<number, LocalInterviewPlan>>({})
const persistedInterviewPlans = ref<InterviewPlanResponse[]>([])

const elapsedTime = ref(0)
const actionStage = ref('正在处理...')
const chatMessagesRef = ref<HTMLElement | null>(null)
const createPanelRef = ref<HTMLElement | null>(null)
let elapsedTimer: ReturnType<typeof setInterval> | null = null

const speechSupported = ref(false)
const isListening = ref(false)
let recognition: any = null
let recognitionSessionId: number | null = null

// ── 计算属性 ──

const activeSession = computed(() =>
  sessions.value.find((s) => s.sessionId === activeSessionId.value) ?? null,
)

const visibleSessions = computed(() =>
  sessions.value.filter((session) => !deletedSessionIds.value.has(session.sessionId)),
)

const interviewRecords = computed<InterviewRecord[]>(() => {
  const grouped = new Map<string, InterviewStatusResponse[]>()

  for (const session of visibleSessions.value) {
    const plan = localInterviewPlans.value[session.sessionId]
    if (plan?.planId) {
      // 有 planId 的会话：按 planId 分组（一次计划包含多位面试官 → 一条档案）
      const existing = grouped.get(plan.planId) ?? []
      grouped.set(plan.planId, [...existing, session])
    } else {
      // 无 planId 的旧会话：各自作为独立档案（一条会话 → 一条档案）
      grouped.set('legacy_' + session.sessionId, [session])
    }
  }

  return [...grouped.entries()]
    .map(([id, groupSessions]) => {
      const sortedSessions = [...groupSessions].sort(comparePlanSessionOrder)
      const latestSession = sortedSessions[sortedSessions.length - 1] ?? sortedSessions[0]
      const plan = sortedSessions
        .map((session) => localInterviewPlans.value[session.sessionId])
        .find(Boolean)
      const completedSessionIds = sortedSessions
        .filter((session) => sessionCompleted(session))
        .map((session) => session.sessionId)
      const completedCount = completedSessionIds.length
      const totalCount = Math.max(plan?.personaIds.length ?? sortedSessions.length, sortedSessions.length)
      const title = plan?.jobLabel ?? '本次多轮面试'
      const job = plan?.jobDescriptionId
        ? jobs.value.find((item) => item.id === plan.jobDescriptionId) ?? null
        : null
      const resumeLabel = plan?.resumeLabel ?? '未知简历版本'
      const personaSummary = plan?.personaNames.join(' / ')
        ?? sortedSessions.map((session) => session.personaName || '面试官').join(' / ')

      return {
        id,
        title,
        subtitle: personaSummary,
        sessions: sortedSessions,
        latestSession,
        job,
        resumeLabel,
        completedCount,
        totalCount,
        isCompleted: totalCount > 0 && completedCount >= totalCount,
        isInProgress: completedCount < totalCount || sortedSessions.some((session) => !sessionCompleted(session)),
        jobDescriptionId: plan?.jobDescriptionId ?? null,
        resumeVersionId: plan?.resumeVersionId ?? null,
      }
    })
    .sort((a, b) => b.latestSession.sessionId - a.latestSession.sessionId)
})

const visibleInterviewRecords = computed(() => fromTarget.value
  ? filterTargetInterviewRecords(interviewRecords.value, selectedJobId.value, selectedVersionId.value)
  : interviewRecords.value)
const recentInterviewRecords = computed(() => visibleInterviewRecords.value.slice(0, 3))

const activeInterviewPlan = computed(() => {
  if (!activeSessionId.value) return null
  return localInterviewPlans.value[activeSessionId.value] ?? null
})
const activePlanStepLabel = computed(() => {
  if (!activeInterviewPlan.value) return ''
  const current = activeInterviewPlan.value.currentPersonaIndex + 1
  const total = Math.max(activeInterviewPlan.value.personaIds.length, 1)
  return `第 ${current} / ${total} 位面试官`
})
const nextPlannedPersona = computed(() => {
  const plan = activeInterviewPlan.value
  if (!plan) return null
  const nextPersonaId = plan.personaIds[plan.currentPersonaIndex + 1]
  if (!nextPersonaId) return null
  return personas.value.find((item) => item.id === nextPersonaId) ?? null
})
const activePlanSessions = computed(() => {
  const plan = activeInterviewPlan.value
  if (!plan) return []
  return sessions.value
    .filter((session) => localInterviewPlans.value[session.sessionId]?.planId === plan.planId)
    .sort(comparePlanSessionOrder)
})
const activePlanCompletedSessionIds = computed(() =>
  activePlanSessions.value
    .filter((session) => sessionCompleted(session))
    .map((session) => session.sessionId),
)
const activePlanCompletionLabel = computed(() => {
  const plan = activeInterviewPlan.value
  if (!plan) return ''
  return `已完成 ${activePlanCompletedSessionIds.value.length}/${plan.personaIds.length} 位`
})
const activePlanFinished = computed(() => {
  const plan = activeInterviewPlan.value
  if (!plan) return true
  return activePlanCompletedSessionIds.value.length >= plan.personaIds.length
})
const canSummarizeActivePlan = computed(() => activePlanFinished.value && activePlanCompletedSessionIds.value.length >= 2)
const canReturnToWorkspace = computed(() => {
  if (!activeSession.value) return true
  if (!activeInterviewPlan.value) return sessionCompleted(activeSession.value)
  return activePlanFinished.value
})
const activeState = computed<SessionState>(() => {
  if (!activeSessionId.value) return createSessionState()
  return sessionStates.value[activeSessionId.value] ?? createSessionState()
})

const activeSessionPersona = computed(() => {
  if (!activeSession.value?.personaName) return null
  return personas.value.find((p) => p.name === activeSession.value!.personaName) ?? null
})

const resumeOptions = computed(() =>
  versions.value.map((item) => ({
    value: item.id,
    label: `v${item.versionNo} · ${createdByLabel(item.createdByType)} · ${formatDate(item.createdAt)}`,
  })),
)

const jobOptions = computed(() =>
  jobs.value.map((item) => ({
    value: item.id,
    label: item.companyName ? `${item.jobTitle}｜${item.companyName}` : item.jobTitle,
  })),
)

const selectedPersonaQueue = computed(() =>
  selectedPersonaIds.value
    .map((id) => personas.value.find((item) => item.id === id))
    .filter((item): item is InterviewerPersona => Boolean(item)),
)
const selectedPersonaSummary = computed(() => {
  if (!selectedPersonaQueue.value.length) return '待选择'
  const names = selectedPersonaQueue.value.map((item) => item.name).join('、')
  return selectedPersonaQueue.value.length > 1 ? `${selectedPersonaQueue.value.length} 位 · ${names}` : names
})
const selectedFocusSummary = computed(() => {
  const focus = selectedFocusTags.value.length ? selectedFocusTags.value.join('、') : '未选择重点方向'
  return interviewSupplement.value ? `${focus} · 已补充说明` : focus
})
const selectedResumeLabel = computed(
  () => resumeOptions.value.find((item) => item.value === selectedVersionId.value)?.label ?? '待选择',
)
const selectedJobLabel = computed(
  () => jobOptions.value.find((item) => item.value === selectedJobId.value)?.label ?? '待选择',
)
const selectedJobEntity = computed(() => jobs.value.find((item) => item.id === selectedJobId.value) ?? null)
const companyProfileTags = computed(() => [
  ...(selectedCompanyProfile.value?.preferenceTags ?? []),
  ...(selectedCompanyProfile.value?.interviewFocus ?? []),
].filter(Boolean))
const hasCompanyProfile = computed(() => Boolean(selectedCompanyProfile.value?.companyName && companyProfileTags.value.length))
const workspaceContextLocked = computed(() => fromWorkspace.value && Boolean(selectedVersionId.value && selectedJobId.value))
const completedSessionCount = computed(() => visibleInterviewRecords.value.filter((r) => r.isCompleted).length)
const inProgressSessionCount = computed(() => visibleInterviewRecords.value.filter((r) => !r.isCompleted).length)
const startInterviewButtonLabel = computed(() => {
  if (selectedPersonaQueue.value.length > 1) return `开始第 1 位：${selectedPersonaQueue.value[0].name}`
  return '开始本次练习'
})

const canCreateSession = computed(
  () => Boolean(selectedVersionId.value && selectedJobId.value && selectedPersonaIds.value.length > 0),
)
const currentQuestion = computed(() => activeSession.value?.currentQuestion ?? null)
const currentIndex = computed(() => Math.max(1, activeSession.value?.currentQuestionIndex ?? 1))
const isCompleted = computed(() => Boolean(activeSession.value?.completed))
const canSubmitAnswer = computed(
  () => !activeReviewMode.value && activeSession.value?.status === 'WAITING_ANSWER' && Boolean(currentQuestion.value),
)
const activePersonaStyle = computed(() =>
  activeSessionPersona.value?.style
    || activeSession.value?.personaTitle
    || '关注回答是否围绕岗位要求、项目证据和表达结构展开。',
)
const customPersonaFormValid = computed(
  () =>
    customPersonaForm.value.name.trim() &&
    customPersonaForm.value.title.trim() &&
    customPersonaForm.value.style.trim(),
)
const formatElapsedTime = computed(() => {
  const sec = elapsedTime.value
  if (sec < 60) return `已用 ${sec} 秒`
  const min = Math.floor(sec / 60)
  const s = sec % 60
  return `已用 ${min} 分 ${s} 秒`
})

const chatMessages = computed<ChatMessage[]>(() => {
  const msgs: ChatMessage[] = []
  const hist = activeState.value.history
  for (const h of hist) {
    msgs.push({ role: 'interviewer', text: h.questionText, questionIndex: h.questionIndex })
    // 跳过未回答的问题（answerText 为空），避免渲染空白用户消息
    // 未回答的问题由下方的 currentQuestion 逻辑单独处理
    if (h.answerText) {
      msgs.push({ role: 'user', text: h.answerText })
    }
    if (h.evaluation) {
      msgs.push({ role: 'evaluation', evaluation: h.evaluation })
    }
  }
  // 当前问题（还没回答的）
  if (currentQuestion.value && !hist.some((h) => h.questionIndex === currentIndex.value)) {
    msgs.push({ role: 'interviewer', text: currentQuestion.value.questionText, questionIndex: currentIndex.value })
  }
  // 发送中的消息
  if (activeState.value.pendingAnswer) {
    msgs.push({ role: 'sending', text: activeState.value.pendingAnswer })
  }
  // 总结
  if (isCompleted.value && activeSession.value?.summaryJson) {
    msgs.push({ role: 'summary' })
  }
  return msgs
})

const summaryData = computed<Record<string, unknown>>(() => {
  if (!activeSession.value?.summaryJson) return {}
  try {
    return JSON.parse(activeSession.value.summaryJson) as Record<string, unknown>
  } catch {
    return {}
  }
})
const summaryDescription = computed(() => {
  if (activeReviewMode.value) {
    return '正在复盘本轮面试对话与评价，你可以在左侧切换查看本次面试的其他轮次。'
  }
  if (!canReturnToWorkspace.value && nextPlannedPersona.value) {
    return `本轮面试已完成，建议继续进入下一位面试官「${nextPlannedPersona.value.name}」，最后再生成整次多轮总结。`
  }
  const score = summaryData.value.overallScore
  return typeof score === 'number'
    ? `综合表现评分 ${score}/100，建议回到简历优化页补齐证据与表达。`
    : '总结已生成，建议回到简历优化页继续迭代。'
})
const summaryStrengths = computed(() => parseTextList(summaryData.value.strengths, []))
const summarySuggestions = computed(() => parseTextList(summaryData.value.suggestions, []))
const activeRoundScoreSummary = computed(() => {
  return summarizeQuestionScores(activeState.value.perQuestionScores)
})
const activePlanReviewSummary = computed(() => {
  const plan = activeInterviewPlan.value
  if (!plan) return null
  const summaries = activePlanSessions.value
    .map((session, index): RoundReviewSummary => {
      const scores = scoresForSession(session.sessionId)
      return {
        sessionId: session.sessionId,
        personaName: session.personaName || plan.personaNames[index] || '面试官',
        personaTitle: session.personaTitle || '模拟面试官',
        order: index + 1,
        completed: sessionCompleted(session),
        questionCount: scores.length || session.totalQuestions || plan.questionCount,
        summary: summarizeQuestionScores(scores),
      }
    })
  const completedSummaries = summaries.filter((item) => item.completed && item.summary)
  const allScores = activePlanSessions.value.flatMap((session) => scoresForSession(session.sessionId))
  const overall = summarizeQuestionScores(allScores)
  const cachedSummary = findCachedPlanSummary(plan.planId)

  return {
    plan,
    rounds: summaries,
    completedRounds: completedSummaries.length,
    totalRounds: Math.max(plan.personaIds.length, summaries.length),
    overall,
    cachedSummary,
  }
})

// ── 辅助函数 ──

function scoresForSession(sessionId: number) {
  const stateScores = sessionStates.value[sessionId]?.perQuestionScores ?? []
  if (stateScores.length) return stateScores
  return sessions.value.find((session) => session.sessionId === sessionId)?.perQuestionScores ?? []
}

function summarizeQuestionScores(scores: PerQuestionScore[]): ScoreSummary | null {
  if (!scores.length) return null
  const totals = scores.reduce(
    (acc, item) => {
      acc.clarity += Number(item.clarity || 0)
      acc.relevance += Number(item.relevance || 0)
      acc.depth += Number(item.depth || 0)
      acc.accuracy += Number(item.accuracy || 0)
      return acc
    },
    { clarity: 0, relevance: 0, depth: 0, accuracy: 0 },
  )
  const dimensions = dimNames.map((item) => {
    const key = item.key as ScoreDimensionKey
    return {
      key,
      label: item.label,
      value: roundToOneDecimal(totals[key] / scores.length),
      color: item.color,
    }
  })
  const sorted = [...dimensions].sort((a, b) => a.value - b.value)
  const average = roundToOneDecimal(dimensions.reduce((sum, item) => sum + item.value, 0) / dimensions.length)
  return {
    average,
    displayAverage: average.toFixed(1),
    dimensions,
    strongest: sorted[sorted.length - 1],
    weakest: sorted[0],
  }
}

function roundToOneDecimal(value: number) {
  return Math.round(value * 10) / 10
}

function questionEvaluationAverage(score: ScoreDetail) {
  return roundToOneDecimal((score.clarity + score.relevance + score.depth + score.accuracy) / 4).toFixed(1)
}

function questionEvaluationCopy(score: ScoreDetail) {
  const dimensions = dimNames.map((item) => {
    const key = item.key as ScoreDimensionKey
    return { label: item.label, value: score[key] }
  })
  const weakest = [...dimensions].sort((a, b) => a.value - b.value)[0]
  return `当前最需要加强：${weakest.label}`
}

function trainingHintForDimension(key: ScoreDimensionKey) {
  const hints: Record<ScoreDimensionKey, string> = {
    clarity: '建议练习“背景—动作—结果”三段式表达，把回答控制在 60-90 秒内。',
    relevance: '建议先复述岗位关键词，再把项目经历对齐到岗位要求，避免泛泛介绍。',
    depth: '建议补充技术取舍、故障定位、边界条件和复盘，减少只讲功能实现。',
    accuracy: '建议核实技术名词、指标和个人职责，避免模糊或夸大的表述。',
  }
  return hints[key]
}

function sessionCompleted(s: InterviewStatusResponse) {
  return s.completed === true && s.status === 'COMPLETED'
}

function sessionFailed(s: InterviewStatusResponse) {
  return s.status === 'FAILED'
}

function sessionCancelled(s: InterviewStatusResponse) {
  return s.status === 'CANCELLED'
}

function roundStatusText(s: InterviewStatusResponse) {
  if (sessionCompleted(s)) return '已完成'
  if (sessionFailed(s)) return '异常中断'
  if (sessionCancelled(s)) return '已取消'
  return '进行中'
}

function roundStatusClass(s: InterviewStatusResponse) {
  return {
    completed: sessionCompleted(s),
    failed: sessionFailed(s),
    cancelled: sessionCancelled(s),
  }
}

function recordStatusText(record: InterviewRecord) {
  if (record.isCompleted) return '已完成'
  if (record.sessions.some(sessionFailed)) return '异常中断'
  if (record.sessions.some(sessionCancelled)) return '已取消'
  return '进行中'
}

function recordStatusClass(record: InterviewRecord) {
  return {
    completed: record.isCompleted,
    failed: record.sessions.some(sessionFailed),
    cancelled: !record.sessions.some(sessionFailed) && record.sessions.some(sessionCancelled),
  }
}

function comparePlanSessionOrder(a: InterviewStatusResponse, b: InterviewStatusResponse) {
  const aIndex = localInterviewPlans.value[a.sessionId]?.currentPersonaIndex ?? Number.MAX_SAFE_INTEGER
  const bIndex = localInterviewPlans.value[b.sessionId]?.currentPersonaIndex ?? Number.MAX_SAFE_INTEGER
  if (aIndex !== bIndex) return aIndex - bIndex
  return a.sessionId - b.sessionId
}

function isQuestionStepCompleted(step: number) {
  if (!activeSession.value) return false
  if (activeSession.value.completed && step <= activeSession.value.totalQuestions) return true
  return activeState.value.history.some((h) => h.questionIndex === step)
}

function recordProgress(record: InterviewRecord) {
  if (record.totalCount <= 0) return 0
  return Math.min(100, Math.max(0, Math.round((record.completedCount / record.totalCount) * 100)))
}

async function openInterviewRecord(record: InterviewRecord) {
  const orderedSessions = [...record.sessions].sort(comparePlanSessionOrder)
  if (record.isCompleted) {
    activeReviewMode.value = true
    const target = orderedSessions[0]
    if (!target) return
    await switchToSession(target.sessionId)
    await preloadRecordHistories(orderedSessions)
    return
  }
  activeReviewMode.value = false
  const target = orderedSessions.find((session) => !sessionCompleted(session)) ?? orderedSessions[0]
  if (target) await switchToSession(target.sessionId)
}

async function preloadRecordHistories(recordSessions: InterviewStatusResponse[]) {
  await Promise.allSettled(recordSessions.map(async (session) => {
    const statusRes = await getInterviewStatus(session.sessionId)
    updateSessionInList(session.sessionId, {
      status: statusRes.data.status,
      currentQuestionIndex: statusRes.data.currentQuestionIndex,
      totalQuestions: statusRes.data.totalQuestions,
      currentQuestion: statusRes.data.currentQuestion ?? null,
      completed: statusRes.data.completed,
      summaryJson: statusRes.data.summaryJson,
      perQuestionScores: statusRes.data.perQuestionScores ?? [],
    })
    const state = getOrCreateSessionState(session.sessionId)
    state.perQuestionScores = statusRes.data.perQuestionScores ?? []
    await refreshSessionHistory(session.sessionId)
  }))
}

async function deleteInterviewRecord(record: InterviewRecord) {
  try {
    await ElMessageBox.confirm(
      `将从历史列表移除「${record.title}」及其 ${record.sessions.length} 轮记录。面试问答数据会保留，计划记录将被标记为隐藏。`,
      '删除面试记录',
      {
        confirmButtonText: '删除记录',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }

  const planId = Number(record.id)
  if (Number.isFinite(planId)) {
    try {
      await deleteInterviewPlan(planId)
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '删除面试计划失败')
      return
    }
  }

  const nextDeleted = new Set(deletedSessionIds.value)
  const deleteIds = record.sessions.map((session) => session.sessionId)
  deleteIds.forEach((sessionId) => {
    nextDeleted.add(sessionId)
    delete localInterviewPlans.value[sessionId]
    delete sessionStates.value[sessionId]
  })
  deletedSessionIds.value = nextDeleted
  persistDeletedSessionIds()
  persistedInterviewPlans.value = persistedInterviewPlans.value.filter((plan) => String(plan.planId) !== record.id)
  sessions.value = sessions.value.filter((session) => !nextDeleted.has(session.sessionId))

  if (activeSessionId.value && nextDeleted.has(activeSessionId.value)) {
    activeSessionId.value = null
  }
  ElMessage.success('已从当前历史列表移除')
}

function loadDeletedSessionIds() {
  try {
    const raw = window.localStorage.getItem(deletedSessionStorageKey)
    const values = raw ? JSON.parse(raw) : []
    deletedSessionIds.value = new Set(Array.isArray(values) ? values.map(Number).filter(Number.isFinite) : [])
  } catch {
    deletedSessionIds.value = new Set()
  }
}

function persistDeletedSessionIds() {
  window.localStorage.setItem(deletedSessionStorageKey, JSON.stringify([...deletedSessionIds.value]))
}

function focusCreatePanel() {
  createPanelRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function toggleFocusTag(tag: string) {
  if (selectedFocusTags.value.includes(tag)) {
    selectedFocusTags.value = selectedFocusTags.value.filter((item) => item !== tag)
    return
  }
  selectedFocusTags.value = [...selectedFocusTags.value, tag]
}

function syncPrimaryPersona() {
  selectedPersonaId.value = selectedPersonaIds.value[0] ?? null
}

function personaPlanIndex(personaId: number) {
  const index = selectedPersonaIds.value.indexOf(personaId)
  return index >= 0 ? index + 1 : 0
}

function togglePersonaPlan(personaId: number) {
  if (selectedPersonaIds.value.includes(personaId)) {
    removePersonaFromPlan(personaId)
    return
  }
  if (selectedPersonaIds.value.length >= 5) {
    ElMessage.warning('每次面试最多选择 5 位面试官')
    return
  }
  selectedPersonaIds.value = [...selectedPersonaIds.value, personaId]
  syncPrimaryPersona()
}

function removePersonaFromPlan(personaId: number) {
  selectedPersonaIds.value = selectedPersonaIds.value.filter((id) => id !== personaId)
  syncPrimaryPersona()
}

function movePersonaInPlan(personaId: number, direction: -1 | 1) {
  const currentIndex = selectedPersonaIds.value.indexOf(personaId)
  const nextIndex = currentIndex + direction
  if (currentIndex < 0 || nextIndex < 0 || nextIndex >= selectedPersonaIds.value.length) return
  const next = [...selectedPersonaIds.value]
  const [item] = next.splice(currentIndex, 1)
  next.splice(nextIndex, 0, item)
  selectedPersonaIds.value = next
  syncPrimaryPersona()
}

function getOrCreateSessionState(sessionId: number): SessionState {
  if (!sessionStates.value[sessionId]) {
    sessionStates.value[sessionId] = createSessionState()
  }
  return sessionStates.value[sessionId]
}

function updateSessionInList(sessionId: number, partial: Partial<InterviewStatusResponse>) {
  const idx = sessions.value.findIndex((s) => s.sessionId === sessionId)
  if (idx >= 0) {
    sessions.value[idx] = { ...sessions.value[idx], ...partial }
  }
}

function upsertSession(session: InterviewStatusResponse) {
  const idx = sessions.value.findIndex((s) => s.sessionId === session.sessionId)
  if (idx >= 0) {
    sessions.value[idx] = { ...sessions.value[idx], ...session }
  } else {
    sessions.value = [session, ...sessions.value]
  }
}

function buildStatusFromPlanRound(round: InterviewPlanRound): InterviewStatusResponse {
  return {
    sessionId: round.sessionId,
    status: round.status,
    currentQuestionIndex: round.currentQuestionIndex,
    totalQuestions: round.totalQuestions,
    currentQuestion: null,
    summaryJson: null,
    completed: round.completed,
    perQuestionScores: null,
    personaName: round.personaName,
    personaTitle: round.personaTitle,
  }
}

function resolvePlanResumeLabel(plan: InterviewPlanResponse) {
  return resumeOptions.value.find((item) => item.value === plan.resumeVersionId)?.label
    ?? `简历版本 #${plan.resumeVersionId}`
}

function resolvePlanJobLabel(plan: InterviewPlanResponse) {
  return jobOptions.value.find((item) => item.value === plan.jobDescriptionId)?.label
    ?? plan.title
    ?? `岗位 #${plan.jobDescriptionId}`
}

function applyBackendPlans(plans: InterviewPlanResponse[]) {
  const nextPlansById = new Map(persistedInterviewPlans.value.map((plan) => [plan.planId, plan]))
  for (const plan of plans) {
    nextPlansById.set(plan.planId, plan)
    const orderedRounds = [...plan.rounds].sort((a, b) => a.roundOrder - b.roundOrder)
    const personaIds = orderedRounds.map((round) => round.personaId)
    const personaNames = orderedRounds.map((round) => round.personaName)
    orderedRounds.forEach((round, index) => {
      if (deletedSessionIds.value.has(round.sessionId)) return
      localInterviewPlans.value[round.sessionId] = {
        planId: String(plan.planId),
        sessionId: round.sessionId,
        resumeVersionId: plan.resumeVersionId,
        resumeLabel: resolvePlanResumeLabel(plan),
        jobDescriptionId: plan.jobDescriptionId,
        jobLabel: resolvePlanJobLabel(plan),
        personaIds,
        personaNames,
        currentPersonaIndex: index,
        questionCount: plan.questionCount,
        focusTags: plan.focusTags ?? [],
        supplement: plan.supplement ?? '',
        summary: plan.summary ?? null,
        summaryGeneratedAt: plan.summaryGeneratedAt ?? null,
        createdAt: plan.createdAt ?? new Date().toISOString(),
      }
      upsertSession(buildStatusFromPlanRound(round))
    })
  }
  persistedInterviewPlans.value = [...nextPlansById.values()]
}

function scrollToBottom() {
  nextTick(() => {
    const el = chatMessagesRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

watch(chatMessages, () => scrollToBottom(), { deep: true })
watch(actionLoading, (val) => {
  if (!val) scrollToBottom()
})

watch(
  () => selectedJobEntity.value?.companyName,
  (companyName) => {
    void loadSelectedCompanyProfile(companyName)
  },
  { immediate: true },
)

onMounted(() => {
  loadDeletedSessionIds()
  loadOptions()
  initSpeechRecognition()
})

// ── 语音识别 ──

function initSpeechRecognition() {
  const SpeechRecognitionConstructor =
    (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  if (!SpeechRecognitionConstructor) {
    speechSupported.value = false
    return
  }
  speechSupported.value = true
  recognition = new SpeechRecognitionConstructor()
  recognition.continuous = false
  recognition.interimResults = true
  recognition.lang = 'zh-CN'
  recognition.onresult = (event: any) => {
    let transcript = ''
    for (let i = 0; i < event.results.length; i++) transcript += event.results[i][0].transcript
    if (transcript && recognitionSessionId != null) {
      const state = getOrCreateSessionState(recognitionSessionId)
      state.answerDraft = state.answerDraft.trim()
        ? state.answerDraft + ' ' + transcript.trim()
        : transcript.trim()
    }
  }
  recognition.onend = () => {
    isListening.value = false
  }
  recognition.onerror = (event: any) => {
    console.error('语音识别错误:', event.error)
    ElMessage.error(`语音识别失败：${event.error}`)
    isListening.value = false
  }
}

function toggleVoiceInput() {
  if (!recognition) return
  if (isListening.value) {
    recognition.stop()
    isListening.value = false
  } else {
    try {
      recognitionSessionId = activeSessionId.value
      recognition.start()
      isListening.value = true
    } catch (e) {
      console.error('启动语音识别失败:', e)
      ElMessage.error('启动语音识别失败')
    }
  }
}

onUnmounted(() => {
  stopElapsedTimer()
})

function handleEnterKey(e: KeyboardEvent) {
  if (e.shiftKey) return
  e.preventDefault()
  if (canSubmitAnswer.value && activeState.value.answerDraft.trim() && !actionLoading.value) {
    handleSubmitAnswer()
  }
}

// ── 数据加载 ──

async function loadSelectedCompanyProfile(companyName?: string | null) {
  selectedCompanyProfile.value = null
  if (!companyName) return
  try {
    const response = await resolveCompanyProfile(companyName)
    selectedCompanyProfile.value = response.data?.companyName ? response.data : null
  } catch {
    selectedCompanyProfile.value = null
  }
}

async function loadOptions() {
  loadingOptions.value = true
  errorMessage.value = ''
  try {
    const [resumeRes, jobRes, personaRes, sessionsRes, plansRes] = await Promise.all([
      listResumes(),
      listJobDescriptions(),
      listInterviewerPersonas(),
      listMyInterviews(),
      listMyInterviewPlans(),
    ])
    resumes.value = resumeRes.data
    jobs.value = jobRes.data
    personas.value = personaRes.data
    if (personas.value.length > 0) {
      selectedPersonaIds.value = [personas.value[0].id]
      syncPrimaryPersona()
    }
    sessions.value = sessionsRes.data.filter((session) => !deletedSessionIds.value.has(session.sessionId))

    if (resumes.value[0]?.id) {
      const queryVersionId = Number(route.query.versionId)
      const queryJobId = Number(route.query.jobId)
      const storedJobId = getWorkspaceSelectedJobId()
      const storedResumeId = getWorkspaceSelectedResumeId()

      let targetResumeId = storedResumeId ?? resumes.value[0].id
      if (fromTarget.value && queryVersionId > 0) {
        const targetVersionRes = await getResumeVersion(queryVersionId)
        targetResumeId = targetVersionRes.data.resumeId
      }
      const versionRes = await getResumeVersions(targetResumeId)
      versions.value = versionRes.data
      selectedVersionId.value = versions.value.find((item) => item.id === queryVersionId)?.id
        ?? resumes.value[0]?.currentVersion?.id
        ?? versions.value[0]?.id
        ?? null
      selectedJobId.value = jobs.value.find((item) => item.id === queryJobId)?.id
        ?? jobs.value.find((item) => item.id === storedJobId)?.id
        ?? jobs.value.find((item) => item.parseStatus === 'succeeded')?.id
        ?? jobs.value[0]?.id
        ?? null
    }
    applyBackendPlans(plansRes.data)
    if (selectedJobId.value) {
      setWorkspaceSelectedJobId(selectedJobId.value)
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载面试上下文失败'
  } finally {
    loadingOptions.value = false
  }
}

// ── 会话切换 ──

async function switchToSession(sessionId: number) {
  const alreadyActive = activeSessionId.value === sessionId

  activeSessionId.value = sessionId
  const state = getOrCreateSessionState(sessionId)

  // 如果该会话还没有加载过历史，从后端加载
  if (!alreadyActive || state.history.length === 0) {
    try {
      // 刷新会话状态
      const statusRes = await getInterviewStatus(sessionId)
      // 如果等待期间用户切换到了其他会话，放弃本次更新
      if (activeSessionId.value !== sessionId) return
      updateSessionInList(sessionId, {
        status: statusRes.data.status,
        currentQuestionIndex: statusRes.data.currentQuestionIndex,
        totalQuestions: statusRes.data.totalQuestions,
        currentQuestion: statusRes.data.currentQuestion ?? null,
        completed: statusRes.data.completed,
        summaryJson: statusRes.data.summaryJson,
      })
      if (statusRes.data.perQuestionScores) {
        state.perQuestionScores = statusRes.data.perQuestionScores
      }

      await refreshSessionHistory(sessionId)
    } catch (e) {
      console.error('加载会话历史失败:', e)
    }
  }
}

async function refreshSessionHistory(sessionId: number) {
  const state = getOrCreateSessionState(sessionId)
  const historyRes = await getSessionHistory(sessionId)
  state.history = historyRes.data.items
}

// ── 单次面试计划总结 ──

async function openPlanReviewDialog() {
  const summary = activePlanReviewSummary.value
  if (!summary) return
  if (summary.cachedSummary) {
    showPlanReviewDialog.value = true
    return
  }
  if (!canSummarizeActivePlan.value) {
    ElMessage.warning('完成本次多轮面试后才能生成整次复盘。')
    return
  }
  await handleGenerateActivePlanSummary(true)
}

async function handleGenerateActivePlanSummary(openAfterGenerate = false) {
  const plan = activeInterviewPlan.value
  if (plan?.planId) {
    await handleGeneratePlanSummary(plan.planId, openAfterGenerate)
    return
  }
  ElMessage.warning('旧版面试记录缺少计划信息，无法生成整次复盘。')
}

async function handleGeneratePlanSummary(planId: string, openAfterGenerate = false) {
  const numericPlanId = Number(planId)
  if (!Number.isFinite(numericPlanId)) {
    ElMessage.warning('旧版面试记录缺少计划信息，无法生成整次总结。')
    return
  }

  const cachedSummary = findCachedPlanSummary(planId)
  if (cachedSummary) {
    if (openAfterGenerate) showPlanReviewDialog.value = true
    else ElMessage.info('整次复盘已生成，可点击复盘入口查看。')
    return
  }

  multiSummaryLoading.value = true
  errorMessage.value = ''

  try {
    const res = await generateInterviewPlanSummary(numericPlanId)
    cachePlanSummary(planId, res.data)
    ElMessage.success('整次复盘已生成')
    if (openAfterGenerate) showPlanReviewDialog.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '生成整次总结失败'
  } finally {
    multiSummaryLoading.value = false
  }
}

function findCachedPlanSummary(planId: string) {
  const plan = Object.values(localInterviewPlans.value).find((item) => item.planId === planId)
  return plan?.summary ?? null
}

function cachePlanSummary(planId: string, summary: MultiSessionSummaryResponse) {
  const generatedAt = new Date().toISOString()
  Object.keys(localInterviewPlans.value).forEach((sessionId) => {
    const plan = localInterviewPlans.value[Number(sessionId)]
    if (plan?.planId === planId) {
      plan.summary = summary
      plan.summaryGeneratedAt = generatedAt
    }
  })
  persistedInterviewPlans.value = persistedInterviewPlans.value.map((plan) =>
    String(plan.planId) === planId
      ? { ...plan, summary, summaryGeneratedAt: generatedAt }
      : plan,
  )
}

// ── 成长趋势 ──

async function loadGrowthData() {
  if (!selectedVersionId.value || !selectedJobId.value || !currentResumeId.value) {
    ElMessage.warning('请先选择简历和岗位')
    return
  }

  growthLoading.value = true
  growthReport.value = null
  errorMessage.value = ''

  try {
    const res = await getInterviewGrowthReport(currentResumeId.value, selectedJobId.value)
    if (!res.data.snapshots.length) {
      ElMessage.info('当前岗位暂无成长快照，请完成一次多轮面试并生成整次复盘后再查看')
      return
    }
    growthReport.value = res.data

    showGrowthDialog.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载成长数据失败'
  } finally {
    growthLoading.value = false
  }
}

// ── 返回角色卡选择 ──

function backToPersona() {
  activeSessionId.value = null
  activeReviewMode.value = false
}

// ── 创建面试官 ──

async function handleCreatePersona() {
  if (!customPersonaFormValid.value) return
  try {
    const res = await createInterviewerPersona({
      name: customPersonaForm.value.name.trim(),
      title: customPersonaForm.value.title.trim(),
      style: customPersonaForm.value.style.trim(),
    })
    personas.value.push(res.data)
    selectedPersonaIds.value = [...selectedPersonaIds.value, res.data.id]
    syncPrimaryPersona()
    showPersonaDialog.value = false
    customPersonaForm.value = { name: '', title: '', style: '' }
    ElMessage.success('面试官创建成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
  }
}

// ── 删除自定义面试官 ──

async function handleDeletePersona(persona: InterviewerPersona) {
  try {
    await ElMessageBox.confirm(
      `确定要删除自定义角色「${persona.name}」吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteInterviewerPersona(persona.id)
    personas.value = personas.value.filter((p) => p.id !== persona.id)
    selectedPersonaIds.value = selectedPersonaIds.value.filter((id) => id !== persona.id)
    if (selectedPersonaId.value === persona.id) {
      selectedPersonaId.value = selectedPersonaIds.value[0] ?? null
    }
    ElMessage.success('已删除')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

// ── 创建并开始面试 ──

async function handleCreateAndStart() {
  if (!selectedVersionId.value || !selectedJobId.value || !selectedPersonaId.value) return
  activeReviewMode.value = false
  setWorkspaceSelectedJobId(selectedJobId.value)
  actionLoading.value = true
  actionStage.value = '正在创建多轮面试计划...'
  startElapsedTimer()
  errorMessage.value = ''

  try {
    const planRes = await createInterviewPlan({
      resumeVersionId: selectedVersionId.value,
      jobDescriptionId: selectedJobId.value,
      questionCount: questionCount.value,
      personaIds: [...selectedPersonaIds.value],
      focusTags: [...selectedFocusTags.value],
      supplement: interviewSupplement.value.trim(),
    })
    applyBackendPlans([planRes.data])
    const firstRound = [...planRes.data.rounds].sort((a, b) => a.roundOrder - b.roundOrder)[0]
    if (!firstRound) {
      throw new Error('面试计划缺少轮次')
    }
    actionStage.value = '正在启动第一轮面试...'
    const startRes = await startInterview(firstRound.sessionId)

    upsertSession(startRes.data)
    activeSessionId.value = startRes.data.sessionId
    getOrCreateSessionState(startRes.data.sessionId)

    ElMessage.success('面试已开始')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '启动面试失败'
  } finally {
    actionLoading.value = false
    stopElapsedTimer()
  }
}

async function startNextPlannedPersona() {
  activeReviewMode.value = false
  const plan = activeInterviewPlan.value
  const nextPersona = nextPlannedPersona.value
  if (!plan || !nextPersona) return
  const nextIndex = plan.currentPersonaIndex + 1
  const nextSession = activePlanSessions.value.find((session) =>
    localInterviewPlans.value[session.sessionId]?.currentPersonaIndex === nextIndex,
  )
  if (!nextSession) {
    errorMessage.value = '未找到下一轮面试会话，请刷新后重试'
    return
  }

  actionLoading.value = true
  actionStage.value = `正在启动下一位面试官：${nextPersona.name}...`
  startElapsedTimer()
  errorMessage.value = ''
  setWorkspaceSelectedJobId(plan.jobDescriptionId)

  try {
    const startRes = await startInterview(nextSession.sessionId)
    upsertSession(startRes.data)
    activeSessionId.value = startRes.data.sessionId
    getOrCreateSessionState(startRes.data.sessionId)

    ElMessage.success(`已进入下一位面试官：${nextPersona.name}`)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '启动下一位面试官失败'
  } finally {
    actionLoading.value = false
    stopElapsedTimer()
  }
}

// ── 提交回答 ──

async function handleSubmitAnswer() {
  if (!activeSession.value || !activeState.value.answerDraft.trim()) return
  const currentAnswer = activeState.value.answerDraft.trim()
  const sessionId = activeSessionId.value!
  const state = getOrCreateSessionState(sessionId)

  state.answerDraft = ''

  // 先显示用户消息
  state.pendingAnswer = currentAnswer
  actionLoading.value = true
  errorMessage.value = ''
  state.retryable = false
  startElapsedTimer()

  const currentQ = activeSession.value.currentQuestion
  state.lastSubmitAnswer = currentAnswer

  try {
    const res = await submitInterviewAnswer(sessionId, { answerText: currentAnswer })

    // 清除发送中状态
    state.pendingAnswer = ''

    if (res.data.retryable) {
      state.retryable = true
      stopElapsedTimer()
      errorMessage.value = 'AI 评价暂时不可用，请稍后重试'
      return
    }

    state.history.push({
      questionIndex: res.data.currentQuestionIndex > 0 ? res.data.currentQuestionIndex - 1 : currentQ?.questionIndex ?? 0,
      questionText: currentQ?.questionText ?? '',
      questionType: currentQ?.questionType ?? '',
      answerText: currentAnswer,
      evaluation: res.data.evaluation ?? null,
    })

    updateSessionInList(sessionId, {
      status: res.data.status,
      currentQuestionIndex: res.data.currentQuestionIndex,
      totalQuestions: res.data.totalQuestions,
      currentQuestion: res.data.nextQuestion ?? null,
      completed: res.data.completed,
    })

    if (res.data.completed) {
      const status = await getInterviewStatus(sessionId)
      updateSessionInList(sessionId, {
        status: status.data.status,
        currentQuestionIndex: status.data.currentQuestionIndex,
        totalQuestions: status.data.totalQuestions,
        currentQuestion: status.data.currentQuestion ?? null,
        completed: status.data.completed,
        summaryJson: status.data.summaryJson,
      })
      state.perQuestionScores = status.data.perQuestionScores ?? []
      await refreshSessionHistory(sessionId)
      ElMessage.success('模拟面试已完成')
    }
  } catch (error) {
    state.pendingAnswer = ''
    errorMessage.value = error instanceof Error ? error.message : '提交回答失败'
    state.retryable = true
  } finally {
    actionLoading.value = false
    stopElapsedTimer()
  }
}

// ── 重试评价 ──

async function retrySubmitAnswer() {
  if (!activeSession.value || !activeState.value.lastSubmitAnswer) return
  const sessionId = activeSessionId.value!
  const state = getOrCreateSessionState(sessionId)

  state.pendingAnswer = state.lastSubmitAnswer
  actionLoading.value = true
  errorMessage.value = ''
  state.retryable = false
  startElapsedTimer()

  try {
    const res = await submitInterviewAnswer(sessionId, { answerText: state.lastSubmitAnswer })

    state.pendingAnswer = ''

    if (res.data.retryable) {
      state.retryable = true
      stopElapsedTimer()
      errorMessage.value = 'AI 评价仍然不可用，请稍后再试'
      return
    }

    if (state.history.length > 0) {
      state.history[state.history.length - 1].evaluation = res.data.evaluation ?? null
    }

    updateSessionInList(sessionId, {
      status: res.data.status,
      currentQuestionIndex: res.data.currentQuestionIndex,
      totalQuestions: res.data.totalQuestions,
      currentQuestion: res.data.nextQuestion ?? null,
      completed: res.data.completed,
    })

    if (res.data.completed) {
      const status = await getInterviewStatus(sessionId)
      updateSessionInList(sessionId, {
        status: status.data.status,
        currentQuestionIndex: status.data.currentQuestionIndex,
        totalQuestions: status.data.totalQuestions,
        currentQuestion: status.data.currentQuestion ?? null,
        completed: status.data.completed,
        summaryJson: status.data.summaryJson,
      })
      state.perQuestionScores = status.data.perQuestionScores ?? []
      await refreshSessionHistory(sessionId)
      ElMessage.success('模拟面试已完成')
    }
  } catch (error) {
    state.pendingAnswer = ''
    errorMessage.value = error instanceof Error ? error.message : '重试失败'
    state.retryable = true
  } finally {
    actionLoading.value = false
    stopElapsedTimer()
  }
}

// ── 导航 ──

function goToOptimization() {
  if (!canReturnToWorkspace.value) {
    ElMessage.info('请先完成本次多轮面试，再回到简历优化。')
    return
  }
  if (fromWorkspace.value) {
    returnToEditor()
    return
  }
  if (selectedVersionId.value && selectedJobId.value) {
    setWorkspaceSelectedJobId(selectedJobId.value)
    router.push(buildResumeEditorLocation({ versionId: selectedVersionId.value }))
    return
  }
  router.push({ name: 'workbench' })
}

function returnToEditor() {
  if (!canReturnToWorkspace.value) {
    ElMessage.info('请先完成本次多轮面试，再回到简历工作台。')
    return
  }
  if (fromTarget.value) {
    router.push({ name: 'workbench', query: targetId.value ? { targetId: String(targetId.value) } : {} })
    return
  }
  markReturnToEditor()
  router.push(buildResumeEditorLocation({ versionId: selectedVersionId.value }))
}

// ── 工具函数 ──

function positiveQueryId(value: unknown): number | null {
  const raw = Array.isArray(value) ? value[0] : value
  const id = Number(raw)
  return Number.isSafeInteger(id) && id > 0 ? id : null
}

function parseTextList(value: unknown, fallback: string[]) {
  if (Array.isArray(value)) return value.map(String).filter(Boolean)
  if (typeof value !== 'string' || !value.trim()) return fallback
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) return parsed.map(String).filter(Boolean)
    if (typeof parsed === 'string') return [parsed]
    if (parsed && typeof parsed === 'object')
      return Object.values(parsed).map(String).filter(Boolean)
  } catch {
    return [value]
  }
  return fallback
}

function startElapsedTimer() {
  elapsedTime.value = 0
  stopElapsedTimer()
  elapsedTimer = setInterval(() => {
    elapsedTime.value++
  }, 1000)
}

function stopElapsedTimer() {
  if (elapsedTimer !== null) {
    clearInterval(elapsedTimer)
    elapsedTimer = null
  }
}

function createdByLabel(type: string) {
  if (type === 'ai_suggestion') return 'AI 建议生成'
  if (type === 'import') return '初始导入'
  if (type === 'user') return '手动创建'
  return type
}

function formatDate(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 10)
}
</script>

<style scoped>
.interview-page {
  box-sizing: border-box;
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(16, 185, 129, 0.1), transparent 32%),
    linear-gradient(135deg, #f8fafc 0%, #eef3f8 100%);
  color: #101a33;
  font-family: Inter, 'Noto Sans SC', ui-sans-serif, system-ui, sans-serif;
  padding: 18px;
}

/* 返回栏 */
.workspace-return-bar {
  display: flex; align-items: center; gap: 14px; margin: 0 0 12px;
  border: 1px solid #e5eaf2; border-radius: 18px; background: rgba(255, 255, 255, 0.92);
  padding: 8px 12px; box-shadow: 0 12px 30px rgba(15, 23, 42, 0.05);
  backdrop-filter: blur(16px);
}
.workspace-return-bar button {
  border: 0; border-radius: 999px; background: #101a33; color: #fff;
  cursor: pointer; font-weight: 800; padding: 8px 12px;
}
.workspace-return-bar button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
.workspace-return-bar span { color: #64748b; font-size: 13px; }

/* 面试大厅 */
.interview-lobby-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 286px;
  gap: 16px;
  min-height: 188px;
  margin-bottom: 14px;
  padding: 20px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 24px;
  background:
    radial-gradient(circle at 8% 0%, rgba(16, 185, 129, 0.14), transparent 34%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.94));
  box-shadow: 0 18px 54px rgba(15, 23, 42, 0.07);
  overflow: hidden;
  position: relative;
}
.interview-lobby-hero::after {
  content: "";
  position: absolute;
  inset: auto -80px -120px auto;
  width: 220px;
  height: 220px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.12);
}
.lobby-hero-copy { position: relative; z-index: 1; max-width: 680px; }
.lobby-hero-copy h1 {
  max-width: 620px;
  margin: 0 0 9px;
  color: #101a33;
  font-size: clamp(26px, 3.2vw, 40px);
  font-weight: 900;
  line-height: 1.12;
  letter-spacing: -0.045em;
}
.lobby-hero-copy p {
  max-width: 620px;
  margin: 0;
  color: #526173;
  font-size: 14px;
  line-height: 1.65;
}
.section-kicker {
  font-size: 11px;
  font-weight: 800;
  color: #059669;
  letter-spacing: 0.14em;
  margin: 0 0 7px;
  text-transform: uppercase;
}
.lobby-hero-actions { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 16px; }
.lobby-ghost-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 38px;
  padding: 8px 15px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: #334155;
  font-weight: 800;
  font-size: 13px;
  cursor: pointer;
  backdrop-filter: blur(10px);
  transition: all 0.18s ease;
}
.lobby-ghost-button:hover:not(:disabled) {
  transform: translateY(-1px);
  border-color: #cfeee2;
  color: #047857;
  box-shadow: 0 14px 30px rgba(16, 185, 129, 0.12);
}
.lobby-ghost-button:disabled { cursor: not-allowed; opacity: 0.48; }
.lobby-hero-panel {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 156px;
  padding: 12px;
  border: 1px solid #e5eaf2;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(18px);
}
.lobby-orbit {
  position: relative;
  height: 92px;
  border-radius: 18px;
  background:
    radial-gradient(circle at 22% 0%, rgba(16, 185, 129, 0.36), transparent 34%),
    linear-gradient(145deg, #101a33, #152238);
  overflow: hidden;
}
.zhida-brand-mark {
  position: absolute;
  inset: 50% auto auto 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
  width: 122px;
  height: 54px;
  padding: 4px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow:
    0 18px 34px rgba(15, 23, 42, 0.24),
    0 0 0 8px rgba(255, 255, 255, 0.08);
}
.zhida-brand-mark::before {
  content: "";
  position: absolute;
  inset: -8px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 22px;
}
.zhida-brand-mark img {
  position: relative;
  z-index: 1;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.zhida-brand-mark::after {
  content: "";
  position: absolute;
  inset: auto 15px -6px;
  height: 6px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.32);
  filter: blur(8px);
}
.lobby-orbit span {
  position: absolute;
  z-index: 2;
  padding: 5px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.78);
  font-size: 11px;
  font-weight: 800;
}
.lobby-orbit span:nth-child(1) { top: 14px; left: 18px; }
.lobby-orbit span:nth-child(2) { right: 18px; top: 32px; }
.lobby-orbit span:nth-child(3) { left: 12px; bottom: 10px; }
.lobby-stat-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-top: 10px; }
.lobby-stat-grid div {
  padding: 8px 9px;
  border-radius: 14px;
  background: #f8fafc;
}
.lobby-stat-grid span { display: block; color: #64748b; font-size: 11px; font-weight: 700; }
.lobby-stat-grid strong { color: #0f172a; font-size: 22px; font-weight: 900; }
.lobby-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 16px;
  align-items: start;
}
.lobby-side {
  position: sticky;
  top: 14px;
  display: grid;
  max-height: calc(100vh - 28px);
  gap: 12px;
  overflow: auto;
  padding-right: 2px;
}
.lobby-side::-webkit-scrollbar { width: 4px; }
.lobby-side::-webkit-scrollbar-thumb { border-radius: 999px; background: #dbe3ef; }
.lobby-create-card { scroll-margin-top: 18px; }
.lobby-selected-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}
.lobby-selected-strip div {
  min-width: 0;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.94);
  border: 1px solid #e5eaf2;
}
.lobby-selected-strip span {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 4px;
}
.lobby-selected-strip strong {
  display: block;
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.persona-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
}
.persona-section-head p { margin: 4px 0 0; color: #94a3b8; font-size: 12px; }
.section-head-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.soft-toggle-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 5px 11px;
  border: 1px solid #dbe3ef;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #475569;
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  transition: all 0.16s ease;
  white-space: nowrap;
}
.soft-toggle-btn:hover {
  border-color: #bbf7d0;
  background: #ecfdf5;
  color: #047857;
}
.add-persona-btn.compact { margin-top: 0; white-space: nowrap; }
.extra-collapsed-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  min-height: 38px;
  align-items: center;
  padding: 9px 11px;
  border: 1px solid #e5eaf2;
  border-radius: 16px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}
.persona-card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }
.lobby-plan-preview {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  padding: 10px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.94);
  border: 1px solid #e5eaf2;
}
.plan-step { display: flex; align-items: center; gap: 10px; }
.plan-step > span {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  background: #10b981;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
}
.plan-step strong { display: block; color: #0f172a; font-size: 13px; }
.plan-step small { display: block; color: #64748b; font-size: 12px; }
.plan-step.muted > span { background: #e2e8f0; color: #64748b; }
.plan-step.muted strong { color: #64748b; }
.plan-step.empty {
  padding: 8px;
  border: 1px dashed #cbd5e1;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.62);
}
.plan-step.empty > span { background: #f1f5f9; color: #94a3b8; }
.plan-step-actions {
  display: flex;
  gap: 4px;
  margin-left: auto;
}
.plan-step-actions button {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  font-weight: 900;
  line-height: 1;
}
.plan-step-actions button:hover:not(:disabled) {
  border-color: #10b981;
  color: #059669;
}
.plan-step-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.38;
}
.lobby-context-extra {
  display: grid;
  gap: 12px;
  margin-top: 14px;
  padding: 12px;
  border: 1px solid #e5eaf2;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
}
.lobby-context-extra.collapsed {
  gap: 8px;
}
.context-extra-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.lobby-context-extra h3 {
  margin: 0;
  color: #0f172a;
  font-size: 14px;
  font-weight: 900;
}
.lobby-context-extra p {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 12px;
}
.focus-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.focus-chip {
  border: 1px solid #dbe3ef;
  border-radius: 999px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
  padding: 6px 11px;
}
.focus-chip:hover {
  border-color: #cfeee2;
  color: #047857;
  background: #ecfdf5;
}
.focus-chip.active {
  border-color: #101a33;
  background: #101a33;
  color: #fff;
}
.supplement-field {
  display: grid;
  gap: 6px;
}
.supplement-field > span {
  color: #374151;
  font-size: 13px;
  font-weight: 800;
}
.start-hint { color: #64748b; font-size: 12px; }
.recent-interview-card {
  padding: 16px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #e5eaf2;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.05);
}
.recent-card-head span,
.recent-card-head strong {
  display: block;
}
.recent-card-head span {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
.recent-card-head strong {
  margin-top: 4px;
  color: #101a33;
  font-size: 18px;
  font-weight: 900;
}
.recent-record-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}
.recent-record {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border: 1px solid #e5eaf2;
  border-radius: 16px;
  background: #fff;
  cursor: pointer;
}
.recent-record:hover {
  border-color: #cfeee2;
  background: #f8fffb;
}
.recent-record strong,
.recent-record span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.recent-record strong { color: #101a33; font-size: 13px; }
.recent-record span { margin-top: 3px; color: #778398; font-size: 11px; font-weight: 700; }
.recent-record button {
  border: 0;
  border-radius: 999px;
  background: #f1f5f9;
  color: #334155;
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  padding: 6px 10px;
}
.recent-empty {
  margin: 12px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

/* 提示 */
.interview-preview-note { margin-bottom: 16px; border-radius: 12px; }

/* 上下文卡片 */
.interview-context-card {
  background: rgba(255, 255, 255, 0.96); border-radius: 24px; padding: 20px;
  box-shadow: 0 16px 46px rgba(15, 23, 42, 0.06); border: 1px solid #e5eaf2;
}
.context-heading { display: flex; gap: 14px; margin-bottom: 16px; align-items: flex-start; }
.context-step {
  width: 32px; height: 32px; border-radius: 10px; background: #10b981; color: #fff;
  display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 14px; flex-shrink: 0;
}
.context-heading h2 { font-size: 18px; font-weight: 700; color: #1e293b; margin: 0 0 2px; }
.context-heading p { font-size: 13px; color: #64748b; margin: 0; }
.context-selectors {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.context-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #e5eaf2;
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.82));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
}
.context-field > span {
  display: inline-flex;
  align-items: center;
  color: #475569;
  font-size: 12px;
  font-weight: 900;
}
.context-field :deep(.el-select__wrapper) {
  min-height: 38px;
  border-radius: 13px;
  background: #fff;
  box-shadow: 0 0 0 1px #dbe3ef inset;
}
.context-field :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #10b981 inset, 0 0 0 4px rgba(16, 185, 129, 0.08);
}
.bound-context-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px;
  border: 1px solid #dbeafe;
  border-radius: 20px;
  background:
    radial-gradient(circle at 0% 0%, rgba(16, 185, 129, 0.1), transparent 34%),
    linear-gradient(135deg, rgba(239, 246, 255, 0.78), rgba(255, 255, 255, 0.96));
}
.bound-context-main {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 12px;
}
.bound-context-fallback {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #101a33, #10b981);
  color: #fff;
  font-size: 20px;
  font-weight: 900;
}
.bound-context-label {
  display: inline-flex;
  margin-bottom: 4px;
  color: #059669;
  font-size: 12px;
  font-weight: 900;
}
.bound-context-main strong,
.bound-context-main p {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bound-context-main strong {
  max-width: 520px;
  color: #0f172a;
  font-size: 16px;
  font-weight: 900;
}
.bound-context-main p {
  max-width: 520px;
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}
.company-profile-pill {
  display: inline-grid;
  gap: 2px;
  margin-top: 8px;
  max-width: 520px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
  padding: 7px 10px;
}
.company-profile-pill span {
  color: #2563eb;
  font-size: 11px;
  font-weight: 900;
}
.company-profile-pill strong {
  color: #1e293b;
  font-size: 12px;
  font-weight: 850;
}
.bound-context-action {
  flex: 0 0 auto;
  min-height: 34px;
  padding: 7px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 999px;
  background: #fff;
  color: #334155;
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
}
.bound-context-action:hover {
  border-color: #bbf7d0;
  background: #ecfdf5;
  color: #047857;
}

/* 角色卡 */
.persona-section { margin-top: 20px; }
.persona-section h3 { font-size: 14px; font-weight: 800; color: #111827; margin: 0; }
.persona-cards {
  position: relative;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  max-height: 360px;
  gap: 10px;
  overflow: auto;
  padding-right: 2px;
}
.persona-cards.condensed {
  max-height: 292px;
  overflow: hidden;
  mask-image: linear-gradient(to bottom, #000 78%, rgba(0, 0, 0, 0.18));
}
.persona-cards::-webkit-scrollbar { width: 4px; }
.persona-cards::-webkit-scrollbar-thumb { border-radius: 999px; background: #dbe3ef; }
.persona-condensed-hint {
  margin: 8px 0 0;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 700;
}
.persona-card {
  min-height: 132px; padding: 12px; border-radius: 16px; border: 1px solid #e5eaf2;
  background: #fff; cursor: pointer; transition: all 0.2s; position: relative;
}
.persona-card:hover { border-color: #cfeee2; box-shadow: 0 14px 28px rgba(16, 185, 129, 0.1); transform: translateY(-1px); }
.persona-card.selected { border-color: #10b981; background: linear-gradient(180deg, #f0fdf4, #fff); box-shadow: 0 14px 30px rgba(16, 185, 129, 0.1); }
.persona-card.primary { outline: 2px solid rgba(16, 26, 51, 0.1); }
.persona-avatar {
  width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-weight: 800; font-size: 16px; color: #fff; margin-bottom: 8px;
}
.avatar-general { background: linear-gradient(135deg, #3b82f6, #8b5cf6); }
.avatar-architect { background: linear-gradient(135deg, #059669, #10b981); }
.avatar-hr { background: linear-gradient(135deg, #f59e0b, #f97316); }
.avatar-algorithm { background: linear-gradient(135deg, #6366f1, #8b5cf6); }
.avatar-product { background: linear-gradient(135deg, #ec4899, #f43f5e); }
.avatar-frontend { background: linear-gradient(135deg, #06b6d4, #3b82f6); }
.avatar-data { background: linear-gradient(135deg, #14b8a6, #0ea5e9); }
.avatar-startup { background: linear-gradient(135deg, #f97316, #ef4444); }
.avatar-foreign { background: linear-gradient(135deg, #1e3a5f, #3b82f6); }
.avatar-campus { background: linear-gradient(135deg, #22c55e, #a3e635); }
.avatar-pressure { background: linear-gradient(135deg, #ef4444, #dc2626); }
.avatar-friendly { background: linear-gradient(135deg, #f59e0b, #fbbf24); }
.avatar-custom { background: linear-gradient(135deg, #8b5cf6, #ec4899); }

.persona-info { display: flex; flex-direction: column; gap: 2px; }
.persona-name { font-weight: 700; font-size: 14px; color: #1f2937; }
.persona-title { font-size: 12px; color: #6b7280; }
.persona-style { font-size: 11px; color: #9ca3af; line-height: 1.4; margin-top: 4px; }
.persona-check { color: #10b981; font-size: 18px; }
.persona-delete-btn {
  display: grid; place-items: center;
  width: 22px; height: 22px; border: 0; border-radius: 8px;
  background: rgba(239, 68, 68, 0.08); color: #ef4444; cursor: pointer;
  font-size: 14px; transition: all 0.15s; flex-shrink: 0;
}
.persona-delete-btn:hover { background: rgba(239, 68, 68, 0.18); }
.persona-order-badge {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: #10b981;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
  box-shadow: 0 8px 18px rgba(16, 185, 129, 0.24);
}
.add-persona-btn {
  margin-top: 10px; display: inline-flex; align-items: center; gap: 4px; border: 1px solid #e5eaf2;
  border-radius: 999px; background: rgba(248, 250, 252, 0.9); color: #64748b; font-size: 12px; font-weight: 900; padding: 6px 12px; cursor: pointer;
}
.add-persona-btn:hover { border-color: #cfeee2; color: #047857; background: #ecfdf5; }
.context-start-row { margin-top: 16px; display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.interview-start-button {
  display: inline-flex; align-items: center; justify-content: center; gap: 7px; min-height: 42px; padding: 10px 24px; border: 0; border-radius: 999px;
  background: linear-gradient(135deg, #059669, #10b981); color: #fff; font-weight: 800; font-size: 15px; cursor: pointer;
  box-shadow: 0 14px 28px rgba(16, 185, 129, 0.22);
  transition: all 0.18s ease;
}
.interview-start-button:disabled { opacity: 0.5; cursor: not-allowed; }
.interview-start-button:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 18px 34px rgba(16, 185, 129, 0.28); }
/* 历史会话 */
.history-sessions-section {
  margin-top: 14px;
  padding: 20px;
  border: 1px solid #e5eaf2;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 46px rgba(15, 23, 42, 0.06);
}
.side-history-section {
  margin-top: 0;
  padding: 16px;
  border-radius: 22px;
}
.history-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}
.history-head h2 { margin: 0 0 4px; color: #0f172a; font-size: 22px; font-weight: 900; letter-spacing: -0.03em; }
.history-head p { margin: 0; color: #64748b; font-size: 13px; }
.history-filter-tabs { display: flex; gap: 8px; margin-bottom: 12px; }
.filter-tab {
  padding: 6px 14px; border: 0; border-radius: 999px; background: transparent;
  font-size: 12px; font-weight: 900; color: #778398; cursor: pointer; transition: all 0.15s;
}
.filter-tab:hover { background: #f1f5f9; color: #047857; }
.filter-tab.active { background: #101a33; color: #fff; }
.history-empty-card {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 180px;
  border: 1px dashed #cbd5e1;
  border-radius: 22px;
  color: #64748b;
  text-align: center;
  background: #f8fafc;
}
.history-empty-card .el-icon { font-size: 30px; color: #94a3b8; }
.history-empty-card strong { color: #0f172a; font-size: 16px; }
.history-empty-card span { max-width: 360px; font-size: 13px; line-height: 1.6; }
.history-record-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(282px, 1fr)); gap: 12px; margin-top: 12px; }
.history-record-card {
  display: flex; flex-direction: column; align-items: stretch; gap: 12px; padding: 14px; border-radius: 18px;
  border: 1px solid #e5eaf2; background: #fff; cursor: pointer; transition: all 0.2s;
}
.history-record-card:hover { border-color: #cfeee2; box-shadow: 0 14px 28px rgba(16, 185, 129, 0.1); transform: translateY(-1px); }
.side-history-section .history-head {
  gap: 10px;
  margin-bottom: 12px;
}
.side-history-section .history-head h2 {
  font-size: 18px;
}
.side-history-section .history-head p {
  font-size: 12px;
  line-height: 1.5;
}
.side-history-section .history-filter-tabs {
  overflow-x: auto;
  padding-bottom: 2px;
}
.side-history-section .filter-tab {
  flex-shrink: 0;
  padding: 6px 10px;
}
.side-history-section .history-record-grid {
  grid-template-columns: 1fr;
  gap: 10px;
  max-height: 46vh;
  overflow: auto;
  padding-right: 2px;
}
.side-history-section .history-record-grid::-webkit-scrollbar { width: 4px; }
.side-history-section .history-record-grid::-webkit-scrollbar-thumb { border-radius: 999px; background: #dbe3ef; }
.side-history-section .history-record-card {
  gap: 8px;
  padding: 10px;
  border-radius: 14px;
  min-width: 0;
  overflow: hidden;
}
.side-history-section .history-empty-card {
  min-height: 132px;
  border-radius: 18px;
}
.side-history-section .history-empty-card .el-icon {
  font-size: 24px;
}
.side-history-section .history-empty-card strong {
  font-size: 14px;
}
.side-history-section .record-round-list {
  max-height: 60px;
  overflow: auto;
}
.side-history-section .record-round-list span {
  font-size: 10px;
  padding: 3px 6px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.side-history-section .hsc-avatar {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  font-size: 12px;
}
.side-history-section .hsc-company-avatar {
  flex-shrink: 0;
  transform: scale(0.82);
  transform-origin: left center;
}
.side-history-section .hsc-name {
  font-size: 13px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.side-history-section .hsc-title {
  font-size: 11px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.side-history-section .hsc-status {
  font-size: 10px;
  padding: 3px 7px;
}
.side-history-section .hsc-open-button {
  min-height: 30px;
  font-size: 12px;
  border-radius: 10px;
  padding: 6px 10px;
}
.side-history-section .hsc-delete-button {
  width: 30px;
  height: 30px;
  border-radius: 10px;
}
.side-history-section .hsc-progress-line {
  font-size: 11px;
  gap: 6px;
}
.side-history-section .hsc-progress-line strong {
  font-size: 12px;
}
.side-history-section .hsc-main {
  gap: 8px;
  min-width: 0;
}
.side-history-section .hsc-info {
  min-width: 0;
  flex: 1;
}
.side-history-section .history-card-top {
  gap: 6px;
}
.side-history-section .record-actions {
  gap: 6px;
}
.history-card-top,
.hsc-main,
.hsc-progress-line { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.hsc-avatar {
  width: 42px; height: 42px; border-radius: 15px; background: linear-gradient(135deg, #0f172a, #10b981);
  display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 14px; color: #fff; flex-shrink: 0;
}
.hsc-company-avatar {
  flex-shrink: 0;
}
.hsc-info { display: flex; flex-direction: column; gap: 2px; flex: 1; min-width: 0; }
.hsc-name { font-weight: 700; font-size: 14px; color: #1f2937; }
.hsc-title { font-size: 12px; color: #6b7280; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hsc-status {
  font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: 6px;
  background: #f1f5f9; color: #64748b; flex-shrink: 0;
}
.hsc-status.completed { background: #f0fdf4; color: #10b981; }
.hsc-status.failed { background: #fef2f2; color: #ef4444; }
.hsc-status.cancelled { background: #fff7ed; color: #f97316; }
.hsc-progress-line span { color: #64748b; font-size: 12px; }
.hsc-progress-line strong { color: #0f172a; font-size: 13px; }
.record-round-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.record-round-list span {
  max-width: 100%;
  overflow: hidden;
  padding: 5px 8px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid #e5eaf2;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.record-round-list span.completed {
  background: #ecfdf5;
  border-color: #bbf7d0;
  color: #047857;
}
.record-round-list span.failed {
  background: #fef2f2;
  border-color: #fecaca;
  color: #b91c1c;
}
.record-round-list span.cancelled {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #c2410c;
}
.record-actions {
  display: flex;
  gap: 8px;
}
.hsc-open-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 34px;
  border: 0;
  border-radius: 12px;
  background: #f1f5f9;
  color: #0f172a;
  font-weight: 800;
  cursor: pointer;
  flex: 1;
}
.hsc-open-button:hover { background: #ecfdf5; color: #047857; }
.hsc-delete-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 12px;
  background: #fff1f2;
  color: #e11d48;
  cursor: pointer;
}
.hsc-delete-button:hover { background: #ffe4e6; }

/* 聊天布局 */
.interview-chat-layout {
  display: flex; gap: 0; border-radius: 24px; overflow: hidden;
  border: 1px solid #e5eaf2;
  box-shadow: 0 18px 54px rgba(15, 23, 42, 0.08); height: calc(100vh - 36px); min-height: 560px;
  background: #fff;
}
.workspace-return-bar + .interview-chat-layout {
  height: calc(100vh - 96px);
}

/* 当前面试侧栏 */
.chat-sidebar {
  width: 188px; flex-shrink: 0; padding: 12px; background: #fff;
  border-right: 1px solid #e5eaf2; display: flex; flex-direction: column; align-items: stretch; gap: 10px;
  overflow-y: auto;
}
.sidebar-back-btn {
  display: flex; align-items: center; justify-content: center; width: 32px; height: 32px;
  border: 0; border-radius: 999px; background: transparent; cursor: pointer; color: #334155;
  transition: all 0.2s; flex-shrink: 0;
}
.sidebar-back-btn:hover:not(:disabled) { color: #047857; background: #f1f5f9; }
.sidebar-back-btn:disabled { cursor: not-allowed; color: #cbd5e1; background: #f8fafc; }
.sidebar-persona-card,
.sidebar-question-card {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid #e5eaf2;
  border-radius: 18px;
  background: #f8fafc;
}
.sidebar-persona-avatar {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 16px;
  color: #fff;
  font-weight: 900;
}
.sidebar-persona-card span,
.sidebar-question-card span {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 900;
}
.sidebar-persona-card strong,
.sidebar-question-card strong {
  color: #101a33;
  font-size: 15px;
  font-weight: 900;
}
.sidebar-persona-card small,
.sidebar-question-card small {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}
.sidebar-persona-card p {
  margin: 2px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.55;
}

/* 题目进度点 */
.sidebar-dots { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
.sidebar-dot {
  width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  border: 2px solid #cbd5e1; font-size: 11px; font-weight: 800; color: #94a3b8; background: #fff;
}
.sidebar-dot.completed { border-color: #10b981; color: #fff; background: #10b981; }
.sidebar-dot.active { border-color: #101a33; color: #101a33; background: #f8fafc; }
.sidebar-dot.viewing { border-color: #f59e0b; color: #f59e0b; background: #fef3c7; }
.chat-sidebar > small { font-size: 11px; color: #94a3b8; text-align: center; }
.sidebar-completed { color: #10b981 !important; font-weight: 700; }
.sidebar-round-card {
  display: grid;
  gap: 6px;
  padding: 10px;
  border: 1px solid #e5eaf2;
  border-radius: 18px;
  background: #fff;
}
.sidebar-round-card > span {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 900;
}
.round-switch-button {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  grid-template-areas:
    "num name"
    "num status";
  gap: 0 8px;
  align-items: center;
  width: 100%;
  padding: 8px;
  border: 1px solid #e5eaf2;
  border-radius: 14px;
  background: #f8fafc;
  color: #334155;
  text-align: left;
  cursor: pointer;
}
.round-switch-button:hover:not(:disabled) { border-color: #cbd5e1; background: #fff; }
.round-switch-button.active { border-color: #101a33; background: #f1f5f9; }
.round-switch-button.completed i { background: #10b981; color: #fff; }
.round-switch-button.failed i { background: #ef4444; color: #fff; }
.round-switch-button.cancelled i { background: #f97316; color: #fff; }
.round-switch-button i {
  grid-area: num;
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #64748b;
  font-style: normal;
  font-size: 11px;
  font-weight: 900;
}
.round-switch-button strong {
  grid-area: name;
  min-width: 0;
  overflow: hidden;
  color: #101a33;
  font-size: 12px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.round-switch-button small {
  grid-area: status;
  color: #94a3b8;
  font-size: 10px;
  font-weight: 800;
}

/* 聊天主区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}
.chat-plan-header {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px 16px;
  padding: 12px 16px;
  border-bottom: 1px solid #e5eaf2;
  background: rgba(255, 255, 255, 0.94);
}
.chat-plan-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  min-width: 140px;
}
.chat-plan-status strong {
  display: block;
  color: #0f172a;
  font-size: 13px;
  font-weight: 900;
}
.chat-plan-status span {
  max-width: 220px;
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-plan-kicker {
  display: block;
  margin-bottom: 2px;
  color: #059669;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
.chat-plan-header strong {
  display: block;
  overflow: hidden;
  color: #0f172a;
  font-size: 15px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-plan-header p {
  margin: 3px 0 0;
  color: #64748b;
  font-size: 12px;
}
.chat-plan-tags {
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 6px;
}
.chat-plan-tags span {
  border-radius: 999px;
  background: #ecfdf5;
  color: #059669;
  font-size: 11px;
  font-weight: 800;
  padding: 4px 8px;
}
.chat-company-focus {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
  padding: 7px 10px;
}
.chat-company-focus span {
  flex: 0 0 auto;
  color: #2563eb;
  font-size: 11px;
  font-weight: 900;
}
.chat-company-focus strong {
  min-width: 0;
  overflow: hidden;
  color: #334155;
  font-size: 12px;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-plan-steps {
  grid-column: 1 / -1;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}
.chat-plan-steps span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
  padding: 5px 9px 5px 5px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #fff;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}
.chat-plan-steps i {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  font-style: normal;
  font-size: 10px;
}
.chat-plan-steps span.done {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #059669;
}
.chat-plan-steps span.done i {
  background: #10b981;
  color: #fff;
}
.chat-plan-steps span.current {
  border-color: #101a33;
  background: #101a33;
  color: #fff;
}
.chat-plan-steps span.current i {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}
.chat-plan-note {
  grid-column: 1 / -1;
  padding: 8px 10px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
}
.plan-review-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  margin: 8px 18px 0;
  padding: 8px 10px 8px 12px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.035);
  backdrop-filter: blur(10px);
}
.plan-review-head {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 10px;
}
.plan-review-head strong {
  display: inline-block;
  margin: 0;
  color: #0f172a;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}
.plan-review-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}
.plan-review-score {
  display: flex;
  align-items: baseline;
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #0f172a;
}
.plan-review-score span {
  font-size: 15px;
  font-weight: 950;
  letter-spacing: -0.04em;
}
.plan-review-score small {
  margin-left: 2px;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}
.plan-review-insight {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 6px;
  min-width: 0;
  padding: 0;
  border-radius: 0;
  background: #f8fafc;
  border: 0;
}
.plan-review-insight span {
  display: inline-flex;
  align-items: center;
  max-width: 128px;
  padding: 4px 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 11px;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.plan-review-insight p {
  flex-basis: 100%;
  margin: 0;
  color: #64748b;
  font-size: 12px;
}
.round-review-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
  margin-top: 12px;
}
.round-review-card {
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
}
.round-review-card.completed {
  border-color: rgba(16, 185, 129, 0.26);
}
.round-review-card > span {
  color: #10a078;
  font-size: 11px;
  font-weight: 900;
}
.round-review-card > strong {
  display: block;
  margin-top: 4px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 900;
}
.round-review-card > small {
  display: block;
  margin-top: 2px;
  color: #64748b;
  font-size: 11px;
}
.round-review-score {
  margin-top: 10px;
}
.round-review-score b {
  color: #0f172a;
  font-size: 22px;
  letter-spacing: -0.04em;
}
.round-review-score em {
  color: #94a3b8;
  font-size: 12px;
  font-style: normal;
}
.round-review-score p,
.round-review-pending {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 11px;
}
.plan-review-ai-summary {
  margin-top: 12px;
  padding: 13px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #fff;
}
.plan-review-ai-summary h4 {
  margin: 0 0 6px;
  color: #0f172a;
  font-size: 14px;
}
.plan-review-ai-summary p {
  margin: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}
.plan-review-list {
  margin-top: 10px;
}
.plan-review-list span {
  color: #0f172a;
  font-size: 12px;
  font-weight: 900;
}
.plan-review-list ul {
  margin: 6px 0 0;
  padding-left: 18px;
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}
.plan-review-generate {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin: 0;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #0f172a;
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  padding: 6px 10px;
}
.plan-review-generate:hover:not(:disabled) { background: #f1f5f9; }
.plan-review-generate:disabled {
  cursor: not-allowed;
  opacity: 0.68;
}
.plan-review-dialog :deep(.el-dialog) {
  border-radius: 24px;
}
.plan-review-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 68vh;
  overflow-y: auto;
  padding-right: 4px;
}
.plan-review-dialog-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px;
  border: 1px solid #e2e8f0;
  border-radius: 22px;
  background:
    radial-gradient(circle at 100% 0, rgba(16, 185, 129, 0.12), transparent 34%),
    linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}
.plan-review-dialog-hero h3 {
  margin: 4px 0 6px;
  color: #0f172a;
  font-size: 20px;
  font-weight: 950;
}
.plan-review-dialog-hero p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}
.plan-review-dialog-score {
  display: flex;
  align-items: baseline;
  flex-shrink: 0;
  padding: 12px 16px;
  border-radius: 18px;
  background: #101a33;
  color: #fff;
}
.plan-review-dialog-score strong {
  font-size: 34px;
  line-height: 1;
  letter-spacing: -0.05em;
}
.plan-review-dialog-score span {
  margin-left: 3px;
  color: rgba(255, 255, 255, 0.64);
  font-size: 14px;
}
.plan-review-dialog-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}
.plan-review-dialog-metric {
  padding: 13px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #fff;
}
.plan-review-dialog-metric span {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}
.plan-review-dialog-metric strong {
  display: block;
  margin: 6px 0 8px;
  color: #0f172a;
  font-size: 22px;
  letter-spacing: -0.04em;
}
.plan-review-dialog-metric div {
  height: 7px;
  overflow: hidden;
  border-radius: 999px;
  background: #edf2f7;
}
.plan-review-dialog-metric i {
  display: block;
  height: 100%;
  border-radius: inherit;
}
.dialog-round-review-grid {
  margin-top: 0;
}
.plan-review-empty-summary {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 22px;
  border: 1px dashed #cbd5e1;
  border-radius: 18px;
  color: #64748b;
  background: #f8fafc;
}
.plan-review-empty-summary .el-icon {
  color: #10b981;
  font-size: 24px;
}
.plan-review-empty-summary strong {
  color: #0f172a;
  font-size: 15px;
}
.plan-review-empty-summary span {
  font-size: 12px;
}
.chat-messages {
  flex: 1 1 0;
  min-height: 0;
  overflow-y: auto;
  padding: 18px 22px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f8fafc;
}
.chat-message { display: flex; gap: 8px; max-width: 80%; position: relative; }
.chat-message.interviewer { align-self: flex-start; flex-direction: row; }
.chat-message.user { align-self: flex-end; flex-direction: row-reverse; }
.chat-message.sending { align-self: flex-end; flex-direction: row-reverse; flex-wrap: wrap; }

/* 消息头像 */
.msg-avatar {
  width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-weight: 800; font-size: 14px; color: #fff; flex-shrink: 0;
}
.user-avatar { background: #101a33; }

/* 消息气泡 */
.msg-bubble { max-width: 100%; padding: 10px 14px; border-radius: 12px; line-height: 1.5; font-size: 14px; }
.interviewer-bubble { background: #fff; color: #1e293b; border: 1px solid #e5eaf2; border-top-left-radius: 4px; }
.user-bubble { background: #101a33; color: #fff; border-top-right-radius: 4px; }
.bubble-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.bubble-name { font-weight: 700; font-size: 12px; color: #64748b; }
.bubble-question-num { font-size: 11px; color: #94a3b8; }
.bubble-text { white-space: pre-wrap; word-break: break-word; }

/* 发送中指示器 */
.sending-indicator {
  display: flex; align-items: center; gap: 6px; margin-top: 4px; margin-right: 4px;
  font-size: 12px; color: #6b7280; align-self: flex-end;
}
.sending-indicator .el-icon { font-size: 14px; color: #10b981; }

/* 内联评价 */
.evaluation-inline {
  align-self: stretch; max-width: 100%; margin: 4px 0; padding: 14px 16px;
  border-radius: 16px; background: #fff; border: 1px solid #e5eaf2;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.04);
}
.eval-header { display: flex; align-items: center; gap: 6px; font-weight: 700; font-size: 14px; color: #1f2937; margin-bottom: 10px; }
.eval-overall-card {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 2px 12px;
  align-items: center;
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}
.eval-overall-card span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}
.eval-overall-card strong {
  grid-row: span 2;
  color: #0f172a;
  font-size: 24px;
  font-weight: 950;
  letter-spacing: -0.04em;
}
.eval-overall-card small {
  color: #94a3b8;
  font-size: 12px;
}
.eval-overall-card p {
  margin: 0;
  color: #64748b;
  font-size: 12px;
}
.eval-score-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 10px; }
.eval-score-item { display: flex; flex-direction: column; gap: 3px; }
.eval-score-item > span { font-size: 11px; color: #6b7280; }
.eval-score-item > strong { font-size: 12px; color: #1f2937; text-align: center; }
.eval-section { margin-top: 8px; }
.eval-section h4 { font-size: 12px; font-weight: 700; color: #374151; margin-bottom: 4px; display: flex; align-items: center; gap: 4px; }
.eval-section ul { margin: 0; padding-left: 16px; font-size: 13px; color: #4b5563; }
.eval-section p { font-size: 13px; color: #4b5563; margin: 0; }
.ref-answer { background: #f0fdf4; border-radius: 8px; padding: 8px 12px; margin-top: 8px; }

/* 内联总结 */
.summary-inline {
  align-self: stretch; max-width: 100%; padding: 20px; border-radius: 18px;
  background:
    radial-gradient(circle at 0% 0%, rgba(16, 185, 129, 0.12), transparent 30%),
    #fff;
  border: 1px solid #e5eaf2;
  box-shadow: 0 16px 38px rgba(15, 23, 42, 0.06);
}
.summary-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.summary-header h3 { font-size: 18px; font-weight: 800; color: #101a33; margin: 0; }
.summary-desc { font-size: 13px; color: #64748b; margin-bottom: 14px; }
.summary-block { margin-bottom: 12px; }
.summary-block h4 { font-size: 14px; font-weight: 700; color: #1f2937; margin-bottom: 6px; }
.summary-block span { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #4b5563; margin-bottom: 4px; }
.summary-scores { margin-top: 12px; }
.summary-scores h4 { font-size: 14px; font-weight: 700; color: #1f2937; margin-bottom: 8px; }
.round-score-overview {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;
  padding: 12px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background: linear-gradient(135deg, #eff6ff, #ffffff);
}
.round-score-main {
  display: flex;
  align-items: baseline;
  justify-content: center;
  min-width: 70px;
  color: #101a33;
}
.round-score-main span {
  font-size: 30px;
  font-weight: 950;
  letter-spacing: -0.06em;
}
.round-score-main small {
  margin-left: 2px;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}
.round-score-copy strong {
  display: block;
  color: #0f172a;
  font-size: 13px;
  font-weight: 900;
}
.round-score-copy p {
  margin: 3px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}
.summary-score-cards { display: flex; flex-direction: column; gap: 6px; }
.summary-score-card { display: flex; align-items: center; gap: 10px; padding: 8px 12px; border-radius: 8px; background: #fff; border: 1px solid #e5e7eb; }
.sq-label { font-weight: 700; font-size: 13px; color: #374151; min-width: 50px; }
.sq-dims { display: flex; gap: 12px; font-size: 12px; color: #6b7280; }
.summary-actions-row { margin-top: 14px; }
.review-inline-hint {
  display: inline-flex;
  padding: 8px 12px;
  border-radius: 999px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}
.interview-primary-button {
  display: inline-flex; align-items: center; gap: 6px; padding: 10px 20px; border: 0; border-radius: 999px;
  background: #101a33; color: #fff; font-weight: 800; font-size: 14px; cursor: pointer;
}
.interview-primary-button:disabled { opacity: 0.5; cursor: not-allowed; }
.interview-primary-button:hover:not(:disabled) { background: #0f172a; }
.interview-secondary-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: 0;
  border-radius: 999px;
  background: #10b981;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 800;
}
.interview-secondary-button:disabled { opacity: 0.5; cursor: not-allowed; }
.interview-secondary-button:hover:not(:disabled) { background: #059669; }
.interview-outline-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border: 1px solid #e5eaf2;
  border-radius: 999px;
  background: #fff;
  color: #0f172a;
  cursor: pointer;
  font-size: 14px;
  font-weight: 800;
}
.interview-outline-button:disabled { opacity: 0.5; cursor: not-allowed; }
.interview-outline-button:hover:not(:disabled) { border-color: #cfeee2; color: #047857; background: #ecfdf5; }

/* 输入栏 */
.chat-input-bar {
  display: flex; align-items: flex-end; gap: 8px; padding: 12px 16px;
  border-top: 1px solid #e5eaf2; background: rgba(255, 255, 255, 0.96);
}
.chat-input-bar .voice-row { display: flex; align-items: center; flex-shrink: 0; }
.chat-textarea { flex: 1; }
.chat-send-btn {
  flex-shrink: 0; padding: 9px 18px; border: 0; border-radius: 999px; background: #10b981;
  color: #fff; font-weight: 800; font-size: 14px; cursor: pointer;
}
.chat-send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.chat-send-btn:hover:not(:disabled) { background: #059669; }
.review-mode-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #e5eaf2;
  background: rgba(248, 250, 252, 0.96);
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}
.review-mode-bar .el-icon { color: #10b981; font-size: 18px; }
.review-mode-bar span { flex: 1; }

/* 语音 */
.voice-button {
  display: flex; align-items: center; justify-content: center; width: 36px; height: 36px;
  border: 1px solid #e5eaf2; border-radius: 999px; background: #fff; cursor: pointer; transition: all 0.2s;
}
.voice-button:hover:not(:disabled) { border-color: #cfeee2; color: #047857; background: #ecfdf5; }
.voice-button:disabled { opacity: 0.5; cursor: not-allowed; }
.voice-button.listening { border-color: #ef4444; color: #ef4444; background: #fef2f2; }
.voice-button .is-pulsing { animation: voice-pulse-icon 1s ease-in-out infinite; }
.voice-hint { font-size: 11px; color: #ef4444; font-weight: 600; white-space: nowrap; }
@keyframes voice-pulse-icon { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.2); } }

/* 重试卡片 */
.retry-card {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 8px;
  padding: 12px 16px; border-radius: 10px; background: #fef3c7; border: 1px solid #fcd34d; color: #92400e; font-size: 14px;
}
.retry-card .retry-button { margin-left: auto; }
.retry-message { display: flex; align-items: center; gap: 8px; width: 100%; }
.retry-answer-preview {
  width: 100%; margin-top: 8px; padding: 8px 12px; border-radius: 8px; background: #fffbeb; border: 1px solid #fde68a;
}
.retry-answer-label { font-size: 12px; color: #92400e; font-weight: 600; }
.retry-answer-preview p { margin: 4px 0 0; font-size: 13px; color: #78350f; line-height: 1.5; max-height: 80px; overflow-y: auto; }

/* 加载条（设置界面用） */
.interview-loading-bar {
  display: flex; align-items: center; gap: 10px; padding: 10px 16px; margin-bottom: 14px;
  border-radius: 16px; background: #ecfdf5; border: 1px solid #bbf7d0; color: #047857; font-size: 14px; font-weight: 700;
}
.interview-loading-bar .elapsed-time { margin-left: auto; color: #6b7280; font-weight: 400; font-size: 13px; }

/* 题目数量拖拽条 */
.question-count-slider {
  margin-top: 12px;
}
.question-slider-shell {
  display: grid;
  grid-template-columns: auto minmax(160px, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  min-height: 42px;
  padding: 2px 2px 2px 4px;
}
.question-slider-shell :deep(.el-slider) {
  --el-slider-main-bg-color: #10b981;
  --el-slider-runway-bg-color: #e2e8f0;
  --el-slider-stop-bg-color: rgba(255, 255, 255, 0.96);
}
.question-slider-shell :deep(.el-slider__runway) {
  height: 8px;
  border-radius: 999px;
}
.question-slider-shell :deep(.el-slider__bar) {
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, #059669, #10b981);
}
.question-slider-shell :deep(.el-slider__button) {
  width: 18px;
  height: 18px;
  border: 4px solid #fff;
  background: #10b981;
  box-shadow: 0 7px 18px rgba(16, 185, 129, 0.28);
}
.slider-limit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 900;
}
.slider-value {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  min-height: 32px;
  border-radius: 999px;
  background: #101a33;
  color: #fff;
  font-size: 13px;
  font-weight: 900;
  text-align: center;
}

@media (max-width: 1180px) {
  .interview-lobby-hero,
  .lobby-shell {
    grid-template-columns: 1fr;
  }
  .lobby-side {
    position: static;
    max-height: none;
    overflow: visible;
    padding-right: 0;
  }
  .side-history-section .history-record-grid {
    max-height: none;
  }
  .lobby-hero-panel {
    max-width: 520px;
  }
}

@media (max-width: 760px) {
  .interview-lobby-hero {
    padding: 22px;
    border-radius: 22px;
  }
  .lobby-hero-copy h1 {
    font-size: 34px;
  }
  .lobby-selected-strip,
  .context-selectors {
    grid-template-columns: 1fr;
  }
  .bound-context-card {
    align-items: stretch;
    flex-direction: column;
  }
  .bound-context-action {
    width: 100%;
  }
  .persona-cards,
  .history-record-grid {
    grid-template-columns: 1fr;
  }
  .history-head,
  .lobby-hero-actions,
  .context-start-row {
    align-items: stretch;
    flex-direction: column;
  }
  .interview-start-button,
  .lobby-ghost-button {
    width: 100%;
  }
  .question-count-slider {
    min-width: 0;
  }
  .chat-plan-header {
    grid-template-columns: 1fr;
  }
  .chat-plan-tags {
    justify-content: flex-start;
  }
}

/* ── 成长趋势 ── */

.growth-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.growth-header {
  display: flex;
  gap: 24px;
}

.growth-header-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.growth-header-info strong {
  font-size: 15px;
  color: #101a33;
}

.growth-header-info small {
  font-size: 12px;
  color: #94a3b8;
}

.growth-header-label {
  font-size: 11px;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.growth-timeline {
  padding: 16px 0;
}

.growth-timeline-line {
  display: flex;
  align-items: center;
  position: relative;
  gap: 0;
}

.growth-timeline-line::before {
  content: '';
  position: absolute;
  top: 12px;
  left: 0;
  right: 0;
  height: 2px;
  background: #e5e7eb;
  z-index: 0;
}

.growth-timeline-dot {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  position: relative;
  z-index: 1;
}

.growth-timeline-dot-inner {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #e5e7eb;
  border: 2px solid white;
  transition: all 0.2s;
}

.growth-timeline-dot.active .growth-timeline-dot-inner {
  background: #101a33;
  width: 14px;
  height: 14px;
}

.growth-timeline-label {
  font-size: 11px;
  color: #64748b;
  text-align: center;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.growth-timeline-dot.active .growth-timeline-label {
  color: #101a33;
  font-weight: 600;
}

.growth-section-title {
  font-size: 14px;
  color: #101a33;
  margin: 0 0 12px 0;
}

.growth-line-chart {
  width: 100%;
  height: auto;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  background: #fafbfc;
}

.growth-legend {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.growth-legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #64748b;
}

.growth-legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.growth-changes-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.growth-change-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 8px;
  background: #f8fafc;
  border-radius: 10px;
  gap: 4px;
}

.growth-change-label {
  font-size: 11px;
  color: #94a3b8;
}

.growth-change-value {
  font-size: 18px;
  font-weight: 700;
  color: #94a3b8;
}

.growth-change-positive {
  color: #10b981;
}

.growth-change-negative {
  color: #ef4444;
}

.growth-snapshots {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.growth-snapshot-card {
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.growth-snapshot-header {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.growth-snapshot-header strong {
  font-size: 14px;
  color: #101a33;
}

.growth-snapshot-badge {
  font-size: 11px;
  color: #64748b;
  background: #e5e7eb;
  padding: 2px 8px;
  border-radius: 6px;
}

.growth-snapshot-scores {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.growth-snapshot-score {
  display: flex;
  align-items: center;
  gap: 8px;
}

.growth-snapshot-score-label {
  font-size: 11px;
  color: #64748b;
  width: 72px;
  flex-shrink: 0;
}

.growth-snapshot-score-bar-wrapper {
  flex: 1;
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.growth-snapshot-score-bar {
  display: block;
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s;
}

.growth-snapshot-score-value {
  font-size: 12px;
  font-weight: 600;
  color: #101a33;
  width: 32px;
  text-align: right;
  flex-shrink: 0;
}

.growth-snapshot-summary {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
  padding-top: 4px;
  border-top: 1px solid #e5e7eb;
}

.growth-snapshot-summary span {
  font-weight: 600;
  color: #101a33;
}

.growth-single-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px;
  color: #94a3b8;
}

.growth-single-hint .el-icon {
  font-size: 32px;
  color: #cbd5e1;
}

.growth-single-hint p {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.growth-single-hint small {
  font-size: 12px;
}

.growth-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px;
  color: #94a3b8;
  font-size: 14px;
}

.growth-loading .el-icon {
  font-size: 28px;
}

@media (max-width: 640px) {
  .growth-changes-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .growth-header {
    flex-direction: column;
    gap: 10px;
  }
}
</style>
