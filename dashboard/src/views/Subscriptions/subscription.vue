<template>
    <lay-container fluid="true" style="padding: 10px">
        <lay-row :space="10">
            <lay-col :md="24">
                <lay-card>
                    <lay-form :model="form" ref="formRef">
                        <lay-row>
                            <lay-col :md="6">
                                <lay-form-item label="客户端ID：" label-width="70">
                                    <lay-input v-model="form.clientId" style="width: 90%"></lay-input>
                                </lay-form-item>
                            </lay-col>
                            <lay-col :md="6">
                                <lay-form-item label="订阅主题：" label-width="70">
                                    <lay-input v-model="form.topic" style="width: 90%"></lay-input>
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
                    <lay-table :columns="columns" :data-source="dataSource" :page="page" @change="change" :size="md"
                               skin='nob'></lay-table>
                </lay-card>
            </lay-col>
        </lay-row>
    </lay-container>

</template>

<script>

import {reactive, ref} from "vue";
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
        const form=reactive({
            clientId:"",
            topic:""
        })
        const formRef=ref(null);

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
            }
        ]
        const reset=()=>{
            formRef.value.reset();
            loadData(1, page.value.limit);
        }
        const change = ({current, limit}) => {
            loadData(current, limit)
        }

        const dataSource = ref([])

        const loadData = async (pageNo, pageSize) => {
            const {data} = await subscriptions_subscription({
                pageSize: pageSize,
                pageNo: pageNo,
                clientId:form.clientId,
                topic:form.topic,
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
            columns,
            dataSource,
            form,
            formRef,
            reset
        }
    }
}
</script>