<template>
  <lay-config-provider
    :themeVariable="appStore.themeVariable"
    :theme="appStore.theme"
  >
    <lay-layout
      :class="[
        appStore.tab ? 'has-tab' : '',
        appStore.collapse ? 'collapse' : '',
        appStore.greyMode ? 'grey-mode' : '',
      ]"
    >
      <!-- side -->
      <lay-side :width="sideWidth" :class="appStore.sideTheme == 'dark' ? 'dark':'light'">
        <lay-logo v-if="appStore.logo"><lay-icon type="layui-icon-vercode" color="#FF5722" size="24px"></lay-icon> smart-mqtt</lay-logo>
        <lay-scroll style="height: calc(100% - 62px)">
          <global-menu :collapse="appStore.collapse"></global-menu>
        </lay-scroll>
      </lay-side>
      <lay-layout style="width: 0px">
        <!-- header -->
        <lay-header>
          <lay-menu class="layui-layout-left">
            <lay-menu-item @click="collapse">
              <lay-icon
                v-if="appStore.collapse"
                type="layui-icon-spread-left"
              ></lay-icon>
              <lay-icon v-else type="layui-icon-shrink-right"></lay-icon>
            </lay-menu-item>
            <lay-menu-item @click="refresh">
              <lay-icon type="layui-icon-refresh-one"></lay-icon>
            </lay-menu-item>
            <lay-menu-item v-if="appStore.breadcrumb">
              <GlobalBreadcrumb></GlobalBreadcrumb>
            </lay-menu-item>
          </lay-menu>
          <lay-menu class="layui-layout-right">
            <lay-menu-item>
              <lay-fullscreen v-slot="{ toggle, isFullscreen }">
                <lay-icon
                  @click="toggle()"
                  :type="
                    isFullscreen
                      ? 'layui-icon-screen-restore'
                      : 'layui-icon-screen-full'
                  "
                ></lay-icon>
              </lay-fullscreen>
            </lay-menu-item>
            <lay-menu-item>
              <lay-dropdown updateAtScroll placement="bottom">
                <lay-icon type="layui-icon-notice"></lay-icon>
                <template #content>
                  <lay-tab
                    type="brief"
                    style="margin: 5px"
                    v-model="currentIndex"
                  >
                    <lay-tab-item title="选项一" id="1"
                      ><div style="padding: 20px">选项一</div></lay-tab-item
                    >
                    <lay-tab-item title="选项二" id="2"
                      ><div style="padding: 20px">选项二</div></lay-tab-item
                    >
                    <lay-tab-item title="选项三" id="3"
                      ><div style="padding: 20px">选项三</div></lay-tab-item
                    >
                  </lay-tab>
                </template>
              </lay-dropdown>
            </lay-menu-item>
            <lay-menu-item>
              <lay-icon type="layui-icon-website"></lay-icon>
            </lay-menu-item>
            <lay-menu-item>
              <lay-dropdown updateAtScroll placement="bottom">
                <lay-icon type="layui-icon-username"></lay-icon>
                <template #content>
                  <lay-dropdown-menu>
                    <lay-dropdown-menu-item>
                      <template #default>用户信息</template>
                    </lay-dropdown-menu-item>
                    <lay-dropdown-menu-item>
                      <template #default>系统设置</template>
                    </lay-dropdown-menu-item>
                    <lay-line></lay-line>
                    <lay-dropdown-menu-item @click="logOut">
                      <template #default>注销登录</template>
                    </lay-dropdown-menu-item>
                  </lay-dropdown-menu>
                </template>
              </lay-dropdown>
            </lay-menu-item>
            <lay-menu-item @click="changeVisible">
              <lay-icon type="layui-icon-more-vertical"></lay-icon>
            </lay-menu-item>
          </lay-menu>
        </lay-header>
        <lay-body>
          <global-tab></global-tab>
          <global-content></global-content>
        </lay-body>
        <lay-footer></lay-footer>
      </lay-layout>
    </lay-layout>
    <global-setup v-model="visible"></global-setup>
  </lay-config-provider>
</template>

<script lang="ts">
import {computed, onMounted, ref} from "vue";
import {useAppStore} from "../store/app";
import {useUserStore} from "../store/user";
import GlobalSetup from "./global/GlobalSetup.vue";
import GlobalContent from "./global/GlobalContent.vue";
import GlobalBreadcrumb from "./global/GlobalBreadcrumb.vue";
import GlobalTab from "./global/GlobalTab.vue";
import GlobalMenu from "./global/GlobalMenu.vue";
import {useRouter} from "vue-router";

export default {
  components: {
    GlobalSetup,
    GlobalContent,
    GlobalTab,
    GlobalMenu,
    GlobalBreadcrumb
  },
  setup() {

    const fullscreenRef = ref(null);
    const appStore = useAppStore();
    const userInfoStore = useUserStore();
    const visible = ref(false);
    const router = useRouter();
    const sideWidth = computed(() => appStore.collapse ? "60px" : "220px")

    onMounted(() => {
      // mobile
      if(document.body.clientWidth < 768) {
        appStore.collapse = true;
      }
      userInfoStore.loadMenus();
      userInfoStore.loadPermissions();

    })

    const changeVisible = function () {
      visible.value = !visible.value;
    };

    const currentIndex = ref("1");

    const collapse = function () {
      appStore.collapse = !appStore.collapse;
    };

    const refresh = function () {
      appStore.routerAlive = false;
      setTimeout(function () {
        appStore.routerAlive = true;
      }, 500);
    };

    const logOut = () => {
      const userInfoStore = useUserStore();
      userInfoStore.token = "";
      userInfoStore.userInfo = {};
      router.push("/login");
    };
    
    return {
      sideWidth,
      changeVisible,
      fullscreenRef,
      collapse,
      appStore,
      refresh,
      visible,
      logOut,
      userInfoStore,
      currentIndex,
    };
  },
};
</script>

<style>
.layui-header .layui-nav-item:hover {
  /*background: whitesmoke;*/
}

.layui-header .layui-nav-item * {
  color: #666 !important;
}

.grey-mode {
  filter: grayscale(1);
}
</style>
