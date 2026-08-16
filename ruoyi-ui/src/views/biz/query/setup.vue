<template>
  <div class="biz-page query-setup">
    <div class="biz-page-head">
      <el-page-header @back="leave" :content="'设置向导 - ' + (queryName || '')" />
    </div>

    <el-alert
      v-if="fromTemplate && !isPublished"
      class="mb16 tpl-ready"
      type="success"
      :closable="true"
      show-icon
      @close="fromTemplate = false"
    >
      <div slot="title" class="tpl-ready-title">
        <span>模板已就绪：字段、页面与演示数据已生成，可直接预览或发布。</span>
        <span class="tpl-ready-actions">
          <el-button type="text" size="mini" @click="jumpPreview">预览</el-button>
          <el-button type="text" size="mini" @click="jumpPublish">去发布</el-button>
          <el-button type="text" size="mini" :loading="publishing" :disabled="!canPublish" @click="doPublish" v-hasPermi="['biz:query:publish']">一键发布</el-button>
        </span>
      </div>
    </el-alert>

    <div class="biz-panel steps-panel mb16">
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step title="准备数据" description="上传 Excel，按字段合并多表" />
        <el-step title="设置查询项" description="学生填什么、看到什么" />
        <el-step title="打扮页面" description="成绩单样式与预览" />
        <el-step title="发布分享" description="链接、二维码与海报" />
      </el-steps>
    </div>

    <el-alert
      v-if="stepHint"
      :title="stepHint"
      :type="stepHintType"
      :closable="false"
      show-icon
      class="mb16"
    />

    <div class="biz-panel step-panel">
    <!-- Step 1: Import + Join -->
    <div v-show="activeStep === 0" class="step-pane">
      <div class="scenario-cards mb16">
        <div class="scenario" @click="applyScenarioTip('score')">
          <strong>成绩查询</strong>
          <span>学生表 + 成绩表，按学号合并</span>
        </div>
        <div class="scenario" @click="applyScenarioTip('staff')">
          <strong>通讯录</strong>
          <span>单表上传即可，姓名模糊查询</span>
        </div>
        <div class="scenario" @click="applyScenarioTip('multi')">
          <strong>多表一对多</strong>
          <span>一人多条记录时选「合并」或「取末条」</span>
        </div>
      </div>
      <query-datasets
        v-if="queryId"
        ref="datasetsStep"
        :query-id="queryId"
        @loaded="onDatasetsLoaded"
        @materialized="onMaterialized"
      />
      <div class="mt16" v-if="hasData">
        <el-button type="success" plain @click="goNext">结果已生成，下一步</el-button>
      </div>
    </div>

    <!-- Step 2: Fields -->
    <div v-show="activeStep === 1" class="step-pane">
      <query-fields
        v-if="queryId && activeStep === 1"
        ref="fieldsStep"
        :embedded="true"
        :query-id-prop="queryId"
      />
    </div>

    <!-- Step 3: Design + Preview -->
    <div v-show="activeStep === 2" class="step-pane">
      <el-tabs v-model="designTab" class="design-tabs">
        <el-tab-pane label="页面设计" name="design">
          <query-page
            v-if="queryId && activeStep === 2"
            ref="pageStep"
            :embedded="true"
            :query-id-prop="queryId"
          />
        </el-tab-pane>
        <el-tab-pane label="真实数据预览" name="preview">
          <query-preview
            v-if="queryId && activeStep === 2 && designTab === 'preview'"
            ref="previewStep"
            :embedded="true"
            :query-id-prop="queryId"
          />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- Step 4: Publish -->
    <div v-show="activeStep === 3" class="step-pane">
      <div class="publish-block">
        <div class="publish-title">发布查询</div>
        <el-descriptions :column="1" border size="small" class="mb16">
          <el-descriptions-item label="名称">{{ queryName }}</el-descriptions-item>
          <el-descriptions-item label="数据行数">{{ (queryInfo && queryInfo.rowCount) || 0 }}</el-descriptions-item>
          <el-descriptions-item label="字段数">{{ fieldCount }}</el-descriptions-item>
          <el-descriptions-item label="查询条件字段">{{ queryFieldCount }}</el-descriptions-item>
          <el-descriptions-item label="结果展示字段">{{ listFieldCount }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <dict-tag :options="dict.type.biz_query_status" :value="(queryInfo && queryInfo.status) || '0'" />
          </el-descriptions-item>
          <el-descriptions-item v-if="queryInfo && queryInfo.publicCode" label="短码">{{ queryInfo.publicCode }}</el-descriptions-item>
        </el-descriptions>

        <div class="checklist mb16">
          <div class="check-item" :class="{ ok: publishChecks.hasData }">
            <i :class="publishChecks.hasData ? 'el-icon-success' : 'el-icon-warning'" />
            数据已导入且解析完成
          </div>
          <div class="check-item" :class="{ ok: publishChecks.hasQueryField }">
            <i :class="publishChecks.hasQueryField ? 'el-icon-success' : 'el-icon-warning'" />
            至少 1 个查询条件字段
          </div>
          <div class="check-item" :class="{ ok: publishChecks.hasListField }">
            <i :class="publishChecks.hasListField ? 'el-icon-success' : 'el-icon-warning'" />
            至少 1 个结果展示字段
          </div>
          <div class="check-item" :class="{ ok: publishChecks.hasRequired }">
            <i :class="publishChecks.hasRequired ? 'el-icon-success' : 'el-icon-warning'" />
            至少 1 个必填查询条件（防空查撞库）
          </div>
          <div class="check-item tip" :class="{ ok: publishChecks.hasCaptchaOrLimit }">
            <i :class="publishChecks.hasCaptchaOrLimit ? 'el-icon-success' : 'el-icon-info'" />
            建议开启验证码或日查询上限（编辑查询可配）
          </div>
        </div>

        <el-alert
          v-if="!canPublish"
          :title="publishBlockReason"
          type="warning"
          :closable="false"
          show-icon
          class="mb16"
        />

        <div v-if="publishedLink" class="qr-box">
          <img v-if="posterDataUrl" :src="posterDataUrl" class="poster-img" alt="poster" />
          <img v-else-if="qrDataUrl" :src="qrDataUrl" alt="qrcode" />
          <el-form :model="posterBg" label-width="90px" size="mini" class="poster-bg-panel">
            <poster-bg-form :model="posterBg" @change="onPosterBgChange" />
            <el-form-item>
              <el-button type="primary" size="mini" :loading="posterSaving" @click="savePosterBg">保存背景并刷新</el-button>
            </el-form-item>
          </el-form>
          <div class="poster-actions">
            <el-button type="primary" size="mini" icon="el-icon-download" :loading="posterLoading" @click="downloadPoster">下载分享海报</el-button>
            <el-button size="mini" @click="copyText(h5Link)">复制 H5</el-button>
            <el-button size="mini" plain @click="copyText(publishedLink)">复制管理端链接</el-button>
            <el-button size="mini" type="success" plain @click="openPublic">模拟学生查询</el-button>
          </div>
          <p class="link-label">管理端公开页</p>
          <p class="link">{{ publishedLink }}</p>
          <p class="link-label">独立 H5</p>
          <p class="link">{{ h5Link }}</p>
        </div>

        <div class="mt16">
          <el-button
            type="primary"
            icon="el-icon-s-promotion"
            :loading="publishing"
            :disabled="!canPublish"
            @click="doPublish"
            v-hasPermi="['biz:query:publish']"
          >{{ isPublished ? '重新发布 / 刷新链接' : '立即发布' }}</el-button>
          <el-button v-if="isPublished && queryInfo.publicCode" @click="openPublic">打开公开页</el-button>
        </div>

        <div class="reach-panel mt16">
          <div class="publish-title">开放时间与预约</div>
          <el-form label-width="100px" size="small">
            <el-form-item label="开始时间">
              <el-date-picker v-model="reachForm.startTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="可选" style="width:100%" />
            </el-form-item>
            <el-form-item label="截止时间">
              <el-date-picker v-model="reachForm.endTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="可选" style="width:100%" />
            </el-form-item>
            <el-form-item label="截止提醒">
              <el-input-number v-model="reachForm.remindHours" :min="0" :max="168" />
              <span class="tip">小时；0 不提醒</span>
            </el-form-item>
            <el-form-item label="提醒邮件" v-if="reachForm.remindHours > 0">
              <el-switch v-model="reachForm.remindMail" active-value="1" inactive-value="0" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" plain size="mini" :loading="reachSaving" @click="saveReachSettings">保存时间设置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div v-if="!isPublished" class="schedule-box mt16">
          <div class="publish-title">预约发布</div>
          <p class="tip-block">设定未来时间后到点自动上线（约每分钟扫描）。若已开启发布审批，到点将提交审批而非直接发布。</p>
          <el-date-picker
            v-model="scheduleAt"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="选择发布时间"
            style="width: 220px; margin-right: 8px"
          />
          <el-button
            type="warning"
            plain
            :loading="scheduling"
            :disabled="!canPublish || !scheduleAt"
            @click="doSchedule"
            v-hasPermi="['biz:query:publish']"
          >确认预约</el-button>
          <el-button
            v-if="queryInfo && queryInfo.publishAt"
            type="text"
            :loading="scheduling"
            @click="doCancelSchedule"
            v-hasPermi="['biz:query:publish']"
          >取消预约（{{ queryInfo.publishAt }}）</el-button>
        </div>
        <el-alert
          v-if="queryInfo && queryInfo.publishAt && !isPublished"
          class="mt16"
          type="info"
          :closable="false"
          show-icon
          :title="'已预约于 ' + queryInfo.publishAt + ' 自动发布'"
        />
      </div>
    </div>
    </div>

    <el-dialog title="发送发布通知" :visible.sync="notifyOpen" width="480px" append-to-body>
      <p class="tip-block mb8">可选：将公开链接发到邮箱（需已配置 SMTP）。</p>
      <el-input v-model="notifyEmails" type="textarea" :rows="2" placeholder="收件邮箱，多个用逗号分隔" />
      <div slot="footer" class="dialog-footer">
        <el-button @click="notifyOpen = false">跳过</el-button>
        <el-button type="primary" :loading="notifySending" @click="doSendPublishNotify">发送</el-button>
      </div>
    </el-dialog>

    <div class="biz-wizard-footer">
      <el-button @click="leave">返回列表</el-button>
      <div class="spacer" />
      <el-button :disabled="activeStep === 0" @click="goPrev">上一步</el-button>
      <el-button v-if="activeStep < 3" type="primary" :loading="nextLoading" @click="handleNext">
        {{ activeStep === 2 ? '保存并下一步' : '下一步' }}
      </el-button>
      <el-button v-else type="success" @click="leave">完成</el-button>
    </div>
  </div>
</template>

<script>
import { getQuery, publishQuery, saveQueryPage, updateQuery } from '@/api/biz/query'
import { scheduleQueryPublish, cancelQuerySchedule, sendPublishNotify } from '@/api/biz/reach'
import { toQrDataUrl, buildSharePoster, downloadDataUrl, resolvePosterBg } from '@/utils/qrcode'
import { parseLayout } from '@/utils/bizQueryField'
import PosterBgForm from '@/components/PosterBgForm'
import QueryDatasets from './datasets'
import QueryFields from './fields'
import QueryPage from './page'
import QueryPreview from './preview'

export default {
  name: 'BizQuerySetup',
  dicts: ['biz_query_status'],
  components: { QueryDatasets, QueryFields, QueryPage, QueryPreview, PosterBgForm },
  data() {
    return {
      queryId: null,
      queryName: '',
      queryInfo: null,
      fieldCount: 0,
      queryFieldCount: 0,
      listFieldCount: 0,
      activeStep: 0,
      designTab: 'design',
      publishing: false,
      scheduling: false,
      scheduleAt: undefined,
      reachSaving: false,
      notifyOpen: false,
      notifyEmails: '',
      notifySending: false,
      reachForm: {
        startTime: undefined,
        endTime: undefined,
        remindHours: 24,
        remindMail: '0'
      },
      nextLoading: false,
      publishedLink: '',
      h5Link: '',
      qrDataUrl: '',
      posterDataUrl: '',
      posterLoading: false,
      posterSaving: false,
      posterBg: {
        posterBgType: 'theme',
        posterBgColor: '#eef2ff',
        posterBgImage: '',
        posterBgOverlay: 40
      },
      pageSnapshot: null,
      datasetCount: 0,
      requiredFieldCount: 0,
      fromTemplate: false
    }
  },
  computed: {
    hasData() {
      return !!(this.queryInfo && this.queryInfo.rowCount > 0 && this.queryInfo.parseStatus !== '1' && this.queryInfo.parseStatus !== '2')
    },
    canPublish() {
      const c = this.publishChecks
      return c.hasData && c.hasQueryField && c.hasListField && c.hasRequired
    },
    publishChecks() {
      const q = this.queryInfo || {}
      const dataOk = !!(q.rowCount > 0 && q.parseStatus !== '1' && q.parseStatus !== '2')
      const captchaOn = String(q.needCaptcha || '0') === '1'
      const limitOn = !!(q.dailyLimit && q.dailyLimit > 0)
      return {
        hasData: dataOk,
        hasQueryField: this.queryFieldCount > 0,
        hasListField: this.listFieldCount > 0,
        hasRequired: this.requiredFieldCount > 0,
        hasCaptchaOrLimit: captchaOn || limitOn
      }
    },
    publishBlockReason() {
      if (!this.publishChecks.hasData) return '请先完成数据导入（解析成功且行数大于 0）后再发布。'
      if (!this.publishChecks.hasQueryField) return '请至少开启一个「查询条件」字段后再发布。'
      if (!this.publishChecks.hasListField) return '请至少开启一个「结果列」字段后再发布。'
      if (!this.publishChecks.hasRequired) return '请至少将一个查询条件设为「必填」，避免空条件枚举撞库。'
      return '暂不可发布'
    },
    isPublished() {
      return String((this.queryInfo && this.queryInfo.status) || '') === '1'
    },
    parseLabel() {
      const row = this.queryInfo || {}
      if (row.parseStatus === '1') return '解析中'
      if (row.parseStatus === '2') return '解析失败'
      if (!row.rowCount) return '待上传'
      return '数据就绪'
    },
    parseTagType() {
      const row = this.queryInfo || {}
      if (row.parseStatus === '1') return 'warning'
      if (row.parseStatus === '2') return 'danger'
      if (!row.rowCount) return 'info'
      return 'success'
    },
    stepHint() {
      if (this.activeStep === 0) {
        if (this.datasetCount >= 2) {
          return '已上传多张表：选择左右表与关联字段（如学号=学号），一对多时建议用「合并」或「取末条」，再点「生成结果」。'
        }
        if (this.hasData) {
          return '数据已就绪。若还要拼接成绩/班级等附表，可继续上传后配置关联；否则进入下一步设置查询项。'
        }
        return '先上传主表 Excel（如学生名单）。若有成绩表/班级表，再上传并按「学号」等字段关联生成结果。'
      }
      if (this.activeStep === 1) {
        return '把「学号」设为精确查询且必填，姓名可模糊；结果列只保留需要展示的字段，敏感信息可开脱敏。'
      }
      if (this.activeStep === 2) {
        return '可选用「成绩单」模板，并指定卡片主标题/摘要字段；保存后用「真实数据预览」或模拟查询验收。'
      }
      return '发布后下载分享海报发到班级群；建议同时开启验证码或日查询上限，降低学号枚举风险。'
    },
    stepHintType() {
      return this.activeStep === 3 ? 'success' : 'info'
    }
  },
  created() {
    this.queryId = this.$route.params.queryId
    this.fromTemplate = this.$route.query.fromTemplate === '1'
    const stepQ = parseInt(this.$route.query.step, 10)
    if (!Number.isNaN(stepQ) && stepQ >= 0 && stepQ <= 3) {
      this.activeStep = stepQ
    }
    this.refreshMeta().then(() => {
      if (this.$route.query.step == null) {
        this.activeStep = this.inferStep()
      }
      // 模板项目即便没带 fromTemplate，也可根据命名识别
      if (!this.fromTemplate && this.queryName && this.queryName.indexOf('（模板）') >= 0) {
        this.fromTemplate = true
      }
      if (this.isPublished && this.queryInfo.publicCode) {
        this.fillPublishLinks(this.queryInfo.publicCode)
      }
    })
  },
  methods: {
    applyFields(fields) {
      const list = fields || []
      this.fieldCount = list.length
      this.queryFieldCount = list.filter(f => f.isQuery === '1').length
      this.listFieldCount = list.filter(f => f.isList === '1').length
      this.requiredFieldCount = list.filter(f => f.isQuery === '1' && f.isRequired !== '0').length
    },
    inferStep() {
      const q = this.queryInfo || {}
      if (!q.rowCount || q.parseStatus === '1' || q.parseStatus === '2') return 0
      if (!this.fieldCount) return 1
      if (String(q.status) === '1') return 3
      return 2
    },
    refreshMeta() {
      return getQuery(this.queryId).then(res => {
        const data = res.data || {}
        this.queryInfo = data.query || {}
        this.queryName = this.queryInfo.queryName || ''
        this.datasetCount = (data.datasets || []).length
        this.applyFields(data.fields)
        this.applyPagePoster(data.page)
        this.reachForm = {
          startTime: this.queryInfo.startTime,
          endTime: this.queryInfo.endTime,
          remindHours: this.queryInfo.remindHours == null ? 24 : this.queryInfo.remindHours,
          remindMail: this.queryInfo.remindMail || '0'
        }
        document.title = '设置向导 - ' + (this.queryName || '')
      })
    },
    applyPagePoster(page) {
      const p = page || {}
      this.pageSnapshot = p
      const layout = parseLayout(p)
      this.posterBg = {
        posterBgType: layout.posterBgType || 'theme',
        posterBgColor: layout.posterBgColor || '#eef2ff',
        posterBgImage: layout.posterBgImage || '',
        posterBgOverlay: layout.posterBgOverlay == null ? 40 : layout.posterBgOverlay
      }
    },
    onPosterBgChange() {
      if (this.h5Link) this.refreshPosterPreview()
    },
    async refreshPosterPreview() {
      if (!this.h5Link) return
      this.posterLoading = true
      try {
        const theme = (this.pageSnapshot && this.pageSnapshot.themeColor) || '#1677ff'
        const bg = resolvePosterBg(this.posterBg, theme)
        this.posterDataUrl = await buildSharePoster({
          title: this.queryName || '查询',
          subtitle: (this.queryInfo && this.queryInfo.queryDesc) || '扫码即可查询',
          link: this.h5Link,
          qrDataUrl: this.qrDataUrl,
          apiBase: process.env.VUE_APP_BASE_API,
          ...bg
        })
      } catch (e) { /* ignore */ }
      finally { this.posterLoading = false }
    },
    savePosterBg() {
      if (!this.queryId) return
      this.posterSaving = true
      const page = this.pageSnapshot || {}
      const layout = { ...parseLayout(page), ...this.posterBg }
      const payload = {
        queryId: Number(this.queryId),
        title: page.title || this.queryName || '',
        subtitle: page.subtitle || '',
        themeColor: page.themeColor || '#1677ff',
        bannerUrl: page.bannerUrl || '',
        resultTips: page.resultTips || '未查询到相关数据',
        layoutJson: JSON.stringify(layout)
      }
      saveQueryPage(payload).then(() => {
        this.pageSnapshot = { ...page, ...payload }
        this.$modal.msgSuccess('海报背景已保存')
        return this.refreshPosterPreview()
      }).finally(() => { this.posterSaving = false })
    },
    syncStepQuery() {
      this.$router.replace({
        path: '/biz/query-setup/index/' + this.queryId,
        query: { ...this.$route.query, step: String(this.activeStep) }
      }).catch(() => {})
    },
    leave() {
      this.$router.push('/biz/query')
    },
    goPrev() {
      if (this.activeStep <= 0) return
      this.activeStep -= 1
      this.syncStepQuery()
    },
    goNext() {
      if (this.activeStep >= 3) return
      this.activeStep += 1
      this.syncStepQuery()
      this.refreshMeta()
    },
    async handleNext() {
      this.nextLoading = true
      try {
        if (this.activeStep === 0) {
          if (!this.hasData) {
            this.$modal.msgWarning('请先上传数据并点击「生成结果」')
            return
          }
          this.goNext()
          return
        }
        if (this.activeStep === 1) {
          const ref = this.$refs.fieldsStep
          if (ref && ref.saveForWizard) {
            await ref.saveForWizard()
          }
          await this.refreshMeta()
          if (this.queryFieldCount < 1 || this.listFieldCount < 1) {
            this.$modal.msgWarning('请至少配置 1 个查询条件字段和 1 个结果展示字段')
            return
          }
          if (this.requiredFieldCount < 1) {
            this.$modal.msgWarning('请至少将一个查询条件设为必填')
            return
          }
          this.goNext()
          return
        }
        if (this.activeStep === 2) {
          const ref = this.$refs.pageStep
          if (ref && ref.saveForWizard) {
            await ref.saveForWizard()
          }
          this.goNext()
        }
      } catch (e) {
        /* child already toasted */
      } finally {
        this.nextLoading = false
      }
    },
    onDatasetsLoaded(data) {
      if (!data) return
      this.queryInfo = data.query || this.queryInfo
      this.queryName = (this.queryInfo && this.queryInfo.queryName) || this.queryName
      this.datasetCount = (data.datasets || []).length
      this.applyFields(data.fields)
    },
    applyScenarioTip(key) {
      const map = {
        score: '成绩场景：先上传学生表为主表，再上传成绩表；关联字段选「学号」，一对多成绩可用「合并」或「取末条」后生成结果。',
        staff: '通讯录场景：通常只需一张表。上传后直接「生成结果」，下一步把姓名设为模糊查询即可。',
        multi: '一对多：若一人对应多行附表，展开会产生多条结果；合并会用「；」拼接，取末条适合只要最新一条。'
      }
      this.$alert(map[key] || '', '场景提示', { confirmButtonText: '知道了' })
    },
    onMaterialized(detail) {
      this.onDatasetsLoaded(detail)
    },
    doPublish() {
      if (!this.canPublish) {
        this.$modal.msgWarning(this.publishBlockReason)
        return
      }
      this.$modal.confirm('确认发布该查询并生成链接？').then(() => {
        this.publishing = true
        return publishQuery(this.queryId)
      }).then(res => {
        const data = res.data || {}
        if (data.pending) {
          this.$modal.msgSuccess(data.message || '已提交发布审批')
          this.refreshMeta()
          return
        }
        const code = data.publicCode || ''
        const path = data.path || ('/q/' + code)
        this.$modal.msgSuccess('发布成功')
        this.fromTemplate = false
        this.activeStep = 3
        this.syncStepQuery()
        this.fillPublishLinks(code || path.replace(/^\/q\//, ''))
        this.refreshMeta()
        this.openPublishNotifyDialog()
      }).catch(() => {}).finally(() => { this.publishing = false })
    },
    saveReachSettings() {
      this.reachSaving = true
      const payload = {
        queryId: this.queryId,
        remindHours: this.reachForm.remindHours,
        remindMail: this.reachForm.remindMail,
        params: {}
      }
      if (this.reachForm.startTime) {
        payload.startTime = this.reachForm.startTime
      } else {
        payload.params.clearStartTime = true
      }
      if (this.reachForm.endTime) {
        payload.endTime = this.reachForm.endTime
      } else {
        payload.params.clearEndTime = true
      }
      updateQuery(payload).then(() => {
        this.$modal.msgSuccess('已保存')
        this.refreshMeta()
      }).catch(() => {}).finally(() => { this.reachSaving = false })
    },
    doSchedule() {
      if (!this.scheduleAt) {
        this.$modal.msgWarning('请选择预约发布时间')
        return
      }
      this.scheduling = true
      scheduleQueryPublish(this.queryId, this.scheduleAt).then(res => {
        const code = (res.data && res.data.publicCode) || ''
        this.$modal.msgSuccess('已预约发布')
        if (code) this.fillPublishLinks(code)
        this.refreshMeta()
      }).catch(() => {}).finally(() => { this.scheduling = false })
    },
    doCancelSchedule() {
      this.$modal.confirm('确定取消预约发布？').then(() => {
        this.scheduling = true
        return cancelQuerySchedule(this.queryId)
      }).then(() => {
        this.$modal.msgSuccess('已取消预约')
        this.refreshMeta()
      }).catch(() => {}).finally(() => { this.scheduling = false })
    },
    openPublishNotifyDialog() {
      const u = this.$store.getters.email || (this.$store.state.user && this.$store.state.user.email) || ''
      this.notifyEmails = u || ''
      this.notifyOpen = true
    },
    doSendPublishNotify() {
      if (!this.notifyEmails) {
        this.$modal.msgWarning('请填写收件邮箱')
        return
      }
      this.notifySending = true
      sendPublishNotify({
        type: 'query',
        projectId: this.queryId,
        emails: this.notifyEmails,
        link: this.h5Link || this.publishedLink
      }).then(() => {
        this.$modal.msgSuccess('通知已发送')
        this.notifyOpen = false
      }).catch(() => {}).finally(() => { this.notifySending = false })
    },
    jumpPreview() {
      this.activeStep = 2
      this.designTab = 'preview'
      this.syncStepQuery()
    },
    jumpPublish() {
      this.activeStep = 3
      this.syncStepQuery()
    },
    async fillPublishLinks(code) {
      const path = '/q/' + code
      this.publishedLink = window.location.origin + path
      const h5Base = (process.env.VUE_APP_H5_BASE || 'http://127.0.0.1:5173').replace(/\/$/, '')
      this.h5Link = h5Base + path
      this.qrDataUrl = await toQrDataUrl(this.h5Link)
      this.posterDataUrl = ''
      await this.refreshPosterPreview()
    },
    downloadPoster() {
      const url = this.posterDataUrl || this.qrDataUrl
      if (!url) return
      downloadDataUrl(url, (this.queryName || 'share') + '-海报.png')
      this.$modal.msgSuccess('已开始下载')
    },
    openPublic() {
      if (this.publishedLink) window.open(this.publishedLink, '_blank')
    },
    copyText(text) {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => this.$modal.msgSuccess('已复制'))
          .catch(() => this.fallbackCopy(text))
      } else {
        this.fallbackCopy(text)
      }
    },
    fallbackCopy(text) {
      const input = document.createElement('input')
      input.value = text
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
      this.$modal.msgSuccess('已复制')
    }
  }
}
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.mb16 { margin-bottom: 16px; }
.mb20 { margin-bottom: 20px; }
.mt16 { margin-top: 16px; }
.query-setup >>> .el-page-header__content {
  font-size: 18px;
  font-weight: 700;
  color: var(--biz-ink);
}
.steps-panel { padding-top: 20px; padding-bottom: 8px; }
.step-panel { min-height: 280px; }
.publish-title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 700;
  color: var(--biz-ink);
}
.reach-panel, .schedule-box {
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--biz-bg-soft, #f5f7fb);
}
.tip { margin-left: 8px; color: var(--biz-muted-soft); font-size: 12px; }
.tip-block { margin: 0 0 10px; color: var(--biz-muted-soft); font-size: 12px; line-height: 1.5; }
.mb8 { margin-bottom: 8px; }
.tpl-ready-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  width: 100%;
}
.tpl-ready-actions { display: inline-flex; align-items: center; gap: 4px; flex-shrink: 0; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.desc { color: var(--biz-muted); font-size: 13px; line-height: 1.6; margin: 0 0 14px; }
.stat-row { margin-top: 12px; display: flex; gap: 16px; color: var(--biz-muted); font-size: 13px; }
.stat-row .fail { color: #f56c6c; }
.checklist { display: grid; gap: 8px; }
.check-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; border-radius: 8px; background: var(--biz-bg-soft);
  color: var(--biz-muted-soft); font-size: 13px;
}
.check-item.ok { background: #f0f9eb; color: #67c23a; }
.biz-wizard-footer .spacer { flex: 1; }
.qr-box { text-align: center; padding: 8px 0 4px; }
.qr-box img { width: 180px; height: 180px; }
.link-label { margin: 12px 0 4px; color: var(--biz-muted-soft); font-size: 12px; text-align: left; }
.qr-box .link { word-break: break-all; color: #666; font-size: 13px; margin: 0 0 8px; text-align: left; }
.design-tabs >>> .el-tabs__header { margin-bottom: 12px; }
.step-pane { min-height: 240px; }

.scenario-cards { display:grid; grid-template-columns: repeat(3, minmax(0,1fr)); gap:10px; }
.scenario {
  border: 1px solid var(--biz-line); border-radius: var(--biz-radius-sm); padding: 12px 14px; cursor: pointer;
  background: #fff; transition: border-color .15s, box-shadow .15s;
}
.scenario:hover {
  border-color: var(--biz-accent-hover);
  box-shadow: 0 4px 14px rgba(29, 78, 216, 0.08);
}
.scenario strong { display:block; margin-bottom: 4px; color: var(--biz-ink); }
.scenario span { font-size:12px; color: var(--biz-muted); line-height:1.4; }
.check-item.tip:not(.ok) { color: var(--biz-muted); }
.poster-img { width: 240px; max-width: 100%; border-radius: 12px; box-shadow: 0 8px 24px rgba(15,23,42,.12); }
.poster-actions { display:flex; flex-wrap:wrap; gap:8px; justify-content:center; margin: 12px 0; }
.poster-bg-panel { max-width: 420px; margin: 12px auto 0; text-align: left; }
@media (max-width: 900px) {
  .scenario-cards { grid-template-columns: 1fr; }
}
</style>
