<template>
  <div v-if="showActions" class="biz-side-actions" :class="{ collapse: collapse }">
    <el-dropdown v-if="!collapse" trigger="click" placement="bottom-start" @command="onCreate">
      <el-button type="primary" class="btn-create" icon="el-icon-plus">新建</el-button>
      <el-dropdown-menu slot="dropdown">
        <el-dropdown-item v-if="canQuery" command="query" icon="el-icon-search">新建查询</el-dropdown-item>
        <el-dropdown-item v-if="canSurvey" command="survey" icon="el-icon-document">新建问卷</el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
    <el-dropdown v-else trigger="click" placement="bottom-start" @command="onCreate">
      <el-button type="primary" class="btn-create-mini" icon="el-icon-plus" circle></el-button>
      <el-dropdown-menu slot="dropdown">
        <el-dropdown-item v-if="canQuery" command="query">新建查询</el-dropdown-item>
        <el-dropdown-item v-if="canSurvey" command="survey">新建问卷</el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>

    <el-dropdown v-if="!collapse" trigger="click" placement="bottom-start" @command="onTemplate">
      <button type="button" class="btn-tpl">
        <i class="el-icon-document-copy"></i>
        <span>从模板创建</span>
      </button>
      <el-dropdown-menu slot="dropdown">
        <el-dropdown-item v-if="canQuery" command="query">查询模板</el-dropdown-item>
        <el-dropdown-item v-if="canSurvey" command="survey">问卷模板</el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
    <el-dropdown v-else trigger="click" placement="bottom-start" @command="onTemplate">
      <button type="button" class="btn-tpl-mini" title="从模板创建">
        <i class="el-icon-document-copy"></i>
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
}
.biz-side-actions.collapse {
  align-items: center;
  padding: 12px 0 8px;
}
.btn-create {
  width: 100%;
  height: 40px;
  border-radius: 8px !important;
  background: #2b6de5 !important;
  border-color: #2b6de5 !important;
  font-weight: 600;
}
.btn-create-mini {
  background: #2b6de5 !important;
  border-color: #2b6de5 !important;
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
}
.btn-tpl:hover {
  background:
    linear-gradient(#f7f9ff, #f7f9ff) padding-box,
    linear-gradient(90deg, #5b8def, #7b61ff, #39c5bb) border-box;
}
.btn-tpl-mini {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid #dce3f0;
  background: #fff;
  color: #3a4a6b;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}
.btn-tpl-mini:hover {
  border-color: #2b6de5;
  color: #2b6de5;
  background: #f5f8ff;
}
</style>
