<template>
  <router-view v-if="immersiveRoute" />

  <el-container v-else class="product-shell">
    <el-header class="product-header">
      <RouterLink class="product-brand" :to="{ name: 'home' }">
        <span class="product-brand-mark">职</span>
        <span>
          <strong>职达</strong>
          <small>ResumeGo</small>
        </span>
      </RouterLink>

      <nav class="product-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.name"
          :to="item.to"
          class="product-nav-item"
          :class="{ active: $route.name === item.routeName }"
        >
          {{ item.name }}
        </RouterLink>
      </nav>

      <div class="product-actions">
        <div class="product-search">
          <el-icon><Search /></el-icon>
          <span>搜索岗位 / 公司 / 技能</span>
        </div>
        <button class="icon-button" type="button" aria-label="资源中心">
          <el-icon><Present /></el-icon>
        </button>
        <button class="icon-button has-dot" type="button" aria-label="通知">
          <el-icon><Bell /></el-icon>
        </button>
        <button class="user-pill" type="button">
          <span>L</span>
        </button>
      </div>
    </el-header>

    <el-main class="product-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Bell, Present, Search } from '@element-plus/icons-vue'

const route = useRoute()
const immersiveRoute = computed(() => route.name === 'home' || route.query.from === 'editor')

const navItems = [
  { name: '工作台', routeName: 'home', to: { name: 'home' } },
  { name: '岗位探索', routeName: 'jobs', to: { name: 'jobs' } },
  { name: '简历', routeName: 'resumes', to: { name: 'resumes' } },
  { name: '面试准备', routeName: 'interview', to: { name: 'interview' } },
]
</script>
