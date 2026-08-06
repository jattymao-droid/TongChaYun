<template>
  <div class="app-container basic-settings">
    <el-form ref="form" :model="form" label-width="120px" v-loading="loading" class="basic-form">
      <el-card shadow="never" class="mb16">
        <div slot="header"><span>站点信息</span></div>
        <el-form-item label="系统名称">
          <el-input v-model="form.title" placeholder="如：通查云" maxlength="50" show-word-limit style="max-width:420px" />
        </el-form-item>
        <el-form-item label="系统 Logo">
          <image-upload v-model="form.logo" :limit="1" :file-size="2" :file-type="['png','jpg','jpeg','svg','webp','gif']" />
          <div class="tip">建议正方形 PNG/SVG，上传后自动用于侧栏与登录页</div>
        </el-form-item>
        <el-form-item label="页脚版权">
          <el-input v-model="form.copyright" type="textarea" :rows="2" placeholder="页脚版权文案" maxlength="200" show-word-limit style="max-width:560px" />
        </el-form-item>
        <el-form-item label="备案号">
          <el-input v-model="form.icp" placeholder="如：京ICP备xxxxxxxx号" maxlength="100" style="max-width:420px" />
        </el-form-item>
        <el-form-item label="显示页脚">
          <el-switch v-model="form.footerVisible" active-value="true" inactive-value="false" />
        </el-form-item>
      </el-card>

      <el-card shadow="never" class="mb16">
        <div slot="header"><span>邮箱验证 / SMTP</span></div>
        <el-form-item label="启用邮件服务">
          <el-switch v-model="form.mailEnabled" active-value="true" inactive-value="false" />
          <span class="tip inline">关闭后无法发信、注册邮箱验证与忘记密码重置</span>
        </el-form-item>
        <el-form-item label="注册邮箱验证">
          <el-switch v-model="form.mailVerifyEnabled" active-value="true" inactive-value="false" :disabled="form.mailEnabled !== 'true'" />
          <span class="tip inline">开启后注册需填写邮箱并验证码</span>
        </el-form-item>
        <el-form-item label="忘记密码重置">
          <el-switch v-model="form.mailResetEnabled" active-value="true" inactive-value="false" :disabled="form.mailEnabled !== 'true'" />
          <span class="tip inline">开启后登录页显示「忘记密码」，可通过邮箱验证码重置</span>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="10">
            <el-form-item label="SMTP 主机">
              <el-input v-model="form.mailHost" placeholder="smtp.qq.com / smtp.163.com" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="端口">
              <el-input v-model="form.mailPort" placeholder="465" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="SSL">
              <el-switch v-model="form.mailSsl" active-value="true" inactive-value="false" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="邮箱账号">
          <el-input v-model="form.mailUsername" placeholder="发信邮箱账号" style="max-width:420px" />
        </el-form-item>
        <el-form-item label="邮箱密码">
          <el-input v-model="form.mailPassword" type="password" show-password placeholder="授权码或密码，不改请留 ******" style="max-width:420px" />
          <div class="tip" v-if="form.mailPasswordSet">已保存密码；留空掩码则保持不变</div>
        </el-form-item>
        <el-form-item label="发件人">
          <el-input v-model="form.mailFrom" placeholder="可选，默认与账号相同" style="max-width:420px" />
        </el-form-item>
        <el-form-item label="发送测试">
          <el-input v-model="testTo" placeholder="收件邮箱" style="max-width:280px; margin-right:10px" />
          <el-button type="primary" plain :loading="testing" @click="handleTestMail" v-hasPermi="['system:basic:edit']">发送测试邮件</el-button>
        </el-form-item>
      </el-card>

      <el-card shadow="never" class="mb16">
        <div slot="header"><span>第三方登录（微信 / QQ）</span></div>
        <el-form-item label="前端根地址">
          <el-input v-model="form.oauthRedirectBase" placeholder="http://127.0.0.1:1024" style="max-width:420px" />
          <div class="tip">登录完成后跳回管理端的地址</div>
        </el-form-item>
        <el-form-item label="接口回调根">
          <el-input v-model="form.oauthCallbackBase" placeholder="http://127.0.0.1:1024/dev-api" style="max-width:420px" />
          <div class="tip">
            需在开放平台登记的回调前缀。预览：
            <code>{{ wechatCallbackPreview }}</code> /
            <code>{{ qqCallbackPreview }}</code>
          </div>
        </el-form-item>
        <el-divider content-position="left">微信开放平台 · 网站应用</el-divider>
        <el-form-item label="启用微信登录">
          <el-switch v-model="form.oauthWechatEnabled" active-value="true" inactive-value="false" />
        </el-form-item>
        <el-form-item label="微信 AppID">
          <el-input v-model="form.oauthWechatAppId" maxlength="64" style="max-width:420px" :disabled="form.oauthWechatEnabled !== 'true'" />
        </el-form-item>
        <el-form-item label="微信 AppSecret">
          <el-input v-model="form.oauthWechatAppSecret" type="password" show-password placeholder="不改请留 ******" maxlength="128" style="max-width:420px" :disabled="form.oauthWechatEnabled !== 'true'" />
          <div class="tip" v-if="form.oauthWechatAppSecretSet">已保存密钥；留空掩码则保持不变</div>
        </el-form-item>
        <el-divider content-position="left">QQ 互联</el-divider>
        <el-form-item label="启用 QQ 登录">
          <el-switch v-model="form.oauthQqEnabled" active-value="true" inactive-value="false" />
        </el-form-item>
        <el-form-item label="QQ AppID">
          <el-input v-model="form.oauthQqAppId" maxlength="64" style="max-width:420px" :disabled="form.oauthQqEnabled !== 'true'" />
        </el-form-item>
        <el-form-item label="QQ AppKey">
          <el-input v-model="form.oauthQqAppKey" type="password" show-password placeholder="不改请留 ******" maxlength="128" style="max-width:420px" :disabled="form.oauthQqEnabled !== 'true'" />
          <div class="tip" v-if="form.oauthQqAppKeySet">已保存密钥；留空掩码则保持不变</div>
        </el-form-item>
      </el-card>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="submit" v-hasPermi="['system:basic:edit']">保存设置</el-button>
        <el-button @click="load">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { getBasicSettings, saveBasicSettings, testMail } from '@/api/system/basic'

