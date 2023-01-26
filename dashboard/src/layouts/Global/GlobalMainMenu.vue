<template>
  <lay-menu
    :tree="true"
    :collapse="collapse"
    :level="appStore.level"
    :inverted="appStore.inverted"
    :theme="appStore.sideTheme"
    :selectedKey="selectedKey"
    @changeSelectedKey="changeSelectedKey"
  >
    <GlobalMainMenuItem :menus="menus"></GlobalMainMenuItem>
  </lay-menu>
</template>

<script lang="ts">
export default {
  name: "GlobalMenu",
};
</script>

<script lang="ts" setup>
import { useAppStore } from "../../store/app";
import GlobalMainMenuItem from "./GlobalMainMenuItem.vue";

const appStore = useAppStore();

interface MenuProps {
  collapse: boolean;
  menus: any[];
  selectedKey: string;
}

const props = withDefaults(defineProps<MenuProps>(), {
  collapse: false,
});

const emits = defineEmits(['changeSelectedKey'])

const changeSelectedKey = (key: string) => {
  emits("changeSelectedKey", key);
};

</script>

<style>
.layui-nav-tree * {
  font-size: 14px;
}

.layui-nav-tree .layui-nav-item > a,
.layui-nav-tree.inverted .layui-nav-item > a {
  padding: 3px 22px;
}

.layui-nav-tree.inverted .layui-this > a {
  padding: 3px 16px;
}

.layui-nav-tree .layui-nav-item > a > span {
  padding-left: 10px;
}

.layui-nav-tree .layui-nav-item > a .layui-nav-more {
  font-size: 12px!important;
  padding: 3px 0px;
}
</style>