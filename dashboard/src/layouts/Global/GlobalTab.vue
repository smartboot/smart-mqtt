<template>
  <div class="global-tab" v-if="appStore.tab">
    <lay-tab
      :modelValue="currentPath"
      :allowClose="true"
      @change="to"
      @close="close"
    >
      <template :key="tab" v-for="tab in tabs">
        <lay-tab-item :id="tab.id" :title="tab.title" :closable="tab.closable">
          <template #title>
            <span class="dot"></span>
            {{ tab.title }}
          </template>
        </lay-tab-item>
      </template>
    </lay-tab>
    <lay-dropdown>
      <lay-icon type="layui-icon-down"></lay-icon>
      <template #content>
        <lay-dropdown-menu>
          <lay-dropdown-menu-item @click="closeAll"
            >关闭全部</lay-dropdown-menu-item
          >
        </lay-dropdown-menu>
        <lay-dropdown-menu>
          <lay-dropdown-menu-item @click="closeOther"
            >关闭其他</lay-dropdown-menu-item
          >
        </lay-dropdown-menu>
        <lay-dropdown-menu>
          <lay-dropdown-menu-item @click="closeCurrent"
            >关闭当前</lay-dropdown-menu-item
          >
        </lay-dropdown-menu>
      </template>
    </lay-dropdown>
  </div>
</template>

<script lang="ts">
export default {
  name: "GlobalTab",
};
</script>

<script lang="ts" setup>
import { ref } from "vue";
import { useAppStore } from "../../store/app";
import { useTab } from "../composable/useTab";

const appStore = useAppStore();

const { tabs, to, close, closeAll, closeOther, closeCurrent, currentPath } =
  useTab();
</script>

<style>
.global-tab {
  display: flex;
  position: relative;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  border-top: 1px solid whitesmoke;
  z-index: 999;
}

.global-tab .layui-tab {
  flex-grow: 1;
  width: calc(100% - 40px);
}

.global-tab .layui-tab .layui-tab-bar {
  border: none;
  border-left: 1px solid whitesmoke;
}

.global-tab .layui-tab .layui-tab-bar.prev {
  border-left: none;
}

.global-tab > i {
  width: 40px;
  background: white;
  height: 100%;
  line-height: 40px;
  text-align: center;
  border-left: 1px solid whitesmoke;
}

.global-tab .layui-tab .dot {
  display: inline-block;
  background-color: whitesmoke;
  margin-right: 8px;
  border-radius: 50px;
  height: 8px;
  width: 8px;
}

.global-tab .layui-tab .layui-this .dot {
  background-color: var(--global-primary-color);
}

.global-tab .layui-tab .layui-tab-close:hover {
  background: transparent !important;
  color: #e2e2e2 !important;
}
</style>
