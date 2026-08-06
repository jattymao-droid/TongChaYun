<template>
  <div
    class="open-query form-shell"
    :class="[{ 'anim-page': layout.formAnim }, 'align-' + layout.formAlign, { compact: layout.formCompact }]"
    :style="shellStyle"
  >
    <div v-if="layout.formShowAmbient" class="ambient" aria-hidden="true">
      <span class="orb orb-a" />
      <span class="orb orb-b" />
    </div>

    <div class="layout" :style="{ maxWidth: contentWidth + 'px' }">
      <header class="hero" :class="{ 'anim-fade-up': layout.formAnim }">
        <div class="hero-text">
          <p v-if="layout.formShowEyebrow" class="eyebrow">通查云 · 查询</p>
          <div class="title-row">
            <img v-if="logoSrc" :src="logoSrc" class="hero-logo" alt="" />
            <h1>{{ page.title || queryName || '数据查询' }}</h1>
          </div>
        </div>
        <div v-if="bannerSrc" class="banner-wrap" :class="{ 'anim-banner': layout.formAnim }">
          <img :src="bannerSrc" class="banner" alt="" />
        </div>
      </header>

      <main
        class="panel"
        :class="['style-' + layout.formPanelStyle, { 'anim-pop anim-delay-1': layout.formAnim }]"
        v-if="needPwd && !unlocked"
      >
        <div class="panel-head">
          <h3>访问验证</h3>
          <p>请输入访问密码后继续</p>
        </div>
        <el-form @submit.native.prevent="unlock" label-position="top" size="small" class="pwd-form">
          <el-form-item label="访问密码">
            <el-input v-model="accessPwd" show-password placeholder="请输入访问密码" @keyup.enter.native="unlock" />
          </el-form-item>
          <el-button type="primary" class="theme-btn anim-btn block-btn" :loading="metaLoading" @click="unlock">进入查询</el-button>
        </el-form>
      </main>

      <main
        class="panel"
        :class="['style-' + layout.formPanelStyle, { 'anim-pop anim-delay-1': layout.formAnim }]"
        v-loading="metaLoading"
        v-else
      >
        <template v-if="queryFields.length">
          <div class="panel-head row">
            <div>
              <h3>
                查询条件
                <span v-if="page.subtitle" class="head-sub">{{ page.subtitle }}</span>
              </h3>
              <p>{{ page.subtitle ? ('共 ' + queryFields.length + ' 项') : ('必填项需填写，选填可组合；至少填写一项（共 ' + queryFields.length + ' 项）') }}</p>
            </div>
            <span v-if="layout.formShowFillHint" class="fill-hint" :class="{ ready: requiredReady }">
              {{ filledCount ? ('已填 ' + filledCount + ' / ' + queryFields.length) : '请填写条件' }}
            </span>
          </div>

          <el-form :model="form" label-position="top" class="query-form" size="small" @submit.native.prevent="goResult">
            <div class="fields" :class="[{ 'anim-stagger': layout.formAnim }, 'cols-' + fieldsCols]">
              <div
                v-for="f in queryFields"
                :key="f.fieldKey"
                class="field-cell"
                :class="{ 'span-all': isWideField(f), filled: isFilled(f) }"
              >
                <el-form-item :required="f.isRequired !== '0'" :label="(f.fieldLabel || f.fieldName) + (f.isRequired === '0' ? '（选填）' : '')">
                  <el-select
                    v-if="f.htmlType === 'select' && String(f.queryType || '').toUpperCase() === 'IN'"
                    v-model="form[f.fieldKey]"
                    multiple
                    clearable
                    filterable
                    collapse-tags
                    :placeholder="'请选择' + (f.fieldLabel || f.fieldName)"
                    style="width:100%"
                  >
                    <el-option v-for="opt in dictOf(f)" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                  <el-select
                    v-else-if="f.htmlType === 'select'"
                    v-model="form[f.fieldKey]"
                    clearable
                    filterable
                    :placeholder="'请选择' + (f.fieldLabel || f.fieldName)"
                    style="width:100%"
                  >
                    <el-option v-for="opt in dictOf(f)" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                  <el-date-picker
                    v-else-if="f.htmlType === 'date' && String(f.queryType || '').toUpperCase() === 'BETWEEN'"
                    v-model="form[f.fieldKey]"
                    type="daterange"
                    value-format="yyyy-MM-dd"
                    range-separator="至"
                    start-placeholder="开始"
                    end-placeholder="结束"
                    style="width:100%"
                  />
                  <el-date-picker
                    v-else-if="f.htmlType === 'date'"
                    v-model="form[f.fieldKey]"
                    type="date"
                    value-format="yyyy-MM-dd"
                    :placeholder="'请选择' + (f.fieldLabel || f.fieldName)"
                    style="width:100%"
                  />
                  <el-input
                    v-else-if="String(f.queryType || '').toUpperCase() === 'IN'"
                    v-model="form[f.fieldKey]"
                    :placeholder="'多个值用逗号分隔'"
                    clearable
                    @keyup.enter.native="goResult"
                  />
                  <el-input
                    v-else
                    v-model="form[f.fieldKey]"
                    :placeholder="'请输入' + (f.fieldLabel || f.fieldName)"
                    clearable
                    @keyup.enter.native="goResult"
                  />
                </el-form-item>
              </div>
            </div>

            <div v-if="needCaptcha" class="captcha-row">
              <el-input v-model="captchaCode" placeholder="验证码" size="small" style="width:140px" @keyup.enter.native="goResult" />
              <img v-if="captchaUrl" :src="captchaUrl" class="captcha-img" @click="refreshCaptcha" alt="captcha" />
              <el-button size="mini" @click="refreshCaptcha">刷新</el-button>
            </div>
            <div class="actions-bar" :class="{ block: layout.formBtnBlock }">
              <el-button class="anim-btn ghost-btn" @click="resetForm">重置</el-button>
              <el-button type="primary" class="theme-btn anim-btn primary-btn" :class="{ 'is-leaving': leaving }" @click="goResult">查询</el-button>
            </div>
          </el-form>
        </template>
        <div v-else-if="!metaLoading" class="empty-box">
          <div class="empty-ico">?</div>
          <p>查询配置不完整或未发布</p>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import { openQueryMeta } from '@/api/biz/query'
