<template>
  <lay-container :fluid="true" style="padding: 10px">
    <lay-row :space="10">

    </lay-row>
  </lay-container>
  <lay-row space="10">
    <lay-col md="24" sm="24" xs="24">
      <lay-field title="资源指标">
        <lay-card>
          <lay-row :space="10">
            <lay-col :md="8">
              <a >
                <h3>连接数</h3>
                <p>
                  <cite>
                      <h1>
                    <lay-count-up :end-val="metric.client_online.value" :duration="2000"></lay-count-up>
                      </h1>
                  </cite>
                </p>
              </a>
            </lay-col>
            <lay-col :md="8">
              <a >
                <h3>主题数</h3>
                <p>
                  <cite>
                      <h1>
                    <lay-count-up :end-val="metric.topic_count.value" :duration="2000"></lay-count-up>
                      </h1>
                  </cite>
                </p>
              </a>
            </lay-col>
            <lay-col :md="8">
              <a >
                <h3>订阅数</h3>
                <p>
                  <cite>
                    <lay-count-up :end-val="metric.subscribe_topic_count.value" :duration="2000"></lay-count-up>
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
        <div class="flowChart" ref="onlineClientChart"></div>
      </lay-card>
    </lay-col>
    <lay-col md="12" sm="24" xs="24">
      <lay-card>
        <template #title><p>在线主题数：</p></template>
        <div class="flowChart" ref="topicCountChart"></div>
      </lay-card>
    </lay-col>
    <lay-col md="12" sm="24" xs="24">
      <lay-card>
        <template v-if="metric.period_message_received" #title><p>消息流入速率：
          {{ metric.period_message_received.value }}
          条/{{ metric.period_message_received.period > 1 ? metric.period_message_received.period : "" }}秒</p>
        </template>
        <template #body>
          <div class="flowChart" ref="period_message_received_chart">
            <lay-result v-if="!metric.period_message_received"  status="failure"
                        :describe="message" title="消息流入速率"></lay-result>
          </div>
        </template>
      </lay-card>
    </lay-col>
    <lay-col md="12" sm="24" xs="24">
      <lay-card>
        <template v-if="metric.period_message_sent" #title> <p>消息流出速率：
          {{ metric.period_message_sent.value }}
          条/{{ metric.period_message_sent.period > 1 ? metric.period_message_sent.period : "" }}秒</p></template>
        <template #body>
          <div class="flowChart" ref="period_message_sent_chart">
            <lay-result v-if="!metric.period_message_sent"  status="failure" :describe="message" title="消息流出速率"></lay-result>
          </div>
        </template>
      </lay-card>
    </lay-col>


  </lay-row>

</template>

<script lang="ts">
import {onMounted, ref} from "vue";
import {dashboard_overview} from "../../api/module/api";
import {Chart} from '@antv/g2';
import {onUnmounted} from "@vue/runtime-core";

export default {
  setup() {
    const message = "升级企业版解锁该指标视图";
    const chartGroup = {}

    const onlineClientChart = ref();
    const topicCountChart = ref();
    const period_message_received_chart = ref();
    const period_message_sent_chart = ref();

    const metric = ref({
      client_online: {value: 0},
      topic_count: {value: 0},
      subscribe_topic_count: {value: 0}
    })

    let timer;

    onMounted(() => {
      const loadData = async () => {
        const {data} = await dashboard_overview();
        metric.value = data.metric

        //客户端在线连接数
        updateChart(chartGroup, onlineClientChart, 'client_online', metric.value.client_online)

        //在线主题连接数
        updateChart(chartGroup, topicCountChart, 'topic_count', metric.value.topic_count)

        //流入速率
        if (metric.value.period_message_received) {
          updateChart(chartGroup, period_message_received_chart, 'period_message_received_queue', metric.value.period_message_received)
        }

        // //流出速率
        if (metric.value.period_message_sent) {
          updateChart(chartGroup, period_message_sent_chart, 'period_message_sent', metric.value.period_message_sent)
        }
      };

      /**
       * 刷新Chart
       */
      const updateChart = (chartGroup, metricRef, metricKey, metric) => {
        let historyQueue = chartGroup[metricKey + '_queue']
        let chart = chartGroup[metricKey]
        if (!chart) {
          chartGroup[metricKey] = flowChart(metricRef.value)
          chartGroup[metricKey + '_queue'] = []
          historyQueue = chartGroup[metricKey + '_queue'];
          historyQueue.push({value:0})
          chart = chartGroup[metricKey]
        }

        if (historyQueue.length > 0 && historyQueue[historyQueue.length - 1].time === metric.time) {

          console.log(metric)
          historyQueue[historyQueue.length - 1] = metric;
        } else {
          historyQueue.push(metric)
        }
        if (historyQueue.length >= 20) {
          historyQueue.shift()
        }
        chart.changeData(historyQueue)
      }
      loadData()
      timer = setInterval(() => {
        loadData()
      }, 2000)
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
    }

    function flowChart(dom) {
      const chart = new Chart({
        container: dom,
        // forceFit: true,
        height: 250,
        autoFit: true,
        padding: [20, 20, 40, 50]
      });
      chart.scale('time', {
        range: [0, 1],
        mask: "YYYY-MM-DD HH:mm:ss",
        tickCount: 50,
        type: 'timeCat'
      });
      chart.axis('time', {
        label: {
          textStyle: {
            fill: '#aaaaaa'
          },
          formatter: function formatter(text) {
            const array = text.split(" ");
            return array && array.length === 2 ? array[1] : "Nan";
          }
        }
      });
      chart.axis('value', {
        label: {
          textStyle: {
            fill: '#aaaaaa'
          },
          formatter: function formatter(text) {
            return text.replace(/(\d)(?=(?:\d{3})+$)/g, '$1,');
          }
        }
      });
      chart.tooltip({
        crosshairs: 'y',
        share: true
      });
      chart.legend({
        attachLast: true
      });

      chart.line().position('time*value');
      chart.area().position('time*value');
      chart.animate(false)
      chart.render();
      return chart;
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