<template>
    <ul class="color-list">
      <li :style="{'background':option}" v-for="(option, index) in options" :key="index" @click="handlerChange(option)"><lay-icon v-if="option == modelValue" type="layui-icon-ok"></lay-icon></li>
    </ul>
</template>

<script lang="ts">
export default {
  name: "GlobalColor",
};
</script>

<script lang="ts" setup>
  
interface ColorProps {
  modelValue: string;
  options?: string[];
}

const props = withDefaults(defineProps<ColorProps>(), {
  modelValue: "#009688",
  options: () => ['#009688','#36b368','#2d8cf0','#f6ad55','#f56c6c','#3963bc']
});

const emits = defineEmits(['update:modelValue'])

const handlerChange = function(color: string) {
    emits('update:modelValue', color);
}
</script>

<style>
.color-list {
    display: inline-block;
    margin: 20px 0px 15px 0px;
}
.color-list li {
    float: left;
    text-align: center;
    width: 24px;
    height: 24px;
    line-height: 24px;
    margin-left: 14px;
    border-radius: 2px;
    box-shadow: 0 1px 2px 0 rgb(0 0 0 / 15%);
    color: white;
}
</style>