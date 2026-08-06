import store from '@/store'
import defaultSettings from '@/settings'

/**
 * 动态修改标题
 */
export function useDynamicTitle() {
  const site = store.state.settings.siteTitle || defaultSettings.title
  if (store.state.settings.dynamicTitle && store.state.settings.title) {
    document.title = store.state.settings.title + ' - ' + site
  } else {
    document.title = site
  }
}
