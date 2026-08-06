<template>
  <div :class="embedded ? 'page-embed' : 'app-container'">
    <el-page-header v-if="!embedded" @back="goBack" :content="'页面设计 - ' + (queryName || '')" class="mb12" />
    <el-tabs v-model="activeTab">
      <el-tab-pane label="条件页" name="form" />
      <el-tab-pane label="结果页" name="result" />
    </el-tabs>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="never" :header="activeTab === 'form' ? '条件页配置' : '结果页样式'">
          <!-- FORM SETTINGS -->
          <div v-show="activeTab === 'form'">
            <el-form :model="form" label-width="110px" size="small">
              <el-form-item label="标题">
                <el-input v-model="form.title" placeholder="查询页标题" />
              </el-form-item>
              <el-form-item label="副标题">
                <el-input v-model="form.subtitle" placeholder="显示在「查询条件」标题后方" />
              </el-form-item>
              <el-form-item label="显示 Logo">
                <el-switch v-model="layout.showLogo" />
                <span class="ml8 muted">开启后显示在标题左侧</span>
              </el-form-item>
              <el-form-item v-if="layout.showLogo" label="Logo">
                <image-upload v-model="layout.logoUrl" :limit="1" :file-size="1" />
              </el-form-item>
              <el-form-item label="主题色">
                <div class="theme-row">
                  <span v-for="c in themePresets" :key="c" class="swatch" :style="{ background: c }" :class="{ active: form.themeColor === c }" @click="form.themeColor = c" />
                  <el-color-picker v-model="form.themeColor" />
                  <span class="ml8">{{ form.themeColor }}</span>
                </div>
              </el-form-item>
              <el-form-item label="Banner">
                <image-upload v-model="form.bannerUrl" :limit="1" :file-size="2" />
              </el-form-item>
              <el-form-item label="空结果提示">
                <el-input v-model="form.resultTips" placeholder="无数据时提示" />
                <div class="tip-presets">
                  <el-tag size="mini" class="tip-chip" v-for="t in tipPresets" :key="t" @click="form.resultTips = t">{{ t }}</el-tag>
                </div>
              </el-form-item>
            </el-form>

            <el-divider content-position="left">条件页布局与效果</el-divider>
            <el-form-item label="布局预设" label-width="110px" class="preset-row">
              <div class="preset-list">
                <button v-for="p in formPresets" :key="p.name" type="button" class="preset-btn plain" @click="applyFormPreset(p)">{{ p.name }}</button>
              </div>
            </el-form-item>
            <el-form :model="layout" label-width="110px" size="small">
              <el-form-item label="面板样式">
                <el-radio-group v-model="layout.formPanelStyle">
                  <el-radio label="glass">毛玻璃</el-radio>
                  <el-radio label="card">卡片阴影</el-radio>
                  <el-radio label="flat">扁平</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="字段列数">
                <el-radio-group v-model="layout.formColumns">
                  <el-radio label="auto">自适应</el-radio>
                  <el-radio label="1">1 列</el-radio>
                  <el-radio label="2">2 列</el-radio>
                  <el-radio label="3">3 列</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="内容宽度">
                <el-radio-group v-model="layout.formWidthMode">
                  <el-radio label="auto">自适应</el-radio>
                  <el-radio label="narrow">窄</el-radio>
                  <el-radio label="medium">中</el-radio>
                  <el-radio label="wide">宽</el-radio>
                  <el-radio label="custom">自定义</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="layout.formWidthMode === 'custom'" label="最大宽度">
                <el-input-number v-model="layout.formMaxWidth" :min="360" :max="1200" :step="40" />
                <span class="ml8 muted">px</span>
              </el-form-item>
              <el-form-item label="标题对齐">
                <el-radio-group v-model="layout.formAlign">
                  <el-radio label="center">居中</el-radio>
                  <el-radio label="left">靠左</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="页面背景">
                <el-radio-group v-model="layout.formBgStyle">
                  <el-radio label="theme">主题氛围</el-radio>
                  <el-radio label="soft">柔和灰</el-radio>
                  <el-radio label="plain">纯色</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="显示眉题">
                <el-switch v-model="layout.formShowEyebrow" />
              </el-form-item>
              <el-form-item label="氛围光斑">
                <el-switch v-model="layout.formShowAmbient" />
              </el-form-item>
              <el-form-item label="填写提示">
                <el-switch v-model="layout.formShowFillHint" />
                <span class="ml8 muted">已填 N 项</span>
              </el-form-item>
              <el-form-item label="紧凑间距">
                <el-switch v-model="layout.formCompact" />
              </el-form-item>
              <el-form-item label="入场动画">
                <el-switch v-model="layout.formAnim" />
              </el-form-item>
              <el-form-item label="查询按钮通栏">
                <el-switch v-model="layout.formBtnBlock" />
                <span class="ml8 muted">小屏更易点按</span>
              </el-form-item>
            </el-form>

            <el-divider content-position="left">分享海报背景</el-divider>
            <el-form :model="layout" label-width="110px" size="small">
              <poster-bg-form
                :model="layout"
                hint="下载分享海报时使用；主题渐变会跟随上方主题色。"
              />
            </el-form>
          </div>

          <!-- RESULT SETTINGS -->
          <div v-show="activeTab === 'result'">
            <el-form-item label="风格预设" label-width="110px" class="preset-row">
              <div class="preset-list">
                <button
                  v-for="p in stylePresets"
                  :key="p.name"
                  type="button"
                  class="preset-btn"
                  :style="{ borderColor: p.themeColor, background: (p.layout.resultBgGradient || p.layout.resultBgColor || '#fff') }"
                  @click="applyPreset(p)"
                >{{ p.name }}</button>
              </div>
            </el-form-item>
            <el-form :model="layout" label-width="110px" size="small">
              <el-form-item label="结果标题">
                <el-input v-model="layout.resultTitle" placeholder="默认：页面标题 + 结果" />
              </el-form-item>
              <el-form-item label="展示模板">
                <el-radio-group v-model="layout.resultStyle">
                  <el-radio label="default">默认</el-radio>
                  <el-radio label="scorecard">成绩单</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="卡片主标题字段">
                <el-select v-model="layout.resultTitleField" clearable filterable placeholder="默认取首个展示字段" style="width:100%">
                  <el-option v-for="f in listFields" :key="'t'+f.fieldKey" :label="f.fieldName + ' (' + f.fieldKey + ')'" :value="f.fieldKey" />
                </el-select>
              </el-form-item>
              <el-form-item label="摘要字段">
                <el-select v-model="layout.resultSummaryFields" multiple clearable filterable collapse-tags placeholder="显示在卡片头部摘要区" style="width:100%">
                  <el-option v-for="f in listFields" :key="'s'+f.fieldKey" :label="f.fieldName" :value="f.fieldKey" />
                </el-select>
              </el-form-item>
              <el-form-item label="空结果引导">
                <el-input v-model="layout.resultEmptyGuide" type="textarea" :rows="2" placeholder="无数据时的补充说明，例如：请核对学号后重试，或联系班主任" />
              </el-form-item>
              <el-form-item label="结果布局">
                <el-radio-group v-model="layout.resultLayout">
                  <el-radio label="table">表格</el-radio>
                  <el-radio label="card">卡片</el-radio>
                  <el-radio label="auto">自适应</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="layout.resultLayout !== 'table'" label="卡片列数">
                <el-radio-group v-model="layout.resultCardColumns">
                  <el-radio :label="1">1 列</el-radio>
                  <el-radio :label="2">2 列</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="每页条数">
                <el-radio-group v-model="layout.resultPageSize">
                  <el-radio :label="10">10</el-radio>
                  <el-radio :label="20">20</el-radio>
                  <el-radio :label="50">50</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="显示序号">
                <el-switch v-model="layout.showSerial" />
              </el-form-item>
              <el-form-item label="紧凑表格">
                <el-switch v-model="layout.resultDense" />
              </el-form-item>
              <el-form-item label="面板样式">
                <el-radio-group v-model="layout.resultPanelStyle">
                  <el-radio label="card">卡片阴影</el-radio>
                  <el-radio label="flat">扁平</el-radio>
                  <el-radio label="glass">毛玻璃</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="内容对齐">
                <el-radio-group v-model="layout.resultContentAlign">
                  <el-radio label="center">居中</el-radio>
                  <el-radio label="left">靠左</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="文字色调">
                <el-radio-group v-model="layout.resultTextTone">
                  <el-radio label="dark">深色（浅底）</el-radio>
                  <el-radio label="light">浅色（深底）</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="内容最大宽">
                <el-input-number v-model="layout.resultMaxWidth" :min="480" :max="1400" :step="40" />
                <span class="ml8 muted">px</span>
              </el-form-item>
              <el-form-item label="显示眉题">
                <el-switch v-model="layout.resultShowEyebrow" />
              </el-form-item>
              <el-form-item label="显示合计">
                <el-switch v-model="layout.resultShowTotal" />
              </el-form-item>
              <el-form-item label="显示导出">
                <el-switch v-model="layout.resultShowExport" />
              </el-form-item>
              <el-form-item label="显示打印">
                <el-switch v-model="layout.resultShowPrint" />
              </el-form-item>
              <el-form-item label="显示条件">
                <el-switch v-model="layout.resultShowConditions" />
              </el-form-item>
              <el-form-item label="空态图标">
                <el-switch v-model="layout.resultShowEmptyIcon" />
              </el-form-item>
              <el-form-item label="入场动画">
                <el-switch v-model="layout.resultAnim" />
              </el-form-item>
              <el-form-item label="结果分布图">
                <el-switch v-model="layout.resultShowChart" />
              </el-form-item>
              <el-form-item v-if="layout.resultShowChart" label="图表类型">
                <el-radio-group v-model="layout.resultChartType">
                  <el-radio label="bar">柱状图</el-radio>
                  <el-radio label="pie">饼图</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-divider content-position="left">背景</el-divider>
              <el-form-item label="背景类型">
                <el-radio-group v-model="layout.resultBgType">
                  <el-radio label="gradient">渐变</el-radio>
                  <el-radio label="color">纯色</el-radio>
                  <el-radio label="image">图片</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="layout.resultBgType === 'color'" label="背景色">
                <div class="theme-row">
                  <span v-for="c in bgColorPresets" :key="c" class="swatch" :style="{ background: c }" :class="{ active: layout.resultBgColor === c }" @click="layout.resultBgColor = c" />
                  <el-color-picker v-model="layout.resultBgColor" />
                </div>
              </el-form-item>
              <el-form-item v-if="layout.resultBgType === 'gradient'" label="渐变预设">
                <div class="grad-list">
                  <div
                    v-for="g in gradientPresets"
                    :key="g.name"
                    class="grad-item"
                    :class="{ active: layout.resultBgGradient === g.value }"
                    :style="{ background: g.value }"
                    @click="onPickGradient(g)"
                  >
                    <span>{{ g.name }}</span>
                  </div>
                </div>
                <el-input v-model="layout.resultBgGradient" type="textarea" :rows="2" class="mt8" placeholder="也可自定义 CSS linear-gradient" />
              </el-form-item>
              <el-form-item v-if="layout.resultBgType === 'image'" label="背景图">
                <image-upload v-model="layout.resultBgImage" :limit="1" :file-size="3" />
              </el-form-item>
              <el-form-item v-if="layout.resultBgType === 'image'" label="遮罩强度">
                <el-slider v-model="layout.resultImageOverlay" :min="0" :max="90" :step="5" show-input />
              </el-form-item>
            </el-form>
          </div>

          <el-alert
            class="mt16"
            type="warning"
            :closable="false"
            show-icon
            :title="activeTab === 'form'
              ? '保存后条件页布局会对公开页与 H5 生效。'
              : '保存后结果页样式对 /q/{短码}/result 与预览生效。'"
          />
          <div class="mt16">
            <el-button type="primary" @click="handleSave" v-hasPermi="['biz:query:edit']">保存</el-button>
            <el-button v-if="publicCode && activeTab === 'form'" @click="openPublishedForm">打开条件页</el-button>
            <el-button v-if="publicCode && activeTab === 'result'" @click="openPublishedResult">打开结果页</el-button>
            <el-button v-if="!embedded" @click="goBack">返回</el-button>
            <span v-if="dirty" class="ml8 dirty">未保存</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never" :header="activeTab === 'form' ? '条件页预览' : '结果页预览'">
          <!-- FORM PREVIEW -->
          <div
            v-if="activeTab === 'form'"
            class="form-preview"
            :class="['panel-' + layout.formPanelStyle, { compact: layout.formCompact }, 'align-' + layout.formAlign]"
            :style="formPreviewStyle"
          >
            <div v-if="layout.formShowAmbient" class="fp-ambient" aria-hidden="true" />
            <div class="fp-inner" :style="{ maxWidth: formPreviewWidth + 'px', margin: layout.formAlign === 'left' ? '0' : '0 auto' }">
              <p v-if="layout.formShowEyebrow" class="eyebrow">通查云 · 查询</p>
              <div class="title-row">
                <img v-if="layout.showLogo && logoPreview" :src="logoPreview" class="hero-logo" alt="" />
                <h2 :style="{ color: form.themeColor }">{{ form.title || '查询标题' }}</h2>
              </div>
              <img v-if="bannerPreview" :src="bannerPreview" class="banner" alt="" />
              <div class="fp-panel" :class="'style-' + layout.formPanelStyle">
                <div class="fp-head">
                  <div>
                    <strong>查询条件</strong>
                    <span v-if="form.subtitle" class="head-sub">{{ form.subtitle }}</span>
                    <span v-else class="muted"> 共 {{ previewFields.length || 3 }} 项</span>
                  </div>
                  <span v-if="layout.formShowFillHint" class="hint">未填写</span>
                </div>
                <div class="fp-fields" :class="'cols-' + formPreviewCols">
                  <div class="fp-field" v-for="f in (previewFields.length ? previewFields : fakeFields)" :key="f.fieldKey || f.name">
                    <label>{{ f.fieldLabel || f.fieldName || f.name }}</label>
                    <el-input size="mini" disabled :placeholder="'请输入'" />
                  </div>
                </div>
                <div class="fp-actions" :class="{ block: layout.formBtnBlock }">
                  <el-button size="mini">重置</el-button>
                  <el-button type="primary" size="mini" :style="{ background: form.themeColor, borderColor: form.themeColor }">查询</el-button>
                </div>
              </div>
            </div>
          </div>

          <!-- RESULT PREVIEW -->
          <div v-else class="result-preview" :class="'tone-' + layout.resultTextTone" :style="resultPreviewStyle">
            <div class="rp-inner" :style="{ maxWidth: layout.resultMaxWidth + 'px', margin: layout.resultContentAlign === 'left' ? '0' : '0 auto', textAlign: layout.resultContentAlign === 'left' ? 'left' : 'center' }">
              <p v-if="layout.resultShowEyebrow" class="eyebrow">查询结果</p>
              <h2 class="rp-title" :style="{ color: form.themeColor }">{{ layout.resultTitle || ((form.title || '查询') + ' · 结果') }}</h2>
              <p v-if="layout.resultShowTotal" class="rp-total">共 3 条结果</p>
              <div v-if="layout.resultShowConditions" class="rp-conds">
                <el-tag size="mini" type="info" effect="plain">姓名：张三</el-tag>
                <el-tag size="mini" type="info" effect="plain">班级：高一</el-tag>
              </div>
              <div class="rp-panel" :class="'style-' + layout.resultPanelStyle">
                <div class="rp-toolbar">
                  <el-button size="mini">返回修改条件</el-button>
                  <el-button v-if="layout.resultShowExport" size="mini">导出</el-button>
                </div>
                <div v-if="layout.resultLayout === 'card' || layout.resultLayout === 'auto'" class="rp-cards" :class="'cols-' + layout.resultCardColumns">
                  <div class="rp-card" v-for="n in 2" :key="n">
                    <div v-if="layout.showSerial" class="serial">#{{ n }}</div>
                    <div>姓名：示例{{ n }}</div>
                    <div>成绩：90</div>
                  </div>
                  <p v-if="layout.resultLayout === 'auto'" class="muted">自适应：小屏卡片 / 大屏表格</p>
                </div>
                <el-table v-else :data="previewRows" size="mini" border :class="{ dense: layout.resultDense }">
                  <el-table-column v-if="layout.showSerial" label="序号" width="60" type="index" />
                  <el-table-column label="姓名" prop="name" />
                  <el-table-column label="成绩" prop="score" />
                </el-table>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getQuery, saveQueryPage } from '@/api/biz/query'