import { getCodeImg } from '@/api/login'
import '@/assets/styles/biz-open-motion.css'
import { isExternal } from '@/utils/validate'
import {
  parseDictOptions,
  normalizeQueryParams,
  hasAllQueryParams,
  hasAnyQueryParam,
  missingQueryFields,
  paramsToRouteQuery,
  saveQueryParams,
  loadQueryParams,
  pwdStorageKey,
  routeQueryToParams,
  parseLayout,
  buildFormPageStyle,
  resolveFormWidth,
  resolveFormColumns,
  resolveAssetUrl
} from '@/utils/bizQueryField'

export default {
  name: 'OpenQuery',
  data() {
    return {
      code: '',
      metaLoading: false,
      queryName: '',
      page: {},
      layout: parseLayout(null),
      queryFields: [],
      form: {},
      needPwd: false,
      unlocked: false,
      accessPwd: '',
      leaving: false,
      needCaptcha: false,
      captchaCode: '',
      captchaUuid: '',
      captchaUrl: ''
    }
  },
  computed: {
    themeColor() {
      return (this.page && this.page.themeColor) || '#1677ff'
    },
    shellStyle() {
      return buildFormPageStyle(this.layout, this.themeColor)
    },
    bannerSrc() {
      const u = this.page && this.page.bannerUrl
      if (!u) return ''
      if (isExternal(u) || u.startsWith('data:')) return u
      return process.env.VUE_APP_BASE_API + u
    },
    logoSrc() {
      if (!this.layout.showLogo) return ''
      return resolveAssetUrl(this.layout.logoUrl, process.env.VUE_APP_BASE_API)
    },
    filledCount() {
      return Object.keys(normalizeQueryParams(this.form, this.queryFields)).length
    },
    requiredReady() {
      return hasAllQueryParams(this.form, this.queryFields)
    },
    contentWidth() {
      if (this.needPwd && !this.unlocked) return 480
      return resolveFormWidth(this.layout, this.queryFields.length)
    },
    fieldsCols() {
      return resolveFormColumns(this.layout, this.queryFields.length)
    }
  },
  created() {
    this.code = this.$route.params.code
    const saved = sessionStorage.getItem(pwdStorageKey(this.code))
    if (saved) this.accessPwd = saved
    this.loadMeta()
  },
  methods: {
    dictOf(f) { return parseDictOptions(f && f.dictOptions) },
    isWideField(f) {
      return f && f.htmlType === 'date' && String(f.queryType || '').toUpperCase() === 'BETWEEN'
    },
    isFilled(f) {
      return !!normalizeQueryParams(this.form, [f])[f.fieldKey]
    },
    loadMeta() {
      this.metaLoading = true
      openQueryMeta(this.code, this.accessPwd || undefined).then(res => {
        const data = res.data || {}
        this.queryName = data.queryName
        this.needPwd = !!data.needPwd
        this.unlocked = !!data.unlocked || !data.needPwd
        if (!this.unlocked) {
          document.title = this.queryName || '数据查询'
          return
        }
        if (this.accessPwd) sessionStorage.setItem(pwdStorageKey(this.code), this.accessPwd)
        this.page = data.page || {}
        this.layout = parseLayout(this.page)
        this.queryFields = data.queryFields || []
        const savedParams = loadQueryParams(this.code)
        const fromRoute = routeQueryToParams(this.$route.query, this.queryFields)
        const form = {}
        this.queryFields.forEach(f => {
          if (savedParams[f.fieldKey] != null && savedParams[f.fieldKey] !== '') form[f.fieldKey] = savedParams[f.fieldKey]
          else if (fromRoute[f.fieldKey] != null) form[f.fieldKey] = fromRoute[f.fieldKey]
          else {
            const op = String(f.queryType || '').toUpperCase()
            form[f.fieldKey] = (op === 'BETWEEN' || (op === 'IN' && f.htmlType === 'select')) ? [] : ''
          }
        })
        this.form = form
        this.needCaptcha = !!data.needCaptcha
        if (this.needCaptcha) this.refreshCaptcha()
        document.title = this.page.title || this.queryName || '数据查询'
      }).catch(err => {
        this.$message.error((err && err.msg) || '查询不存在或未发布')
      }).finally(() => { this.metaLoading = false })
    },
    unlock() {
      if (!this.accessPwd) return this.$message.warning('请输入访问密码')
      this.loadMeta()
    },
    resetForm() {
      Object.keys(this.form).forEach(k => {
        const f = this.queryFields.find(x => x.fieldKey === k)
        const op = String((f && f.queryType) || '').toUpperCase()
        this.form[k] = (op === 'BETWEEN' || (op === 'IN' && f && f.htmlType === 'select')) ? [] : ''
      })
    },
    refreshCaptcha() {
      getCodeImg().then(res => {
        this.captchaUuid = res.uuid
        this.captchaUrl = 'data:image/gif;base64,' + res.img
        this.captchaCode = ''
      }).catch(() => {})
    },
    goResult() {
      const params = normalizeQueryParams(this.form, this.queryFields)
      const missing = missingQueryFields(this.form, this.queryFields)
      if (missing.length) {
        const names = missing.map(f => f.fieldLabel || f.fieldName || f.fieldKey).join('、')
        return this.$message.warning('请填写必填查询条件：' + names)
      }
      if (!hasAnyQueryParam(params)) {
        return this.$message.warning('请至少填写一项查询条件')
      }
      if (this.needCaptcha && !this.captchaCode) {
        return this.$message.warning('请输入验证码')
      }
      if (this.accessPwd) sessionStorage.setItem(pwdStorageKey(this.code), this.accessPwd)
      saveQueryParams(this.code, params)
      try {
        sessionStorage.setItem('biz_q_captcha_' + this.code, JSON.stringify({
          code: this.captchaCode || '',
          uuid: this.captchaUuid || ''
        }))
      } catch (e) {}
      this.leaving = true
      setTimeout(() => {
        this.$router.push({ path: '/q/' + this.code + '/result', query: paramsToRouteQuery(params, 1) })
      }, 220)
    }
  }
}
</script>

