<template>
  <div class="navbar" :class="'nav' + navType">
    <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

    <breadcrumb v-if="navType == 1" id="breadcrumb-container" class="breadcrumb-container" />
    <top-nav v-if="navType == 2" id="topmenu-container" class="topmenu-container" />
    <template v-if="navType == 3">
      <logo v-show="showLogo" :collapse="false"></logo>
      <top-bar id="topbar-container" class="topbar-container" />
    </template>
    <div class="right-menu">
      <template v-if="device!=='mobile'">
        <div class="right-tools">
          <search id="header-search" class="right-menu-item hover-effect" />

          <screenfull id="screenfull" class="right-menu-item hover-effect" />

          <el-tooltip content="布局大小" effect="dark" placement="bottom">
            <size-select id="size-select" class="right-menu-item hover-effect" />
          </el-tooltip>

          <el-tooltip content="消息通知" effect="dark" placement="bottom">
            <header-notice id="header-notice" class="notice-entry" />
          </el-tooltip>
        </div>
        <span class="right-divider" aria-hidden="true" />
      </template>

      <el-dropdown class="avatar-container right-menu-item hover-effect" trigger="hover">
        <div class="avatar-wrapper">
          <img :src="avatar" class="user-avatar" alt="">
          <span class="user-nickname">{{ nickName }}</span>
          <i class="el-icon-arrow-down user-caret" />
        </div>
        <el-dropdown-menu slot="dropdown">
          <router-link to="/user/profile">
            <el-dropdown-item>个人中心</el-dropdown-item>
          </router-link>
          <el-dropdown-item @click.native="setLayout" v-if="setting">
            <span>布局设置</span>
          </el-dropdown-item>
          <el-dropdown-item @click.native="lockScreen">
            <span>锁定屏幕</span>
          </el-dropdown-item>
          <el-dropdown-item divided @click.native="logout">
            <span>退出登录</span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import TopNav from './TopNav'
import TopBar from './TopBar'
import Logo from './Sidebar/Logo'
import Hamburger from '@/components/Hamburger'
import Screenfull from '@/components/Screenfull'
import SizeSelect from '@/components/SizeSelect'
import Search from '@/components/HeaderSearch'
import HeaderNotice from './HeaderNotice'

export default {
  components: {
    Breadcrumb,
    Logo,
    TopNav,
    TopBar,
    Hamburger,
    Screenfull,
    SizeSelect,
    Search,
    HeaderNotice
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'avatar',
      'device',
      'nickName'
    ]),
    setting: {
      get() {
        return this.$store.state.settings.showSettings
      }
    },
    navType: {
      get() {
        return this.$store.state.settings.navType
      }
    },
    showLogo: {
      get() {
        return this.$store.state.settings.sidebarLogo
      }
    }
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    setLayout(event) {
      this.$emit('setLayout')
    },
    lockScreen() {
      const currentPath = this.$route.fullPath
      this.$store.dispatch('lock/lockScreen', currentPath).then(() => {
        this.$router.push('/lock')
      })
    },
    logout() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('LogOut').then(() => {
          location.href = '/index'
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar.nav3 {
  .hamburger-container {
    display: none !important;
  }
}

.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,21,41,.06);
  display: flex;
  align-items: center;
  padding: 0 8px 0 0;
  box-sizing: border-box;

  .hamburger-container {
    line-height: 50px;
    height: 100%;
    cursor: pointer;
    transition: background .3s;
    -webkit-tap-highlight-color:transparent;
    display: flex;
    align-items: center;
    flex-shrink: 0;
    margin-right: 4px;
    padding: 0 12px;

    &:hover {
      background: rgba(0, 0, 0, .025)
    }
  }

  .breadcrumb-container {
    flex-shrink: 0;
    min-width: 0;
  }

  .topmenu-container {
    position: absolute;
    left: 50px;
  }

  .topbar-container {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    overflow: hidden;
    margin-left: 8px;
  }

  .right-menu {
    height: 100%;
    display: flex;
    align-items: center;
    margin-left: auto;
    padding-right: 12px;
    gap: 4px;

    &:focus {
      outline: none;
    }

    .right-tools {
      display: flex;
      align-items: center;
      gap: 4px;
      height: 100%;
    }

    .right-divider {
      width: 1px;
      height: 20px;
      margin: 0 10px 0 8px;
      background: #e5e7eb;
      flex-shrink: 0;
    }

    .notice-entry {
      display: inline-flex;
      align-items: center;
      margin: 0 2px;
    }

    .right-menu-item {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 36px;
      height: 36px;
      padding: 0 8px;
      margin: 0 2px;
      border-radius: 8px;
      font-size: 18px;
      color: #5a5e66;
      line-height: 1;
      box-sizing: border-box;

      &.hover-effect {
        cursor: pointer;
        transition: background .2s, color .2s;

        &:hover {
          background: #f3f4f6;
          color: #303133;
        }
      }
    }

    .avatar-container {
      margin: 0 0 0 2px;
      padding: 0 6px 0 8px;
      min-width: auto;
      height: 40px;
      border-radius: 20px;

      &.hover-effect:hover {
        background: #f3f4f6;
      }

      .avatar-wrapper {
        display: flex;
        align-items: center;
        gap: 8px;
        height: 100%;
        position: relative;

        .user-avatar {
          cursor: pointer;
          width: 28px;
          height: 28px;
          border-radius: 50%;
          flex-shrink: 0;
          object-fit: cover;
        }

        .user-nickname {
          max-width: 88px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          font-size: 13px;
          font-weight: 600;
          color: #303133;
          line-height: 1;
        }

        .user-caret {
          font-size: 12px;
          color: #909399;
          margin-left: -2px;
        }
      }
    }
  }
}

.biz-notify {
  display: inline-flex !important;
  align-items: center;
  ::v-deep .el-badge__content {
    top: 10px;
  }
}
</style>
