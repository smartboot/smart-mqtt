<template>
    <lay-row space="10">
        <lay-col sm="6" md="6">
            <lay-input v-model="clientId" placeholder="客户端ID"></lay-input>
        </lay-col>
        <lay-col sm="6" md="6">
            <lay-input v-model="username" placeholder="用户名"></lay-input>
        </lay-col>
        <lay-col sm="6" md="6">
            <lay-select v-model="brokerIps" placeholder="Broker节点" multiple allow-clear>
                <lay-select-option v-for="(item) in brokerList" :value='item.value'
                                   :label="item.label"></lay-select-option>
            </lay-select>
        </lay-col>
        <lay-col sm="6" md="6">
            <lay-button prefix-icon="layui-icon-search"  @click="change({current:1,limit:page.limit})">搜索</lay-button>
            <lay-button>重置</lay-button>
        </lay-col>
    </lay-row>
    <lay-row space="10">
        <lay-table :columns="columns2" :data-source="dataSource" :page="page" @change="change" :size="md" skin='nob'>
            <template #status="{ data }">
                <div v-if="data.status=='connected'">
                    <lay-badge type="dot" theme="green" ripple></lay-badge>
                    已连接
                </div>
                <div v-if="data.status=='disconnect'">
                    <lay-badge type="dot"></lay-badge>
                    已离线
                </div>
            </template>
        </lay-table>
    </lay-row>
</template>

<script>

import {ref} from "vue";
import {brokers, connections} from "../../api/module/api";

export default {
    setup() {
        const page = ref({
            total: 0,
            limit: 10,
            current: 1,
            showRefresh: true,
            showCount: true,
        })
        const username = ref("");
        const clientId = ref("");
        const brokerIps = ref([]);
        const brokerList = ref([

        ]);

        const columns2 = [
            {
                title: "客户端ID",
                width: "120px",
                key: "clientId",
            }, {
                title: "用户名",
                width: "120px",
                key: "username"
            }, {
                title: "状态",
                width: "80px",
                key: "status",
                customSlot: "status"
            }, {
                title: "客户端IP地址",
                width: "80px",
                key: "ip_address"
            }, {
                title: "Broker地址",
                width: "80px",
                key: "broker_ip_address"
            }, {
                title: "心跳",
                width: "40px",
                key: "keepalive"
            }, {
                title: "Clean Start",
                width: "80px",
                key: "clean_start"
            }, {
                title: "会话过期间隔",
                width: "80px",
                key: "expiry_interval"
            }, {
                title: "连接时间",
                width: "180px",
                key: "connect_time"
            }
        ]

        const dataSource = ref([])
        const change = ({current, limit}) => {
            layer.msg("current:" + current + " limit:" + limit);
            loadData(current, limit)
        }

        const loadBrokers=async ()=>{
            const {data}=await brokers();
            console.log(data)
            const array=[];
            data.map(broker=>{
                array.push({label:broker["name"],value:broker['ipAddress']+":"+broker['port']})
            })
            brokerList.value=array;
        }
        loadBrokers();

        const loadData = async (pageNo, pageSize) => {
            const {data} = await connections({
                pageSize: pageSize,
                pageNo: pageNo,
                clientId:clientId.value,
                username:username.value,
                brokers:brokerIps.value
            });
            console.log(data)
            dataSource.value = data.list
            page.value.total = data.total;
            page.value.limit = data.pageSize;
        };
        loadData(page.value.current, page.value.limit)

        return {
            page,
            change,
            columns2,
            dataSource,
            brokerList,
            brokerIps,
            clientId,
            username
        }
    }
}
</script>