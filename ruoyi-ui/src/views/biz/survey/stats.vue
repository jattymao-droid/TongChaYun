<template>
  <div class="biz-page stats-page" v-loading="loading">
    <div class="biz-page-head">
      <el-page-header @back="goBack" :content="'统计 - ' + (stats.surveyName || '')" />
      <div class="head-actions">
        <el-button size="small" icon="el-icon-document" @click="goAnswers">答卷列表</el-button>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button
          type="warning"
          plain
          size="small"
          icon="el-icon-download"
          :loading="exporting"
          @click="handleExportStats"
          v-hasPermi="['biz:survey:query']"
        >导出统计</el-button>
      </div>
    </div>

    <el-row :gutter="14" class="mb16 metric-row">
      <el-col :xs="12" :sm="8" :md="4">
        <div class="biz-stat-card">
          <div class="stat-icon"><i class="el-icon-document-checked"></i></div>
          <div>
            <div class="stat-label">有效答卷</div>
            <div class="stat-value">{{ stats.answerCount || 0 }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <div class="biz-stat-card">
          <div class="stat-icon tone-muted"><i class="el-icon-document-delete"></i></div>
          <div>
            <div class="stat-label">无效答卷</div>
            <div class="stat-value">{{ stats.invalidCount || 0 }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <div class="biz-stat-card">
          <div class="stat-icon tone-teal"><i class="el-icon-tickets"></i></div>
          <div>
            <div class="stat-label">全部答卷</div>
            <div class="stat-value">{{ stats.totalCount || 0 }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <div class="biz-stat-card">
          <div class="stat-icon tone-violet"><i class="el-icon-view"></i></div>
          <div>
            <div class="stat-label">浏览量</div>
            <div class="stat-value">{{ stats.viewCount || 0 }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <div class="biz-stat-card">
          <div class="stat-icon tone-amber"><i class="el-icon-data-line"></i></div>
          <div>
            <div class="stat-label">转化率</div>
            <div class="stat-value">{{ convertRateText }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <div class="biz-stat-card">
          <div class="stat-icon tone-rose"><i class="el-icon-s-order"></i></div>
          <div>
            <div class="stat-label">选择题</div>
            <div class="stat-value">{{ choiceCount }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mb16 answer-matrix-card">
      <div slot="header" class="matrix-head">
        <span>答题列表<span class="sub">（每人一行）</span></span>
        <div class="matrix-tools">
          <el-radio-group v-model="matrixValidFlag" size="mini" @change="onMatrixFilterChange">
            <el-radio-button label="1">有效</el-radio-button>
            <el-radio-button label="0">无效</el-radio-button>
            <el-radio-button label="all">全部</el-radio-button>
          </el-radio-group>
          <el-button
            size="mini"
            type="warning"
            plain
            icon="el-icon-download"
            :loading="matrixExporting"
            @click="handleExportMatrix"
            v-hasPermi="['biz:survey:query']"
          >导出 Excel</el-button>
          <el-button size="mini" icon="el-icon-refresh" :loading="matrixLoading" @click="loadMatrix">刷新</el-button>
        </div>
      </div>
      <div v-loading="matrixLoading">
        <el-table
          v-if="matrixRows.length"
          :data="matrixRows"
          size="mini"
          border
          class="matrix-table"
          max-height="520"
        >
          <el-table-column label="#" prop="label" width="64" fixed align="center" />
          <el-table-column label="状态" width="72" fixed align="center">
            <template slot-scope="scope">
              <el-tag size="mini" :type="scope.row.validFlag === '0' ? 'info' : 'success'">
                {{ scope.row.validFlag === '0' ? '无效' : '有效' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" prop="submitTime" width="160" fixed />
          <el-table-column label="渠道" prop="channelCode" width="90" align="center">
            <template slot-scope="scope">{{ scope.row.channelCode || '-' }}</template>
          </el-table-column>
          <el-table-column
            v-for="col in matrixColumns"
            :key="col.questionId"
            :label="col.title"
            min-width="140"
            :show-overflow-tooltip="col.qType !== 'agreement' && col.qType !== 'signature' && col.qType !== 'file'"
          >
            <template slot="header">
              <span>{{ col.title }}</span>
              <el-tag size="mini" type="info" class="ml6">{{ typeLabel(col.qType) }}</el-tag>
            </template>
            <template slot-scope="scope">
              <template v-if="col.qType === 'agreement'">
                <el-button type="text" size="mini" @click="openAgreement(scope.row, col)">查看协议</el-button>
                <span class="cell-agree">{{ cellDisplay(scope.row, col) || '-' }}</span>
              </template>
              <template v-else-if="col.qType === 'signature'">
                <img
                  v-if="cellMediaUrl(scope.row, col)"
                  :src="cellMediaUrl(scope.row, col)"
                  class="sig-thumb"
                  alt="signature"
                  @click="previewImage(cellMediaUrl(scope.row, col))"
                />
                <span v-else>-</span>
              </template>
              <template v-else-if="col.qType === 'file'">
                <a
                  v-if="cellMediaUrl(scope.row, col)"
                  :href="cellMediaUrl(scope.row, col)"
                  target="_blank"
                  rel="noopener"
                >{{ cellFileName(scope.row, col) || '附件' }}</a>
                <span v-else>-</span>
              </template>
              <span v-else>{{ cellDisplay(scope.row, col) || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right" align="center">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="goAnswerDetail(scope.row.answerId)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else-if="!matrixLoading" description="暂无答卷" :image-size="60" />
        <pagination
          v-show="matrixTotal > 0"
          :total="matrixTotal"
          :page.sync="matrixPageNum"
          :limit.sync="matrixPageSize"
          :page-sizes="[10, 20, 30, 50]"
          @pagination="loadMatrix"
        />
      </div>
    </el-card>

    <section class="stats-below">
      <div class="section-bar">
        <h3 class="section-title">统计分析</h3>
        <p class="section-desc">趋势、渠道与各题作答分布</p>
      </div>

      <el-row
        v-if="(stats.funnel || []).length || (stats.channelCompare || []).length"
        :gutter="16"
        class="mb16"
      >
        <el-col :xs="24" :md="(stats.channelCompare || []).length ? 10 : 24" v-if="(stats.funnel || []).length">
          <el-card shadow="never" class="panel-card">
            <div slot="header" class="panel-head">
              <span class="panel-title">公开页漏斗</span>
              <span class="panel-hint">浏览 → 填写 → 提交</span>
            </div>
            <el-table :data="stats.funnel || []" size="mini" border>
              <el-table-column label="阶段" prop="label" min-width="100" />
              <el-table-column label="事件" prop="events" width="80" align="right" />
              <el-table-column label="UV" prop="uv" width="80" align="right" />
              <el-table-column label="转化%" prop="rate" width="80" align="right">
                <template slot-scope="scope">{{ scope.row.rate }}%</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="(stats.funnel || []).length ? 14 : 24" v-if="(stats.channelCompare || []).length">
          <el-card shadow="never" class="panel-card">
            <div slot="header" class="panel-head">
              <span class="panel-title">渠道对比</span>
              <span class="panel-hint">UV / 提交 / 转化</span>
            </div>
            <el-table :data="stats.channelCompare || []" size="mini" border max-height="280">
              <el-table-column label="渠道" prop="channelCode" min-width="100" :show-overflow-tooltip="true" />
              <el-table-column label="浏览UV" prop="viewUv" width="90" align="right" />
              <el-table-column label="提交" prop="submitCnt" width="80" align="right" />
              <el-table-column label="转化%" prop="convertRate" width="80" align="right">
                <template slot-scope="scope">{{ scope.row.convertRate }}%</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <el-row
        v-if="(stats.riskByIp || []).length || (stats.riskByDevice || []).length || (stats.blacklist || []).length"
        :gutter="16"
        class="mb16"
      >
        <el-col :xs="24" :lg="12" v-if="(stats.riskByIp || []).length">
          <el-card shadow="never" class="panel-card">
            <div slot="header" class="panel-head">
              <span class="panel-title">风控 · IP</span>
              <span class="panel-hint">Top 提交 IP</span>
            </div>
            <el-table :data="(stats.riskByIp || []).slice(0, 20)" size="mini" border max-height="320">
              <el-table-column label="IP" prop="key" min-width="120" :show-overflow-tooltip="true" />
              <el-table-column label="提交" prop="submitCnt" width="70" align="right" />
              <el-table-column label="无效" prop="invalidCnt" width="70" align="right" />
              <el-table-column label="无效率" prop="invalidRate" width="80" align="right">
                <template slot-scope="scope">{{ scope.row.invalidRate }}%</template>
              </el-table-column>
              <el-table-column label="均耗时ms" prop="avgCostMs" width="90" align="right" />
              <el-table-column label="操作" width="120" align="center" fixed="right">
                <template slot-scope="scope">
                  <el-button
                    type="text"
                    size="mini"
                    v-hasPermi="['biz:survey:edit']"
                    :disabled="!scope.row.key || scope.row.key === '(空)'"
                    @click="banRisk('ip', scope.row.key)"
                  >拉黑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12" v-if="(stats.riskByDevice || []).length">
          <el-card shadow="never" class="panel-card">
            <div slot="header" class="panel-head">
              <span class="panel-title">风控 · 设备</span>
              <span class="panel-hint">Top clientToken</span>
            </div>
            <el-table :data="(stats.riskByDevice || []).slice(0, 20)" size="mini" border max-height="320">
              <el-table-column label="设备" prop="key" min-width="140" :show-overflow-tooltip="true" />
              <el-table-column label="提交" prop="submitCnt" width="70" align="right" />
              <el-table-column label="无效" prop="invalidCnt" width="70" align="right" />
              <el-table-column label="无效率" prop="invalidRate" width="80" align="right">
                <template slot-scope="scope">{{ scope.row.invalidRate }}%</template>
              </el-table-column>
              <el-table-column label="均耗时ms" prop="avgCostMs" width="90" align="right" />
              <el-table-column label="操作" width="120" align="center" fixed="right">
                <template slot-scope="scope">
                  <el-button
                    type="text"
                    size="mini"
                    v-hasPermi="['biz:survey:edit']"
                    :disabled="!scope.row.key || scope.row.key === '(空)'"
                    @click="banRisk('device', scope.row.key)"
                  >拉黑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="24" v-if="(stats.blacklist || []).length" style="margin-top:12px">
          <el-card shadow="never" class="panel-card">
            <div slot="header" class="panel-head">
              <span class="panel-title">项目黑名单</span>
              <span class="panel-hint">{{ (stats.blacklist || []).length }} 条</span>
            </div>
            <el-table :data="stats.blacklist || []" size="mini" border>
              <el-table-column label="类型" prop="kind" width="90" align="center">
                <template slot-scope="scope">{{ scope.row.kind === 'device' ? '设备' : 'IP' }}</template>
              </el-table-column>
              <el-table-column label="值" prop="value" min-width="160" :show-overflow-tooltip="true" />
              <el-table-column label="原因" prop="reason" min-width="120" :show-overflow-tooltip="true" />
              <el-table-column label="时间" prop="createTime" width="160" />
              <el-table-column label="操作" width="90" align="center">
                <template slot-scope="scope">
                  <el-button
                    type="text"
                    size="mini"
                    v-hasPermi="['biz:survey:edit']"
                    @click="unbanRisk(scope.row)"
                  >移除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <el-row
        v-if="(stats.dailyTrends || []).length || (stats.channels || []).length"
        :gutter="16"
        class="mb16 overview-charts"
      >
        <el-col :xs="24" :md="(stats.channels || []).length ? 14 : 24" v-if="(stats.dailyTrends || []).length">
          <el-card shadow="never" class="panel-card">
            <div slot="header" class="panel-head">
              <span class="panel-title">每日提交趋势</span>
              <span class="panel-hint">有效答卷</span>
            </div>
            <div ref="dailyChart" class="chart trend-chart" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="(stats.dailyTrends || []).length ? 10 : 24" v-if="(stats.channels || []).length">
          <el-card shadow="never" class="panel-card">
            <div slot="header" class="panel-head">
              <span class="panel-title">渠道分布</span>
              <span class="panel-hint">{{ (stats.channels || []).length }} 个渠道</span>
            </div>
            <div class="channel-body">
              <div ref="channelChart" class="chart channel-chart" />
              <el-table :data="stats.channels || []" size="mini" class="channel-table" :show-header="true">
                <el-table-column label="渠道" prop="channelCode" min-width="100" :show-overflow-tooltip="true" />
                <el-table-column label="答卷数" prop="count" width="80" align="right" />
              </el-table>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="mb16 panel-card" v-if="choiceQuestions.length >= 2">
        <div slot="header" class="panel-head">
          <span class="panel-title">交叉分析</span>
          <span class="panel-hint">任选两道选择题交叉对比</span>
        </div>
        <el-form :inline="true" size="small" class="cross-form">
          <el-form-item label="行题目">
            <el-select v-model="crossQ1" placeholder="选择单选题" style="width:220px" @change="onCrossChange">
              <el-option v-for="q in choiceQuestions" :key="'r'+q.questionId" :label="q.title" :value="q.questionId" :disabled="q.questionId===crossQ2" />
            </el-select>
          </el-form-item>
          <el-form-item label="列题目">
            <el-select v-model="crossQ2" placeholder="选择单选题" style="width:220px" @change="onCrossChange">
              <el-option v-for="q in choiceQuestions" :key="'c'+q.questionId" :label="q.title" :value="q.questionId" :disabled="q.questionId===crossQ1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="crossLoading" :disabled="!crossQ1 || !crossQ2" @click="loadCross">分析</el-button>
          </el-form-item>
        </el-form>
        <p v-if="cross && cross.pairedCount != null" class="cross-meta">有效配对答卷 {{ cross.pairedCount }}</p>
        <el-table v-if="crossTableRows.length" :data="crossTableRows" size="small" border class="cross-table">
          <el-table-column prop="rowLabel" :label="(cross.q1 && cross.q1.title) || '行'" min-width="140" fixed />
          <el-table-column v-for="col in crossColLabels" :key="col.value" :label="col.label" :prop="'c_' + col.value" min-width="90" align="center" />
        </el-table>
        <el-empty v-else-if="crossLoaded" description="暂无交叉数据" :image-size="60" />
      </el-card>

      <div
        v-if="(stats.questions || []).length || (stats.textQuestions || []).length"
        class="section-bar section-bar-sm"
      >
        <h3 class="section-title">分题统计</h3>
        <p class="section-desc">选择题分布与填空样本</p>
      </div>

      <div class="question-grid" v-if="(stats.questions || []).length">
        <el-card
          v-for="q in (stats.questions || [])"
          :key="'c-' + q.questionId"
          shadow="never"
          class="panel-card question-card"
        >
          <div slot="header" class="panel-head q-head">
            <div class="q-title-wrap">
              <span class="panel-title">{{ q.title }}</span>
              <el-tag size="mini" type="info" effect="plain">{{ typeLabel(q.qType) }}</el-tag>
            </div>
            <span v-if="q.answeredCount != null" class="panel-hint">作答 {{ q.answeredCount }}</span>
          </div>
          <template v-if="q.qType === 'matrix_radio'">
            <div v-for="row in (q.rows || [])" :key="row.rowValue" class="matrix-row-block">
              <div class="row-title">{{ row.rowLabel }}</div>
              <el-table :data="row.options || []" size="mini">
                <el-table-column label="选项" prop="label" min-width="100" />
                <el-table-column label="票数" prop="count" width="72" align="center" />
                <el-table-column label="占比" min-width="140">
                  <template slot-scope="scope">
                    <el-progress :percentage="Number(scope.row.percent) || 0" :stroke-width="10" :show-text="true" />
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
          <div v-else class="choice-split">
            <div class="choice-table-wrap">
              <el-table :data="q.options || []" size="small">
                <el-table-column label="选项" prop="label" min-width="90" :show-overflow-tooltip="true" />
                <el-table-column label="票数" prop="count" width="72" align="center" />
                <el-table-column label="占比" min-width="120">
                  <template slot-scope="scope">
                    <el-progress :percentage="Number(scope.row.percent) || 0" :stroke-width="10" />
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div class="choice-chart-wrap">
              <div :ref="'chart-' + q.questionId" class="chart q-chart" />
            </div>
          </div>
        </el-card>
      </div>

      <div class="question-grid text-grid" v-if="(stats.textQuestions || []).length">
        <el-card
          v-for="q in (stats.textQuestions || [])"
          :key="'t-' + q.questionId"
          shadow="never"
          class="panel-card question-card text-card"
        >
          <div slot="header" class="panel-head q-head">
            <div class="q-title-wrap">
              <span class="panel-title">{{ q.title }}</span>
              <el-tag size="mini" type="info" effect="plain">{{ typeLabel(q.qType) }}</el-tag>
            </div>
            <div class="q-meta">
              <span class="panel-hint">共 {{ q.count || 0 }} 条</span>
              <span v-if="q.avg != null" class="panel-hint">均值 {{ q.avg }}</span>
              <span v-if="q.qType === 'nps' && q.npsScore != null" class="panel-hint nps">NPS {{ q.npsScore }}</span>
            </div>
          </div>
          <el-row v-if="q.qType === 'nps'" :gutter="10" class="nps-row">
            <el-col :xs="12" :sm="6"><div class="nps-metric">推荐者<br><b>{{ q.promoters || 0 }}</b></div></el-col>
            <el-col :xs="12" :sm="6"><div class="nps-metric">被动者<br><b>{{ q.passives || 0 }}</b></div></el-col>
            <el-col :xs="12" :sm="6"><div class="nps-metric">贬损者<br><b>{{ q.detractors || 0 }}</b></div></el-col>
            <el-col :xs="12" :sm="6"><div class="nps-metric accent">NPS<br><b>{{ q.npsScore != null ? q.npsScore : '-' }}</b></div></el-col>
          </el-row>
          <div v-if="(q.distribution || []).length" class="choice-split mb12">
            <div class="choice-table-wrap">
              <el-table :data="q.distribution" size="mini" max-height="240">
                <el-table-column label="分值" prop="label" width="88" />
                <el-table-column label="票数" prop="count" width="72" align="center" />
                <el-table-column label="占比" min-width="120">
                  <template slot-scope="scope">
                    <el-progress :percentage="Number(scope.row.percent) || 0" :stroke-width="10" />
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div class="choice-chart-wrap">
              <div :ref="'dist-' + q.questionId" class="chart q-chart" />
            </div>
          </div>
          <div v-if="(q.trends || []).length" :ref="'trend-' + q.questionId" class="chart trend-chart mb12" />
          <el-table v-if="(q.samples || []).length" :data="sampleRows(q)" size="small" max-height="280" class="sample-table">
            <el-table-column type="index" width="48" label="#" />
            <el-table-column label="答案样本（最多 50 条）" prop="value" :show-overflow-tooltip="true" />
          </el-table>
          <el-empty v-else description="暂无填空答案" :image-size="56" />
        </el-card>
      </div>

      <el-empty
        v-if="!loading && !(stats.questions || []).length && !(stats.textQuestions || []).length && !(stats.dailyTrends || []).length"
        description="暂无统计数据"
      />
    </section>

    <el-dialog
      :title="agreeDialog.title || '协议详情'"
      :visible.sync="agreeDialog.open"
      width="800px"
      append-to-body
      custom-class="agree-dialog"
    >
      <agreement-document
        v-if="agreeDialog.open"
        :title="agreeDialog.title"
        :content="agreeDialog.content"
        :agree-label="agreeDialog.agreeLabel"
        :agreed="agreeDialog.agreed"
        :answer-label="agreeDialog.label"
        :submit-time="agreeDialog.submitTime"
        :signatures="agreeDialog.signatures"
        @preview="previewImage"
      />
    </el-dialog>

    <el-dialog :visible.sync="imgPreview.open" width="640px" append-to-body title="预览">
      <img v-if="imgPreview.url" :src="imgPreview.url" class="preview-full" alt="preview" />
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
require('echarts/theme/macarons')
import AgreementDocument from '@/components/biz/AgreementDocument'
import { getSurveyStats, getSurveyCrossStats, getSurveyAnswerMatrix, exportSurveyStats, exportSurveyAnswers, addSurveyBlacklist, removeSurveyBlacklist } from '@/api/biz/survey'
import { blobValidate } from '@/utils/ruoyi'
import { saveAs } from 'file-saver'

const TYPE_LABELS = {
  radio: '单选',
  checkbox: '多选',
  select: '下拉',
  yesno: '是非',
  image_radio: '图片单选',
  image_checkbox: '图片多选',
  likert: '量表',
  cascade_select: '级联',
  matrix_radio: '矩阵单选',
  input: '填空',
  textarea: '多行填空',
  phone: '手机',
  date: '日期',
  datetime: '日期时间',
  time: '时间',
  rate: '评分',
  nps: 'NPS',
  number: '数字',
  slider: '滑块',
  file: '附件',
  email: '邮箱',
  url: '网址',
  idcard: '身份证',
  agreement: '协议同意',
  signature: '手写签名'
}

export default {
  name: 'BizSurveyStats',
  components: { AgreementDocument },
  data() {
    return {
      loading: false,
      exporting: false,
      surveyId: null,
      stats: {},
      charts: {},
      crossQ1: null,
      crossQ2: null,
      cross: null,
      crossLoading: false,
      crossLoaded: false,
      matrixLoading: false,
      matrixExporting: false,
      matrixValidFlag: '1',
      matrixPageNum: 1,
      matrixPageSize: 20,
      matrixTotal: 0,
      matrixColumns: [],
      matrixRows: [],
      agreeDialog: {
        open: false,
        title: '',
        label: '',
        submitTime: '',
        content: '',
        agreeLabel: '',
        agreed: false,
        signatures: []
      },
      imgPreview: { open: false, url: '' }
    }
  },
  computed: {
    choiceQuestions() {
      const types = ['radio', 'select', 'yesno', 'image_radio', 'likert']
      return (this.stats.questions || []).filter(q => types.includes(q.qType))
    },
    choiceCount() {
      return (this.stats.questions || []).filter(q => q.qType !== 'matrix_radio').length
    },
    convertRateText() {
      const v = this.stats.convertRate
      if (v == null || v === '') return '0%'
      return v + '%'
    },
    crossColLabels() {
      return ((this.cross && this.cross.q2 && this.cross.q2.options) || []).map(o => ({
        value: String(o.value),
        label: o.label || o.value
      }))
    },
    crossTableRows() {
      if (!this.cross || !this.cross.q1) return []
      const rows = (this.cross.q1.options || []).map(o => {
        const r = { rowLabel: o.label || o.value, rowValue: String(o.value) }
        this.crossColLabels.forEach(c => { r['c_' + c.value] = 0 })
        return r
      })
      const map = {}
      rows.forEach(r => { map[r.rowValue] = r })
      ;(this.cross.cells || []).forEach(cell => {
        const rv = String(cell.rowValue)
        const cv = String(cell.colValue)
        if (!map[rv]) {
          const r = { rowLabel: cell.rowLabel || rv, rowValue: rv }
          this.crossColLabels.forEach(c => { r['c_' + c.value] = 0 })
          map[rv] = r
          rows.push(r)
        }
        map[rv]['c_' + cv] = Number(cell.count) || 0
      })
      return rows
    }
  },
  created() {
    this.surveyId = this.$route.params.surveyId
    this.load()
  },
  beforeDestroy() {
    this.disposeCharts()
    window.removeEventListener('resize', this.resizeCharts)
  },
  methods: {
    typeLabel(t) {
      return TYPE_LABELS[t] || t
    },
    goBack() { this.$router.push('/biz/survey') },
    goAnswers() {
      this.$router.push('/biz/survey-answers/index/' + this.surveyId)
    },
    goAnswerDetail(answerId) {
      this.$router.push({
        path: '/biz/survey-answers/index/' + this.surveyId,
        query: { answerId }
      })
    },
    banRisk(kind, value) {
      if (!value || value === '(空)') return
      const label = kind === 'device' ? '设备' : 'IP'
      this.$modal.confirm('确认拉黑该' + label + '，并将已有答卷标记为无效？').then(() => {
        return addSurveyBlacklist(this.surveyId, {
          kind,
          value,
          reason: '统计页一键拉黑',
          markInvalid: true
        })
      }).then(() => {
        this.$modal.msgSuccess('已拉黑')
        this.load()
      }).catch(() => {})
    },
    unbanRisk(row) {
      if (!row || !row.id) return
      this.$modal.confirm('确认移除该黑名单？').then(() => {
        return removeSurveyBlacklist(this.surveyId, row.id)
      }).then(() => {
        this.$modal.msgSuccess('已移除')
        this.load()
      }).catch(() => {})
    },
    cellOf(row, col) {
      if (!row || !col || !row.cells) return null
      return row.cells[String(col.questionId)] || null
    },
    cellDisplay(row, col) {
      const c = this.cellOf(row, col)
      return c && c.display != null ? c.display : ''
    },
    cellMediaUrl(row, col) {
      const c = this.cellOf(row, col)
      if (!c || !c.url) return ''
      const path = String(c.url)
      if (path.startsWith('http') || path.startsWith('data:')) return path
      return process.env.VUE_APP_BASE_API + path
    },
    cellFileName(row, col) {
      const c = this.cellOf(row, col)
      return (c && (c.fileName || c.display)) || '附件'
    },
    mediaUrlFromCell(cell) {
      if (!cell || !cell.url) return ''
      const path = String(cell.url)
      if (path.startsWith('http') || path.startsWith('data:')) return path
      return process.env.VUE_APP_BASE_API + path
    },
    openAgreement(row, col) {
      const cell = this.cellOf(row, col)
      const agreed = cell && (cell.raw === '1' || cell.display === '已同意')
      const signatures = (col.boundSignatures || []).map(sig => {
        const sc = row.cells && row.cells[String(sig.questionId)]
        return {
          questionId: sig.questionId,
          title: sig.title,
          url: this.mediaUrlFromCell(sc)
        }
      })
      this.agreeDialog = {
        open: true,
        title: col.title || '协议详情',
        label: row.label || ('#' + row.answerId),
        submitTime: row.submitTime || '',
        content: col.content || '',
        agreeLabel: col.agreeLabel || '我已阅读并同意',
        agreed: !!agreed,
        signatures
      }
    },
    previewImage(url) {
      if (!url) return
      this.imgPreview = { open: true, url }
    },
    onMatrixFilterChange() {
      this.matrixPageNum = 1
      this.loadMatrix()
    },
    handleExportMatrix() {
      const flag = this.matrixValidFlag
      const scopeTip = flag === '0' ? '无效' : (flag === 'all' ? '全部' : '有效')
      this.$modal.confirm('确认导出当前筛选下的「' + scopeTip + '」答题列表为 Excel？（每人一行，含各题答案）').then(() => {
        this.matrixExporting = true
        const q = {}
        if (flag === '0' || flag === '1') q.validFlag = flag
        return exportSurveyAnswers(this.surveyId, q)
      }).then(async data => {
        if (blobValidate(data)) {
          const suffix = flag === '0' ? '-无效答卷' : (flag === 'all' ? '-全部答卷' : '-有效答卷')
          saveAs(new Blob([data]), (this.stats.surveyName || 'survey') + suffix + '.xlsx')
        } else {
          this.$modal.msgError('导出失败')
        }
      }).catch(() => {}).finally(() => { this.matrixExporting = false })
    },
    sampleRows(q) {
      return (q.samples || []).map(v => ({ value: v }))
    },
    loadMatrix() {
      if (!this.surveyId) return
      this.matrixLoading = true
      getSurveyAnswerMatrix(this.surveyId, {
        pageNum: this.matrixPageNum,
        pageSize: this.matrixPageSize,
        validFlag: this.matrixValidFlag
      }).then(res => {
        const data = res.data || {}
        this.matrixTotal = Number(data.total) || 0
        this.matrixColumns = data.columns || []
        this.matrixRows = data.rows || []
      }).finally(() => { this.matrixLoading = false })
    },
    disposeCharts() {
      Object.keys(this.charts).forEach(k => {
        if (this.charts[k]) this.charts[k].dispose()
      })
      this.charts = {}
    },
    resizeCharts() {
      Object.keys(this.charts).forEach(k => this.charts[k] && this.charts[k].resize())
    },
    initChart(key, el) {
      const node = Array.isArray(el) ? el[0] : el
      if (!node) return null
      if (this.charts[key]) this.charts[key].dispose()
      const chart = echarts.init(node, 'macarons')
      this.charts[key] = chart
      return chart
    },
    renderCharts() {
      this.$nextTick(() => {
        this.disposeCharts()
        if ((this.stats.dailyTrends || []).length) {
          const chart = this.initChart('daily', this.$refs.dailyChart)
          if (chart) {
            const dates = this.stats.dailyTrends.map(t => t.date)
            const counts = this.stats.dailyTrends.map(t => Number(t.count) || 0)
            chart.setOption({
              tooltip: { trigger: 'axis' },
              grid: { left: 40, right: 20, top: 30, bottom: 30 },
              xAxis: { type: 'category', data: dates },
              yAxis: { type: 'value', minInterval: 1 },
              series: [{ name: '答卷数', type: 'bar', data: counts, barMaxWidth: 36, itemStyle: { color: '#1d4ed8' } }]
            })
          }
        }
        if ((this.stats.channels || []).length) {
          const chart = this.initChart('channel', this.$refs.channelChart)
          if (chart) {
            chart.setOption({
              tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
              series: [{
                type: 'pie',
                radius: ['40%', '68%'],
                center: ['50%', '50%'],
                data: this.stats.channels.map(c => ({ name: c.channelCode, value: Number(c.count) || 0 })),
                label: { formatter: '{b}\n{d}%' }
              }]
            })
          }
        }
        ;(this.stats.questions || []).filter(q => q.qType !== 'matrix_radio').forEach(q => {
          const chart = this.initChart('chart-' + q.questionId, this.$refs['chart-' + q.questionId])
          if (!chart) return
          const data = (q.options || []).map(o => ({ name: o.label || o.value, value: Number(o.count) || 0 }))
          chart.setOption({
            tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
            legend: { type: 'scroll', bottom: 0 },
            series: [{
              type: 'pie',
              radius: ['35%', '62%'],
              center: ['50%', '45%'],
              data,
              label: { formatter: '{b}\n{d}%' }
            }]
          })
        })
        ;(this.stats.textQuestions || []).forEach(q => {
          if ((q.distribution || []).length) {
            const chart = this.initChart('dist-' + q.questionId, this.$refs['dist-' + q.questionId])
            if (chart) {
              chart.setOption({
                tooltip: { trigger: 'axis' },
                grid: { left: 40, right: 16, top: 24, bottom: 28 },
                xAxis: { type: 'category', data: q.distribution.map(d => d.label || d.value) },
                yAxis: { type: 'value', minInterval: 1 },
                series: [{
                  type: 'bar',
                  data: q.distribution.map(d => Number(d.count) || 0),
                  barMaxWidth: 28,
                  itemStyle: { color: '#36cfc9' }
                }]
              })
            }
          }
          if ((q.qType === 'rate' || q.qType === 'nps' || q.qType === 'slider') && (q.trends || []).length) {
            const chart = this.initChart('trend-' + q.questionId, this.$refs['trend-' + q.questionId])
            if (!chart) return
            chart.setOption({
              tooltip: { trigger: 'axis' },
              grid: { left: 40, right: 20, top: 30, bottom: 30 },
              xAxis: { type: 'category', data: q.trends.map(t => t.date) },
              yAxis: { type: 'value', scale: true },
              series: [{
                name: '日均',
                type: 'line',
                smooth: true,
                data: q.trends.map(t => Number(t.avg) || 0),
                areaStyle: { opacity: 0.08 }
              }]
            })
          }
        })
        window.removeEventListener('resize', this.resizeCharts)
        window.addEventListener('resize', this.resizeCharts)
        setTimeout(() => this.resizeCharts(), 80)
      })
    },
    load() {
      this.loading = true
      getSurveyStats(this.surveyId).then(res => {
        this.stats = res.data || {}
        this.renderCharts()
        if (this.choiceQuestions.length >= 2 && !this.crossQ1) {
          this.crossQ1 = this.choiceQuestions[0].questionId
          this.crossQ2 = this.choiceQuestions[1].questionId
          this.loadCross()
        }
      }).finally(() => { this.loading = false })
      this.loadMatrix()
    },
    onCrossChange() {
      this.cross = null
      this.crossLoaded = false
    },
    loadCross() {
      if (!this.crossQ1 || !this.crossQ2 || this.crossQ1 === this.crossQ2) return
      this.crossLoading = true
      getSurveyCrossStats(this.surveyId, this.crossQ1, this.crossQ2).then(res => {
        this.cross = res.data || {}
        this.crossLoaded = true
      }).finally(() => { this.crossLoading = false })
    },
    handleExportStats() {
      this.$modal.confirm('确认导出当前问卷的统计汇总 Excel？若已做交叉分析将一并导出。').then(() => {
        this.exporting = true
        const q = {}
        if (this.crossQ1 && this.crossQ2 && this.crossQ1 !== this.crossQ2) {
          q.q1 = this.crossQ1
          q.q2 = this.crossQ2
        }
        return exportSurveyStats(this.surveyId, q)
      }).then(async data => {
        if (blobValidate(data)) {
          saveAs(new Blob([data]), (this.stats.surveyName || 'survey') + '-统计汇总.xlsx')
        } else {
          this.$modal.msgError('导出失败')
        }
      }).catch(() => {}).finally(() => { this.exporting = false })
    }
  }
}
</script>

<style scoped>
.stats-page >>> .el-page-header__content {
  font-size: 18px;
  font-weight: 700;
  color: var(--biz-ink);
}
.metric-row .el-col { margin-bottom: 14px; }
.biz-stat-card .stat-icon.tone-muted { background: #f1f5f9; color: #64748b; }
.biz-stat-card .stat-icon.tone-teal { background: #ecfdf5; color: #0f766e; }
.biz-stat-card .stat-icon.tone-violet { background: #f5f3ff; color: #6d28d9; }
.biz-stat-card .stat-icon.tone-amber { background: #fff7ed; color: #c2410c; }
.biz-stat-card .stat-icon.tone-rose { background: #fff1f2; color: #e11d48; }
.mb16 { margin-bottom: 16px; }
.mb12 { margin-bottom: 12px; }
.mt8 { margin-top: 8px; }

.stats-below {
  margin-top: 8px;
  padding-top: 8px;
}

.section-bar {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
  margin: 8px 0 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--biz-line);
}

.section-bar-sm {
  margin-top: 20px;
}

.section-title {
  margin: 0;
  font-size: 16px;
  font-weight: 650;
  color: var(--biz-ink);
  letter-spacing: 0.02em;
}

.section-desc {
  margin: 0;
  font-size: 12px;
  color: var(--biz-muted-soft);
}

.panel-card {
  border: 1px solid var(--biz-line);
  border-radius: var(--biz-radius);
  overflow: hidden;
}

.panel-card >>> .el-card__header {
  padding: 12px 16px;
  background: var(--biz-bg-soft);
  border-bottom: 1px solid var(--biz-line);
}

.panel-card >>> .el-card__body {
  padding: 14px 16px 16px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.panel-title {
  font-size: 14px;
  font-weight: 650;
  color: #0f172a;
  line-height: 1.4;
}

.panel-hint {
  font-size: 12px;
  color: #94a3b8;
  font-weight: normal;
}

.overview-charts .panel-card {
  height: 100%;
  margin-bottom: 16px;
}

.channel-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.channel-chart {
  height: 200px;
  min-height: 180px;
}

.channel-table {
  width: 100%;
}

.cross-form {
  margin-bottom: 4px;
}

.cross-form >>> .el-form-item {
  margin-bottom: 10px;
}

.cross-meta {
  margin: 0 0 12px;
  font-size: 12px;
  color: #64748b;
}

.cross-table {
  width: 100%;
}

.question-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.text-grid {
  grid-template-columns: 1fr;
}

.question-card {
  margin-bottom: 0;
  min-width: 0;
}

.q-head {
  align-items: flex-start;
}

.q-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
}

.q-title-wrap .panel-title {
  max-width: 100%;
  word-break: break-word;
}

.q-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.choice-split {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(180px, 0.85fr);
  gap: 12px;
  align-items: stretch;
}

.choice-table-wrap,
.choice-chart-wrap {
  min-width: 0;
}

.chart {
  height: 260px;
  width: 100%;
  min-height: 220px;
}

.q-chart {
  height: 240px;
  min-height: 200px;
}

.trend-chart {
  height: 220px;
  min-height: 180px;
}

.row-title {
  font-weight: 600;
  margin: 0 0 8px;
  color: #334155;
  font-size: 13px;
}

.matrix-row-block + .matrix-row-block {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #e2e8f0;
}

.nps {
  color: #2563eb;
  font-weight: 600;
}

.nps-row {
  margin-bottom: 12px;
}

.nps-metric {
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  padding: 10px 8px;
  text-align: center;
  color: #64748b;
  font-size: 12px;
  margin-bottom: 8px;
}

.nps-metric b {
  display: block;
  margin-top: 4px;
  font-size: 20px;
  color: #0f172a;
}

.nps-metric.accent {
  background: #eff6ff;
  border-color: #dbeafe;
  color: #3b82f6;
}

.nps-metric.accent b {
  color: #1d4ed8;
}

.sample-table {
  width: 100%;
}

.matrix-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.matrix-tools { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.matrix-table { width: 100%; }
.ml6 { margin-left: 6px; }
.cell-agree { margin-left: 6px; color: #64748b; font-size: 12px; }
.sig-thumb {
  max-width: 88px;
  max-height: 40px;
  object-fit: contain;
  cursor: pointer;
  vertical-align: middle;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
}
.preview-full { display: block; max-width: 100%; margin: 0 auto; }

@media (max-width: 1200px) {
  .question-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .choice-split {
    grid-template-columns: 1fr;
  }

  .q-chart {
    height: 220px;
  }

  .section-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>
