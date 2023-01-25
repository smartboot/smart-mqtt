<template>
  <lay-row space="10">
    <lay-col sm="6" md="6">
      <lay-input placeholder="主题"></lay-input>
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

import {onMounted, ref} from "vue";
import {subscriptions_topics} from "../../api/module/api";

export default {
  setup() {

    const columns2 = [
      {
        title: "主题",
        width: "120px",
        key: "topic"
      }, {
        title: "节点",
        width: "80px",
        key: "brokerIpAddress"
      }, {
        title: "操作",
        width: "180px",
        key: "ip"
      }
    ]

    const dataSource2 = ref([])

    onMounted(() => {
      const loadData = async () => {
        const {data} = await subscriptions_topics();
        console.log(data)
        dataSource2.value = data
      };
      loadData()
    })

    return {
      columns2,
      dataSource2
    }
  }
}
</script>