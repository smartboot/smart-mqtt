<template>
    <div class="global-content" :class="{'has-tab':appStore.tab}">
			<router-view  v-slot="{ Component }" v-if="appStore.routerAlive">
      	<lay-transition type="fade">
					<keep-alive :include="appStore.keepAliveList">
						<component :is="Component" />
					</keep-alive>
      	</lay-transition>
			</router-view>
    </div>
</template>

<script lang="ts">
export default {
    name: "GlobalContent"
}
</script>

<script lang="ts" setup>
import { useAppStore } from "../../store/app";

const appStore = useAppStore();
</script>

<style scoped>
.global-content.has-tab{
    height: calc(100% - 46px);
    overflow: auto;
}
.global-content {
    height: 100%;
    overflow: auto;
}
</style>