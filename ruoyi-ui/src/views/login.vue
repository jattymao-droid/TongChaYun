<template>
  <div class="js-home sk">
    <header class="js-nav">
      <div class="js-nav-inner">
        <a class="js-brand" href="#top" @click.prevent="scrollTo('top')">
          <img class="js-logo" :src="brandLogo" alt="通查云" />
          <span class="js-name">{{ siteTitle }}</span>
        </a>
        <nav class="js-links">
          <a href="#products" @click.prevent="scrollTo('products')">产品能力</a>
          <a href="#workflow" @click.prevent="scrollTo('workflow')">工作流</a>
          <a href="#scenes" @click.prevent="scrollTo('scenes')">场景</a>
          <a href="#why" @click.prevent="scrollTo('why')">为什么选我们</a>
        </nav>
        <div class="js-nav-actions">
          <a href="javascript:;" class="js-link-btn" @click.prevent="openLogin()">登录</a>
          <router-link v-if="register" class="js-link-btn" to="/register">注册</router-link>
          <button type="button" class="js-cta" @click="openLogin()">立即体验</button>
        </div>
      </div>
    </header>

    <main id="top">
      <!-- Hero -->
      <section class="sk-hero">
        <div class="sk-hero-bg" aria-hidden="true">
          <div class="sk-hero-mesh"></div>
          <div class="sk-hero-orbs">
            <span class="orb o1"></span>
            <span class="orb o2"></span>
            <span class="orb o3"></span>
          </div>
          <canvas ref="heroCanvas" class="sk-hero-canvas"></canvas>
          <div class="sk-hero-vignette"></div>
        </div>
        <div class="sk-hero-inner">
          <div class="sk-hero-copy">
            <p class="sk-brand-mark">{{ siteTitle }}</p>
            <h1>开箱即用的<br>查询与问卷一站式平台</h1>
            <p class="sk-lead">Excel 导入、拖拽设计、短链发布、自动统计，一套流程覆盖 PC 与 H5。</p>
            <div class="js-hero-ctas">
              <button type="button" class="js-cta lg" @click="openLogin()">立即体验</button>
              <button type="button" class="js-ghost" @click="scrollTo('products')">了解产品</button>
            </div>
          </div>
          <div class="sk-hero-visual" aria-hidden="true">
            <div class="sk-device">
              <div class="sk-device-bar">
                <i></i><i></i><i></i>
                <em>{{ siteTitle }} · 管理端</em>
              </div>
              <div class="sk-device-body">
                <div class="sk-side">
                  <div class="sk-side-item on">仪表盘</div>
                  <div class="sk-side-item">我的查询</div>
                  <div class="sk-side-item">我的问卷</div>
                  <div class="sk-side-item">答卷通知</div>
                </div>
                <div class="sk-main">
                  <div class="sk-toolbar">
                    <strong>仪表盘</strong>
                    <span class="sk-pill live">今日</span>
                  </div>
                  <div class="sk-stats">
                    <div><em>查询项目</em><b>12</b></div>
                    <div><em>问卷项目</em><b>8</b></div>
                    <div><em>今日 UV</em><b>1.2k</b></div>
                  </div>
                  <div class="sk-preview">
                    <div class="sk-preview-head">最近修改</div>
                    <div class="sk-chips-mini">
                      <span>问卷</span>
                      <span>进行中</span>
                    </div>
                    <div class="sk-score-row">
                      <div><em>答卷</em><b>286</b></div>
                      <div><em>浏览</em><b>1.4k</b></div>
                      <div class="hot"><em>转化</em><b>21%</b></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Dual entry like 开源/企业 -->
      <section class="sk-deploy" id="products">
        <div class="sk-deploy-head">
          <p class="sk-kicker">选择适合您的能力</p>
          <h2>快查与问卷，同一套发布体验</h2>
        </div>
        <div class="sk-deploy-grid">
          <article class="sk-deploy-card">
            <div class="sk-num">01</div>
            <h3>对外快查</h3>
            <p>上传 Excel 即可开放查询，适合成绩、录取、证书与进度公示。</p>
            <ul>
              <li><strong>一分钟上线</strong><span>导入即可生成查询页</span></li>
              <li><strong>安全可控</strong><span>密码 · 验证码 · 日限</span></li>
              <li><strong>结果多样</strong><span>表格 / 成绩单式展示</span></li>
            </ul>
            <button type="button" class="js-cta" @click="openLogin()">体验快查</button>
          </article>
          <article class="sk-deploy-card accent">
            <div class="sk-num">02</div>
            <h3>问卷调查</h3>
            <p>拖拽设计问卷，多题型与逻辑跳转，回收统计与导出一体完成。</p>
            <ul>
              <li><strong>可视化设计</strong><span>题型 · 逻辑 · 主题</span></li>
              <li><strong>多渠道触达</strong><span>短链 · 海报 · H5</span></li>
              <li><strong>数据交付</strong><span>明细 · 交叉 · 导出</span></li>
            </ul>
            <button type="button" class="js-cta" @click="openLogin()">体验问卷</button>
          </article>
        </div>
      </section>

      <!-- Trust strip -->
      <section class="sk-trust" aria-label="适用对象">
        <p>真实场景 · 持续服务学校、培训机构与运营团队</p>
        <div class="sk-trust-row">
          <span v-for="t in trustTags" :key="t">{{ t }}</span>
        </div>
      </section>

      <!-- Workflow 01-04 -->
      <section class="sk-workflow" id="workflow">
        <div class="sk-section-head">
          <p class="sk-eyebrow">从创建到洞察</p>
          <h2>同一条工作流，覆盖发布全链路</h2>
          <p>选题模板、配置规则、多端触达、回收分析，保持在一条路径里。</p>
        </div>
        <ol class="sk-steps">
          <li v-for="s in workflow" :key="s.n">
            <span class="sk-step-n">{{ s.n }}</span>
            <strong>{{ s.title }}</strong>
            <em>{{ s.desc }}</em>
          </li>
        </ol>
        <div class="sk-capability">
          <article v-for="c in capabilities" :key="c.title">
            <h4>{{ c.title }}</h4>
            <p>{{ c.desc }}</p>
          </article>
        </div>
      </section>

      <!-- Scenes -->
      <section class="sk-scenes" id="scenes">
        <div class="sk-section-head">
          <p class="sk-eyebrow">典型场景</p>
          <h2>从成绩查询到反馈收集</h2>
          <p>按业务快速起步，少配置即可上线。</p>
        </div>
        <div class="sk-scene-grid">
          <article v-for="s in scenes" :key="s.title">
            <span class="sk-scene-tag">{{ s.tag }}</span>
            <h3>{{ s.title }}</h3>
            <p>{{ s.desc }}</p>
          </article>
        </div>
      </section>

      <!-- Why -->
      <section class="sk-why" id="why">
        <div class="sk-section-head">
          <p class="sk-eyebrow">为什么选择{{ siteTitle }}</p>
          <h2>专业能力，更可控的长期成本</h2>
          <p>把预算用在产品能力上，而不是重复搭建与运维。</p>
        </div>
        <div class="sk-why-grid">
          <article v-for="w in whyItems" :key="w.title">
            <strong>{{ w.metric }}</strong>
            <h3>{{ w.title }}</h3>
            <p>{{ w.desc }}</p>
          </article>
        </div>
      </section>

      <!-- Bottom CTA -->
      <section class="sk-cta-band">
        <div class="sk-cta-inner">
          <p class="sk-eyebrow light">立即开始</p>
          <h2>选择适合你的方式，体验完整产品</h2>
          <p>登录管理端创建查询或问卷；需要账号可先注册。</p>
          <div class="js-hero-ctas center">
            <button type="button" class="js-cta lg light" @click="openLogin()">立即体验</button>
            <router-link v-if="register" class="js-ghost light" to="/register">免费注册</router-link>
            <button v-else type="button" class="js-ghost light" @click="scrollTo('products')">了解能力</button>
          </div>
        </div>
      </section>
    </main>

    <el-dialog
      :visible.sync="loginOpen"
      width="440px"
      top="8vh"
      custom-class="js-login-dialog"
      append-to-body
      :close-on-click-modal="true"
      @opened="onLoginOpened"
    >
      <div slot="title" class="js-dialog-title">
        <img class="js-logo sm" :src="brandLogo" :alt="siteTitle" />
        <div class="js-dialog-title-text">
          <strong>欢迎回来</strong>
          <em>登录 {{ siteTitle }} 管理端</em>
        </div>
      </div>
      <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="js-login-card in-dialog" @submit.native.prevent>
        <el-form-item prop="username" class="login-field">
          <el-input
            v-model="loginForm.username"
            type="text"
            auto-complete="username"
            placeholder="请输入账号"
            clearable
          >
            <i slot="prefix" class="el-input__icon el-icon-user login-field-icon" />
          </el-input>
        </el-form-item>
        <el-form-item prop="password" class="login-field">
          <el-input
            v-model="loginForm.password"
            type="password"
            auto-complete="current-password"
            placeholder="请输入密码"
            show-password
            @keyup.enter.native="handleLogin"
          >
            <i slot="prefix" class="el-input__icon el-icon-lock login-field-icon" />
          </el-input>
        </el-form-item>
        <el-form-item prop="code" v-if="captchaEnabled" class="code-item login-field">
          <table class="js-code-table" cellspacing="0" cellpadding="0" style="width:100%;table-layout:fixed;border-collapse:separate;border-spacing:0;">
            <tr>
              <td class="js-code-td-input" style="padding:0 10px 0 0;vertical-align:middle;">
                <el-input
                  v-model="loginForm.code"
                  class="code-input"
                  auto-complete="off"
                  placeholder="请输入验证码"
                  @keyup.enter.native="handleLogin"
                >
                  <i slot="prefix" class="el-input__icon el-icon-key login-field-icon" />
                </el-input>
              </td>
              <td class="js-code-td-img" style="width:120px;padding:0;vertical-align:middle;">
                <div class="login-code" title="点击刷新验证码" style="width:120px;height:50px;box-sizing:border-box;" @click="getCode">
                  <img v-if="codeUrl" :src="codeUrl" class="login-code-img" alt="验证码" style="display:block;width:100%;height:100%;object-fit:contain;" />
                  <span v-else class="login-code-placeholder">点击获取</span>
                </div>
              </td>
            </tr>
          </table>
        </el-form-item>
        <div class="login-extra-row">
          <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
          <router-link
            class="forgot-link"
            to="/forgotPassword"
            @click.native="loginOpen = false"
          >忘记密码？</router-link>
        </div>
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          class="js-login-btn"
          @click.native.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登录中…</span>
        </el-button>
        <div v-if="oauthWechatEnabled || oauthQqEnabled" class="social-login">
          <div class="social-divider"><span>其他登录方式</span></div>
          <div class="social-btns">
            <button
              v-if="oauthWechatEnabled"
              type="button"
              class="social-btn wechat"
              :disabled="socialLoading"
              aria-label="微信登录"
              @click="startOauth('wechat')"
            >
              <span class="social-icon" aria-hidden="true">
                <svg viewBox="0 0 48 48" width="22" height="22">
                  <path fill="currentColor" d="M17.6 8.4C10.7 8.4 5 13.2 5 19.1c0 3.4 1.9 6.4 4.9 8.4l-.9 3.4c-.2.7.5 1.3 1.1.9l4-2.1c1.1.3 2.3.5 3.5.5 1 0 1.9-.1 2.8-.3-.2-.7-.3-1.5-.3-2.3 0-5.6 5.1-10.1 11.4-10.1.4 0 .8 0 1.2.1C31.4 11.8 25.1 8.4 17.6 8.4zm-4.3 6.4c.9 0 1.6.7 1.6 1.6s-.7 1.6-1.6 1.6-1.6-.7-1.6-1.6.7-1.6 1.6-1.6zm8.6 0c.9 0 1.6.7 1.6 1.6s-.7 1.6-1.6 1.6-1.6-.7-1.6-1.6.7-1.6 1.6-1.6z"/>
                  <path fill="currentColor" d="M32.9 22.2c-5.5 0-9.9 3.8-9.9 8.5 0 2.7 1.5 5.1 3.9 6.7l-.7 2.6c-.1.5.4 1 .9.7l3.1-1.6c.8.2 1.7.3 2.7.3 5.5 0 9.9-3.8 9.9-8.5s-4.4-8.7-9.9-8.7zm-3.6 5.5c.7 0 1.2.5 1.2 1.2s-.5 1.2-1.2 1.2-1.2-.5-1.2-1.2.5-1.2 1.2-1.2zm7.2 0c.7 0 1.2.5 1.2 1.2s-.5 1.2-1.2 1.2-1.2-.5-1.2-1.2.5-1.2 1.2-1.2z"/>
                </svg>
              </span>
              <span class="social-label">微信</span>
            </button>
            <button
              v-if="oauthQqEnabled"
              type="button"
              class="social-btn qq"
              :disabled="socialLoading"
              aria-label="QQ登录"
              @click="startOauth('qq')"
            >
              <span class="social-icon" aria-hidden="true">
                <svg viewBox="0 0 48 48" width="22" height="22">
                  <path fill="currentColor" d="M24.1 6c-3.8 0-10.4 2.2-10.4 12.1v2c0 .1-3.5 7.4-3.5 11.5 0 1.1.3 1.7.5 1.7.2 0 1.5-1.1 2.7-3.4.2.3.4.6.6.9.8.2 2.3.5 4 .5h.1c.9 2 2.4 3.5 6.1 3.5s5.2-1.5 6.1-3.5h.1c1.7 0 3.2-.3 4-.5.2-.3.4-.6.6-.9 1.2 2.3 2.5 3.4 2.7 3.4.2 0 .5-.6.5-1.7 0-4.1-3.5-11.4-3.5-11.5v-2C34.5 8.2 27.9 6 24.1 6z"/>
                </svg>
              </span>
              <span class="social-label">QQ</span>
            </button>
          </div>
          <p v-if="socialLoading" class="social-tip">正在跳转第三方登录…</p>
        </div>
        <div class="card-foot login-foot" v-if="register">
          <span class="foot-register">
            还没有账号？
            <router-link class="register-link" to="/register" @click.native="loginOpen = false">免费注册</router-link>
          </span>
        </div>
      </el-form>
    </el-dialog>

    <footer class="sk-footer">
      <div class="sk-footer-grid">
        <div>
          <div class="js-brand compact">
            <img class="js-logo" :src="brandLogo" alt="通查云" />
            <span class="js-name">{{ siteTitle }}</span>
          </div>
          <p class="sk-footer-desc">覆盖快查、问卷、模板与短链发布的一站式平台，支持私有化部署。</p>
        </div>
        <div>
          <h4>产品导航</h4>
          <a href="#products" @click.prevent="scrollTo('products')">产品能力</a>
          <a href="#workflow" @click.prevent="scrollTo('workflow')">工作流</a>
          <a href="#scenes" @click.prevent="scrollTo('scenes')">场景</a>
        </div>
        <div>
          <h4>支持与资源</h4>
          <a href="javascript:;" @click.prevent="openLogin()">登录管理端</a>
          <router-link v-if="register" to="/register">注册账号</router-link>
          <a href="#why" @click.prevent="scrollTo('why')">为什么选我们</a>
        </div>
      </div>
      <p class="js-copy">{{ siteFooter }}</p>
    </footer>
  </div>
