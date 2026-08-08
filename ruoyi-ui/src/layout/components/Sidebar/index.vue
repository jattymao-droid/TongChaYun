<template>
  <div :class="['sidebar-theme-wrapper', 'sidebar-ss', {'has-logo':showLogo}, settings.sideTheme]" :style="{ backgroundColor: sidebarBg }">
    <logo v-if="showLogo" :collapse="isCollapse" />
    <biz-sidebar-actions :collapse="isCollapse" />
    <el-scrollbar :class="settings.sideTheme" wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :background-color="sidebarBg"
        :text-color="menuText"
        :unique-opened="true"
        :active-text-color="activeColor"
        :collapse-transition="false"
        mode="vertical"
      >
        <sidebar-item
          v-for="(route, index) in sidebarRouters"
          :key="route.path + index"
          :item="route"
          :base-path="route.path"
        />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex'
import Logo from './Logo'
import SidebarItem from './SidebarItem'
import BizSidebarActions from './BizSidebarActions'
import variables from '@/assets/styles/variables.scss'

export default {
  components: { SidebarItem, Logo, BizSidebarActions },
  computed: {
    ...mapState(['settings']),
    ...mapGetters(['sidebarRouters', 'sidebar']),
    activeMenu() {
      const route = this.$route
      const { meta, path } = route
      if (meta.activeMenu) return meta.activeMenu
      return path
    },
    showLogo() {
      return this.$store.state.settings.sidebarLogo
    },
    variables() {
      return variables
    },
    isCollapse() {
      return !this.sidebar.opened
    },
    isLight() {
      return this.settings.sideTheme === 'theme-light'
    },
    sidebarBg() {
      return this.isLight ? variables.menuLightBackground : variables.menuBackground
    },
    menuText() {
      return this.isLight ? '#4e5969' : variables.menuColor
    },
    activeColor() {
      return this.isLight ? '#1d4ed8' : this.settings.theme
    }
  }
}
</script>
