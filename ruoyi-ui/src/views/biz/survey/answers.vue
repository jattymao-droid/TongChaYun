<template>
  <div class="app-container">
    <div class="page-head mb16">
      <el-page-header @back="goBack" :content="'答卷列表 - ' + (survey.surveyName || '')" />
      <div class="head-actions">
        <el-button size="small" icon="el-icon-data-analysis" @click="goStats">统计分析</el-button>
      </div>
    </div>
    <el-form :model="queryParams" size="small" :inline="true" class="mb16">
      <el-form-item label="渠道">
        <el-input v-model="queryParams.channelCode" placeholder="渠道码" clearable style="width: 140px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="提交IP">
        <el-input v-model="queryParams.submitIp" placeholder="IP 片段" clearable style="width: 140px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="有效性">
        <el-select v-model="queryParams.validFlag" clearable placeholder="全部" style="width: 110px">
          <el-option label="有效" value="1" />
          <el-option label="无效" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="提交时间">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="yyyy-MM-dd"
          range-separator="-"
          start-placeholder="开始"
          end-placeholder="结束"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['biz:survey:query']">导出答卷</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="答卷ID" prop="answerId" width="90" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.validFlag === '0' ? 'info' : 'success'">
            {{ scope.row.validFlag === '0' ? '无效' : '有效' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="渠道" prop="channelCode" width="100" align="center">
        <template slot-scope="scope">{{ scope.row.channelCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="提交IP" prop="submitIp" width="140" align="center" />
      <el-table-column label="耗时(ms)" prop="costMs" width="100" align="center" />
      <el-table-column label="提交时间" prop="submitTime" width="180" align="center" />
      <el-table-column label="备注" prop="remark" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="showDetail(scope.row)">详情</el-button>
          <el-button
            v-if="scope.row.validFlag !== '0'"
            type="text"
            size="mini"
            @click="markInvalid(scope.row)"
            v-hasPermi="['biz:survey:edit']"
          >标为无效</el-button>
          <el-button
            v-else
            type="text"
            size="mini"
            @click="markValid(scope.row)"
            v-hasPermi="['biz:survey:edit']"
          >恢复有效</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="答卷详情" :visible.sync="detailOpen" width="640px" append-to-body>
      <div v-loading="detailLoading">
        <el-descriptions :column="1" border size="small" v-if="detail">
          <el-descriptions-item label="答卷ID">{{ detail.answerId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="mini" :type="detail.validFlag === '0' ? 'info' : 'success'">
              {{ detail.validFlag === '0' ? '无效' : '有效' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ detail.submitTime }}</el-descriptions-item>
          <el-descriptions-item label="IP">{{ detail.submitIp }}</el-descriptions-item>
          <el-descriptions-item label="渠道">{{ detail.channelCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注">
            <el-input
              v-model="detailRemark"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              placeholder="可填写无效原因或备注"
            />
            <el-button type="text" size="mini" class="mt8" @click="saveRemark" v-hasPermi="['biz:survey:edit']">保存备注</el-button>
          </el-descriptions-item>
        </el-descriptions>
        <el-table :data="detailItems" size="small" class="mt12">
          <el-table-column label="题目" prop="questionTitle" min-width="180" />
          <el-table-column label="题型" prop="qType" width="100" />
          <el-table-column label="答案" min-width="180">
            <template slot-scope="scope">
              <a v-if="scope.row.qType === 'file' && fileHref(scope.row.answerValue)" :href="fileHref(scope.row.answerValue)" target="_blank">{{ fileName(scope.row.answerValue) }}</a>
              <span v-else>{{ scope.row.displayValue || scope.row.answerValue }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getSurvey, listSurveyAnswers, getSurveyAnswer, exportSurveyAnswers, updateSurveyAnswer } from '@/api/biz/survey'
import { blobValidate } from '@/utils/ruoyi'
import { saveAs } from 'file-saver'

export default {
  name: 'BizSurveyAnswers',
  data() {
    return {
      loading: false,
      surveyId: null,
      survey: {},
      list: [],
      total: 0,
      dateRange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        surveyId: null,
        channelCode: undefined,
        submitIp: undefined,
        validFlag: undefined
      },
      detailOpen: false,
      detailLoading: false,
      detail: null,
      detailItems: [],
      detailRemark: ''
    }
  },
  created() {
    this.surveyId = this.$route.params.surveyId
    this.queryParams.surveyId = this.surveyId
    getSurvey(this.surveyId).then(res => {
      this.survey = (res.data && res.data.survey) || {}
    })
    this.getList().then(() => {
      const aid = this.$route.query.answerId
      if (aid) {
        this.showDetail({ answerId: Number(aid) })
      }
    })
  },
  methods: {
    goBack() {
      this.$router.push('/biz/survey')
    },
    goStats() {
      this.$router.push('/biz/survey-stats/index/' + this.surveyId)
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.queryParams.channelCode = undefined
      this.queryParams.submitIp = undefined
      this.queryParams.validFlag = undefined
      this.handleQuery()
    },
    buildFilter() {
      const q = {
        channelCode: this.queryParams.channelCode,
        submitIp: this.queryParams.submitIp,
        validFlag: this.queryParams.validFlag
      }
      this.addDateRange(q, this.dateRange)
      return q
    },
    handleExport() {
      const tip = (this.queryParams.channelCode || this.queryParams.submitIp || this.queryParams.validFlag || (this.dateRange && this.dateRange.length))
        ? '确认导出当前筛选条件下的答卷明细（含有效性、备注、各题答案）？'
        : '确认导出全部答卷明细（含有效性、备注、各题答案）？'
      this.$modal.confirm(tip).then(() => exportSurveyAnswers(this.surveyId, this.buildFilter())).then(async data => {
        if (blobValidate(data)) {
          saveAs(new Blob([data]), (this.survey.surveyName || 'survey') + '-答卷明细.xlsx')
        } else {
          this.$modal.msgError('导出失败')
        }
      }).catch(() => {})
    },
    getList() {
      this.loading = true
      const params = { ...this.queryParams }
      this.addDateRange(params, this.dateRange)
      return listSurveyAnswers(params).then(res => {
        this.list = (res.rows || []).map(r => ({
          ...r,
          validFlag: r.validFlag == null || r.validFlag === '' ? '1' : r.validFlag
        }))
        this.total = res.total || 0
      }).finally(() => { this.loading = false })
    },
    fileHref(val) {
      try {
        const obj = typeof val === 'string' && val.trim().startsWith('{') ? JSON.parse(val) : { fileName: val }
        const path = obj.fileName || obj.url || ''
        if (!path) return ''
        if (path.startsWith('http')) return path
        return process.env.VUE_APP_BASE_API + path
      } catch (e) { return '' }
    },
    fileName(val) {
      try {
        const obj = typeof val === 'string' && val.trim().startsWith('{') ? JSON.parse(val) : { originalFilename: val }
        return obj.originalFilename || obj.newFileName || obj.fileName || '附件'
      } catch (e) { return '附件' }
    },
    showDetail(row) {
      this.detailOpen = true
      this.detailLoading = true
      getSurveyAnswer(row.answerId).then(res => {
        this.detail = res.data || {}
        if (this.detail.validFlag == null || this.detail.validFlag === '') this.detail.validFlag = '1'
        this.detailRemark = this.detail.remark || ''
        this.detailItems = this.detail.items || []
      }).finally(() => { this.detailLoading = false })
    },
    markInvalid(row) {
      this.$prompt('可填写无效原因（可选）', '标为无效', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: '如：测试数据 / 刷卷',
        inputValue: row.remark || ''
      }).then(({ value }) => {
        return updateSurveyAnswer(row.answerId, { validFlag: '0', remark: value || '' })
      }).then(() => {
        this.$modal.msgSuccess('已标为无效（统计将排除）')
        this.getList()
      }).catch(() => {})
    },
    markValid(row) {
      updateSurveyAnswer(row.answerId, { validFlag: '1' }).then(() => {
        this.$modal.msgSuccess('已恢复为有效')
        this.getList()
      })
    },
    saveRemark() {
      if (!this.detail) return
      updateSurveyAnswer(this.detail.answerId, { remark: this.detailRemark || '' }).then(() => {
        this.detail.remark = this.detailRemark
        this.$modal.msgSuccess('备注已保存')
        this.getList()
      })
    }
  }
}
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.head-actions { display: flex; gap: 8px; }
.mb16 { margin-bottom: 16px; }
.mt8 { margin-top: 8px; }
.mt12 { margin-top: 12px; }
</style>
