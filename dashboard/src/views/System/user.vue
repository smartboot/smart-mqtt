<!--
  - Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
  -
  -  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
  -
  -  Enterprise users are required to use this project reasonably
  -  and legally in accordance with the AGPL-3.0 open source agreement
  -  without special permission from the smartboot organization.
  -->

<template>
    <lay-container fluid="true" style="padding: 10px">
        <lay-row space="10">
            <lay-col md="24">
                <lay-card>
                    <lay-form :model="form" ref="formRef">
                        <lay-row>
                            <lay-col :md="6">
                                <lay-form-item label="用户：" label-width="70">
                                    <lay-input v-model="form.clientId" style="width: 90%"></lay-input>
                                </lay-form-item>
                            </lay-col>
                            <lay-col :md="6">
                                <lay-form-item label-width="0">
                                    <lay-button type="primary" @click="change({current:1,limit:page.limit})">查询</lay-button>
                                    <lay-button @click="reset">新增</lay-button>
                                </lay-form-item>
                            </lay-col>
                        </lay-row>
                    </lay-form>
                </lay-card>
            </lay-col>
            <lay-col :md="24">
                <lay-card>
                    <lay-table :columns="columns" :data-source="dataSource" :page="page" size="md" skin='nob'>
                        <template v-slot:operator="">
                            <lay-button type="primary">编辑</lay-button>
                            <lay-button>修改密码</lay-button>
                            <lay-button border="red" border-style="dashed">删除</lay-button>
                        </template>
                    </lay-table>
                </lay-card>
            </lay-col>
        </lay-row>
    </lay-container>

</template>

<script lang="ts">

import {reactive, ref} from "vue";
import {system_user_list} from "../../api/module/api";

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
                title: "用户",
                width: "200px",
                key: "username",
                // customSlot: "username"
            }, {
                title: "备注",
                width: "180px",
                key: "desc",
                // customSlot: "desc"
            }, {
                title: "操作",
                width: "200px",
                customSlot: "operator",
                key: "operator",
                align: "center",
            }
        ]
        const reset=()=>{
            formRef.value.reset();
            loadData(1, page.value.limit);
        }
        const dataSource = ref([])
        const modalVisible = ref(false)

        const loadData = async (pageNo, pageSize) => {
            const {data} = await system_user_list({
                pageSize: pageSize,
                pageNo: pageNo,
            });
            console.log(data)
            console.log(data)
            dataSource.value = data.list
            page.value.total = data.total;
            page.value.limit = data.pageSize;
        };
        loadData(page.value.current, page.value.limit)

        return {
            page,
            columns,
            dataSource,
            modalVisible,
            form,
            formRef,
            reset
        }
    }
}
</script>