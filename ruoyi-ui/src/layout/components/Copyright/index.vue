<template>
  <footer v-if="visible" class="copyright">
    <span>{{ content }}</span>
    <span v-if="icp" class="icp">{{ icp }}</span>
  </footer>
</template>

<script>
export default {
  computed: {
    visible() {
      return this.$store.state.settings.footerVisible
    },
    content() {
      return this.$store.state.settings.footerContent || this.$store.state.settings.siteCopyright
    },
    icp() {
      return this.$store.state.settings.siteIcp
    }
  }
}
</script>

<style scoped>
.copyright {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 36px;
  padding: 10px 20px;
  text-align: right;
  background-color: #f8f8f8;
  color: #666;
  font-size: 13px;
  border-top: 1px solid #e7e7e7;
  z-index: 999;
}
.icp {
  margin-left: 12px;
  color: #94a3b8;
}
</style>
