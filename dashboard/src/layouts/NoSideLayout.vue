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
      <lay-layout style="width: 0px">
        <!-- header -->
        <lay-header>
          <lay-logo><lay-icon type="layui-icon-vercode" color="#FF5722" size="36px"></lay-icon>smart-license</lay-logo>
        </lay-header>
        <lay-body>
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
import {layer} from "@layui/layer-vue";

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

      layer.notifiy({
        icon: 1,
        title:"欢迎访问",
        content:"已升级到 layui-vue 1.7.3 版本。"
      })
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
  background: whitesmoke;
}

.layui-header .layui-nav-item * {
  color: #666 !important;
}

.grey-mode {
  filter: grayscale(1);
}
</style>
