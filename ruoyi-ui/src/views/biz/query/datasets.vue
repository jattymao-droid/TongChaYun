<template>
  <div class="query-datasets">
    <el-card shadow="never" class="mb16">
      <div slot="header" class="card-head">
        <span>数据表</span>
        <el-tag size="mini" type="info">可上传多张表，再按自定义字段关联</el-tag>
      </div>
      <p class="desc">先上传主表与附表（Excel）。关联结果会生成一张宽表，供后续字段配置与开放查询使用。</p>

      <el-table :data="datasets" size="small" border empty-text="暂无数据表，请先上传">
        <el-table-column label="名称" min-width="140">
          <template slot-scope="scope">
            <el-input v-model="scope.row.datasetName" size="mini" @change="renameDataset(scope.row)" />
          </template>
        </el-table-column>
        <el-table-column label="编码" prop="datasetCode" width="90" />
        <el-table-column label="角色" width="100" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.isPrimary === '1'" size="mini" type="success">主表</el-tag>
            <el-button v-else type="text" size="mini" @click="setPrimary(scope.row)">设为主表</el-button>
          </template>
        </el-table-column>
        <el-table-column label="行数" prop="rowCount" width="80" align="center" />
        <el-table-column label="字段" min-width="200">
          <template slot-scope="scope">
            <span class="header-chips">
              <el-tag
                v-for="h in parseHeaders(scope.row.headersJson)"
                :key="h.key"
                size="mini"
                effect="plain"
                class="chip"
              >{{ h.name }}</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" style="color:#f56c6c" @click="removeDataset(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="upload-bar mt16">
        <el-input v-model="uploadName" size="small" placeholder="表名称（可选，默认用 Sheet 名）" style="width:220px" clearable />
        <el-checkbox v-model="uploadAsPrimary" :disabled="datasets.length === 0">设为主表</el-checkbox>
        <el-upload
          ref="dsUpload"
          action="#"
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :show-file-list="false"
          :on-change="onFileChange"
        >
          <el-button size="small" icon="el-icon-upload2" :loading="uploading" v-hasPermi="['biz:query:edit']">选择并上传 Excel</el-button>
        </el-upload>
      </div>
    </el-card>

    <el-card shadow="never" class="mb16">
      <div slot="header" class="card-head">
        <span>关联配置</span>
        <el-button type="text" size="mini" icon="el-icon-plus" :disabled="datasets.length < 2" @click="addRelation">添加关联</el-button>
      </div>
      <el-alert
        v-if="datasets.length < 2"
        title="上传至少 2 张表后可配置关联。仅 1 张表时直接点「生成结果」即可。"
        type="info"
        :closable="false"
        class="mb12"
      />

      <div v-for="(rel, ri) in relations" :key="ri" class="rel-block">
        <div class="rel-head">
          <span class="rel-title">关联 {{ ri + 1 }}</span>
          <el-select v-model="rel.joinType" size="mini" style="width:110px">
            <el-option label="LEFT 保留主侧" value="LEFT" />
            <el-option label="INNER 仅匹配" value="INNER" />
          </el-select>
          <el-select v-model="rel.multiMatch" size="mini" style="width:150px" title="一对多时如何处理">
            <el-option label="一对多：展开" value="EXPAND" />
            <el-option label="一对多：取首条" value="FIRST" />
            <el-option label="一对多：取末条" value="LAST" />
            <el-option label="一对多：合并" value="CONCAT" />
          </el-select>
          <el-button type="text" size="mini" style="color:#f56c6c" @click="relations.splice(ri, 1)">删除</el-button>
        </div>
        <div class="rel-row">
          <el-select v-model="rel.leftDatasetId" placeholder="左表" size="small" style="width:180px" @change="onLeftChange(rel)">
            <el-option v-for="d in datasets" :key="'L'+d.datasetId" :label="d.datasetName + (d.isPrimary==='1'?'（主）':'')" :value="d.datasetId" />
          </el-select>
          <span class="join-label">关联</span>
          <el-select v-model="rel.rightDatasetId" placeholder="右表" size="small" style="width:180px" @change="ensureKeys(rel)">
            <el-option
              v-for="d in datasets"
              :key="'R'+d.datasetId"
              :label="d.datasetName"
              :value="d.datasetId"
              :disabled="d.datasetId === rel.leftDatasetId"
            />
          </el-select>
        </div>
        <p class="multi-hint">一对多：展开会复制主行；取首/末只留一条；合并用「；」拼接附表字段。</p>
        <div class="keys-title">关联字段（可多个，须同时匹配）</div>
        <div v-for="(jk, ki) in rel.joinKeys" :key="ki" class="key-row">
          <el-select v-model="jk.leftKey" placeholder="左表字段" size="small" filterable style="width:200px">
            <el-option
              v-for="h in headersOf(rel.leftDatasetId)"
              :key="'lk'+h.key"
              :label="h.name"
              :value="h.key"
            />
          </el-select>
          <span class="eq">=</span>
          <el-select v-model="jk.rightKey" placeholder="右表字段" size="small" filterable style="width:200px">
            <el-option
              v-for="h in headersOf(rel.rightDatasetId)"
              :key="'rk'+h.key"
              :label="h.name"
              :value="h.key"
            />
          </el-select>
          <el-button type="text" icon="el-icon-delete" :disabled="rel.joinKeys.length <= 1" @click="rel.joinKeys.splice(ki, 1)" />
        </div>
        <el-button type="text" size="mini" icon="el-icon-plus" @click="rel.joinKeys.push({ leftKey: '', rightKey: '' })">增加关联字段</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="gen-bar">
        <div>
          <div class="gen-title">生成关联结果</div>
          <div class="gen-desc">将按关联配置物化成查询宽表；已有字段设置会尽量按字段编码保留。</div>
          <div v-if="resultHint" class="gen-hint">{{ resultHint }}</div>
        </div>
        <div class="gen-actions">
          <el-button @click="saveRelationsOnly" :loading="savingRel" v-hasPermi="['biz:query:edit']">仅保存关联</el-button>
          <el-button type="primary" icon="el-icon-magic-stick" :loading="materializing" @click="doMaterialize" v-hasPermi="['biz:query:edit']">生成结果</el-button>
        </div>
      </div>
      <div v-if="joinReport && (joinReport.relations || []).length" class="join-report">
        <div class="report-title">未匹配报告</div>
        <el-table :data="joinReport.relations" size="mini" border>
          <el-table-column label="左表" prop="leftName" min-width="120" />
          <el-table-column label="右表" prop="rightName" min-width="120" />
          <el-table-column label="类型" prop="joinType" width="70" align="center" />
          <el-table-column label="一对多" prop="multiMatch" width="80" align="center" />
          <el-table-column label="左侧行" prop="leftRows" width="70" align="center" />
          <el-table-column label="匹配" prop="matchedLeft" width="60" align="center" />
          <el-table-column label="多匹配" prop="multiHitLeft" width="70" align="center" />
          <el-table-column label="未匹配" prop="unmatchedLeft" width="70" align="center" />
          <el-table-column label="结果行" prop="resultRows" width="80" align="center" />
          <el-table-column label="未匹配样例" min-width="200">
            <template slot-scope="scope">
              <span class="sample">{{ (scope.row.unmatchedSamples || []).slice(0, 5).join(' · ') || '—' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script>
import {
  getQuery,
  uploadQueryDataset,
  updateQueryDataset,
  deleteQueryDataset,
  saveQueryRelations,
  materializeQueryJoin
} from '@/api/biz/query'

export default {
  name: 'BizQueryDatasets',
  props: {
    queryId: { type: [String, Number], required: true }
  },
  data() {
    return {
      datasets: [],
      relations: [],
      uploadName: '',
      uploadAsPrimary: false,
      uploadFile: null,
      uploading: false,
      savingRel: false,
      materializing: false,
      resultHint: '',
      joinReport: null
    }
  },
  watch: {
    queryId: {
      immediate: true,
      handler() {
        this.reload()
      }
    }
  },
  methods: {
    parseHeaders(json) {
      if (!json) return []
      try {
        const list = typeof json === 'string' ? JSON.parse(json) : json
        return Array.isArray(list) ? list : []
      } catch (e) {
        return []
      }
    },
    headersOf(datasetId) {
      const ds = this.datasets.find(d => d.datasetId === datasetId)
      return ds ? this.parseHeaders(ds.headersJson) : []
    },
    reload() {
      if (!this.queryId) return Promise.resolve()
      return getQuery(this.queryId).then(res => {
        const data = res.data || {}
        this.datasets = data.datasets || []
        const rels = (data.relations || []).map(r => ({
          leftDatasetId: r.leftDatasetId,
          rightDatasetId: r.rightDatasetId,
          joinType: r.joinType || 'LEFT',
          multiMatch: r.multiMatch || 'EXPAND',
          joinKeys: (r.joinKeys && r.joinKeys.length)
            ? r.joinKeys.map(k => ({ leftKey: k.leftKey, rightKey: k.rightKey }))
            : [{ leftKey: '', rightKey: '' }]
        }))
        this.relations = rels
        const q = data.query || {}
        this.resultHint = q.parseMsg || (q.rowCount ? ('当前结果行数：' + q.rowCount) : '')
        this.uploadAsPrimary = this.datasets.length === 0
        this.$emit('loaded', data)
      })
    },
    onFileChange(file) {
      this.uploadFile = file.raw
      this.$nextTick(() => this.submitUpload())
    },
    submitUpload() {
      if (!this.uploadFile) return
      this.uploading = true
      const isPrimary = this.datasets.length === 0 || this.uploadAsPrimary ? '1' : '0'
      uploadQueryDataset(this.queryId, this.uploadFile, {
        datasetName: this.uploadName,
        isPrimary,
        mode: 'replace'
      }).then(() => {
        this.$modal.msgSuccess('上传成功')
        this.uploadFile = null
        this.uploadName = ''
        this.$refs.dsUpload && this.$refs.dsUpload.clearFiles()
        return this.reload()
      }).finally(() => { this.uploading = false })
    },
    renameDataset(row) {
      updateQueryDataset({ datasetId: row.datasetId, datasetName: row.datasetName }).then(() => {
        this.$modal.msgSuccess('已更新名称')
      })
    },
    setPrimary(row) {
      updateQueryDataset({ datasetId: row.datasetId, isPrimary: '1' }).then(() => {
        this.$modal.msgSuccess('已设为主表')
        this.reload()
      })
    },
    removeDataset(row) {
      this.$modal.confirm('确认删除数据表「' + row.datasetName + '」？相关关联也会清除。').then(() => {
        return deleteQueryDataset(this.queryId, row.datasetId)
      }).then(() => {
        this.$modal.msgSuccess('已删除')
        this.reload()
      }).catch(() => {})
    },
    addRelation() {
      const primary = this.datasets.find(d => d.isPrimary === '1') || this.datasets[0]
      const secondary = this.datasets.find(d => d.datasetId !== (primary && primary.datasetId))
      this.relations.push({
        leftDatasetId: primary ? primary.datasetId : null,
        rightDatasetId: secondary ? secondary.datasetId : null,
        joinType: 'LEFT',
        multiMatch: 'EXPAND',
        joinKeys: [{ leftKey: '', rightKey: '' }]
      })
    },
    onLeftChange(rel) {
      if (rel.rightDatasetId === rel.leftDatasetId) rel.rightDatasetId = null
      this.ensureKeys(rel)
    },
    ensureKeys(rel) {
      if (!rel.joinKeys || !rel.joinKeys.length) {
        rel.joinKeys = [{ leftKey: '', rightKey: '' }]
      }
    },
    buildPayload() {
      return this.relations.map(r => ({
        leftDatasetId: r.leftDatasetId,
        rightDatasetId: r.rightDatasetId,
        joinType: r.joinType || 'LEFT',
        joinKeys: (r.joinKeys || []).filter(k => k.leftKey && k.rightKey)
      }))
    },
    saveRelationsOnly() {
      if (this.datasets.length >= 2 && this.relations.length === 0) {
        this.$modal.msgWarning('请先添加关联，或直接生成单表结果')
        return
      }
      for (const r of this.relations) {
        if (!r.leftDatasetId || !r.rightDatasetId) {
          this.$modal.msgError('请完整选择左右表')
          return
        }
        const keys = (r.joinKeys || []).filter(k => k.leftKey && k.rightKey)
        if (!keys.length) {
          this.$modal.msgError('每条关联至少配置一个字段对')
          return
        }
      }
      this.savingRel = true
      saveQueryRelations(this.queryId, this.buildPayload()).then(() => {
        this.$modal.msgSuccess('关联已保存')
        this.reload()
      }).finally(() => { this.savingRel = false })
    },
    doMaterialize() {
      const run = () => {
        this.materializing = true
        const payload = this.buildPayload()
        const saveFirst = this.datasets.length >= 2
          ? saveQueryRelations(this.queryId, payload)
          : Promise.resolve()
        return saveFirst.then(() => materializeQueryJoin(this.queryId)).then(res => {
          const detail = res.data || {}
          const q = detail.query || {}
          this.$modal.msgSuccess(q.parseMsg || '生成成功')
          this.resultHint = q.parseMsg || ''
          this.joinReport = detail.joinReport || null
          this.$emit('materialized', detail)
          return this.reload()
        }).finally(() => { this.materializing = false })
      }
      if (this.datasets.length >= 2 && this.relations.length === 0) {
        this.$modal.confirm('尚未配置关联，将仅使用主表生成结果。是否继续？').then(run).catch(() => {})
        return
      }
      for (const r of this.relations) {
        if (!r.leftDatasetId || !r.rightDatasetId) {
          this.$modal.msgError('请完整选择左右表')
          return
        }
        if (!(r.joinKeys || []).some(k => k.leftKey && k.rightKey)) {
          this.$modal.msgError('请为每条关联配置至少一个字段对（支持多字段）')
          return
        }
      }
      run()
    }
  }
}
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.mb16 { margin-bottom: 16px; }
.mt16 { margin-top: 16px; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.desc { color: var(--biz-muted); font-size: 13px; line-height: 1.6; margin: 0 0 14px; }
.header-chips { display: flex; flex-wrap: wrap; gap: 4px; }
.chip { margin: 0; }
.upload-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }
.rel-block {
  border: 1px solid var(--biz-line);
  border-radius: var(--biz-radius-sm);
  padding: 12px 14px;
  margin-bottom: 12px;
  background: var(--biz-bg-soft);
}
.rel-head { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.rel-title { font-weight: 600; color: var(--biz-ink); margin-right: auto; }
.rel-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 10px; }
.join-label { color: var(--biz-muted-soft); font-size: 13px; }
.keys-title { font-size: 12px; color: var(--biz-muted-soft); margin: 4px 0 8px; }
.key-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
.eq { color: var(--biz-muted); font-weight: 600; }
.gen-bar { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.gen-title { font-weight: 600; color: var(--biz-ink); margin-bottom: 4px; }
.gen-desc { font-size: 13px; color: var(--biz-muted); line-height: 1.5; }
.gen-hint { margin-top: 8px; font-size: 12px; color: #67c23a; }
.gen-actions { display: flex; gap: 8px; flex-shrink: 0; }

.join-report { margin-top: 16px; padding-top: 12px; border-top: 1px dashed #e5e7eb; }
.report-title { font-weight: 600; margin-bottom: 8px; }
.sample { color: #6b7280; font-size: 12px; word-break: break-all; }
.multi-hint { color:#6b7280; font-size:12px; margin:6px 0 8px; }
</style>
