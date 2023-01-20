<template>
  <lay-table :columns="columns2" :data-source="dataSource2" :size="md" skin='nob'></lay-table>
  <lay-progress percent="40" size="big"></lay-progress>
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
        key:"status"
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
        key:"memory"
      },{
        title:"操作系统CPU负载",
        width: "180px",
        key:"cpu"
      }
    ]

    const dataSource2 = [
      {node:"smart-mqtt@192.168.0.1", status:"运行中", runtime:"1 小时 4 分 14 秒",version:"v0.13",pid:1232,memory:'20%',cpu:'98%'},
      {node:"smart-mqtt@192.168.0.2", status:"已暂停", runtime:"-",version:"v0.13",pid:232,memory:'40%',cpu:'30%'},
    ]

    return {
      columns2,
      dataSource2
    }
  }
}
</script>