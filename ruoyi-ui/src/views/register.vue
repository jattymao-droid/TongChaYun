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
          <router-link class="js-cta" to="/login">立即体验</router-link>
        </div>
      </div>
    </header>

    <section class="js-login-section sk-register">
      <div class="js-login-wrap">
        <div class="js-login-aside">
          <p class="sk-status"><i class="sk-status-dot"></i>模板快速起步已开启</p>
          <h2>注册{{ siteTitle }}，立刻发布</h2>
          <p>创建账号后即可搭建查询与问卷。数据默认仅本人可见，发布后通过短链分享。</p>
          <ul class="js-aside-list">
            <li>模板起步，少配置即可上线</li>
            <li>查询 / 问卷同一套管理体验</li>
            <li>支持验证码与访问审计</li>
          </ul>
        </div>

        <el-form ref="registerForm" :model="registerForm" :rules="registerRules" class="js-login-card" @submit.native.prevent>
          <h3>创建账号</h3>
          <p class="js-login-tip">账号、手机号、邮箱均可用于登录</p>
          <el-form-item prop="username">
            <el-input v-model="registerForm.username" type="text" auto-complete="off" placeholder="账号" prefix-icon="el-icon-user" />
          </el-form-item>
          <el-form-item prop="phonenumber">
            <el-input v-model="registerForm.phonenumber" type="tel" auto-complete="tel" placeholder="手机号" prefix-icon="el-icon-phone-outline" maxlength="11" />
          </el-form-item>
          <el-form-item prop="email">
            <el-input v-model="registerForm.email" type="email" auto-complete="email" placeholder="邮箱" prefix-icon="el-icon-message" />
          </el-form-item>
          <el-form-item prop="password" :rules="registerPwdValidator">
            <el-input
              v-model="registerForm.password"
              type="password"
              auto-complete="off"
              placeholder="密码"
              prefix-icon="el-icon-lock"
              show-password
              @keyup.enter.native="handleRegister"
            />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              auto-complete="off"
              placeholder="确认密码"
              prefix-icon="el-icon-lock"
              show-password
              @keyup.enter.native="handleRegister"
            />
          </el-form-item>
          <template v-if="mailVerifyEnabled">
            <el-form-item prop="emailCode" class="code-item">
              <el-input v-model="registerForm.emailCode" placeholder="邮箱验证码" prefix-icon="el-icon-key" class="code-input" />
              <el-button size="medium" :disabled="mailCodeSeconds > 0" :loading="mailCodeSending" @click="sendMailCode">
                {{ mailCodeSeconds > 0 ? (mailCodeSeconds + 's') : '获取验证码' }}
              </el-button>
            </el-form-item>
          </template>
          <el-form-item prop="code" v-if="captchaEnabled" class="code-item">
            <el-input
              v-model="registerForm.code"
              auto-complete="off"
              placeholder="验证码"
              prefix-icon="el-icon-key"
              class="code-input"
              @keyup.enter.native="handleRegister"
            />
            <div class="login-code" @click="getCode" title="点击刷新验证码">
              <img :src="codeUrl" class="login-code-img" alt="验证码" />
            </div>
          </el-form-item>
          <el-button :loading="loading" type="primary" class="submit-btn" @click.native.prevent="handleRegister">
            <span v-if="!loading">注册并开始</span>
            <span v-else>注册中…</span>
          </el-button>
          <div class="card-foot">
            已有账号？
            <router-link to="/login">去登录</router-link>
          </div>
        </el-form>
      </div>
    </section>
  </div>
</template>

<script>
import { getCodeImg, register } from '@/api/login'
import passwordRule from '@/utils/passwordRule'

import { mapState } from 'vuex'
import { sendRegisterEmailCode } from '@/api/system/basic'

export default {
  mixins: [passwordRule],
  computed: {
    ...mapState('settings', {
      siteTitle: s => s.siteTitle,
      brandLogo: s => s.siteLogo,
      mailVerifyEnabled: s => s.mailVerifyEnabled
    })
  },
  data() {
    const equalToPassword = (rule, value, callback) => {
      if (this.registerForm.password !== value) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      codeUrl: '',
      registerForm: {
        username: '',
        phonenumber: '',
        password: '',
        confirmPassword: '',
        email: '',
        emailCode: '',
        code: '',
        uuid: ''
      },
      mailCodeSeconds: 0,
      mailCodeSending: false,
      mailCodeTimer: null,
      registerRules: {
        username: [
          { required: true, trigger: 'blur', message: '请输入您的账号' },
          { min: 2, max: 20, message: '用户账号长度必须介于 2 和 20 之间', trigger: 'blur' }
        ],
        phonenumber: [
          { required: true, trigger: 'blur', message: '请输入手机号' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, trigger: 'blur', message: '请再次输入您的密码' },
          { required: true, validator: equalToPassword, trigger: 'blur' }
        ],
        email: [
          { required: true, trigger: 'blur', message: '请输入邮箱' },
          { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
        ],
        emailCode: [{
          validator: (rule, value, callback) => {
            if (!this.mailVerifyEnabled) return callback()
            if (!value) return callback(new Error('请输入邮箱验证码'))
            callback()
          }, trigger: 'blur'
        }],
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
  methods: {
    sendMailCode() {
      const fields = this.captchaEnabled ? ['email', 'code'] : ['email']
      let ok = true
      this.$refs.registerForm.validateField(fields, err => {
        if (err) ok = false
      })
      if (!ok) return
      this.mailCodeSending = true
      sendRegisterEmailCode({
        email: this.registerForm.email,
        code: this.registerForm.code,
        uuid: this.registerForm.uuid
      }).then(() => {
        this.$modal.msgSuccess('验证码已发送')
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
          this.registerForm.uuid = res.uuid
        }
      })
    },
    handleRegister() {
      this.$refs.registerForm.validate(valid => {
        if (!valid) return
        this.loading = true
        register(this.registerForm).then(() => {
          const username = this.registerForm.username
          this.$alert("<font color='red'>恭喜你，您的账号 " + username + " 注册成功！</font>", '系统提示', {
            dangerouslyUseHTMLString: true,
            type: 'success'
          }).then(() => {
            this.$router.push('/login')
          }).catch(() => {})
        }).catch(() => {
          this.loading = false
          if (this.captchaEnabled) this.getCode()
        })
      })
    }
  },
  beforeDestroy() {
    clearInterval(this.mailCodeTimer)
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
@import "~@/assets/styles/auth-page.scss";
</style>
