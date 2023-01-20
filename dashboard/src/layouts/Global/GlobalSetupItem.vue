<template>
  <div class="global-setup-item">
    <div class="global-setup-item-label">
      {{ label }}
    </div>
    <div class="global-setup-item-extra">
      <slot></slot>
    </div>
  </div>
</template>

<script lang="ts">
export default {
  name: "GlobalSetupItem",
};
</script>

<script lang="ts" setup>
interface SetupItemProps {
  label: string;
}

const props = withDefaults(defineProps<SetupItemProps>(), {
  label: "标题",
});
</script>

<style scoped>
.global-setup-item {
    height: 50px;
    line-height: 50px;
    padding: 0px 15px;
}
.global-setup-item-label {
    float: left;
    font-size: 14px;
}
.global-setup-item-extra {
    float: right;
}
</style>
