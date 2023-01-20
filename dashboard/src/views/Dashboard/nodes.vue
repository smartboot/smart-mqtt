<template>
  <lay-table :columns="columns2" :data-source="dataSource2" :size="md" skin='nob'>
    <template #status="{ data }">
      <div v-if="data.status==1">
        <lay-badge type="dot" theme="blue" ripple></lay-badge>运行中
      </div>
      <div v-if="data.status==2">
        <lay-badge type="dot"></lay-badge>已离线
      </div>
    </template>

    <template v-slot:memory="{ data }">
      <lay-progress v-if="data.memory<30" :percent="data.memory" :show-text="true" style="width:100px"></lay-progress>
      <lay-progress v-if="data.memory>=30" theme="orange" :percent="data.memory" :show-text="true" style="width:100px"></lay-progress>
    </template>
    <template v-slot:cpu="{ data }">
      <lay-progress v-if="data.cpu<60" :percent="data.cpu" :show-text="true" style="width:100px"></lay-progress>
      <lay-progress v-if="data.cpu>=60" theme="orange" :percent="data.cpu" :show-text="true" style="width:100px"></lay-progress>
    </template>
  </lay-table>
</template>

<script>

export default {
  setup() {
    const columns2 = [
      {
        title:"名称",
        width:"200px",
        key:"node"
      },{
        title:"状态",
        width: "180px",
        key:"status",
        customSlot: "status"
      },{
        title:"运行时长",
        width: "180px",
        key:"runtime"
      },{
        title:"版本信息",
        width: "180px",
        key:"version"
      },{
        title:"进程",
        width: "180px",
        key:"pid"
      },{
        title:"操作系统内存",
        width: "180px",
        key:"memory",
        customSlot:"memory"
      },{
        title:"操作系统CPU负载",
        width: "180px",
        key:"cpu",
        customSlot: "cpu"
      }
    ]

    const dataSource2 = [
      {node:"smart-mqtt@192.168.0.1", status:"1", runtime:"1 小时 4 分 14 秒",version:"v0.13",pid:1232,memory:'20',cpu:'98'},
      {node:"smart-mqtt@192.168.0.2", status:"2", runtime:"-",version:"v0.13",pid:232,memory:'99',cpu:'30'},
    ]

    return {
      columns2,
      dataSource2
    }
  }
}
</script>