<template>
    <lay-container fluid="true" style="padding: 10px">
    <lay-row space="10">
        <lay-col :md="24">
            <lay-card>
                <lay-form :model="form" ref="formRef">
                    <lay-row>
                        <lay-col :md="6">
                            <lay-form-item label-width="0">
                            <lay-input v-model="form.clientId" placeholder="客户端ID"></lay-input>
                            </lay-form-item>
                        </lay-col>
                        <lay-col :md="6">
                            <lay-form-item label-width="0">
                            <lay-input v-model="form.username" placeholder="用户名"></lay-input>
                            </lay-form-item>
                        </lay-col>
                        <lay-col :md="6">
                            <lay-form-item label-width="0">
                            <lay-select v-model="form.brokerIps" placeholder="Broker节点" multiple allow-clear>
                                <lay-select-option v-for="(item) in brokerList" :value='item.value'
                                                   :label="item.label"></lay-select-option>
                            </lay-select>
                            </lay-form-item>
                        </lay-col>
                        <lay-col :md="6">
                            <lay-form-item label-width="0">
                            <lay-button type="primary" @click="change({current:1,limit:page.limit})">查询</lay-button>
                            <lay-button @click="reset">重置</lay-button>
                            </lay-form-item>
                        </lay-col>
                    </lay-row>
                </lay-form>
            </lay-card>
        </lay-col>
        <lay-col :md="24">
            <lay-card>
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
            </lay-card>
        </lay-col>
    </lay-row>
    </lay-container>
</template>

<script>

import {reactive, ref} from "vue";
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
        const form=reactive({
            username:"",
            clientId:"",
            brokerIps:[]
        })
        const formRef=ref(null);
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
        const reset=()=>{
            formRef.value.reset();
            loadData(1, page.value.limit);
        }
        const change = ({current, limit}) => {
            // layer.msg("current:" + current + " limit:" + limit);
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
                clientId:form.clientId,
                username:form.username,
                brokers:form.brokerIps
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
            form,
            formRef,
            reset
        }
    }
}
</script>