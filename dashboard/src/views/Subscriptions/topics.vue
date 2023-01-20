<template>
  <lay-row space="10">
    <lay-col sm="6" md="6">
      <lay-input placeholder="主题" ></lay-input>
    </lay-col>

    <lay-col sm="6" md="6">
      <lay-button native-type="submit">搜索</lay-button>
      <lay-button native-type="submit">刷新</lay-button>
    </lay-col>
  </lay-row>
  <lay-row space="10">
    <lay-table :columns="columns2" :data-source="dataSource2" :size="md" skin='nob'></lay-table>
  </lay-row>

</template>

<script>

export default {
  setup() {

    const columns2 = [
      {
        title:"主题",
        width: "120px",
        key:"topic"
      },{
        title:"节点",
        width: "80px",
        key:"qos"
      },{
        title:"操作",
        width: "180px",
        key:"ip"
      }
    ]

    const dataSource2 = [
    ]

    return {
      columns2,
      dataSource2
    }
  }
}
</script>