</template>

<script>
import { getCodeImg, getOauthConfig } from '@/api/login'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import { mapState } from 'vuex'

export default {
  name: 'Login',
  data() {
    return {
      codeUrl: '',
      loginForm: {
        username: '',
        password: '',
        rememberMe: false,
        code: '',
        uuid: ''
      },
      loginRules: {
        username: [{ required: true, trigger: 'blur', message: '请输入您的账号' }],
        password: [{ required: true, trigger: 'blur', message: '请输入您的密码' }],
        code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
      },
      loading: false,
      loginOpen: false,
      captchaEnabled: true,
      register: false,
      redirect: undefined,
      oauthWechatEnabled: false,
      oauthQqEnabled: false,
      socialLoading: false,
      trustTags: ['中小学', '培训机构', '高校教务', '招生办', '运营团队', '教研组'],
      workflow: [
        { n: '01', title: '创建项目', desc: '模板、内容与主题' },
        { n: '02', title: '配置规则', desc: '条件、校验与安全' },
        { n: '03', title: '发布触达', desc: '短链、海报与 H5' },
        { n: '04', title: '回收分析', desc: '统计、明细与导出' }
      ],
      capabilities: [
        { title: '灵活创建内容', desc: '模板起步，支持 Excel 导入与题库复用，快速生成查询页或问卷初稿。' },
        { title: '编排专业规则', desc: '查询条件、脱敏、日限，以及问卷跳题显隐，配置清晰可预览。' },
        { title: '多渠道发布', desc: '通过短链、二维码与分享海报触达 PC 与手机端用户。' },
        { title: '分析与数据交付', desc: '实时统计、答卷明细与 Excel 导出，支持无效卷标记与审计。' }
      ],
      scenes: [
        { tag: '快查', title: '成绩查询', desc: '期末 / 模拟考成绩开放查询，成绩单式结果页。' },
        { tag: '快查', title: '录取与分班', desc: '录取结果、分班信息一键公示，家长自助查询。' },
        { tag: '问卷', title: '满意度调研', desc: '家长 / 学员反馈快速收集，回收后即看统计。' },
        { tag: '问卷', title: '活动报名', desc: '报名信息收集与导出，适合校园活动与公开课。' },
        { tag: '快查', title: '证书查询', desc: '获奖证书与资格状态查询，支持打印导出。' },
        { tag: '问卷', title: '课堂测评', desc: '课堂测评与结果回收分析，辅助教学改进。' }
      ],
      whyItems: [
        { metric: '1 分钟', title: '查询即可上线', desc: '上传 Excel，配置条件后即可生成公开查询页。' },
        { metric: '双端', title: 'PC / H5 同步', desc: '同一短链适配桌面与手机，扫码即达。' },
        { metric: '私有化', title: '数据自主可控', desc: '可本地部署，数据归属清晰，适合校内与机构内网。' }
      ]
    }
  },
  computed: {
    ...mapState('settings', {
      siteTitle: s => s.siteTitle,
      brandLogo: s => s.siteLogo,
      siteCopyright: s => s.siteCopyright,
      siteIcp: s => s.siteIcp
    }),
    siteFooter() {
      const icp = this.siteIcp ? (' · ' + this.siteIcp) : ''
      return (this.siteCopyright || '') + icp
    }
  },
  watch: {
    $route: {
      handler(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  created() {
    this.$store.dispatch('settings/loadSiteInfo')
    this.getCode()
    this.getCookie()
    this.loadOauthConfig()
    this.handleOauthReturn()
    // 仅从受保护页跳转回来时自动打开登录；首页 / 落地页不弹
    if (this.shouldAutoOpenLogin(this.$route.query && this.$route.query.redirect)) {
      this.loginOpen = true
    }
  },
  mounted() {
    this.initHeroMotion()
  },
  beforeDestroy() {
    this.teardownHeroMotion()
  },
  methods: {
    loadOauthConfig() {
      getOauthConfig().then(res => {
        const d = res.data || res || {}
        this.oauthWechatEnabled = d.wechatEnabled === true || d.wechatEnabled === 'true'
        this.oauthQqEnabled = d.qqEnabled === true || d.qqEnabled === 'true'
      }).catch(() => {
        this.oauthWechatEnabled = false
        this.oauthQqEnabled = false
      })
    },
    startOauth(provider) {
      if (this.socialLoading) return
      const ready = provider === 'wechat' ? this.oauthWechatEnabled : this.oauthQqEnabled
      if (!ready) {
        this.$message.warning('请先在「系统管理 → 基础设置 → 第三方登录」中启用并填写 AppID / 密钥')
        return
      }
      this.socialLoading = true
      const base = process.env.VUE_APP_BASE_API || '/dev-api'
      window.location.href = `${base}/login/oauth/${provider}/authorize`
    },
    handleOauthReturn() {
      const q = this.$route.query || {}
      if (q.oauthError) {
        this.loginOpen = true
        const msg = Array.isArray(q.oauthError) ? q.oauthError[0] : q.oauthError
        this.$message.error(decodeURIComponent(msg) || '第三方登录失败')
        this.clearOauthQuery()
        return
      }
      if (!q.oauthTicket) return
      const ticket = Array.isArray(q.oauthTicket) ? q.oauthTicket[0] : q.oauthTicket
      this.socialLoading = true
      this.loginOpen = true
      this.$store.dispatch('OauthLoginByTicket', ticket).then(() => {
        this.clearOauthQuery()
        this.$router.push({ path: this.redirect || '/' }).catch(() => {})
      }).catch(() => {
        this.socialLoading = false
        this.clearOauthQuery()
      })
    },
    clearOauthQuery() {
      const query = { ...(this.$route.query || {}) }
      delete query.oauthTicket
      delete query.oauthError
      this.$router.replace({ path: this.$route.path, query }).catch(() => {})
    },
    initHeroMotion() {
      if (typeof window === 'undefined') return
      if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
      // 移动端关闭 canvas，避免首屏卡顿
      if (window.matchMedia && window.matchMedia('(max-width: 960px)').matches) return
      const canvas = this.$refs.heroCanvas
      const hero = this.$el && this.$el.querySelector('.sk-hero')
      if (!canvas || !hero) return
      this._heroParticles = []
      this._heroMeteors = []
      this._heroRipples = []
      this._heroMouse = { x: -9999, y: -9999, active: false }
      this._heroRunning = true
      this._heroTick = 0
      const resize = () => {
        const rect = hero.getBoundingClientRect()
        const dpr = Math.min(window.devicePixelRatio || 1, 2)
        canvas.width = Math.max(1, Math.floor(rect.width * dpr))
        canvas.height = Math.max(1, Math.floor(rect.height * dpr))
        canvas.style.width = rect.width + 'px'
        canvas.style.height = rect.height + 'px'
        this._heroCtx = canvas.getContext('2d')
        this._heroDpr = dpr
        this._heroRect = rect
        this.seedHeroParticles(rect.width, rect.height)
      }
      const onMove = (e) => {
        const rect = this._heroRect || hero.getBoundingClientRect()
        this._heroMouse.x = e.clientX - rect.left
        this._heroMouse.y = e.clientY - rect.top
        this._heroMouse.active = true
      }
      const onLeave = () => {
        this._heroMouse.active = false
        this._heroMouse.x = -9999
        this._heroMouse.y = -9999
      }
      this._heroResize = resize
      this._heroMove = onMove
      this._heroLeave = onLeave
      resize()
      window.addEventListener('resize', resize)
      hero.addEventListener('mousemove', onMove)
      hero.addEventListener('mouseleave', onLeave)
      const loop = () => {
        if (!this._heroRunning) return
        this._heroTick += 1
        this.drawHeroParticles()
        this._heroRaf = requestAnimationFrame(loop)
      }
      this._heroRaf = requestAnimationFrame(loop)
    },
    teardownHeroMotion() {
      this._heroRunning = false
      if (this._heroRaf) cancelAnimationFrame(this._heroRaf)
      if (this._heroResize) window.removeEventListener('resize', this._heroResize)
      const hero = this.$el && this.$el.querySelector('.sk-hero')
      if (hero) {
        if (this._heroMove) hero.removeEventListener('mousemove', this._heroMove)
        if (this._heroLeave) hero.removeEventListener('mouseleave', this._heroLeave)
      }
      this._heroRaf = null
      this._heroResize = null
      this._heroMove = null
      this._heroLeave = null
      this._heroClick = null
      this._heroCtx = null
      this._heroParticles = []
      this._heroMeteors = []
      this._heroRipples = []
    },
    seedHeroParticles(w, h) {
      const count = Math.max(28, Math.min(48, Math.floor((w * h) / 28000)))
      const list = []
      for (let i = 0; i < count; i++) {
        const speed = 0.18 + Math.random() * 0.35
        const angle = Math.random() * Math.PI * 2
        list.push({
          x: Math.random() * w,
          y: Math.random() * h,
          vx: Math.cos(angle) * speed,
          vy: Math.sin(angle) * speed,
          r: 1.2 + Math.random() * 1.8,
          pulse: Math.random() * Math.PI * 2,
          hue: Math.random() > 0.55 ? 0 : 1
        })
      }
      this._heroParticles = list
      this._heroMeteors = []
      this._heroSize = { w, h }
    },
    drawHeroParticles() {
      const ctx = this._heroCtx
      const size = this._heroSize
      const pts = this._heroParticles
      if (!ctx || !size || !pts || !pts.length) return
      const dpr = this._heroDpr || 1
      const { w, h } = size
      const mouse = this._heroMouse || { x: -9999, y: -9999, active: false }
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      ctx.clearRect(0, 0, w, h)

      const linkDist = Math.min(140, Math.max(90, w * 0.1))
      for (let i = 0; i < pts.length; i++) {
        const p = pts[i]
        if (mouse.active) {
          const mdx = mouse.x - p.x
          const mdy = mouse.y - p.y
          const md = Math.sqrt(mdx * mdx + mdy * mdy) || 1
          if (md < 140) {
            p.vx += (mdx / md) * 0.018
            p.vy += (mdy / md) * 0.018
          }
        }
        const sp = Math.sqrt(p.vx * p.vx + p.vy * p.vy) || 1
        if (sp > 0.9) {
          p.vx *= 0.97
          p.vy *= 0.97
        }
        p.x += p.vx
        p.y += p.vy
        p.pulse += 0.03
        if (p.x < -20) p.x = w + 20
        if (p.x > w + 20) p.x = -20
        if (p.y < -20) p.y = h + 20
        if (p.y > h + 20) p.y = -20
      }

      for (let i = 0; i < pts.length; i++) {
        for (let j = i + 1; j < pts.length; j++) {
          const a = pts[i]
          const b = pts[j]
          const dx = a.x - b.x
          const dy = a.y - b.y
          const dist = Math.sqrt(dx * dx + dy * dy)
          if (dist > linkDist) continue
          const alpha = (1 - dist / linkDist) * 0.22
          ctx.beginPath()
          ctx.moveTo(a.x, a.y)
          ctx.lineTo(b.x, b.y)
          ctx.strokeStyle = `rgba(37, 99, 235, ${alpha})`
          ctx.lineWidth = 1
          ctx.stroke()
        }
      }

      for (let i = 0; i < pts.length; i++) {
        const p = pts[i]
        const glow = p.r + 0.8 + Math.sin(p.pulse) * 0.5
        ctx.beginPath()
        ctx.arc(p.x, p.y, glow, 0, Math.PI * 2)
        ctx.fillStyle = p.hue ? 'rgba(14, 165, 233, 0.55)' : 'rgba(37, 99, 235, 0.5)'
        ctx.fill()
      }

      if (mouse.active) {
        const halo = ctx.createRadialGradient(mouse.x, mouse.y, 0, mouse.x, mouse.y, 70)
        halo.addColorStop(0, 'rgba(96, 165, 250, 0.14)')
        halo.addColorStop(1, 'rgba(96, 165, 250, 0)')
        ctx.fillStyle = halo
        ctx.beginPath()
        ctx.arc(mouse.x, mouse.y, 70, 0, Math.PI * 2)
        ctx.fill()
      }
    },
    shouldAutoOpenLogin(redirect) {
      if (!redirect) return false
      let path = String(redirect)
      try { path = decodeURIComponent(path) } catch (e) { /* ignore */ }
      path = path.split('?')[0].split('#')[0]
      return path !== '/' && path !== '/index' && path !== '/login'
    },
    scrollTo(id) {
      if (id === 'top') {
        window.scrollTo({ top: 0, behavior: 'smooth' })
        return
      }
      const el = document.getElementById(id)
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    },
    openLogin() {
      this.loginOpen = true
      if (this.captchaEnabled && !this.codeUrl) this.getCode()
    },
    onLoginOpened() {
      this.$nextTick(() => {
        if (this.$refs.loginForm) this.$refs.loginForm.clearValidate()
        const input = document.querySelector('.js-login-dialog input')
        if (input) input.focus()
      })
    },
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        this.register = res.registerEnabled === true || res.registerEnabled === 'true'
        if (this.captchaEnabled) {
          this.codeUrl = 'data:image/gif;base64,' + res.img
          this.loginForm.uuid = res.uuid
        }
      })
    },
    getCookie() {
      const username = Cookies.get('username')
      const password = Cookies.get('password')
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) {
          this.openLogin()
          return
        }
        this.loading = true
        if (this.loginForm.rememberMe) {
          Cookies.set('username', this.loginForm.username, { expires: 30 })
          Cookies.set('password', encrypt(this.loginForm.password), { expires: 30 })
          Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 })
        } else {
          Cookies.remove('username')
          Cookies.remove('password')
          Cookies.remove('rememberMe')
        }
        this.$store.dispatch('Login', this.loginForm).then(() => {
          this.$router.push({ path: this.redirect || '/' }).catch(() => {})
        }).catch(() => {
          this.loading = false
          if (this.captchaEnabled) this.getCode()
        })
      })
    }
  }
}
</script>