import { isExternal } from '@/utils/validate'
import PosterBgForm from '@/components/PosterBgForm'
import {
  defaultLayout,
  buildResultPageStyle,
  buildFormPageStyle,
  parseLayout,
  resultStylePresets,
  formStylePresets,
  resolveFormWidth,
  resolveFormColumns,
  resolveAssetUrl
} from '@/utils/bizQueryField'

export default {
  name: 'BizQueryPageDesign',
  components: { PosterBgForm },
  props: {
    embedded: { type: Boolean, default: false },
    queryIdProp: { type: [String, Number], default: null }
  },
  data() {
    return {
      activeTab: 'form',
      queryId: null,
      queryName: '',
      publicCode: '',
      previewFields: [],
      listFields: [],
      fakeFields: [{ name: '姓名' }, { name: '班级' }, { name: '学号' }],
      savedSnapshot: '',
      stylePresets: resultStylePresets(),
      formPresets: formStylePresets(),
      themePresets: ['#1677ff', '#0f766e', '#c2410c', '#1f2937', '#7c3aed'],
      bgColorPresets: ['#f5f7fb', '#f0fdf4', '#fff7ed', '#f8fafc', '#fdf2f8'],
      gradientPresets: [
        { name: '清蓝', value: 'linear-gradient(180deg, #e8f1ff 0%, #f7f7f7 280px, #f7f7f7 100%)', tone: 'dark' },
        { name: '薄荷绿', value: 'linear-gradient(180deg, #ecfdf5 0%, #f7f7f7 280px, #f7f7f7 100%)', tone: 'dark' },
        { name: '暖沙', value: 'linear-gradient(180deg, #fff7ed 0%, #fafafa 280px, #fafafa 100%)', tone: 'dark' },
        { name: '暮紫', value: 'linear-gradient(180deg, #f5f3ff 0%, #f7f7f7 280px, #f7f7f7 100%)', tone: 'dark' },
        { name: '深空', value: 'linear-gradient(180deg, #1f2937 0%, #111827 100%)', tone: 'light' }
      ],
      tipPresets: [
        '未查询到相关数据',
        '未查询到相关数据，请核对信息后重试',
        '暂无匹配结果，请调整条件'
      ],
      layout: defaultLayout(),
      previewRows: [
        { name: '张三', score: '92' },
        { name: '李四', score: '88' }
      ],
      form: {
        queryId: null,
        title: '',
        subtitle: '',
        themeColor: '#1677ff',
        bannerUrl: '',
        resultTips: '未查询到相关数据',
        layoutJson: ''
      }
    }
  },
  computed: {
    bannerPreview() {
      const u = this.form.bannerUrl
      if (!u) return ''
      if (isExternal(u) || u.startsWith('data:')) return u
      return process.env.VUE_APP_BASE_API + u
    },
    logoPreview() {
      return resolveAssetUrl(this.layout.logoUrl, process.env.VUE_APP_BASE_API)
    },
    fieldCount() {
      return (this.previewFields && this.previewFields.length) || 3
    },
    formPreviewWidth() {
      return resolveFormWidth(this.layout, this.fieldCount)
    },
    formPreviewCols() {
      return resolveFormColumns(this.layout, this.fieldCount)
    },
    formPreviewStyle() {
      return {
        ...buildFormPageStyle(this.layout, this.form.themeColor),
        minHeight: '460px',
        borderRadius: '8px',
        padding: '16px',
        boxSizing: 'border-box',
        position: 'relative',
        overflow: 'hidden'
      }
    },
    resultPreviewStyle() {
      const style = buildResultPageStyle(this.layout, this.form.themeColor, process.env.VUE_APP_BASE_API)
      return {
        ...style,
        minHeight: '420px',
        borderRadius: '8px',
        padding: '16px',
        boxSizing: 'border-box'
      }
    },
    dirty() {
      return this.snapshotOf() !== this.savedSnapshot
    }
  },
  created() {
    this.queryId = this.queryIdProp != null ? this.queryIdProp : this.$route.params.queryId
    this.load()
    window.addEventListener('beforeunload', this.onBeforeUnload)
  },
  beforeDestroy() {
    window.removeEventListener('beforeunload', this.onBeforeUnload)
  },
  beforeRouteLeave(to, from, next) {
    if (this.embedded || !this.dirty) return next()
    this.$confirm('有未保存的修改，确定离开？', '提示', { type: 'warning' }).then(() => next()).catch(() => next(false))
  },
  methods: {
    snapshotOf() {
      return JSON.stringify({
        form: {
          title: this.form.title,
          subtitle: this.form.subtitle,
          themeColor: this.form.themeColor,
          bannerUrl: this.form.bannerUrl,
          resultTips: this.form.resultTips
        },
        layout: this.layout
      })
    },
    onBeforeUnload(e) {
      if (!this.dirty) return
      e.preventDefault()
      e.returnValue = ''
    },
    onPickGradient(g) {
      this.layout.resultBgGradient = g.value
      if (g.tone) this.layout.resultTextTone = g.tone
    },
    applyPreset(p) {
      this.form.themeColor = p.themeColor
      this.layout = Object.assign(defaultLayout(), this.layout, p.layout || {})
      this.$message.success('已应用「' + p.name + '」，请保存')
    },
    applyFormPreset(p) {
      this.layout = Object.assign({}, this.layout, p.layout || {})
      this.$message.success('已应用「' + p.name + '」，请保存')
    },
    load() {
      getQuery(this.queryId).then(res => {
        const data = res.data || {}
        this.queryName = data.query && data.query.queryName
        this.publicCode = (data.query && data.query.publicCode) || ''
        const page = data.page || {}
        this.layout = parseLayout(page)
        this.previewFields = (data.fields || []).filter(f => f.isQuery === '1').slice(0, 6)
        this.listFields = (data.fields || []).filter(f => f.isList !== '0')
        if (!this.listFields.length) this.listFields = data.fields || []
        this.form = {
          queryId: Number(this.queryId),
          title: page.title || this.queryName || '',
          subtitle: page.subtitle || '',
          themeColor: page.themeColor || '#1677ff',
          bannerUrl: page.bannerUrl || '',
          resultTips: page.resultTips || '未查询到相关数据',
          layoutJson: page.layoutJson || ''
        }
        this.$nextTick(() => { this.savedSnapshot = this.snapshotOf() })
      })
    },
    handleSave() {
      const payload = {
        ...this.form,
        layoutJson: JSON.stringify({ ...this.layout })
      }
      return saveQueryPage(payload).then(() => {
        this.form.layoutJson = payload.layoutJson
        this.savedSnapshot = this.snapshotOf()
        this.$modal.msgSuccess('保存成功')
      })
    },
    saveForWizard() {
      const payload = {
        ...this.form,
        layoutJson: JSON.stringify({ ...this.layout })
      }
      return saveQueryPage(payload).then(() => {
        this.form.layoutJson = payload.layoutJson
        this.savedSnapshot = this.snapshotOf()
        this.$modal.msgSuccess('页面设计已保存')
      })
    },
    openPublishedResult() {
      if (!this.publicCode) return this.$modal.msgWarning('尚未发布')
      window.open(window.location.origin + '/q/' + this.publicCode + '/result', '_blank')
    },
    openPublishedForm() {
      if (!this.publicCode) return this.$modal.msgWarning('尚未发布')
      window.open(window.location.origin + '/q/' + this.publicCode, '_blank')
    },
    goBack() {
      if (!this.dirty) {
        this.$router.push('/biz/query')
        return
      }
      this.$confirm('有未保存的修改，确定离开？', '提示', { type: 'warning' }).then(() => {
        this.savedSnapshot = this.snapshotOf()
        this.$router.push('/biz/query')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.mt8 { margin-top: 8px; }
.mt16 { margin-top: 16px; }
.ml8 { margin-left: 8px; }
.muted { color: #909399; font-size: 12px; }
.dirty { color: #e6a23c; font-size: 13px; }
.theme-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.swatch { width: 22px; height: 22px; border-radius: 4px; cursor: pointer; border: 2px solid transparent; display: inline-block; }
.swatch.active { border-color: #303133; }
.preset-row { margin-bottom: 12px; }
.preset-list { display: flex; flex-wrap: wrap; gap: 8px; }
.preset-btn {
  border: 2px solid #dcdfe6; border-radius: 8px; padding: 8px 12px; cursor: pointer;
  font-size: 12px; color: #303133; min-width: 84px;
}
.preset-btn.plain { background: #fff; }
.preset-btn:hover { filter: brightness(0.98); }
.tip-presets { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 6px; }
.tip-chip { cursor: pointer; max-width: 100%; }
.grad-list { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; width: 100%; }
.grad-item {
  height: 48px; border-radius: 8px; cursor: pointer; border: 2px solid transparent;
  display: flex; align-items: flex-end; padding: 6px 8px; color: #303133; font-size: 12px;
  text-shadow: 0 1px 0 rgba(255,255,255,.6);
}
.grad-item.active { border-color: #1677ff; }

.form-preview { }
.fp-ambient {
  position: absolute; inset: 0; pointer-events: none;
  background: radial-gradient(280px 160px at 20% 30%, rgba(22,119,255,.12), transparent 70%);
}
.fp-inner { position: relative; text-align: center; }
.align-left .fp-inner { text-align: left; }
.form-preview .banner { width: 100%; max-height: 120px; object-fit: cover; border-radius: 10px; margin: 4px 0 12px; }
.eyebrow { margin: 0 0 4px; font-size: 11px; letter-spacing: .1em; text-transform: uppercase; color: #64748b; font-weight: 600; }
.title-row {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin: 0 0 8px;
  max-width: 100%;
}
.align-left .title-row { justify-content: flex-start; }
.hero-logo {
  width: 40px;
  height: 40px;
  object-fit: contain;
  border-radius: 8px;
  flex-shrink: 0;
}
.form-preview h2 { margin: 0; font-size: 22px; }
.head-sub { margin-left: 8px; color: #64748b; font-size: 12px; font-weight: 400; }
.fp-panel { text-align: left; padding: 14px; border-radius: 12px; }
.fp-panel.style-card { background: #fff; box-shadow: 0 8px 24px rgba(15,23,42,.06); }
.fp-panel.style-flat { background: #fff; border: 1px solid #e5e7eb; }
.fp-panel.style-glass { background: rgba(255,255,255,.78); backdrop-filter: blur(10px); border: 1px solid rgba(255,255,255,.6); }
.fp-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.fp-head .hint { font-size: 12px; color: #94a3b8; background: #f1f5f9; padding: 2px 8px; border-radius: 999px; }
.fp-fields { display: grid; gap: 8px 12px; }
.fp-fields.cols-1 { grid-template-columns: 1fr; }
.fp-fields.cols-2 { grid-template-columns: 1fr 1fr; }
.fp-fields.cols-3 { grid-template-columns: 1fr 1fr 1fr; }
.fp-field label { display: block; font-size: 12px; color: #475569; margin-bottom: 4px; font-weight: 600; }
.compact .fp-fields { gap: 4px 10px; }
.fp-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; padding-top: 10px; border-top: 1px solid #eef2f7; }
.fp-actions.block .el-button--primary { flex: 1; }

.rp-title { margin: 0 0 6px; font-size: 22px; }
.rp-total { margin: 0 0 10px; color: var(--result-muted, #666); font-size: 13px; }
.rp-conds { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; justify-content: inherit; }
.rp-panel { padding: 12px; border-radius: 12px; text-align: left; }
.rp-panel.style-card { background: #fff; box-shadow: 0 6px 24px rgba(22,119,255,.08); }
.rp-panel.style-flat { background: #fff; border: 1px solid #ebeef5; }
.rp-panel.style-glass { background: rgba(255,255,255,.72); backdrop-filter: blur(10px); border: 1px solid rgba(255,255,255,.5); }
.tone-light .rp-panel.style-glass { background: rgba(255,255,255,.16); border-color: rgba(255,255,255,.28); color: #f8fafc; }
.rp-toolbar { display: flex; justify-content: space-between; margin-bottom: 10px; }
.rp-cards { display: grid; gap: 8px; }
.rp-cards.cols-2 { grid-template-columns: 1fr 1fr; }
.rp-card { border: 1px solid #eef2f7; border-radius: 8px; padding: 10px; background: #fafbff; font-size: 13px; color: #303133; }
.rp-card .serial { color: #1677ff; font-weight: 600; margin-bottom: 4px; }
.dense ::v-deep .el-table td, .dense ::v-deep .el-table th { padding: 4px 0; }
</style>
