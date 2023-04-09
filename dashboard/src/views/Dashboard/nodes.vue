<template>
    <lay-card>
  <lay-table :columns="columns2" :data-source="dataSource2" size="md" skin='nob'>
    <template #status="{ data }">
      <div v-if="data.status=='running'">
        <lay-badge type="dot" theme="blue" ripple></lay-badge>
        运行中
      </div>
      <div v-if="data.status=='stopped'">
        <lay-badge type="dot"></lay-badge>
        已离线
      </div>
      <div v-if="data.status=='unknown'">
          <lay-badge type="dot"></lay-badge>
          未知
      </div>
    </template>

    <template v-slot:memory="{ data }">
      <lay-progress v-if="data.memory<30" :percent="data.memory" :show-text="true" style="width:100px"></lay-progress>
      <lay-progress v-if="data.memory>=30" theme="orange" :percent="data.memory" :show-text="true"
                    style="width:100px"></lay-progress>
    </template>
    <template v-slot:cpu="{ data }">
      <lay-progress v-if="data.cpu<60" :percent="data.cpu" :show-text="true" style="width:100px"></lay-progress>
      <lay-progress v-if="data.cpu>=60" theme="orange" :percent="data.cpu" :show-text="true"
                    style="width:100px"></lay-progress>
    </template>
  </lay-table>
    </lay-card>
</template>

<script lang="ts">

import {onMounted, ref} from "vue";
import {dashboard_nodes} from "../../api/module/api";
import {onUnmounted} from "@vue/runtime-core";

export default {
  setup() {
    const columns2 = [
      {
        title: "名称",
        width: "200px",
        key: "name"
      }, {
        title: "状态",
        width: "180px",
        key: "status",
        customSlot: "status"
      }, {
        title: "运行时长",
        width: "180px",
        key: "runtime"
      }, {
        title: "版本信息",
        width: "180px",
        key: "version"
      }, {
        title: "进程",
        width: "180px",
        key: "pid"
      }, {
        title: "操作系统内存",
        width: "180px",
        key: "memory",
        customSlot: "memory"
      }, {
        title: "操作系统CPU负载",
        width: "180px",
        key: "cpu",
        customSlot: "cpu"
      }
    ]
    const dataSource2 = ref([])

    let timer;

    onMounted(() => {
      const loadData = async () => {
        const {data} = await dashboard_nodes();
        console.log(data)
        dataSource2.value=data
      };
      loadData()
      timer = setInterval(() => {
        loadData()
      }, 2000)
    })

    onUnmounted(() => {
      console.log("clear timer")
      clearInterval(timer)
    })

    return {
      columns2,
      dataSource2
    }
  }
}
</script>