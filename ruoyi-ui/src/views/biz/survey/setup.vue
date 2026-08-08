<template>
  <div class="biz-page survey-setup">
    <div class="biz-page-head">
      <el-page-header @back="leave" :content="'问卷设置 - ' + (surveyName || '')" />
    </div>

    <el-alert
      v-if="fromTemplate && !isPublished"
      class="mb16"
      type="success"
      :closable="true"
      show-icon
      @close="fromTemplate = false"
    >
      <div slot="title" class="tpl-ready-title">
        <span>模板已就绪：题目与主题已生成，可预览后直接发布。</span>
        <span class="tpl-ready-actions">
          <el-button type="text" size="mini" @click="jumpPreview">预览</el-button>
          <el-button type="text" size="mini" @click="jumpPublish">去发布</el-button>
          <el-button type="text" size="mini" :loading="publishing" :disabled="!canPublish" @click="doPublish" v-hasPermi="['biz:survey:publish']">一键发布</el-button>
        </span>
      </div>
    </el-alert>

    <div class="biz-panel steps-panel mb16">
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step title="基础设置" description="名称与规则" />
        <el-step title="题目设计" description="题型与选项" />
        <el-step title="预览校验" description="检查跳题" />
        <el-step title="发布" description="生成公开链接" />
      </el-steps>
    </div>

    <el-alert :title="stepHint" :type="stepHintType" :closable="false" show-icon class="mb16" />

    <div class="biz-panel step-panel">
    <!-- Step 0: Basic -->
    <div v-show="activeStep === 0" class="step-pane">
      <el-card shadow="never" class="inner-card">
        <el-form ref="basicForm" :model="form" :rules="rules" label-width="100px" size="small" class="basic-form">
          <el-form-item label="名称" prop="surveyName">
            <el-input v-model="form.surveyName" placeholder="请输入问卷名称" maxlength="100" show-word-limit />
          </el-form-item>
          <el-form-item label="描述" prop="surveyDesc">
            <el-input v-model="form.surveyDesc" type="textarea" :rows="3" placeholder="说明用途或填写须知" />
          </el-form-item>
          <el-form-item label="访问密码">
            <el-input v-model="form.accessPwd" placeholder="留空不加密；已设置时为占位，可改或清空" show-password />
          </el-form-item>
          <el-form-item label="开始时间">
            <el-date-picker v-model="form.startTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="可选" style="width:100%" />
          </el-form-item>
          <el-form-item label="截止时间">
            <el-date-picker v-model="form.endTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="可选" style="width:100%" />
          </el-form-item>
          <el-form-item label="截止提醒">
            <el-input-number v-model="form.remindHours" :min="0" :max="168" />
            <span class="tip">小时；0 表示不提醒。到点站内通知负责人</span>
          </el-form-item>
          <el-form-item label="提醒邮件" v-if="form.remindHours > 0">
            <el-switch v-model="form.remindMail" active-value="1" inactive-value="0" />
            <span class="tip">同时发邮件（需基础设置开启 SMTP）</span>
          </el-form-item>
          <el-form-item label="允许多次">
            <el-switch v-model="form.allowMulti" active-value="1" inactive-value="0" />
            <span class="tip">关闭后同一浏览器仅可提交一次（配合指纹/本地标记）</span>
          </el-form-item>
          <el-form-item label="答卷上限">
            <el-input-number v-model="form.maxAnswers" :min="0" :max="1000000" />
            <span class="tip">0 表示不限</span>
          </el-form-item>
          <el-form-item label="每日上限">
            <el-input-number v-model="form.dailyLimit" :min="0" :max="1000000" />
            <span class="tip">0 表示不限；控制单日提交量</span>
          </el-form-item>
          <el-form-item label="提交验证码">
            <el-switch v-model="form.needCaptcha" active-value="1" inactive-value="0" />
            <span class="tip">开启后公开页提交需填写图形验证码</span>
          </el-form-item>

          <el-divider content-position="left">完成页</el-divider>
          <el-form-item label="成功标题">
            <el-input v-model="form.successTitle" maxlength="40" show-word-limit placeholder="提交成功" />
          </el-form-item>
          <el-form-item label="成功文案">
            <el-input v-model="form.successMsg" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="感谢您的参与" />
          </el-form-item>
          <el-form-item label="显示再填">
            <el-switch v-model="form.showFillAgain" />
            <span class="tip">仍受「允许多次」限制；关闭后完成页不显示「再填一份」</span>
          </el-form-item>
          <el-form-item label="跳转链接">
            <el-input v-model="form.successRedirectUrl" maxlength="500" placeholder="选填，如 https://example.com" />
          </el-form-item>
          <el-form-item label="跳转延迟" v-if="form.successRedirectUrl">
            <el-input-number v-model="form.successRedirectSec" :min="0" :max="60" />
            <span class="tip">秒；0 表示立即跳转</span>
          </el-form-item>

          <el-form-item label="主题色">
            <div class="theme-row">
              <span
                v-for="c in themePresets"
                :key="c.color"
                class="swatch"
                :style="{ background: c.color }"
                :class="{ active: form.themeColor === c.color }"
                @click="pickTheme(c)"
                :title="c.name"
              />
              <el-color-picker v-model="form.themeColor" size="mini" />
              <span class="tip">{{ form.themeColor }}</span>
            </div>
          </el-form-item>
          <el-form-item label="填答方式">
            <el-radio-group v-model="form.fillMode">
              <el-radio label="all">整页展示</el-radio>
              <el-radio label="step">一页一题</el-radio>
              <el-radio label="pages">按分页</el-radio>
            </el-radio-group>
            <span class="tip">「按分页」按设计端分页符分页；一页一题显示进度与上下题切换</span>
          </el-form-item>

          <el-divider content-position="left">H5 标题样式</el-divider>
          <el-form-item label="显示标题">
            <el-switch v-model="form.showTitle" />
            <span class="tip">关闭后公开页不展示问卷标题</span>
          </el-form-item>
          <el-form-item label="显示简介">
            <el-switch v-model="form.showDesc" />
            <span class="tip">控制简介文案是否显示</span>
          </el-form-item>
          <el-form-item label="标题对齐" v-if="form.showTitle">
            <el-radio-group v-model="form.titleAlign">
              <el-radio label="center">居中</el-radio>
              <el-radio label="left">左对齐</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="标题大小" v-if="form.showTitle">
            <el-radio-group v-model="form.titleSize">
              <el-radio label="sm">小</el-radio>
              <el-radio label="md">中</el-radio>
              <el-radio label="lg">大</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="标题颜色" v-if="form.showTitle">
            <div class="theme-row">
              <el-color-picker v-model="form.titleColor" size="mini" />
              <el-button type="text" size="mini" @click="form.titleColor = ''">跟随主题色</el-button>
              <span class="tip">{{ form.titleColor || '使用主题色' }}</span>
            </div>
          </el-form-item>

          <el-divider content-position="left">H5 页面背景</el-divider>
          <poster-bg-form
            :model="form"
            prefix="pageBg"
            type-label="页面背景"
            hint="作用于独立 H5 与管理端公开填写页；「主题渐变」跟随上方主题色。"
          />

          <el-divider content-position="left">分享海报背景</el-divider>
          <poster-bg-form
            :model="form"
            prefix="posterBg"
            hint="发布后下载的分享海报使用此背景；主题渐变跟随上方主题色。"
          />
          <el-divider content-position="left">触达与 Webhook</el-divider>
          <el-form-item label="答卷邮件">
            <el-switch v-model="form.mailNotify" active-value="1" inactive-value="0" />
            <span class="tip">默认关闭；开启后新答卷发到下方邮箱（或负责人账号邮箱）</span>
          </el-form-item>
          <el-form-item label="通知邮箱" v-if="form.mailNotify === '1'">
            <el-input v-model="form.mailNotifyTo" placeholder="可填多个，逗号分隔；留空用负责人邮箱" clearable />
          </el-form-item>
          <el-form-item label="Webhook">
            <el-input v-model="form.webhookUrl" placeholder="答卷提交后 POST JSON，可选" clearable />
            <div class="tip">提交成功后异步回调（含签名与重试），不阻塞填写。</div>
            <div class="tip wecom-tip">
              企微群机器人：在群「添加群机器人」复制 Webhook 地址粘贴到此；消息体为 JSON，
              可用机器人「文本」模式对接中间服务，或直接用下方「发送测试」验证连通。
            </div>
            <el-button v-if="form.webhookUrl" type="text" size="mini" @click="handleTestWebhook">发送测试</el-button>
          </el-form-item>
          <el-form-item label="签名密钥">
            <el-input v-model="form.webhookSecret" placeholder="可选，HMAC-SHA256；请求头 X-TongChaYun-Signature" show-password clearable />
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- Step 1: Design -->
    <div v-show="activeStep === 1" class="step-pane design-step">
      <survey-design
        v-if="surveyId && activeStep === 1"
        ref="designStep"
        :embedded="true"
        :survey-id-prop="surveyId"
      />
    </div>

    <!-- Step 2: Preview -->
    <div v-show="activeStep === 2" class="step-pane">
      <survey-preview
        v-if="surveyId && activeStep === 2"
        ref="previewStep"
        :embedded="true"
        :survey-id-prop="surveyId"
      />
      <el-card shadow="never" class="mt16" v-if="questions.length">
        <survey-jump-flow :questions="questions" />
      </el-card>
    </div>

    <!-- Step 3: Publish -->
    <div v-show="activeStep === 3" class="step-pane">
      <el-card shadow="never">
        <div slot="header">发布问卷</div>
        <el-descriptions :column="1" border size="small" class="mb16">
          <el-descriptions-item label="名称">{{ surveyName }}</el-descriptions-item>
          <el-descriptions-item label="题目数">{{ questionCount }}</el-descriptions-item>
          <el-descriptions-item label="答卷数">{{ (surveyInfo && surveyInfo.answerCount) || 0 }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <dict-tag :options="dict.type.biz_survey_status" :value="(surveyInfo && surveyInfo.status) || '0'" />
          </el-descriptions-item>
          <el-descriptions-item v-if="surveyInfo && surveyInfo.publicCode" label="短码">{{ surveyInfo.publicCode }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="!canPublish"
          title="请先完成基础设置并至少保存一道题目后再发布。"
          type="warning"
          :closable="false"
          show-icon
          class="mb16"
        />

        <div v-if="publishedLink" class="qr-box">
          <img v-if="posterDataUrl" :src="posterDataUrl" class="poster-img" alt="poster" />
          <img v-else-if="qrDataUrl" :src="qrDataUrl" alt="qrcode" />
          <el-form :model="form" label-width="90px" size="mini" class="poster-bg-panel">
            <poster-bg-form :model="form" @change="refreshPosterPreview" />
            <el-form-item>
              <el-button type="primary" size="mini" :loading="posterSaving" @click="savePosterBg">保存背景并刷新</el-button>
            </el-form-item>
          </el-form>
          <div class="poster-actions">
            <el-button type="primary" size="mini" icon="el-icon-download" :loading="posterLoading" @click="downloadPoster">下载分享海报</el-button>
            <el-button size="mini" @click="copyText(h5Link)">复制 H5</el-button>
            <el-button size="mini" plain @click="copyText(publishedLink)">复制管理端链接</el-button>
          </div>
          <p class="link-label">管理端公开页</p>
          <p class="link">{{ publishedLink }}</p>
          <p class="link-label">独立 H5</p>
          <p class="link">{{ h5Link }}</p>

          <div class="channel-panel">
            <div class="channel-head">
              <span class="channel-title">渠道链接</span>
              <el-button type="text" size="mini" icon="el-icon-plus" @click="addChannel">添加渠道</el-button>
            </div>
            <p class="link-label tip">为不同投放入口生成带 <code>?channel=</code> 的链接，统计页可按渠道汇总。</p>
            <div class="channel-presets">
              <el-button
                v-for="p in channelPresets"
                :key="p"
                size="mini"
                plain
                @click="addChannelPreset(p)"
              >+ {{ p }}</el-button>
            </div>
            <el-table v-if="form.channels.length" :data="form.channels" size="mini" class="channel-table" border>
              <el-table-column label="渠道码" min-width="120">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.code" size="mini" maxlength="32" placeholder="如 微信" @change="persistChannels" />
                </template>
              </el-table-column>
              <el-table-column label="备注" min-width="120">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.label" size="mini" maxlength="40" placeholder="可选" @change="persistChannels" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="220" align="center">
                <template slot-scope="scope">
                  <el-button type="text" size="mini" @click="copyText(channelH5Link(scope.row.code))">复制 H5</el-button>
                  <el-button type="text" size="mini" @click="copyText(channelAdminLink(scope.row.code))">复制管理端</el-button>
                  <el-button type="text" size="mini" style="color:#f56c6c" @click="removeChannel(scope.$index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无渠道，可点上方快捷添加" :image-size="48" />
            <el-button size="mini" type="primary" plain class="mt8" :loading="channelSaving" @click="persistChannels">保存渠道</el-button>
          </div>
        </div>

        <div class="mt16">
          <el-button
            type="primary"
            icon="el-icon-s-promotion"
            :loading="publishing"
            :disabled="!canPublish"
            @click="doPublish"
            v-hasPermi="['biz:survey:publish']"
          >{{ isPublished ? '重新发布 / 刷新链接' : '立即发布' }}</el-button>
          <el-button v-if="isPublished && surveyInfo.publicCode" @click="openPublic">打开公开页</el-button>
        </div>

        <div v-if="!isPublished" class="schedule-box mt16">
          <div class="schedule-title">预约发布</div>
          <p class="tip">设定未来时间后，到点自动上线（约每分钟扫描一次）。短链可先生成。</p>
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
            v-hasPermi="['biz:survey:publish']"
          >确认预约</el-button>
          <el-button
            v-if="surveyInfo && surveyInfo.publishAt"
            type="text"
            :loading="scheduling"
            @click="doCancelSchedule"
            v-hasPermi="['biz:survey:publish']"
          >取消预约（{{ surveyInfo.publishAt }}）</el-button>
        </div>
        <el-alert
          v-if="surveyInfo && surveyInfo.publishAt && !isPublished"
          class="mt16"
          type="info"
          :closable="false"
          show-icon
          :title="'已预约于 ' + surveyInfo.publishAt + ' 自动发布'"
        />

        <survey-jump-flow :questions="questions" />
      </el-card>
    </div>
    </div>

    <el-dialog title="发送发布通知" :visible.sync="notifyOpen" width="480px" append-to-body>
      <p class="tip mb8">可选：将公开链接发到邮箱（需已配置 SMTP）。</p>
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
        {{ nextLabel }}
      </el-button>
      <el-button v-else type="success" @click="leave">完成</el-button>
    </div>
  </div>
</template>

<script>
import { getSurvey, updateSurvey, publishSurvey, testSurveyWebhook } from '@/api/biz/survey'
import { scheduleSurveyPublish, cancelSurveySchedule, sendPublishNotify } from '@/api/biz/reach'
import { toQrDataUrl, buildSharePoster, downloadDataUrl, resolvePosterBg } from '@/utils/qrcode'
import { withChannelParam } from '@/utils/bizSurveyTheme'
import PosterBgForm from '@/components/PosterBgForm'
import SurveyDesign from './design'
import SurveyPreview from './preview'
import SurveyJumpFlow from '@/components/biz/SurveyJumpFlow'

export default {
  name: 'BizSurveySetup',
  dicts: ['biz_survey_status'],
  components: { SurveyDesign, SurveyPreview, SurveyJumpFlow, PosterBgForm },
  data() {
    return {
      surveyId: null,
      surveyName: '',
      surveyInfo: null,
      questions: [],
      questionCount: 0,
      activeStep: 0,
      nextLoading: false,
      publishing: false,
      scheduling: false,
      scheduleAt: undefined,
      notifyOpen: false,
      notifyEmails: '',
      notifySending: false,
      publishedLink: '',
      h5Link: '',
      qrDataUrl: '',
      posterDataUrl: '',
      posterLoading: false,
      posterSaving: false,
      channelSaving: false,
      channelPresets: ['微信', '海报', '短信', '现场'],
      fromTemplate: false,
      themePresets: [
        { name: '蓝', color: '#1677ff', bg: 'linear-gradient(180deg, #f5f8ff 0%, #f7f7f7 280px, #f7f7f7 100%)' },
        { name: '绿', color: '#0f766e', bg: 'linear-gradient(180deg, #ecfdf5 0%, #f7f7f7 280px, #f7f7f7 100%)' },
        { name: '橙', color: '#c2410c', bg: 'linear-gradient(180deg, #fff7ed 0%, #f7f7f7 280px, #f7f7f7 100%)' },
        { name: '墨', color: '#1f2937', bg: 'linear-gradient(180deg, #f3f4f6 0%, #f7f7f7 280px, #f7f7f7 100%)' }
      ],
      form: {
        surveyId: undefined,
        surveyName: '',
        surveyDesc: '',
        accessPwd: '',
        startTime: undefined,
        endTime: undefined,
        remindHours: 24,
        remindMail: '0',
        mailNotify: '0',
        mailNotifyTo: '',
        allowMulti: '1',
        maxAnswers: 0,
        dailyLimit: 0,
        needCaptcha: '0',
        themeColor: '#1677ff',
        themeBg: 'linear-gradient(180deg, #f5f8ff 0%, #f7f7f7 280px, #f7f7f7 100%)',
        fillMode: 'all',
        pageBgType: 'theme',
        pageBgColor: '#f5f7fb',
        pageBgImage: '',
        pageBgOverlay: 40,
        titleAlign: 'center',
        titleSize: 'md',
        titleColor: '',
        showTitle: true,
        showDesc: true,
        posterBgType: 'theme',
        posterBgColor: '#eef2ff',
        posterBgImage: '',
        posterBgOverlay: 40,
        successTitle: '提交成功',
        successMsg: '感谢您的参与',
        successRedirectUrl: '',
        successRedirectSec: 0,
        showFillAgain: true,
        channels: [],
        webhookUrl: '',
        webhookSecret: ''
      },
      rules: {
        surveyName: [{ required: true, message: '名称不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    canPublish() {
      return this.questionCount > 0 && !!(this.form.surveyName || this.surveyName)
    },
    isPublished() {
      return String((this.surveyInfo && this.surveyInfo.status) || '') === '1'
    },
    nextLabel() {
      if (this.activeStep === 0) return '保存并下一步'
      if (this.activeStep === 1) return '保存并下一步'
      return '下一步'
    },
    stepHint() {
      const hints = [
        '第一步：完善问卷名称、主题色、H5 标题/背景与访问规则。',
        '第二步：添加题目、配置选项与跳题逻辑，记得保存。',
        '第三步：用真实交互预览检查题型与跳题，不会真正提交。',
        '第四步：发布后生成公开短链、二维码与分享海报，可设置海报背景后下载分享。'
      ]
      return hints[this.activeStep] || ''
    },
    stepHintType() {
      return this.activeStep === 3 ? 'success' : 'info'
    }
  },
  created() {
    this.surveyId = this.$route.params.surveyId
    this.fromTemplate = this.$route.query.fromTemplate === '1'
    const stepQ = parseInt(this.$route.query.step, 10)
    if (!Number.isNaN(stepQ) && stepQ >= 0 && stepQ <= 3) this.activeStep = stepQ
    this.refreshMeta().then(() => {
      if (this.$route.query.step == null) this.activeStep = this.inferStep()
      if (!this.fromTemplate && this.surveyName && this.surveyName.indexOf('（模板）') >= 0) {
        this.fromTemplate = true
      }
      if (this.isPublished && this.surveyInfo.publicCode) {
        this.fillPublishLinks(this.surveyInfo.publicCode)
      }
    })
  },
  methods: {
    inferStep() {
      const s = this.surveyInfo || {}
      if (!this.questionCount) return this.form.surveyName ? 1 : 0
      if (String(s.status) === '1') return 3
      return 2
    },
    refreshMeta() {
      return getSurvey(this.surveyId).then(res => {
        const data = res.data || {}
        const s = data.survey || {}
        this.surveyInfo = s
        this.surveyName = s.surveyName || ''
        this.questions = data.questions || []
        this.questionCount = this.questions.length
        let theme = {}
        try { theme = s.themeJson ? JSON.parse(s.themeJson) : {} } catch (e) { theme = {} }
        this.form = {
          surveyId: s.surveyId,
          surveyName: s.surveyName,
          surveyDesc: s.surveyDesc,
          accessPwd: s.accessPwd,
          startTime: s.startTime,
          endTime: s.endTime,
          remindHours: s.remindHours == null ? 24 : s.remindHours,
          remindMail: s.remindMail || '0',
          mailNotify: s.mailNotify || '0',
          mailNotifyTo: s.mailNotifyTo || '',
          allowMulti: s.allowMulti || '1',
          maxAnswers: s.maxAnswers == null ? 0 : s.maxAnswers,
          dailyLimit: s.dailyLimit == null ? 0 : s.dailyLimit,
          needCaptcha: s.needCaptcha || '0',
          themeColor: theme.color || '#1677ff',
          themeBg: theme.bg || 'linear-gradient(180deg, #f5f8ff 0%, #f7f7f7 280px, #f7f7f7 100%)',
          fillMode: theme.fillMode === 'step' || theme.fillMode === 'pages' ? theme.fillMode : 'all',
          pageBgType: theme.pageBgType || 'theme',
          pageBgColor: theme.pageBgColor || '#f5f7fb',
          pageBgImage: theme.pageBgImage || '',
          pageBgOverlay: theme.pageBgOverlay == null ? 40 : theme.pageBgOverlay,
          titleAlign: theme.titleAlign === 'left' ? 'left' : 'center',
          titleSize: ['sm', 'md', 'lg'].includes(theme.titleSize) ? theme.titleSize : 'md',
          titleColor: theme.titleColor || '',
          showTitle: theme.showTitle === false || theme.showTitle === '0' ? false : true,
          showDesc: theme.showDesc === false || theme.showDesc === '0' ? false : true,
          posterBgType: theme.posterBgType || 'theme',
          posterBgColor: theme.posterBgColor || '#eef2ff',
          posterBgImage: theme.posterBgImage || '',
          posterBgOverlay: theme.posterBgOverlay == null ? 40 : theme.posterBgOverlay,
          successTitle: theme.successTitle || '提交成功',
          successMsg: theme.successMsg || '感谢您的参与',
          successRedirectUrl: theme.successRedirectUrl || '',
          successRedirectSec: theme.successRedirectSec == null ? 0 : Number(theme.successRedirectSec) || 0,
          showFillAgain: theme.showFillAgain === false || theme.showFillAgain === '0' ? false : true,
          channels: Array.isArray(theme.channels)
            ? theme.channels.map(c => ({
              code: String((c && (c.code || c.name)) || '').trim(),
              label: String((c && (c.label || c.name || c.code)) || '').trim()
            })).filter(c => c.code)
            : [],
          webhookUrl: s.webhookUrl || '',
          webhookSecret: s.webhookSecret || ''
        }
        document.title = '问卷设置 - ' + (this.surveyName || '')
      })
    },
    pickTheme(c) {
      this.form.themeColor = c.color
      this.form.themeBg = c.bg
    },
    buildBasicPayload() {
      const payload = { ...this.form }
      let prev = {}
      try { prev = (this.surveyInfo && this.surveyInfo.themeJson) ? JSON.parse(this.surveyInfo.themeJson) : {} } catch (e) { prev = {} }
      payload.themeJson = JSON.stringify({
        ...prev,
        color: payload.themeColor || '#1677ff',
        bg: payload.themeBg || '',
        fillMode: payload.fillMode === 'step' || payload.fillMode === 'pages' ? payload.fillMode : 'all',
        pageBgType: payload.pageBgType || 'theme',
        pageBgColor: payload.pageBgColor || '#f5f7fb',
        pageBgImage: payload.pageBgImage || '',
        pageBgOverlay: payload.pageBgOverlay == null ? 40 : payload.pageBgOverlay,
        titleAlign: payload.titleAlign === 'left' ? 'left' : 'center',
        titleSize: ['sm', 'md', 'lg'].includes(payload.titleSize) ? payload.titleSize : 'md',
        titleColor: payload.titleColor || '',
        showTitle: payload.showTitle !== false,
        showDesc: payload.showDesc !== false,
        posterBgType: payload.posterBgType || 'theme',
        posterBgColor: payload.posterBgColor || '#eef2ff',
        posterBgImage: payload.posterBgImage || '',
        posterBgOverlay: payload.posterBgOverlay == null ? 40 : payload.posterBgOverlay,
        successTitle: payload.successTitle || '提交成功',
        successMsg: payload.successMsg || '感谢您的参与',
        successRedirectUrl: (payload.successRedirectUrl || '').trim(),
        successRedirectSec: payload.successRedirectSec == null ? 0 : Number(payload.successRedirectSec) || 0,
        showFillAgain: payload.showFillAgain !== false,
        channels: (payload.channels || [])
          .map(c => ({
            code: String((c && c.code) || '').trim().slice(0, 32),
            label: String((c && c.label) || '').trim().slice(0, 40)
          }))
          .filter(c => c.code)
      })
      ;[
        'themeColor', 'themeBg', 'fillMode',
        'pageBgType', 'pageBgColor', 'pageBgImage', 'pageBgOverlay',
        'titleAlign', 'titleSize', 'titleColor', 'showTitle', 'showDesc',
        'posterBgType', 'posterBgColor', 'posterBgImage', 'posterBgOverlay',
        'successTitle', 'successMsg', 'successRedirectUrl', 'successRedirectSec', 'showFillAgain', 'channels'
      ].forEach(k => delete payload[k])
      return payload
    },
    saveBasic() {
      return new Promise((resolve, reject) => {
        this.$refs.basicForm.validate(valid => {
          if (!valid) return reject(new Error('invalid'))
          updateSurvey(this.buildBasicPayload()).then(() => {
            this.$modal.msgSuccess('基础设置已保存')
            this.surveyName = this.form.surveyName
            resolve()
          }).catch(reject)
        })
      })
    },
    handleTestWebhook() {
      this.saveBasic().then(() => testSurveyWebhook(this.surveyId)).then(() => {
        this.$modal.msgSuccess('已发送测试回调')
      }).catch(() => {})
    },
    syncStepQuery() {
      this.$router.replace({
        path: '/biz/survey-setup/index/' + this.surveyId,
        query: { ...this.$route.query, step: String(this.activeStep) }
      }).catch(() => {})
    },
    leave() {
      this.confirmLeaveDesign().then(ok => {
        if (ok) this.$router.push('/biz/survey')
      })
    },
    confirmLeaveDesign() {
      const ref = this.$refs.designStep
      if (this.activeStep === 1 && ref && ref.isDirty && ref.isDirty()) {
        return this.$confirm('题目设计有未保存修改，确定离开？', '提示', { type: 'warning' })
          .then(() => true).catch(() => false)
      }
      return Promise.resolve(true)
    },
    goPrev() {
      this.confirmLeaveDesign().then(ok => {
        if (!ok) return
        if (this.activeStep > 0) this.activeStep -= 1
      })
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
          await this.saveBasic()
          this.goNext()
          return
        }
        if (this.activeStep === 1) {
          const ref = this.$refs.designStep
          if (ref && ref.saveForWizard) await ref.saveForWizard()
          this.goNext()
          return
        }
        if (this.activeStep === 2) {
          this.goNext()
        }
      } catch (e) {
        /* toasted */
      } finally {
        this.nextLoading = false
      }
    },
    doPublish() {
      this.$modal.confirm('确认发布该问卷并生成公开链接？').then(() => {
        this.publishing = true
        return publishSurvey(this.surveyId)
      }).then(res => {
        const data = res.data || {}
        if (data.pending) {
          this.$modal.msgSuccess(data.message || '已提交发布审批')
          this.refreshMeta()
          return
        }
        const code = data.publicCode || ''
        const path = data.path || ('/s/' + code)
        this.$modal.msgSuccess('发布成功')
        this.fromTemplate = false
        this.activeStep = 3
        this.syncStepQuery()
        this.fillPublishLinks(code || path.replace(/^\/s\//, ''))
        this.refreshMeta()
        this.openPublishNotifyDialog()
      }).catch(() => {}).finally(() => { this.publishing = false })
    },
    doSchedule() {
      if (!this.scheduleAt) {
        this.$modal.msgWarning('请选择预约发布时间')
        return
      }
      this.scheduling = true
      scheduleSurveyPublish(this.surveyId, this.scheduleAt).then(res => {
        const code = (res.data && res.data.publicCode) || ''
        this.$modal.msgSuccess('已预约发布')
        if (code) this.fillPublishLinks(code)
        this.refreshMeta()
      }).catch(() => {}).finally(() => { this.scheduling = false })
    },
    doCancelSchedule() {
      this.$modal.confirm('确定取消预约发布？').then(() => {
        this.scheduling = true
        return cancelSurveySchedule(this.surveyId)
      }).then(() => {
        this.$modal.msgSuccess('已取消预约')
        this.refreshMeta()
      }).catch(() => {}).finally(() => { this.scheduling = false })
    },
    openPublishNotifyDialog() {
      const u = this.$store.getters.email || (this.$store.state.user && this.$store.state.user.email) || ''
      this.notifyEmails = u || this.form.mailNotifyTo || ''
      this.notifyOpen = true
    },
    doSendPublishNotify() {
      if (!this.notifyEmails) {
        this.$modal.msgWarning('请填写收件邮箱')
        return
      }
      this.notifySending = true
      sendPublishNotify({
        type: 'survey',
        projectId: this.surveyId,
        emails: this.notifyEmails,
        link: this.h5Link || this.publishedLink
      }).then(() => {
        this.$modal.msgSuccess('通知已发送')
        this.notifyOpen = false
      }).catch(() => {}).finally(() => { this.notifySending = false })
    },
    jumpPreview() {
      this.activeStep = 2
      this.syncStepQuery()
    },
    jumpPublish() {
      this.activeStep = 3
      this.syncStepQuery()
    },
    async fillPublishLinks(code) {
      const path = '/s/' + code
      this.publishedLink = window.location.origin + path
      const h5Base = (process.env.VUE_APP_H5_BASE || 'http://127.0.0.1:5173').replace(/\/$/, '')
      this.h5Link = h5Base + path
      this.qrDataUrl = await toQrDataUrl(this.h5Link)
      this.posterDataUrl = ''
      await this.refreshPosterPreview()
    },
    async refreshPosterPreview() {
      if (!this.h5Link) return
      this.posterLoading = true
      try {
        const bg = resolvePosterBg(this.form, this.form.themeColor)
        this.posterDataUrl = await buildSharePoster({
          title: this.surveyName || this.form.surveyName || '问卷',
          subtitle: this.form.surveyDesc || '扫码即可填写',
          brand: '通查云 · 问卷',
          link: this.h5Link,
          qrDataUrl: this.qrDataUrl,
          apiBase: process.env.VUE_APP_BASE_API,
          ...bg
        })
      } catch (e) { /* ignore */ }
      finally { this.posterLoading = false }
    },
    savePosterBg() {
      this.posterSaving = true
      this.saveBasic().then(() => this.refreshPosterPreview())
        .catch(() => {})
        .finally(() => { this.posterSaving = false })
    },
    downloadPoster() {
      const url = this.posterDataUrl || this.qrDataUrl
      if (!url) return
      downloadDataUrl(url, (this.surveyName || 'survey') + '-海报.png')
      this.$modal.msgSuccess('已开始下载')
    },
    openPublic() {
      if (this.publishedLink) window.open(this.publishedLink, '_blank')
    },
    channelH5Link(code) {
      return withChannelParam(this.h5Link, code)
    },
    channelAdminLink(code) {
      return withChannelParam(this.publishedLink, code)
    },
    addChannel() {
      if (!this.form.channels) this.form.channels = []
      this.form.channels.push({ code: '', label: '' })
    },
    addChannelPreset(name) {
      if (!this.form.channels) this.form.channels = []
      const code = String(name || '').trim()
      if (!code) return
      if (this.form.channels.some(c => c.code === code)) {
        this.$modal.msgWarning('渠道已存在：' + code)
        return
      }
      this.form.channels.push({ code, label: code })
      this.persistChannels()
    },
    removeChannel(index) {
      this.form.channels.splice(index, 1)
      this.persistChannels()
    },
    persistChannels() {
      if (!Array.isArray(this.form.channels)) this.form.channels = []
      this.form.channels = this.form.channels
        .map(c => ({
          code: String((c && c.code) || '').trim().slice(0, 32),
          label: String((c && c.label) || '').trim().slice(0, 40)
        }))
        .filter(c => c.code)
      this.channelSaving = true
      this.saveBasic().catch(() => {}).finally(() => { this.channelSaving = false })
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
.mb16 { margin-bottom: 16px; }
.mb20 { margin-bottom: 20px; }
.mt16 { margin-top: 16px; }
.survey-setup >>> .el-page-header__content {
  font-size: 18px;
  font-weight: 700;
  color: var(--biz-ink);
}
.steps-panel { padding-top: 20px; padding-bottom: 8px; }
.step-panel { min-height: 280px; }
.inner-card {
  border: none !important;
  box-shadow: none !important;
}
.inner-card >>> .el-card__body { padding: 0; }
.tpl-ready-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  width: 100%;
}
.tpl-ready-actions { display: inline-flex; align-items: center; gap: 4px; flex-shrink: 0; }
.basic-form { max-width: 640px; }
.tip { margin-left: 8px; color: var(--biz-muted-soft); font-size: 12px; }
.wecom-tip { margin: 6px 0 0; margin-left: 0; line-height: 1.5; }
.schedule-box {
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--biz-bg-soft, #f5f7fb);
}
.schedule-title {
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--biz-ink);
}
.mb8 { margin-bottom: 8px; }
.theme-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.swatch { width: 22px; height: 22px; border-radius: 4px; cursor: pointer; border: 2px solid transparent; display: inline-block; }
.swatch.active { border-color: var(--biz-accent); }
.biz-wizard-footer .spacer { flex: 1; }
.qr-box { text-align: center; padding: 8px 0 4px; }
.qr-box img { width: 180px; height: 180px; }
.poster-img { width: 240px; max-width: 100%; border-radius: 12px; box-shadow: 0 8px 24px rgba(15,23,42,.12); }
.poster-actions { display:flex; flex-wrap:wrap; gap:8px; justify-content:center; margin: 12px 0; }
.poster-bg-panel { max-width: 420px; margin: 12px auto 0; text-align: left; }
.link-label { margin: 12px 0 4px; color: var(--biz-muted-soft); font-size: 12px; text-align: left; }
.qr-box .link { word-break: break-all; color: #666; font-size: 13px; margin: 0 0 8px; text-align: left; }
.channel-panel {
  margin-top: 16px;
  padding: 12px;
  border: 1px solid var(--biz-line);
  border-radius: var(--biz-radius-sm);
  text-align: left;
  background: var(--biz-bg-soft);
}
.channel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.channel-title { font-weight: 600; color: var(--biz-ink); }
.channel-presets { display: flex; flex-wrap: wrap; gap: 6px; margin: 8px 0 12px; }
.channel-table { width: 100%; background: #fff; }
.mt8 { margin-top: 8px; }
.step-pane { min-height: 240px; }
.design-step {
  min-height: 0;
  overflow: hidden;
}
.design-step ::v-deep .survey-studio.embedded {
  height: calc(100vh - 280px);
  max-height: calc(100vh - 280px);
  min-height: 480px;
}
</style>
