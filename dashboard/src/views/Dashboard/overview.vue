<template>
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
    <lay-col md="24" sm="24" xs="24">
      <lay-card>
        <template #title><p>集群拓扑：</p></template>
        <div id="echarts-amap" ref="chinaRef"></div>
      </lay-card>
    </lay-col>
    <lay-col md="12" sm="24" xs="24" v-for="(metric,key) in metrics" :key="key">
      <lay-card>
        <div class="chart" :ref="metric.chartRef"/>
      </lay-card>
    </lay-col>
  </lay-row>

</template>

<script lang="ts">
import {ref} from "vue";
import {dashboard_overview} from "../../api/module/api";
import {onUnmounted} from "@vue/runtime-core";
import * as echarts from 'echarts';
import {EChartsType} from 'echarts';
import 'echarts-extension-amap';

interface Metric {
  code?: string
  desc?: string
  period?: number
  time?: string
  value: number
}

interface MetricModel {
  key: string,
  chartRef: string,
  chart?: EChartsType
  queue?: Metric[]
}

const updateChart = (model: MetricModel, metric: Metric) => {
  const metricRef = model.chart
  let historyQueue = model.queue || []
  if (historyQueue.length > 0 && historyQueue[historyQueue.length - 1].time === metric.time) {
    historyQueue[historyQueue.length - 1] = metric;
  } else {
    historyQueue.push(metric)
  }
  if (historyQueue.length >= 20) {
    historyQueue.shift()
  }
  model.queue = historyQueue
  metricRef?.hideLoading()
  metricRef?.setOption({
    title: {
      text: metric.desc
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {type: 'cross'}
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: historyQueue.map((item: Metric) => item?.time?.substring(11))
    },
    yAxis: {
      // boundaryGap: [0, '50%'],
      type: 'value',
      axisLabel: {
        formatter: '{value}'
      }
    },
    series: [
      {
        name: '数量',
        type: 'line',
        smooth: true,
        // symbol: 'none',
        stack: 'a',
        areaStyle: {
          // normal: {}
        },
        data: historyQueue.map((item: Metric) => item.value)
      }
    ]
  })

}
export default {
  mounted() {

    const defaultOption = {
      title: {
        text: '数据加载中...'
      },
      tooltip: {},
      legend: {
        data: ['数量']
      },
      xAxis: {
        data: []
      },
      yAxis: {},
      series: [
        {
          name: '数量',
          type: 'bar',
          data: []
        }
      ]
    };
    this.metrics.map((metric) => {
      metric.chart = echarts.init(this.$refs[metric.chartRef][0])
      metric.chart.setOption(defaultOption)
      metric.chart.showLoading();
    })

  },
  setup() {
    const items = ['client_online', 'topic_count', 'period_message_received', 'period_message_sent'];
    const metrics: MetricModel[] = items.map(key => {
      return {key: key, chartRef: 'chart_' + key}
    })

    const license = ref({
      username: "",
      password: "",
      desc: "",
      datetime: []

    });

    const metric = ref<Record<string, Metric>>({
      client_online: {value: 0},
      topic_count: {value: 0},
      subscribe_topic_count: {value: 0}
    })

    const loadData = async () => {
      const {data} = await dashboard_overview();
      metric.value = data.metric

      metrics.map(metric => {
        updateChart(metric, data.metric[metric.key])
      })

    };

    /**
     * 刷新Chart
     */
    loadData()
    let timer = setInterval(() => {
      loadData()
    }, 5000)
    onUnmounted(() => {
      console.log("clear timer")
      clearInterval(timer)
    })
    return {
      metrics,
      metric,
      license
    }
  }
}

</script>
<style>
.flowChart {
  width: 100%;
  /*height: 150px;*/
}

.chart {
  height: 300px;
  width: 100%;
}
</style>