export default {
  name: 'SystemBasic',
  data() {
    return {
      loading: false,
      saving: false,
      testing: false,
      testTo: '',
      form: {
        title: '',
        logo: '',
        copyright: '',
        icp: '',
        footerVisible: 'true',
        mailEnabled: 'false',
        mailHost: '',
        mailPort: '465',
        mailUsername: '',
        mailPassword: '',
        mailFrom: '',
        mailSsl: 'true',
        mailVerifyEnabled: 'false',
        mailResetEnabled: 'false',
        mailPasswordSet: false,
        oauthRedirectBase: 'http://127.0.0.1:1024',
        oauthCallbackBase: 'http://127.0.0.1:1024/dev-api',
        oauthWechatEnabled: 'false',
        oauthWechatAppId: '',
        oauthWechatAppSecret: '',
        oauthWechatAppSecretSet: false,
        oauthQqEnabled: 'false',
        oauthQqAppId: '',
        oauthQqAppKey: '',
        oauthQqAppKeySet: false
      }
    }
  },
  computed: {
    wechatCallbackPreview() {
      const base = (this.form.oauthCallbackBase || '').replace(/\/+$/, '')
      return base ? (base + '/login/oauth/callback/wechat') : ''
    },
    qqCallbackPreview() {
      const base = (this.form.oauthCallbackBase || '').replace(/\/+$/, '')
      return base ? (base + '/login/oauth/callback/qq') : ''
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      getBasicSettings().then(res => {
        const d = res.data || {}
        this.form = {
          title: d.title || '',
          logo: (!d.logo || d.logo === '/logo.svg') ? '' : d.logo,
          copyright: d.copyright || '',
          icp: d.icp || '',
          footerVisible: d.footerVisible === 'false' ? 'false' : 'true',
          mailEnabled: d.mailEnabled === 'true' ? 'true' : 'false',
          mailHost: d.mailHost || '',
          mailPort: d.mailPort || '465',
          mailUsername: d.mailUsername || '',
          mailPassword: d.mailPassword || '',
          mailFrom: d.mailFrom || '',
          mailSsl: d.mailSsl === 'false' ? 'false' : 'true',
          mailVerifyEnabled: d.mailVerifyEnabled === 'true' ? 'true' : 'false',
          mailResetEnabled: d.mailResetEnabled === 'true' ? 'true' : 'false',
          mailPasswordSet: !!d.mailPasswordSet,
          oauthRedirectBase: d.oauthRedirectBase || 'http://127.0.0.1:1024',
          oauthCallbackBase: d.oauthCallbackBase || 'http://127.0.0.1:1024/dev-api',
          oauthWechatEnabled: d.oauthWechatEnabled === 'true' ? 'true' : 'false',
          oauthWechatAppId: d.oauthWechatAppId || '',
          oauthWechatAppSecret: d.oauthWechatAppSecret || '',
          oauthWechatAppSecretSet: !!d.oauthWechatAppSecretSet,
          oauthQqEnabled: d.oauthQqEnabled === 'true' ? 'true' : 'false',
          oauthQqAppId: d.oauthQqAppId || '',
          oauthQqAppKey: d.oauthQqAppKey || '',
          oauthQqAppKeySet: !!d.oauthQqAppKeySet
        }
        if (!this.testTo && this.form.mailUsername) this.testTo = this.form.mailUsername
      }).finally(() => { this.loading = false })
    },
    submit() {
      this.saving = true
      const payload = { ...this.form, logo: this.form.logo || '/logo.svg' }
      saveBasicSettings(payload).then(() => {
        this.$modal.msgSuccess('保存成功')
        this.$store.dispatch('settings/loadSiteInfo', true)
        this.load()
      }).finally(() => { this.saving = false })
    },
    handleTestMail() {
      if (!this.testTo) {
        this.$modal.msgWarning('请填写收件邮箱')
        return
      }
      this.testing = true
      // save first so test uses latest smtp if edited
      const payload = { ...this.form, logo: this.form.logo || '/logo.svg' }
      saveBasicSettings(payload).then(() => testMail(this.testTo)).then(() => {
        this.$modal.msgSuccess('测试邮件已发送，请查收')
      }).finally(() => { this.testing = false })
    }
  }
}
</script>

<style scoped>
.basic-form { max-width: 900px; }
.mb16 { margin-bottom: 16px; }
.tip { color: #94a3b8; font-size: 12px; line-height: 1.5; margin-top: 6px; }
.tip.inline { margin-left: 12px; margin-top: 0; }
.tip code {
  display: inline-block;
  margin-top: 4px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #334155;
  font-size: 12px;
  word-break: break-all;
}
</style>
