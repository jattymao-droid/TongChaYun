<template>
  <div :class="embedded ? 'fields-embed' : 'biz-page'">
    <div v-if="!embedded" class="biz-page-head">
      <el-page-header @back="goBack" :content="'字段配置 - ' + (detail.query && detail.query.queryName || '')" />
    </div>
    <div :class="embedded ? '' : 'biz-panel'">
    <el-alert title="可配置查询条件（必填/选填组合）、匹配方式、结果列与脱敏。公开查询至少填一项条件，必填项需全部填写。" type="info" :closable="false" show-icon class="mb12" />
    <el-table :data="fields" v-loading="loading" border>
      <el-table-column label="键" prop="fieldKey" width="70" align="center" />
      <el-table-column label="原表头" prop="fieldName" min-width="110" :show-overflow-tooltip="true" />
      <el-table-column label="显示名" min-width="120">
        <template slot-scope="scope">
          <el-input v-model="scope.row.fieldLabel" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="查询条件" width="80" align="center">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.isQuery" active-value="1" inactive-value="0" @change="onQueryToggle(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="必填" width="70" align="center">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.isRequired" active-value="1" inactive-value="0" :disabled="scope.row.isQuery !== '1'" />
        </template>
      </el-table-column>
      <el-table-column label="控件" width="110">
        <template slot-scope="scope">
          <el-select v-model="scope.row.htmlType" size="mini" :disabled="scope.row.isQuery !== '1'">
            <el-option label="文本" value="input" />
            <el-option label="下拉" value="select" />
            <el-option label="日期" value="date" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="匹配方式" width="120">
        <template slot-scope="scope">
          <el-select v-model="scope.row.queryType" size="mini" :disabled="scope.row.isQuery !== '1'">
            <el-option label="等于" value="EQ" />
            <el-option label="模糊" value="LIKE" />
            <el-option label="区间" value="BETWEEN" />
            <el-option label="大于" value="GT" />
            <el-option label="大于等于" value="GTE" />
            <el-option label="小于" value="LT" />
            <el-option label="小于等于" value="LTE" />
            <el-option label="多选包含" value="IN" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="结果脱敏" width="120">
        <template slot-scope="scope">
          <el-select v-model="scope.row.maskType" size="mini">
            <el-option label="不脱敏" value="none" />
            <el-option label="手机号" value="phone" />
            <el-option label="身份证" value="idcard" />
            <el-option label="姓名" value="name" />
            <el-option label="邮箱" value="email" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="下拉选项" min-width="140">
        <template slot-scope="scope">
          <el-input
            v-model="scope.row.dictOptions"
            size="mini"
            type="textarea"
            :rows="2"
            :disabled="scope.row.isQuery !== '1' || scope.row.htmlType !== 'select'"
            placeholder='如 A,B 或 ["优","良"]'
          />
        </template>
      </el-table-column>
      <el-table-column label="结果列" width="80" align="center">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.isList" active-value="1" inactive-value="0" />
        </template>
      </el-table-column>
      <el-table-column label="默认排序" width="90" align="center">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.isSortable"
            active-value="1"
            inactive-value="0"
            @change="val => onDefaultSortChange(scope.$index, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="排序" width="90">
        <template slot-scope="scope">
          <el-input-number v-model="scope.row.sort" size="mini" :min="1" controls-position="right" style="width: 90px" />
        </template>
      </el-table-column>
    </el-table>

    <el-card shadow="never" class="mt16 sample-card" v-loading="sampleLoading">
      <div slot="header" class="sample-head">
        <span>数据样例（前 {{ sampleRows.length || 5 }} 行）</span>
        <el-button type="text" icon="el-icon-refresh" @click="loadSample">刷新</el-button>
      </div>
      <el-table v-if="sampleRows.length" :data="sampleRows" size="mini" border max-height="280">
        <el-table-column
          v-for="f in fields"
          :key="f.fieldKey"
          :prop="f.fieldKey"
          :label="f.fieldLabel || f.fieldName"
          min-width="100"
          show-overflow-tooltip
        />
      </el-table>
      <el-empty v-else description="暂无样例数据，请先导入 Excel" :image-size="56" />
    </el-card>

    <div class="mt16" v-if="!embedded">
      <el-button type="primary" @click="handleSave" v-hasPermi="['biz:query:edit']">保存配置</el-button>
      <el-button @click="goBack">返回</el-button>
    </div>
    <div class="mt16" v-else>
      <el-button type="primary" plain size="small" @click="handleSave" v-hasPermi="['biz:query:edit']">保存字段配置</el-button>
    </div>
    </div>
  </div>
</template>

<script>
import { getQuery, saveQueryFields, querySampleRows } from '@/api/biz/query'

export default {
  name: 'BizQueryFields',
  props: {
    embedded: { type: Boolean, default: false },
    queryIdProp: { type: [String, Number], default: null }
  },
  data() {
    return {
      loading: false,
      sampleLoading: false,
      queryId: null,
      detail: { query: {}, fields: [] },
      fields: [],
      sampleRows: []
    }
  },
  created() {
    this.queryId = this.queryIdProp != null ? this.queryIdProp : this.$route.params.queryId
    this.load()
  },
  methods: {
    onDefaultSortChange(idx, val) {
      if (val === '1') {
        this.fields.forEach((f, i) => {
          if (i !== idx) f.isSortable = '0'
        })
      }
    },
    onQueryToggle(row) {
      if (row.isQuery !== '1') {
        row.isRequired = '0'
      } else if (row.isRequired == null || row.isRequired === '') {
        row.isRequired = '1'
      }
    },
    load() {
      this.loading = true
      getQuery(this.queryId).then(res => {
        this.detail = res.data || {}
        this.fields = (this.detail.fields || []).map(f => ({
          ...f,
          htmlType: f.htmlType || 'input',
          queryType: f.queryType || 'EQ',
          dictOptions: f.dictOptions || '',
          isSortable: f.isSortable || '0',
          isRequired: f.isRequired == null || f.isRequired === '' ? '1' : f.isRequired,
          maskType: f.maskType || 'none'
        }))
        this.loading = false
        this.loadSample()
      }).catch(() => { this.loading = false })
    },
    loadSample() {
      if (!this.queryId) return
      this.sampleLoading = true
      querySampleRows(this.queryId, 5).then(res => {
        const list = res.data || []
        this.sampleRows = list.map(item => {
          try {
            return typeof item.rowData === 'string' ? JSON.parse(item.rowData) : (item.rowData || {})
          } catch (e) {
            return {}
          }
        })
      }).catch(() => { this.sampleRows = [] }).finally(() => { this.sampleLoading = false })
    },
    buildPayload() {
      return this.fields.map(f => {
        const row = { ...f }
        if (row.htmlType !== 'select') row.dictOptions = row.dictOptions || ''
        if (row.htmlType === 'date' && row.queryType === 'LIKE') row.queryType = 'EQ'
        if (row.queryType === 'IN' && row.htmlType === 'input') {
          // keep allowed; public form accepts comma-separated values
        }
        return row
      })
    },
    handleSave() {
      return saveQueryFields(this.queryId, this.buildPayload()).then(() => {
        this.$modal.msgSuccess('保存成功')
        this.load()
      })
    },
    saveForWizard() {
      if (!this.fields.length) {
        this.$modal.msgWarning('暂无字段，请先返回上一步导入数据')
        return Promise.reject(new Error('no fields'))
      }
      return saveQueryFields(this.queryId, this.buildPayload()).then(() => {
        this.$modal.msgSuccess('字段配置已保存')
        this.load()
      })
    },
    goBack() { this.$router.push('/biz/query') }
  }
}
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.mt16 { margin-top: 16px; }
.sample-head { display: flex; align-items: center; justify-content: space-between; }
.sample-card { margin-top: 16px; }
</style>