<style scoped>
.open-query {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  padding: 32px 16px 48px;
  box-sizing: border-box;
}
.align-left .hero { text-align: left; }
.align-center .hero { text-align: center; }
.ambient { pointer-events: none; position: absolute; inset: 0; overflow: hidden; }
.orb {
  position: absolute; border-radius: 50%; filter: blur(40px); opacity: .4;
  animation: orbFloat 12s ease-in-out infinite alternate;
}
.orb-a { width: 260px; height: 260px; left: -50px; top: 100px; background: color-mix(in srgb, var(--theme) 35%, #fff); }
.orb-b { width: 200px; height: 200px; right: -30px; top: 260px; background: color-mix(in srgb, var(--theme) 22%, #cbd5e1); animation-delay: -4s; }
@keyframes orbFloat {
  from { transform: translateY(0) scale(1); }
  to { transform: translateY(20px) scale(1.05); }
}
.layout { position: relative; margin: 0 auto; width: 100%; }
.hero { margin: 0 0 16px; }
.banner-wrap { overflow: hidden; border-radius: 14px; margin-top: 14px; margin-bottom: 0; box-shadow: 0 10px 28px rgba(15, 23, 42, .1); }
.hero .banner { display: block; width: 100%; max-height: 160px; object-fit: cover; }
.eyebrow {
  margin: 0 0 6px; font-size: 12px; letter-spacing: .1em; text-transform: uppercase;
  color: color-mix(in srgb, var(--theme) 70%, #64748b); font-weight: 600;
}
.hero h1 {
  margin: 0; font-size: 28px; letter-spacing: -.02em; font-weight: 700;
  color: var(--theme); line-height: 1.25;
}
.title-row {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  max-width: 100%;
}
.align-left .title-row { justify-content: flex-start; }
.hero-logo {
  width: 48px;
  height: 48px;
  object-fit: contain;
  border-radius: 10px;
  flex-shrink: 0;
  background: #fff;
  box-shadow: 0 4px 12px rgba(15, 23, 42, .08);
}
.hero-text { margin-bottom: 0; }

.panel { border-radius: 16px; padding: 20px 20px 16px; box-sizing: border-box; }
.panel.style-card { background: #fff; box-shadow: 0 10px 36px rgba(15, 23, 42, .06); border: 1px solid transparent; }
.panel.style-flat { background: #fff; border: 1px solid #e5e7eb; }
.panel.style-glass {
  background: rgba(255,255,255,.88); border: 1px solid rgba(255,255,255,.8);
  backdrop-filter: blur(14px); box-shadow: 0 10px 36px rgba(15, 23, 42, .06);
}
.panel-head { margin-bottom: 16px; }
.panel-head.row { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.panel-head h3 { margin: 0 0 4px; font-size: 16px; color: #0f172a; display: flex; flex-wrap: wrap; align-items: baseline; gap: 8px; }
.panel-head .head-sub { font-size: 13px; font-weight: 400; color: #64748b; }
.panel-head p { margin: 0; font-size: 13px; color: #64748b; }
.fill-hint {
  flex-shrink: 0; font-size: 12px; color: #94a3b8; padding: 4px 10px;
  border-radius: 999px; background: #f1f5f9;
}
.fill-hint.ready { color: var(--theme); background: color-mix(in srgb, var(--theme) 12%, #fff); font-weight: 600; }

.fields { display: grid; gap: 4px 16px; }
.fields.cols-1 { grid-template-columns: 1fr; }
.fields.cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.fields.cols-3 { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.field-cell.span-all { grid-column: 1 / -1; }
.field-cell.filled ::v-deep .el-form-item__label { color: var(--theme); }
.compact .fields { gap: 0 12px; }
.compact .query-form ::v-deep .el-form-item { margin-bottom: 10px; }
.query-form ::v-deep .el-form-item { margin-bottom: 14px; }
.query-form ::v-deep .el-form-item__label { color: #334155; font-weight: 600; line-height: 1.3; padding-bottom: 4px; }
.query-form ::v-deep .el-input__inner,
.query-form ::v-deep .el-range-editor.el-input__inner { border-radius: 10px; }

.actions-bar {
  display: flex; align-items: center; justify-content: flex-end; gap: 10px;
  margin-top: 8px; padding-top: 16px; border-top: 1px solid #eef2f7;
}
.actions-bar.block { flex-wrap: wrap; }
.actions-bar.block .primary-btn { flex: 1; min-width: 140px; }
.theme-btn { background: var(--theme); border-color: var(--theme); }
.primary-btn { min-width: 120px; }
.block-btn { width: 100%; }
.ghost-btn { color: #64748b; }
.captcha-row {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin: 4px 0 12px;
}
.captcha-img {
  height: 36px; border-radius: 6px; cursor: pointer; border: 1px solid #e5e7eb;
}
.theme-btn.is-leaving {
  background-image: linear-gradient(90deg, var(--theme), color-mix(in srgb, var(--theme) 65%, #fff), var(--theme));
  background-size: 200% 100%;
  animation: bizShimmer .9s linear infinite;
}
@keyframes bizShimmer {
  0% { background-position: 0% 50%; }
  100% { background-position: 100% 50%; }
}
.empty-box { text-align: center; padding: 36px 12px; color: #94a3b8; }
.empty-ico {
  width: 48px; height: 48px; margin: 0 auto 10px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--theme) 12%, #fff); color: var(--theme); font-weight: 700; font-size: 20px;
}
@media (max-width: 720px) {
  .fields.cols-2, .fields.cols-3 { grid-template-columns: 1fr; }
  .hero h1 { font-size: 24px; }
  .open-query { padding: 20px 12px 36px; }
  .panel { padding: 16px 14px 14px; }
  .panel-head.row { flex-direction: column; align-items: stretch; }
  .actions-bar {
    position: sticky; bottom: 8px; z-index: 2; margin-top: 12px; padding: 10px;
    border: 1px solid #eef2f7; border-radius: 12px; background: rgba(255,255,255,.94); backdrop-filter: blur(10px);
  }
  .primary-btn { flex: 1; }
}
@media (min-width: 721px) and (max-width: 960px) {
  .fields.cols-3 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (prefers-reduced-motion: reduce) {
  .orb { animation: none; }
}
</style>
