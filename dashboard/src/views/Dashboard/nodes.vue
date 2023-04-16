<template>
    <lay-card>
        <lay-table :columns="columns2" :data-source="dataSource2" size="md" skin='nob'>
            <template #name="{data}">
                <a @click="editBroker(data)" href="javascript:void(0);">
                    {{ data.name }}<lay-icon type="layui-icon-edit" style="position: absolute;right: 10px;"></lay-icon>
                </a>
            </template>
            <template #status="{ data }">
                <div v-if="data.status=='running'">
                    <lay-badge type="dot" theme="blue" ripple></lay-badge>
                    运行中
                </div>
                <div v-if="data.status=='stopped'">
                    <lay-badge type="dot"></lay-badge>
                    已离线
                </div>
                <div v-if="data.status=='unknown'">
                    <lay-badge type="dot"></lay-badge>
                    未知
                </div>
            </template>

            <template v-slot:memory="{ data }">
                <lay-progress v-if="data.memory<30" :percent="data.memory" :show-text="true"
                              style="width:100px"></lay-progress>
                <lay-progress v-if="data.memory>=30" theme="orange" :percent="data.memory" :show-text="true"
                              style="width:100px"></lay-progress>
            </template>
            <template v-slot:cpu="{ data }">
                <lay-progress v-if="data.cpu<60" :percent="data.cpu" :show-text="true"
                              style="width:100px"></lay-progress>
                <lay-progress v-if="data.cpu>=60" theme="orange" :percent="data.cpu" :show-text="true"
                              style="width:100px"></lay-progress>
            </template>
        </lay-table>
    </lay-card>
    <lay-layer type="drawer" v-model="modalVisible" title="MQTT Broker配置">
        <lay-card>
            <lay-button type="primary">保存并重启</lay-button>
            <lay-button type="default" @click="">仅保存</lay-button>
        </lay-card>

        <lay-tab type="card" v-model="currentTab">
            <lay-tab-item v-for="(n, index) in brokerConfig" :title="n.title" :id="index">
                <lay-form :model="brokerForm">
                    <lay-card v-if="n.dynamic_config&&n.dynamic_config.length>0" title="动态生效">
                        <lay-form-item v-for="(item, i) in n.dynamic_config" :label="item.label" :prop="item.prop"
                                       mode="inline">
                            <lay-input :placeholder="item.placeholder"/>
                        </lay-form-item>
                    </lay-card>
                    <lay-card v-if="n.restart_config&&n.restart_config.length>0" title="重启生效">
                        <lay-form-item v-for="(item, i) in n.restart_config" :label="item.label" :prop="item.prop"
                                       mode="inline">
                            <lay-input :placeholder="item.placeholder"/>
                        </lay-form-item>
                    </lay-card>
                </lay-form>
            </lay-tab-item>
        </lay-tab>
    </lay-layer>
</template>

<script lang="ts">

import {onMounted, reactive, ref} from "vue";
import {dashboard_nodes} from "../../api/module/api";
import {onUnmounted} from "@vue/runtime-core";

export default {
    setup() {
        const columns2 = [
            {
                title: "名称",
                width: "200px",
                key: "name",
                customSlot: "name"
            }, {
                title: "状态",
                width: "180px",
                key: "status",
                customSlot: "status"
            }, {
                title: "运行时长",
                width: "120px",
                key: "runtime"
            }, {
                title: "版本信息",
                width: "120px",
                key: "version"
            }, {
                title: "进程",
                width: "120px",
                key: "pid"
            }, {
                title: "操作系统内存",
                width: "180px",
                key: "memory",
                customSlot: "memory"
            }, {
                title: "操作系统CPU负载",
                width: "180px",
                key: "cpu",
                customSlot: "cpu"
            }
        ]
        const brokerConfig = ref([
            {
                title: '基础配置',
                dynamic_config: [
                    {
                        label: 'MaxPacketSize',
                        prop: 'MaxPacketSize',
                        placeholder: 'MQTT最大消息体字节数'
                    },
                    {
                        label: 'TopicLimit',
                        prop: 'TopicLimit',
                        placeholder: 'topic数量上限'
                    },
                ],
                restart_config: [
                    {
                        label: 'Host',
                        prop: 'host',
                        placeholder: 'Broker服务IP地址'
                    },
                    {
                        label: 'Port',
                        prop: 'port',
                        placeholder: 'Broker服务端口号'
                    },
                ]
            },
            {
                title: '企业插件',
                dynamic_config: [
                    {
                        label: 'Host',
                        prop: 'host',
                        placeholder: 'Broker服务IP地址'
                    },
                    {
                        label: 'Port',
                        prop: 'port',
                        placeholder: 'Broker服务端口号'
                    },
                ],
                restart_config: [
                    {
                        label: 'Host',
                        prop: 'host',
                        placeholder: 'Broker服务IP地址'
                    },
                    {
                        label: 'Port',
                        prop: 'port',
                        placeholder: 'Broker服务端口号'
                    },
                ]
            },
        ])
        const dataSource2 = ref([])
        const modalVisible = ref(false)
        const currentTab = ref(0)
        const brokerForm = reactive({
            host: '',
            port: '',
            bufferSize: '',
            maxPacketSize: '',
            topicLimit: '',
            maxInflight: '',
        })
        let timer;

        const editBroker = (brokerId) => {
            modalVisible.value = true
        }

        onMounted(() => {
            const loadData = async () => {
                const {data} = await dashboard_nodes();
                console.log(data)
                dataSource2.value = data
            };
            loadData()
            timer = setInterval(() => {
                loadData()
            }, 2000)
        })

        onUnmounted(() => {
            console.log("clear timer")
            clearInterval(timer)
        })

        return {
            columns2,
            dataSource2,
            modalVisible,
            brokerForm,
            currentTab,
            brokerConfig,
            editBroker
        }
    }
}
</script>