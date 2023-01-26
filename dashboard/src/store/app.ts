import {defineStore} from 'pinia'

export const useAppStore = defineStore({
  id: 'app',
  state: () => {
    return {
      tab: true,
      logo: true,
      level: true,
      inverted: false,
      routerAlive: true,
      collapse: false, 
      subfield: false,
      subfieldPosition: "side",
      theme: 'light',
      breadcrumb: true,
      sideWidth: "220px",
      sideTheme: 'dark',
      greyMode: false,
      accordion: true,
      keepAliveList: [],
      themeVariable: {
        "--global-checked-color": "#5fb878",
        "--global-primary-color": "#009688",
        "--global-normal-color": "#1e9fff",
        "--global-danger-color": "#ff5722",
        "--global-warm-color": "#ffb800",
      },
    }
  },
  persist: {
    storage: localStorage,
    paths: ['tab', 'theme', 'logo', 'level', 'inverted', 'breadcrumb', 'sideTheme', 'greyMode', 'accordion' ,'keepAliveList', 'themeVariable','subfield'],
  }
})