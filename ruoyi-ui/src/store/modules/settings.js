import defaultSettings from '@/settings'
import { useDynamicTitle } from '@/utils/dynamicTitle'
import { getSiteInfo } from '@/api/system/basic'
import { resolveSiteAsset } from '@/utils/siteAsset'

const { sideTheme, showSettings, navType, tagsView, tagsViewPersist, tagsIcon, tagsViewStyle, fixedHeader, sidebarLogo, dynamicTitle, footerVisible, footerContent } = defaultSettings

const storageSetting = JSON.parse(localStorage.getItem('layout-setting')) || ''
const state = {
  title: '',
  siteTitle: defaultSettings.title || '通查云',
  siteLogo: (process.env.BASE_URL || '/') + 'logo.svg?v=20260807',
  siteCopyright: footerContent,
  siteIcp: '',
  mailVerifyEnabled: false,
  mailResetEnabled: false,
  theme: storageSetting.theme || '#1d4ed8',
  sideTheme: storageSetting.sideTheme || sideTheme,
  showSettings: showSettings,
  navType: storageSetting.navType === undefined ? navType : storageSetting.navType,
  tagsView: storageSetting.tagsView === undefined ? tagsView : storageSetting.tagsView,
  tagsViewPersist: storageSetting.tagsViewPersist === undefined ? tagsViewPersist : storageSetting.tagsViewPersist,
  tagsIcon: storageSetting.tagsIcon === undefined ? tagsIcon : storageSetting.tagsIcon,
  tagsViewStyle: storageSetting.tagsViewStyle === undefined ? tagsViewStyle : storageSetting.tagsViewStyle,
  fixedHeader: storageSetting.fixedHeader === undefined ? fixedHeader : storageSetting.fixedHeader,
  sidebarLogo: storageSetting.sidebarLogo === undefined ? sidebarLogo : storageSetting.sidebarLogo,
  dynamicTitle: storageSetting.dynamicTitle === undefined ? dynamicTitle : storageSetting.dynamicTitle,
  footerVisible: storageSetting.footerVisible === undefined ? footerVisible : storageSetting.footerVisible,
  footerContent: footerContent
}

const mutations = {
  CHANGE_SETTING: (state, { key, value }) => {
    if (Object.prototype.hasOwnProperty.call(state, key)) {
      state[key] = value
    }
  },
  SET_TITLE: (state, title) => {
    state.title = title
  },
  SET_SITE: (state, payload) => {
    if (payload.siteTitle != null) state.siteTitle = payload.siteTitle
    if (payload.siteLogo != null) state.siteLogo = payload.siteLogo
    if (payload.siteCopyright != null) state.siteCopyright = payload.siteCopyright
    if (payload.siteIcp != null) state.siteIcp = payload.siteIcp
    if (payload.mailVerifyEnabled != null) state.mailVerifyEnabled = payload.mailVerifyEnabled
    if (payload.mailResetEnabled != null) state.mailResetEnabled = payload.mailResetEnabled
    if (payload.footerVisible != null) state.footerVisible = payload.footerVisible
    if (payload.footerContent != null) state.footerContent = payload.footerContent
  }
}

const actions = {
  changeSetting({ commit }, data) {
    commit('CHANGE_SETTING', data)
  },
  setTitle({ commit }, title) {
    commit('SET_TITLE', title)
    useDynamicTitle()
  },
  loadSiteInfo({ commit }, force) {
    return getSiteInfo().then(res => {
      const d = res.data || {}
      const title = d.title || defaultSettings.title || '通查云'
      const copyright = d.copyright || defaultSettings.footerContent
      const footerOn = d.footerVisible !== 'false' && d.footerVisible !== false
      commit('SET_SITE', {
        siteTitle: title,
        siteLogo: resolveSiteAsset(d.logo),
        siteCopyright: copyright,
        siteIcp: d.icp || '',
        mailVerifyEnabled: d.mailVerifyEnabled === 'true' || d.mailVerifyEnabled === true,
        mailResetEnabled: d.mailResetEnabled === 'true' || d.mailResetEnabled === true,
        footerVisible: footerOn,
        footerContent: copyright
      })
      // sync document title base
      defaultSettings.title = title
      useDynamicTitle()
      return d
    }).catch(() => null)
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
