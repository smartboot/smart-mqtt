<template>
  <lay-row space="10">
    <lay-col sm="6" md="6">
      <lay-input placeholder="节点" ></lay-input>
    </lay-col>
    <lay-col sm="6" md="6">
      <lay-input placeholder="用户名"></lay-input>
    </lay-col>
    <lay-col sm="6" md="6">
      <lay-input placeholder="Topic" ></lay-input>
    </lay-col>

    <lay-col sm="6" md="6">
      <lay-button native-type="submit">搜索</lay-button>
      <lay-button native-type="submit">刷新</lay-button>
      <lay-button type="primary" radius="true" size="xs"><lay-icon type="layui-icon-down"></lay-icon></lay-button>
    </lay-col>
  </lay-row>
  <lay-row space="10">
    <lay-table :columns="columns2" :data-source="dataSource2" :size="md" skin='nob'></lay-table>
  </lay-row>

</template>

<script>

import {onMounted, ref} from "vue";
import {subscriptions_subscription} from "../../api/module/api";

export default {
  setup() {

    const columns2 = [
      {
        title:"客户端ID",
        width:"120px",
        key:"clientId"
      },{
        title:"主题",
        width: "120px",
        key:"topic"
      },{
        title:"QoS",
        width: "80px",
        key:"qos"
      },{
        title:"No Local",
        width: "180px",
        key:"ip"
      },{
        title:"Retain",
        width: "80px",
        key:"heart"
      }
    ]

    const dataSource2 = ref([])

    onMounted(() => {
      const loadData = async () => {
        const {data} = await subscriptions_subscription();
        console.log(data)
        dataSource2.value=data
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