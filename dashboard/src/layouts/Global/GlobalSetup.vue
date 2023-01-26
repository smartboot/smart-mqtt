<template>
  <lay-layer :title="false" :closeBtn="false" type="drawer" area="300px" v-model="visible">
    <div class="global-setup">    
      <div class="global-setup-title">Overall style</div>
      <global-setup-theme v-model="appStore.sideTheme"></global-setup-theme>
      <global-setup-theme v-model="appStore.subfieldPosition" :options="groupOptions" :disabled="!appStore.subfield"></global-setup-theme>
      <global-color v-model="appStore.themeVariable['--global-primary-color']"></global-color>
      <lay-line></lay-line>
      <global-setup-item label="多选项卡">
        <lay-switch v-model="appStore.tab" size="xs"></lay-switch>
      </global-setup-item>
      <global-setup-item label="菜单层级">
        <lay-switch v-model="appStore.level" size="xs"></lay-switch>
      </global-setup-item>
      <global-setup-item label="菜单反选">
        <lay-switch v-model="appStore.inverted" size="xs"></lay-switch>
      </global-setup-item>
      <global-setup-item label="菜单折叠">
        <lay-switch v-model="appStore.collapse" size="xs"></lay-switch>
      </global-setup-item>
      <global-setup-item label="手风琴">
        <lay-switch v-model="appStore.accordion" size="xs"></lay-switch>
      </global-setup-item>
      <global-setup-item label="夜间模式">
        <lay-switch v-model="appStore.theme" onswitch-value="dark" unswitch-value="light" size="xs"></lay-switch>
      </global-setup-item>
      <global-setup-item label="侧边标题">
        <lay-switch v-model="appStore.logo" size="xs"></lay-switch>
      </global-setup-item>
      <global-setup-item label="灰色模式">
        <lay-switch v-model="appStore.greyMode" size="xs"></lay-switch>
      </global-setup-item>
      <global-setup-item label="面包屑">
        <lay-switch v-model="appStore.breadcrumb" size="xs"></lay-switch>
      </global-setup-item>
      <global-setup-item label="菜单分栏">
        <lay-switch v-model="appStore.subfield" size="xs"></lay-switch>
      </global-setup-item>
      <div style="padding:15px">
        <lay-button border="green" border-style="dashed" :fluid="true">重置配置</lay-button>
      </div>
    </div>
  </lay-layer>
</template>

<script lang="ts">
export default {
  name: "GlobalSetup",
};
</script>

<script lang="ts" setup>
import globalSetupItem from "./GlobalSetupItem.vue";
import globalSetupTheme from "./GlobalSetupTheme.vue";
import globalColor from "./GlobalColor.vue";
import { useAppStore } from "../../store/app";
import { ref, watch } from "vue";

const appStore = useAppStore();
const emits = defineEmits(["update:modelValue"]);

interface SetupProps {
  modelValue: boolean;
}

const props = withDefaults(defineProps<SetupProps>(), {
  modelValue: false,
});

const groupOptions = ref([
    {logo:'#28333e',head:'white',side:'#28333e',body:'#f4f5f7', value: 'side'},
    {logo:'#28333e',head:'#28333e',side:'white',body:'#f4f5f7', value: 'head'},
])

const visible = ref(props.modelValue);

watch(visible, (val) => {
  emits("update:modelValue", val);
});

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val;
  }
);
</script>

<style>
.global-setup {
  padding: 10px;
}

.global-setup-title {
  font-size: 13px;
  margin-bottom: 10px;
  padding: 10px 10px 0px 10px;
}
.global-setup .layui-colorpicker {
  margin-right: 10px;
}
</style>