<style lang="scss">
@import "~@/assets/styles/auth-page.scss";

.js-login-dialog .js-code-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
}
.js-login-dialog .js-code-td-input {
  padding: 0 10px 0 0;
  vertical-align: middle;
}
.js-login-dialog .js-code-td-img {
  width: 120px;
  padding: 0;
  vertical-align: middle;
}
.js-login-dialog .js-code-table .login-code {
  box-sizing: border-box;
  width: 120px;
  height: 50px;
  border: 1.5px solid #e2e8f0;
  border-radius: 14px;
  overflow: hidden;
  background: #f8fafc;
  cursor: pointer;
}
.js-login-dialog .js-code-table .login-code:hover {
  border-color: var(--js-brand, #1d4ed8);
}
.js-login-dialog .js-code-table .login-code-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.js-login-dialog .login-code-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 12px;
  color: #94a3b8;
}

.v-modal {
  backdrop-filter: blur(8px);
  background: rgba(15, 23, 42, 0.42) !important;
}

.login-extra-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 14px;
}
.forgot-link {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
  text-decoration: none;
  line-height: 1.4;
  transition: color 0.15s ease;
  &:hover {
    color: #1d4ed8;
  }
}
.login-foot {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px 14px;
  margin-top: 20px;
  padding-top: 18px;
  border-top: none;
  background: linear-gradient(180deg, transparent, rgba(248, 250, 252, 0.9));
  position: relative;
  &::before {
    content: '';
    position: absolute;
    left: 8%;
    right: 8%;
    top: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, #e2e8f0 20%, #e2e8f0 80%, transparent);
  }
}
.login-foot .foot-register {
  margin-left: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.4;
}
.login-foot .register-link {
  margin-left: 2px;
  color: #1d4ed8;
  font-weight: 600;
  text-decoration: none;
  transition: color 0.15s ease;
  &:hover {
    color: #1e3a8a;
  }
}
.social-login {
  margin-top: 20px;
}
.social-divider {
  display: flex;
  align-items: center;
  color: #94a3b8;
  font-size: 12px;
  letter-spacing: 0.02em;
  margin-bottom: 16px;
  &::before,
  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: linear-gradient(90deg, transparent, #e2e8f0 18%, #e2e8f0 82%, transparent);
  }
  span {
    padding: 0 12px;
    white-space: nowrap;
  }
}
.social-btns {
  display: flex;
  justify-content: center;
  gap: 32px;
}
.social-btn {
  appearance: none;
  border: 0;
  background: transparent;
  padding: 0;
  cursor: pointer;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #64748b;
  transition: transform .2s ease, opacity .15s ease;
  &:hover:not(:disabled) {
    transform: translateY(-2px);
  }
  &:active:not(:disabled) {
    transform: translateY(0);
  }
  &:disabled {
    opacity: .55;
    cursor: not-allowed;
  }
  &:focus-visible {
    outline: none;
    .social-icon {
      box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.22);
    }
  }
}
.social-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  transition: box-shadow .2s ease, transform .2s ease, filter .2s ease;
  svg {
    display: block;
  }
}
.social-label {
  font-size: 12px;
  line-height: 1;
  color: #64748b;
  transition: color .15s ease;
}
.social-btn.wechat .social-icon {
  background: linear-gradient(145deg, #1aad19 0%, #07c160 55%, #06ad56 100%);
  box-shadow: 0 4px 12px rgba(7, 193, 96, 0.28);
}
.social-btn.wechat:hover:not(:disabled) .social-icon {
  box-shadow: 0 6px 16px rgba(7, 193, 96, 0.4);
  filter: brightness(1.04);
}
.social-btn.wechat:hover:not(:disabled) .social-label {
  color: #07c160;
}
.social-btn.qq .social-icon {
  background: linear-gradient(145deg, #2aaef5 0%, #12b7f5 55%, #0a9de0 100%);
  box-shadow: 0 4px 12px rgba(18, 183, 245, 0.28);
}
.social-btn.qq:hover:not(:disabled) .social-icon {
  box-shadow: 0 6px 16px rgba(18, 183, 245, 0.4);
  filter: brightness(1.04);
}
.social-btn.qq:hover:not(:disabled) .social-label {
  color: #12b7f5;
}
.social-tip {
  margin: 12px 0 0;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}

@media (max-width: 480px) {
  .js-login-dialog.el-dialog {
    width: calc(100vw - 32px) !important;
    margin-top: 6vh !important;
  }
}
</style>
