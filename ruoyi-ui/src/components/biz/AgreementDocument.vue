<template>
  <article class="agreement-doc">
    <header class="doc-header">
      <h1 class="doc-title">{{ title || '协议详情' }}</h1>
      <div class="doc-meta">
        <span v-if="answerLabel">答卷编号：{{ answerLabel }}</span>
        <span v-if="answerLabel && submitTime" class="meta-sep">|</span>
        <span v-if="submitTime">签署时间：{{ submitTime }}</span>
      </div>
    </header>

    <section class="doc-body">
      <div v-if="content" class="doc-content" v-html="content" />
      <div v-else class="doc-content">
        <p class="doc-empty">暂无协议正文</p>
      </div>
    </section>

    <section class="doc-agree">
      <div class="agree-line">
        <span class="agree-mark" :class="{ on: agreed }" aria-hidden="true">
          <i v-if="agreed" class="el-icon-check" />
        </span>
        <span class="agree-text">本人确认：{{ agreeLabel || '我已阅读并同意' }}</span>
        <span class="agree-status" :class="agreed ? 'ok' : 'no'">
          {{ agreed ? '已同意' : '未同意' }}
        </span>
      </div>
    </section>

    <section v-if="hasSignatures" class="doc-sign">
      <div class="sign-heading">签署栏</div>
      <div
        v-for="(sig, idx) in signatures"
        :key="sig.questionId || idx"
        class="sign-row"
      >
        <div class="sign-label">{{ sig.title || '签署人签名' }}</div>
        <div class="sign-pad">
          <img
            v-if="sig.url"
            :src="sig.url"
            class="sign-img"
            alt="signature"
            @click="$emit('preview', sig.url)"
          />
          <div v-else class="sign-empty">
            <span class="sign-line" />
            <span class="sign-hint">未签署</span>
          </div>
        </div>
        <div class="sign-date">
          <span class="date-label">日期</span>
          <span class="date-value">{{ submitTime || '　　年　　月　　日' }}</span>
        </div>
      </div>
    </section>

    <footer class="doc-footer">
      <p>本协议由通查云问卷系统生成存档</p>
    </footer>
  </article>
</template>

<script>
export default {
  name: 'AgreementDocument',
  props: {
    title: { type: String, default: '' },
    content: { type: String, default: '' },
    agreeLabel: { type: String, default: '我已阅读并同意' },
    agreed: { type: Boolean, default: false },
    answerLabel: { type: String, default: '' },
    submitTime: { type: String, default: '' },
    signatures: { type: Array, default: () => [] }
  },
  computed: {
    hasSignatures() {
      return Array.isArray(this.signatures) && this.signatures.length > 0
    }
  }
}
</script>

<style scoped>
.agreement-doc {
  background: #fff;
  border: 1px solid #d4d4d8;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
  padding: 36px 40px 28px;
  color: #1c1917;
  max-height: 72vh;
  overflow: auto;
}

.doc-header {
  text-align: center;
  margin-bottom: 28px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e7e5e4;
}

.doc-title {
  margin: 0 0 12px;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.12em;
  line-height: 1.4;
  font-family: "Songti SC", "Noto Serif SC", "Source Han Serif SC", "SimSun", serif;
}

.doc-meta {
  font-size: 12px;
  color: #78716c;
  line-height: 1.6;
}

.meta-sep {
  margin: 0 8px;
  color: #d6d3d1;
}

.doc-body {
  margin-bottom: 28px;
}

.doc-content {
  font-size: 14px;
  line-height: 1.9;
  color: #292524;
  text-align: justify;
}

.doc-content >>> p {
  margin: 0 0 0.9em;
  text-indent: 2em;
}

.doc-content >>> p:last-child {
  margin-bottom: 0;
}

.doc-content >>> h1,
.doc-content >>> h2,
.doc-content >>> h3 {
  text-indent: 0;
  margin: 1em 0 0.6em;
  font-weight: 650;
}

.doc-content >>> ul,
.doc-content >>> ol {
  margin: 0 0 0.9em;
  padding-left: 2em;
  text-indent: 0;
}

.doc-content >>> .doc-empty,
.doc-content .doc-empty {
  text-indent: 0;
  color: #a8a29e;
  text-align: center;
}

.doc-agree {
  margin: 8px 0 28px;
  padding: 14px 16px;
  border: 1px solid #e7e5e4;
  background: #fafaf9;
}

.agree-line {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 14px;
  line-height: 1.6;
}

.agree-mark {
  flex: none;
  width: 18px;
  height: 18px;
  margin-top: 2px;
  border: 1px solid #a8a29e;
  border-radius: 2px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: transparent;
  background: #fff;
}

.agree-mark.on {
  color: #166534;
  border-color: #166534;
  background: #f0fdf4;
}

.agree-text {
  flex: 1;
  color: #44403c;
}

.agree-status {
  flex: none;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 2px;
  border: 1px solid transparent;
}

.agree-status.ok {
  color: #166534;
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.agree-status.no {
  color: #78716c;
  background: #f5f5f4;
  border-color: #e7e5e4;
}

.doc-sign {
  margin-top: 8px;
  padding-top: 20px;
  border-top: 1px dashed #d6d3d1;
}

.sign-heading {
  font-size: 14px;
  font-weight: 650;
  letter-spacing: 0.08em;
  margin-bottom: 16px;
  color: #1c1917;
}

.sign-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px 24px;
  margin-bottom: 20px;
  align-items: end;
}

.sign-row:last-child {
  margin-bottom: 0;
}

.sign-label {
  grid-column: 1 / -1;
  font-size: 13px;
  color: #57534e;
}

.sign-pad {
  min-width: 0;
}

.sign-img {
  display: block;
  max-width: 280px;
  width: 100%;
  max-height: 120px;
  object-fit: contain;
  object-position: left bottom;
  background: #fff;
  border-bottom: 1px solid #a8a29e;
  padding: 4px 0 6px;
  cursor: zoom-in;
}

.sign-empty {
  position: relative;
  width: 240px;
  max-width: 100%;
  height: 72px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.sign-line {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 18px;
  border-bottom: 1px solid #a8a29e;
}

.sign-hint {
  position: relative;
  z-index: 1;
  font-size: 12px;
  color: #a8a29e;
  margin-bottom: 22px;
}

.sign-date {
  text-align: right;
  font-size: 13px;
  color: #57534e;
  white-space: nowrap;
  padding-bottom: 4px;
}

.date-label {
  margin-right: 8px;
}

.date-value {
  color: #292524;
}

.doc-footer {
  margin-top: 28px;
  padding-top: 14px;
  border-top: 1px solid #e7e5e4;
  text-align: center;
}

.doc-footer p {
  margin: 0;
  font-size: 11px;
  color: #a8a29e;
  letter-spacing: 0.04em;
}

@media (max-width: 640px) {
  .agreement-doc {
    padding: 24px 16px 20px;
  }

  .doc-title {
    font-size: 18px;
    letter-spacing: 0.06em;
  }

  .sign-row {
    grid-template-columns: 1fr;
  }

  .sign-date {
    text-align: left;
  }
}

@media print {
  .agreement-doc {
    max-height: none;
    overflow: visible;
    box-shadow: none;
    border: none;
    padding: 0;
  }
}
</style>
