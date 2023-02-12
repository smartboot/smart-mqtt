<template>
  <lay-row space="10">
    <lay-col sm="6" md="6">
      <lay-input placeholder="节点"></lay-input>
    </lay-col>
    <lay-col sm="6" md="6">
      <lay-input placeholder="用户名"></lay-input>
    </lay-col>
    <lay-col sm="6" md="6">
      <lay-input placeholder="Topic"></lay-input>
    </lay-col>

    <lay-col sm="6" md="6">
      <lay-button native-type="submit">搜索</lay-button>
      <lay-button native-type="submit">刷新</lay-button>
      <lay-button type="primary" radius="true" size="xs">
        <lay-icon type="layui-icon-down"></lay-icon>
      </lay-button>
    </lay-col>
  </lay-row>
  <lay-row space="10">
    <lay-table :columns="columns" :data-source="dataSource" :page="page" @change="change" :size="md"
               skin='nob'></lay-table>
  </lay-row>

</template>

<script>

import {ref} from "vue";
import {subscriptions_subscription} from "../../api/module/api";

export default {
  setup() {

    const page = ref({
      total: 0,
      limit: 10,
      current: 1,
      showRefresh: true,
      showCount: true
    })

    const columns = [
      {
        title: "客户端ID",
        width: "120px",
        key: "clientId"
      }, {
        title: "主题",
        width: "120px",
        key: "topic"
      }, {
        title: "QoS",
        width: "80px",
        key: "qos"
      }, {
        title: "No Local",
        width: "180px",
        key: "ip"
      }, {
        title: "Retain",
        width: "80px",
        key: "heart"
      }
    ]

    const change = ({current, limit}) => {
      loadData(current, limit)
    }

    const dataSource = ref([])

    const loadData = async (pageNo, pageSize) => {
      const {data} = await subscriptions_subscription({pageSize: pageSize, pageNo: pageNo});
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