<template>
    <lay-row space="10">
        <lay-col md="8">
            <lay-table :height="height" :max-height="height" :columns="connect_columns" :data-source="connect_dataSource" size="md" skin='nob'>
                <template #metric="{ data }">
                    <div class="metric-cell">
                        <p>{{ data.code }}</p>
                        <span> {{ data.desc }} </span>
                    </div>
                </template>
            </lay-table>
        </lay-col>

        <lay-col md="8">
            <lay-table :height="height" :max-height="height" :columns="session_columns" :data-source="session_dataSource" size="md" skin='nob'>
                <template #metric="{ data }">
                    <div class="metric-cell">
                        <p>{{ data.code }}</p>
                        <span> {{ data.desc }} </span>
                    </div>
                </template>
            </lay-table>
        </lay-col>

        <lay-col md="8">
            <lay-table :height="height" :max-height="height" :columns="access_columns" :data-source="access_dataSource" size="md" skin='nob'>
                <template #metric="{ data }">
                    <div class="metric-cell">
                        <p>{{ data.code }}</p>
                        <span> {{ data.desc }} </span>
                    </div>
                </template>
            </lay-table>
        </lay-col>
    </lay-row>
    <lay-row>
        <lay-space direction="vertical" fill>
            消息传输
            <lay-line theme="red"></lay-line>
        </lay-space>
    </lay-row>
    <lay-container fluid>
        <lay-row space="10">
            <!--      <lay-col md="8">-->
            <!--        <lay-table :columns="bytes_columns" :data-source="bytes_dataSource" size="md" skin='nob'>-->
            <!--          <template #metric="{ data }">-->
            <!--            <div class="metric-cell">-->
            <!--              <p>{{ data.code }}</p>-->
            <!--              <span> {{ data.desc }} </span>-->
            <!--            </div>-->
            <!--          </template>-->
            <!--        </lay-table>-->
            <!--      </lay-col>-->

            <lay-col md="8">
                <lay-table :height="height" :max-height="height" :columns="packet_columns" :data-source="packet_dataSource" size="md" skin='nob'>
                    <template #metric="{ data }">
                        <div class="metric-cell">
                            <p>{{ data.code }}</p>
                            <span> {{ data.desc }} </span>
                        </div>
                    </template>
                </lay-table>
            </lay-col>

            <lay-col md="8">
                <lay-table :height="height" :max-height="height" :columns="message_columns" :data-source="message_dataSource" size="md" skin='nob'>
                    <template #metric="{ data }">
                        <div class="metric-cell">
                            <p>{{ data.code }}</p>
                            <span> {{ data.desc }} </span>
                        </div>
                    </template>
                </lay-table>
            </lay-col>

            <lay-col md="8">
                <lay-table :height="height" :max-height="height" :columns="delivery_columns" :data-source="delivery_dataSource" size="md" skin='nob'>
                    <template #metric="{ data }">
                        <div class="metric-cell">
                            <p>{{ data.code }}</p>
                            <span> {{ data.desc }} </span>
                        </div>
                    </template>
                </lay-table>
            </lay-col>
        </lay-row>
    </lay-container>
</template>

<script lang="ts">

import {onMounted, ref} from "vue";
import {dashboard_metrics} from "../../api/module/api";

export default {
    setup() {
        const height = ref("250px");
        const connect_columns = [
            {
                title: "连接",
                width: "200px",
                key: "metric",
                customSlot: "metric"
            }, {
                title: "",
                width: "80px",
                key: "value",
            }
        ]

        const connect_dataSource = ref([])

        const session_columns = [
            {
                title: "会话",
                width: "200px",
                key: "metric",
                customSlot: "metric"
            }, {
                title: "",
                width: "180px",
                key: "value",
            }
        ]

        const session_dataSource = ref([])

        const access_columns = [
            {
                title: "认证与权限",
                width: "200px",
                key: "metric",
                customSlot: "metric"
            }, {
                title: "",
                width: "180px",
                key: "value",
            }
        ]

        const access_dataSource = ref([])

        // const bytes_columns = [
        //   {
        //     title:"流量收发（字节）",
        //     width:"200px",
        //     key:"metric",
        //     customSlot: "metric"
        //   },{
        //     title:"",
        //     width: "180px",
        //     key:"value",
        //   }
        // ]
        //
        // const bytes_dataSource = ref([])

        const packet_columns = [
            {
                title: "报文",
                width: "200px",
                key: "metric",
                customSlot: "metric"
            }, {
                title: "",
                width: "180px",
                key: "value",
            }
        ]

        const packet_dataSource = ref([])


        const message_columns = [
            {
                title: "消息数量",
                width: "200px",
                key: "metric",
                customSlot: "metric"
            }, {
                title: "",
                width: "180px",
                key: "value",
            }
        ]

        const message_dataSource = ref([])


        const delivery_columns = [
            {
                title: "消息分发",
                width: "200px",
                key: "metric",
                customSlot: "metric"
            }, {
                title: "",
                width: "180px",
                key: "value",
            }
        ]

        const delivery_dataSource = ref([])

        onMounted(() => {
            const loadData = async () => {
                const {data} = await dashboard_metrics();
                console.log(data)
                // bytes_dataSource.value=data['group']['bytes'];
                connect_dataSource.value = data['group']['connection'];
                session_dataSource.value = data['group']['session'];
                packet_dataSource.value = data['group']['packet'];
                access_dataSource.value = data['group']['access'];
                message_dataSource.value = data['group']['message'];
                delivery_dataSource.value = data['group']['delivery'];
            };
            loadData()
        })
        return {
            connect_columns,
            connect_dataSource,
            session_columns,
            session_dataSource,
            packet_columns,
            packet_dataSource,
            access_columns,
            access_dataSource,
            // bytes_columns,
            // bytes_dataSource,
            message_columns,
            message_dataSource,
            delivery_columns,
            delivery_dataSource,
            height
        }
    }
}
</script>
<style>
.metric-cell {
    display: block;
}

.metric-cell span {
    color: #8c8c8c;
}
</style>