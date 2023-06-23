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


  </lay-row>
  <lay-row>
<!--    <lay-col md="4" sm="24" xs="24">adsfa</lay-col>-->
    <lay-col md="24" sm="24" xs="24">
      <lay-card>
        <div class="chart" id="chinaMap" ref="chinaRef"></div>
      </lay-card>
    </lay-col>
  </lay-row>
  <lay-row>
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
import {china} from "../../assets/map/china"
import {city} from "../../assets/map/city"
import {MapChart} from "echarts/charts";

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


const convertData = function (data) {
  var res = [];
  for (var i = 0; i < data.length; i++) {
    var geoCoord = city[data[i].name];
    if (geoCoord) {
      // console.log(geoCoord.concat(data[i].value))
      res.push({
        name: data[i].name,
        value: geoCoord.concat(data[i].value)
      });
    }
  }
  console.log(res.length)
  return res;
};

var COLOR_ALL = [
  '#37A2DA',
  '#e06343',
  '#37a354',
  '#b55dba',
  '#b5bd48',
  '#8378EA',
  '#96BFFF'
];
const convertClientData = function (data) {
  var res = [];
  for (var i = 0; i < data.length; i++) {
    var geoCoord = city[data[i].name];
    if (geoCoord) {
      // console.log(geoCoord.concat(data[i].value))
      let v=data[i].value
      let color
      if(v<10){
        color='#ff85c0';
      }else if(v<100){
        color='#1677ff';
      }else if(v<1000){
        color='#1d39c4';
      }else if(v<10000){
        color='#fa8c16';
      }else{
        color='#f5222d';
      }
      res.push({
        name: data[i].name,
        value: geoCoord.concat(data[i].value),
        itemStyle: {
          color: color,
          borderType: 'dashed',
          // opacity: 0.5
        }
      });
    }
  }
  console.log(res.length)
  return res;
};
let chinaChart:EChartsType;
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

    chinaChart=echarts.init(this.chinaRef);
    echarts.use([MapChart]);
    echarts.registerMap("chinaMap",china);
    chinaChart.setOption({
      geo:{
        type:'map',
         map:'chinaMap',
        roam:true,
        zoom:2,
        center: [104.114129, 37.550339],
      },
      title: {
        text: 'smart-mqtt监控大屏',
        subtext: '24小时在线视图',
        sublink: 'http://smartboot.tech',
        left: 'center'
      },
      tooltip: {
        trigger: 'item'
      },
    })
  },
  setup() {
    const items = ['client_online', 'topic_count', 'packets_publish_received', 'packets_publish_sent','packets_received','packets_sent'];
    const metrics: MetricModel[] = items.map(key => {
      return {key: key, chartRef: 'chart_' + key}
    })

    const chinaRef=ref()

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

      //更新地图
      // console.log("r",data.group.clientRegions)
      const clients=data.group.clientRegions.map(m=>{return {name:m.code,value:m.value};});
      const brokers=data.group.brokerNodes.map(m=>{return {name:m.code,value:m.value};});
      console.log("d",clients)
      chinaChart.setOption({
        series: [
          {
            name: '客户端',
            type: 'scatter',
            coordinateSystem: 'geo',
            data: convertClientData(clients),
            symbolSize: function (val) {
              return Math.min(Math.max(val[2],10),40) ;
            },
            encode: {
              value: 2
            },
            label: {
              formatter: '{b}',
              position: 'right',
              // show: true
            },
            emphasis: {
              label: {
                show: true
              }
            }
          },
          {
            name: '服务节点',
            type: 'effectScatter',
            coordinateSystem: 'geo',
            data: convertData(
                brokers
                    .sort(function (a, b) {
                      return b.value - a.value;
                    })
                    .slice(0, 6)
            ),
            symbolSize: 20,
            encode: {
              value: 2
            },
            showEffectOn: 'render',
            rippleEffect: {
              brushType: 'stroke'
            },
            label: {
              formatter: '{b}',
              position: 'right',
              show: true
            },
            itemStyle: {
              shadowBlur: 10,
              shadowColor: '#333'
            },
            emphasis: {
              scale: true
            },
            zlevel: 1
          }
        ]
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
      chinaRef,
      metrics,
      metric,
      license
    }
  }
}

</script>
<style>
#chinaMap {
  width: 100%;
  height: 600px;
}

.chart {
  height: 300px;
  width: 100%;
}
</style>