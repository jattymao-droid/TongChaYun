<template>
  <div v-if="showActions" class="biz-side-actions" :class="{ collapse: collapse }">
    <el-dropdown trigger="click" placement="bottom-start" @command="onCreate">
      <el-button type="primary" class="btn-create" :class="{ mini: collapse }" :circle="collapse">
        <i class="el-icon-plus"></i>
        <span class="btn-label">新建</span>
      </el-button>
      <el-dropdown-menu slot="dropdown">
        <el-dropdown-item v-if="canQuery" command="query" icon="el-icon-search">新建查询</el-dropdown-item>
        <el-dropdown-item v-if="canSurvey" command="survey" icon="el-icon-document">新建问卷</el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>

    <el-dropdown trigger="click" placement="bottom-start" @command="onTemplate">
      <button type="button" class="btn-tpl" :class="{ mini: collapse }" :title="collapse ? '从模板创建' : undefined">
        <i class="el-icon-document-copy"></i>
        <span class="btn-label">从模板创建</span>
      </button>
      <el-dropdown-menu slot="dropdown">
        <el-dropdown-item v-if="canQuery" command="query">查询模板</el-dropdown-item>
        <el-dropdown-item v-if="canSurvey" command="survey">问卷模板</el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
  </div>
</template>

<script>
import { checkPermi } from '@/utils/permission'

export default {
  name: 'BizSidebarActions',
  props: {
    collapse: { type: Boolean, default: false }
  },
  computed: {
    canQuery() { return checkPermi(['biz:query:add']) },
    canSurvey() { return checkPermi(['biz:survey:add']) },
    showActions() { return this.canQuery || this.canSurvey }
  },
  methods: {
    onCreate(cmd) {
      if (cmd === 'query') this.$router.push({ path: '/biz/query', query: { action: 'create' } })
      else if (cmd === 'survey') this.$router.push({ path: '/biz/survey', query: { action: 'create' } })
    },
    onTemplate(cmd) {
      if (cmd === 'query') this.$router.push({ path: '/biz/query', query: { action: 'template' } })
      else if (cmd === 'survey') this.$router.push({ path: '/biz/survey', query: { action: 'template' } })
    }
  }
}
</script>

<style scoped>
.biz-side-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px 8px;
  transition: padding 0.28s cubic-bezier(0.4, 0, 0.2, 1), gap 0.28s ease;
}
.biz-side-actions.collapse {
  align-items: center;
  padding: 12px 0 8px;
  gap: 8px;
}
.btn-create {
  width: 100%;
  height: 40px;
  border-radius: 8px !important;
  background: var(--biz-accent) !important;
  border-color: var(--biz-accent) !important;
  font-weight: 600;
  overflow: hidden;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1), height 0.28s ease, border-radius 0.28s ease !important;
}
.btn-create.mini {
  width: 36px;
  height: 36px;
  min-width: 36px;
  padding: 0 !important;
  border-radius: 50% !important;
}
.btn-create .btn-label {
  display: inline-block;
  max-width: 80px;
  margin-left: 4px;
  overflow: hidden;
  white-space: nowrap;
  vertical-align: middle;
  opacity: 1;
  transition: max-width 0.24s ease, opacity 0.18s ease, margin 0.24s ease;
}
.btn-create.mini .btn-label {
  max-width: 0;
  margin-left: 0;
  opacity: 0;
}
.btn-tpl {
  width: 100%;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 8px;
  border: 1px solid transparent;
  background:
    linear-gradient(#fff, #fff) padding-box,
    linear-gradient(90deg, #5b8def, #7b61ff, #39c5bb) border-box;
  color: #3a4a6b;
  font-size: 14px;
  cursor: pointer;
  overflow: hidden;
  transition:
    width 0.28s cubic-bezier(0.4, 0, 0.2, 1),
    height 0.28s ease,
    border-radius 0.28s ease,
    background 0.18s ease;
}
.btn-tpl.mini {
  width: 36px;
  height: 36px;
  gap: 0;
  border: 1px solid #dce3f0;
  background: #fff;
}
.btn-tpl:hover {
  background:
    linear-gradient(#f7f9ff, #f7f9ff) padding-box,
    linear-gradient(90deg, #5b8def, #7b61ff, #39c5bb) border-box;
}
.btn-tpl.mini:hover {
  border-color: var(--biz-accent);
  color: var(--biz-accent);
  background: var(--biz-accent-soft);
}
.btn-tpl .btn-label {
  display: inline-block;
  max-width: 120px;
  overflow: hidden;
  white-space: nowrap;
  opacity: 1;
  transition: max-width 0.24s ease, opacity 0.18s ease;
}
.btn-tpl.mini .btn-label {
  max-width: 0;
  opacity: 0;
}
</style>
