<template>
  <div
    class="sidebar-logo-container"
    :class="{ collapse: collapse }"
    role="link"
    tabindex="0"
    :title="displayTitle"
    @click="goHome"
    @keyup.enter="goHome"
  >
    <span class="sidebar-logo-mark">
      <img v-if="logo" :src="logo" class="sidebar-logo" :alt="displayTitle" />
      <span v-else class="sidebar-logo-fallback">{{ titleInitial }}</span>
    </span>
    <span v-show="!collapse" class="sidebar-title">{{ displayTitle }}</span>
  </div>
</template>

<script>
import { mapState } from 'vuex'

export default {
  name: 'SidebarLogo',
  props: {
    collapse: {
      type: Boolean,
      required: true
    }
  },
  computed: {
    ...mapState('settings', {
      siteTitle: state => state.siteTitle,
      logo: state => state.siteLogo
    }),
    displayTitle() {
      const t = (this.siteTitle || '').trim()
      return t || process.env.VUE_APP_TITLE || '通查云'
    },
    titleInitial() {
      return (this.displayTitle || '明').charAt(0)
    }
  },
  methods: {
    goHome() {
      this.$router.push('/')
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebar-logo-container {
  position: relative;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 0 16px;
  box-sizing: border-box;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border-bottom: 1px solid #e8ecf2;
  flex-shrink: 0;
  font-size: 16px;
  cursor: pointer;
  overflow: hidden;
  user-select: none;
  gap: 11px;
  transition: background 0.18s ease;

  &:hover {
    background: linear-gradient(180deg, #ffffff 0%, #eff6ff 100%);

    .sidebar-logo-mark {
      transform: translateY(-1px);
      box-shadow:
        0 1px 2px rgba(15, 23, 42, 0.06),
        0 8px 18px rgba(29, 78, 216, 0.22);
    }

    .sidebar-title {
      color: #1e3a8a;
    }
  }

  &:focus-visible {
    outline: none;
    box-shadow: inset 0 0 0 2px rgba(29, 78, 216, 0.35);
  }

  .sidebar-logo-mark {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    flex: 0 0 34px;
    border-radius: 10px;
    background: #fff;
    box-shadow:
      0 1px 2px rgba(15, 23, 42, 0.05),
      0 4px 12px rgba(29, 78, 216, 0.16);
    overflow: hidden;
    transition: transform 0.18s ease, box-shadow 0.18s ease;
  }

  .sidebar-logo {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: contain;
    border-radius: 10px;
  }

  .sidebar-logo-fallback {
    width: 100%;
    height: 100%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(145deg, #3b82f6, #1d4ed8);
    color: #fff;
    font-size: 15px;
    font-weight: 700;
    letter-spacing: 0;
  }

  .sidebar-title {
    flex: 1 1 auto;
    min-width: 0;
    margin: 0;
    padding: 0;
    color: #0f172a;
    font-size: 18px;
    font-weight: 700;
    line-height: 1.2;
    height: auto;
    font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
    letter-spacing: 0.06em;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    transition: color 0.18s ease;
  }

  &.collapse {
    padding: 0;
    justify-content: center;
    gap: 0;

    .sidebar-logo-mark {
      width: 36px;
      height: 36px;
      flex-basis: 36px;
      border-radius: 11px;
    }

    .sidebar-logo,
    .sidebar-logo-fallback {
      border-radius: 11px;
    }
  }
}
</style>
