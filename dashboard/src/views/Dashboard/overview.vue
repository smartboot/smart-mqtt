<template>
    <lay-container :fluid="true" style="padding: 10px">
        <lay-row :space="10">

        </lay-row>
    </lay-container>
    <lay-row space="10">
        <lay-col md="24" sm="24" xs="24">
            <lay-field title="资源指标">
                <lay-card>
                    <lay-row space="10">
                        <lay-col md="8">
                            <a>
                                <h3>连接数</h3>
                                <p>
                                    <cite>
                                        <h1>
                                            <lay-count-up :end-val="metric.client_online.value"
                                                          :duration="2000"></lay-count-up>
                                        </h1>
                                    </cite>
                                </p>
                            </a>
                        </lay-col>
                        <lay-col md="8">
                            <a>
                                <h3>主题数</h3>
                                <p>
                                    <cite>
                                        <h1>
                                            <lay-count-up :end-val="metric.topic_count.value"
                                                          :duration="2000"></lay-count-up>
                                        </h1>
                                    </cite>
                                </p>
                            </a>
                        </lay-col>
                        <lay-col md="8">
                            <a>
                                <h3>订阅数</h3>
                                <p>
                                    <cite>
                                        <lay-count-up :end-val="metric.subscribe_topic_count.value"
                                                      :duration="2000"></lay-count-up>
                                    </cite>
                                </p>
                            </a>
                        </lay-col>
                    </lay-row>
                </lay-card>
            </lay-field>
        </lay-col>
        <lay-col md="12" sm="24" xs="24">
            <lay-card>
                <template #title><p>在线连接数：</p></template>
                <Bar v-if="onlineClientChart" :data="onlineClientChart" :options="options"/>
            </lay-card>
        </lay-col>
        <lay-col md="12" sm="24" xs="24">
            <lay-card>
                <template #title><p>在线主题数：</p></template>
                <Bar v-if="topicCountChart" :data="topicCountChart" :options="options"/>
            </lay-card>
        </lay-col>
        <lay-col md="12" sm="24" xs="24">
            <lay-card>
                <template v-if="metric.period_message_received" #title><p>消息流入速率：
                    {{ metric.period_message_received.value }}
                    条/{{
                    metric.period_message_received.period > 1 ? metric.period_message_received.period : ""
                    }}秒</p>
                </template>
                <template #body>
                    <Bar v-if="period_message_received_chart" :data="period_message_received_chart" :options="options"/>
                    <lay-result v-if="!period_message_received_chart" status="failure"
                                :describe="message" title="消息流入速率"></lay-result>
                </template>
            </lay-card>
        </lay-col>
        <lay-col md="12" sm="24" xs="24">
            <lay-card>
                <template v-if="metric.period_message_sent" #title><p>消息流出速率：
                    {{ metric.period_message_sent.value }}
                    条/{{ metric.period_message_sent.period > 1 ? metric.period_message_sent.period : "" }}秒</p>
                </template>
                <template #body>
                    <Bar v-if="period_message_sent_chart" :data="period_message_sent_chart" :options="options"/>
                    <lay-result v-if="!period_message_sent_chart" status="failure" :describe="message"
                                title="消息流出速率"></lay-result>
                </template>
            </lay-card>
        </lay-col>


    </lay-row>

</template>

<script lang="ts">
import {onMounted, ref} from "vue";
import {dashboard_overview} from "../../api/module/api";
import {onUnmounted} from "@vue/runtime-core";
import {BarElement, CategoryScale, Chart as ChartJS, Legend, LinearScale, Title, Tooltip} from 'chart.js'
import {Bar} from 'vue-chartjs'

interface Metric {
    code?: string
    desc?: string
    period?: number
    time?: string
    value: number
}

interface MetricGroup {
    client_online: Metric
    topic_count: Metric
    subscribe_topic_count: Metric
}

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend)

export default {
    components: {
        Bar
    },
    setup() {

        const message = "升级企业版解锁该指标视图";
        const chartGroup = {}

        const onlineClientChart = ref();
        const topicCountChart = ref();
        const period_message_received_chart = ref();
        const period_message_sent_chart = ref();
        const options = ref({
            responsive: true,
            maintainAspectRatio: false
        })

        const metric = ref<Record<string, Metric>>({
            client_online: {value: 0},
            topic_count: {value: 0},
            subscribe_topic_count: {value: 0}
        })

        let timer;

        onMounted(() => {
            const loadData = async () => {
                const {data} = await dashboard_overview();
                console.log(data.metric);
                metric.value = data.metric

                //客户端在线连接数
                updateChart(onlineClientChart, 'client_online', metric.value.client_online)

                //在线主题连接数
                updateChart(topicCountChart, 'topic_count', metric.value.topic_count)

                //流入速率
                if (metric.value.period_message_received) {
                    updateChart(period_message_received_chart, 'period_message_received_queue', metric.value.period_message_received)
                }

                // //流出速率
                if (metric.value.period_message_sent) {
                    updateChart(period_message_sent_chart, 'period_message_sent', metric.value.period_message_sent)
                }
            };

            /**
             * 刷新Chart
             */
            const updateChart = (metricRef: any, metricKey: string, metric: Metric) => {
                let historyQueue = chartGroup[metricKey] || []
                if (historyQueue.length > 0 && historyQueue[historyQueue.length - 1].time === metric.time) {
                    historyQueue[historyQueue.length - 1] = metric;
                } else {
                    historyQueue.push(metric)
                }
                if (historyQueue.length >= 20) {
                    historyQueue.shift()
                }
                chartGroup[metricKey] = historyQueue
                console.log(historyQueue);
                metricRef.value = {
                    labels: historyQueue.map((item: Metric) => item?.time?.substring(11)),
                    datasets: [
                        {
                            label: historyQueue[0]["desc"],
                            backgroundColor: '#f87979',
                            data: historyQueue.map((item: Metric) => item.value)
                        }
                    ]
                }
            }

            loadData()
            timer = setInterval(() => {
                loadData()
            }, 5000)
        });
        onUnmounted(() => {
            console.log("clear timer")
            clearInterval(timer)
        })
        return {
            onlineClientChart,
            topicCountChart,
            period_message_received_chart,
            period_message_sent_chart,
            message,
            metric,
            options
        }
    }
}

</script>
<style>
.flowChart {
    width: 100%;
    /*height: 150px;*/
}

</style>