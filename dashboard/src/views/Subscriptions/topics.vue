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
    <lay-table :columns="columns" :data-source="dataSource" :page="page" @change="change" :size="md"
               skin='nob'></lay-table>
  </lay-row>

</template>

<script>

import {ref} from "vue";
import {subscriptions_topics} from "../../api/module/api";

export default {
  setup() {

    const columns = [
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

    const dataSource = ref([])

    const page = ref({
      total: 0,
      limit: 10,
      current: 1,
      showRefresh: true,
      showCount: true
    })
    const change = ({current, limit}) => {
      loadData(current, limit)
    }

    const loadData = async (pageNo, pageSize) => {
      const {data} = await subscriptions_topics({pageSize: pageSize, pageNo: pageNo});
      console.log(data)
      dataSource.value = data.list
      page.value.total = data.total;
      page.value.limit = data.pageSize;
    };
    loadData(page.value.current, page.value.limit)

    return {
      page,
      change,
      columns,
      dataSource
    }
  }
}
</script>