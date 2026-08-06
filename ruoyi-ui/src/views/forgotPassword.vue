<template>
  <div class="js-home sk">
    <header class="js-nav">
      <div class="js-nav-inner">
        <router-link class="js-brand" to="/login">
          <img class="js-logo" :src="brandLogo" alt="通查云" />
          <span class="js-name">{{ siteTitle }}</span>
        </router-link>
        <nav class="js-links">
          <router-link to="/login">产品首页</router-link>
        </nav>
        <div class="js-nav-actions">
          <router-link class="js-link-btn" to="/login">登录</router-link>
        </div>
      </div>
    </header>

    <section class="js-login-section sk-register">
      <div class="js-login-wrap">
        <div class="js-login-aside">
          <p class="sk-status"><i class="sk-status-dot"></i>邮箱验证重置</p>
          <h2>找回{{ siteTitle }}密码</h2>
          <p>使用账号绑定的邮箱获取验证码，即可设置新密码。请确保个人资料中已填写邮箱。</p>
          <ul class="js-aside-list">
            <li>验证码 10 分钟内有效</li>
            <li>重置成功后请使用新密码登录</li>
            <li>如收不到邮件，请检查垃圾箱或联系管理员</li>
          </ul>
        </div>

        <el-form v-if="mailResetEnabled" ref="form" :model="form" :rules="rules" class="js-login-card" @submit.native.prevent>
          <h3>重置密码</h3>
          <p class="js-login-tip">填写绑定邮箱与验证码</p>
          <el-form-item prop="email">
            <el-input v-model="form.email" placeholder="绑定邮箱" prefix-icon="el-icon-message" />
          </el-form-item>
          <el-form-item prop="code" v-if="captchaEnabled" class="code-item">
            <el-input
              v-model="form.code"
              auto-complete="off"
              placeholder="图形验证码"
              prefix-icon="el-icon-key"
              class="code-input"
            />
            <div class="login-code" @click="getCode" title="点击刷新验证码">
              <img :src="codeUrl" class="login-code-img" alt="验证码" />
            </div>
          </el-form-item>
          <el-form-item prop="emailCode" class="code-item">
            <el-input v-model="form.emailCode" placeholder="邮箱验证码" prefix-icon="el-icon-key" class="code-input" />
            <el-button size="medium" :disabled="mailCodeSeconds > 0" :loading="mailCodeSending" @click="sendMailCode">
              {{ mailCodeSeconds > 0 ? (mailCodeSeconds + 's') : '获取验证码' }}
            </el-button>
          </el-form-item>
          <el-form-item prop="password" :rules="registerPwdValidator">
            <el-input
              v-model="form.password"
              type="password"
              auto-complete="new-password"
              placeholder="新密码"
              prefix-icon="el-icon-lock"
              show-password
            />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              auto-complete="new-password"
              placeholder="确认新密码"
              prefix-icon="el-icon-lock"
              show-password
              @keyup.enter.native="handleReset"
            />
          </el-form-item>
          <el-button :loading="loading" type="primary" class="submit-btn" @click.native.prevent="handleReset">
            <span v-if="!loading">确认重置</span>
            <span v-else>提交中…</span>
          </el-button>
          <div class="card-foot">
            想起密码了？
            <router-link to="/login">去登录</router-link>
          </div>
        </el-form>

        <div v-else class="js-login-card">
          <h3>功能未开启</h3>
          <p class="js-login-tip">管理员尚未开启「忘记密码邮箱重置」，请联系管理员通过后台重置密码，或开启邮件服务后重试。</p>
          <div class="card-foot">
            <router-link to="/login">返回登录</router-link>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import { getCodeImg, sendResetEmailCode, resetPasswordByEmail } from '@/api/login'
import passwordRule from '@/utils/passwordRule'
import { mapState } from 'vuex'

export default {
  name: 'ForgotPassword',
  mixins: [passwordRule],
  computed: {
    ...mapState('settings', {
      siteTitle: s => s.siteTitle,
      brandLogo: s => s.siteLogo,
      mailResetEnabled: s => s.mailResetEnabled
    })
  },
  data() {
    const equalToPassword = (rule, value, callback) => {
      if (this.form.password !== value) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      codeUrl: '',
      form: {
        email: '',
        emailCode: '',
        password: '',
        confirmPassword: '',
        code: '',
        uuid: ''
      },
      mailCodeSeconds: 0,
      mailCodeSending: false,
      mailCodeTimer: null,
      rules: {
        email: [
          { required: true, trigger: 'blur', message: '请输入绑定邮箱' },
          {
            validator: (rule, value, callback) => {
              if (!value) return callback(new Error('请输入绑定邮箱'))
              if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return callback(new Error('请输入正确的邮箱地址'))
              callback()
            },
            trigger: 'blur'
          }
        ],
        emailCode: [{ required: true, trigger: 'blur', message: '请输入邮箱验证码' }],
        confirmPassword: [
          { required: true, trigger: 'blur', message: '请再次输入新密码' },
          { validator: equalToPassword, trigger: 'blur' }
        ],
        code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
      },
      loading: false,
      captchaEnabled: true
    }
  },
  created() {
    this.$store.dispatch('settings/loadSiteInfo')
    this.getCode()
  },
  beforeDestroy() {
    clearInterval(this.mailCodeTimer)
  },
  methods: {
    sendMailCode() {
      const fields = this.captchaEnabled ? ['email', 'code'] : ['email']
      let ok = true
      this.$refs.form.validateField(fields, err => {
        if (err) ok = false
      })
      if (!ok) return
      this.mailCodeSending = true
      sendResetEmailCode({
        email: this.form.email,
        code: this.form.code,
        uuid: this.form.uuid
      }).then(res => {
        this.$modal.msgSuccess(res.msg || '若该邮箱已绑定账号，验证码将发送至邮箱')
        this.mailCodeSeconds = 60
        clearInterval(this.mailCodeTimer)
        this.mailCodeTimer = setInterval(() => {
          this.mailCodeSeconds -= 1
          if (this.mailCodeSeconds <= 0) clearInterval(this.mailCodeTimer)
        }, 1000)
        this.getCode()
      }).catch(() => {
        this.getCode()
      }).finally(() => { this.mailCodeSending = false })
    },
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = 'data:image/gif;base64,' + res.img
          this.form.uuid = res.uuid
        }
      })
    },
    handleReset() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.loading = true
        resetPasswordByEmail({
          email: this.form.email,
          emailCode: this.form.emailCode,
          password: this.form.password,
          code: this.form.code,
          uuid: this.form.uuid
        }).then(res => {
          this.$alert(res.msg || '密码已重置，请使用新密码登录', '重置成功', {
            type: 'success',
            callback: () => {
              this.$router.push('/login')
            }
          })
        }).catch(() => {
          this.getCode()
        }).finally(() => { this.loading = false })
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
@import "~@/assets/styles/auth-page.scss";

.code-item {
  ::v-deep .el-form-item__content {
    display: flex;
    gap: 10px;
    align-items: center;
  }
  .code-input { flex: 1; }
  .login-code {
    height: 38px;
    cursor: pointer;
    img { height: 38px; display: block; border-radius: 4px; }
  }
}
.submit-btn { width: 100%; margin-top: 4px; }
.card-foot { margin-top: 16px; text-align: center; color: #64748b; font-size: 13px; }
</style>
