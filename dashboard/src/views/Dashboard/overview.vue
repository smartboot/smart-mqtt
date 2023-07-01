<template>
  <!--指标-->
  <lay-row space="10">
    <lay-col md="8" v-for="(k,v) in metric">
      <lay-card shadow="always" class="metricCard">
        <lay-row>
          <lay-col md="10">
            <lay-space direction="vertical" size="lg" fill wrap>
              <div :style="`width: 20px;height: 10px;background-color:`+k.color"></div>
              <h2>{{ k.title }}</h2>
              <span style="font-size: 37px;">
                    <lay-count-up :end-val="k.value"
                                  :duration="2000"></lay-count-up>
                  </span>
            </lay-space>
          </lay-col>
          <lay-col md="14">
            <lay-avatar :src="k.avatar" style="width: 60px;height: 60px" radius></lay-avatar>
          </lay-col>
        </lay-row>
      </lay-card>
    </lay-col>
  </lay-row>
  <!--地图-->
  <lay-row>
    <lay-col md="16" sm="24" xs="24">
      <lay-card>
        <div class="chart" id="chinaMap" ref="chinaRef"></div>
      </lay-card>
    </lay-col>
    <lay-col md="8" sm="24" xs="24" style="background: whitesmoke;padding: 10px;">
      <lay-carousel v-model="activeNode" anim="fade" style="height: 600px" :interval="5000" :autoplay="true"
                    arrow="none">
        <lay-carousel-item v-for="node in clusterNodes" :id="node.localAddress"
                           :style="`height: 100%;background-color:`+node.color">
          <!--            <div style="color: white;text-align: center;width:100%;line-height:600px;background-color:#009688;">-->
          <lay-card style="height: 100%;">
            <template v-slot:title>
              <h1>Broker节点：{{ node.localAddress }}</h1>
            </template>
            <template v-slot:body>
              <!--                  {{ node }}-->
              <!--                  <lay-container :fluid="true" style="padding: 10px">-->
              <lay-row>
                <lay-col md="18">
                  <div :ref="node.ref" style="width: 100%;height: 300px"></div>
                </lay-col>
                <lay-col md="6">
                  <lay-card title="操作系统">
                    {{ node.osName }}
                  </lay-card>
                  <lay-card title="系统架构">
                    {{node.osArch}}
                  </lay-card>
                  <lay-card title="内存规格">
                    {{ node.memoryLimit/1024.0/1024/1024 }} GB
                  </lay-card>
                </lay-col>
              </lay-row>
              <lay-card>
                <template #title>服务器信息</template>
                <table class="layui-table" style="width: 100%;border: 1px">
                  <tr>
                    <td style="width: 40%">smart-mqtt 版本：</td>
                    <td>{{ node.version }}</td>
                  </tr>
                  <tr>
                    <td>JVM提供商：</td>
                    <td> {{ node.vmVendor }} {{ node.vmVersion }}</td>
                  </tr>
                  <tr>
                    <td>主机名：</td>
                    <td>{{ node.hostName }}</td>
                  </tr>
                  <tr>
                    <td>启动时间：</td>
                    <td>{{ node?.startTime }}</td>
                  </tr>
                  <tr>
                    <td>授权有效期：</td>
                    <td>
                      <lay-progress v-if="node.cpuUsage<60" :percent="node.cpuUsage" :show-text="true"
                                    style="width:100px"></lay-progress>
                      <lay-progress v-if="node.cpuUsage>=60" theme="orange" :percent="node.cpuUsage" :show-text="true"
                                    style="width:100px"></lay-progress>
                    </td>
                  </tr>
                </table>
              </lay-card>
              <!--                  </lay-container>-->
            </template>
          </lay-card>
          <!--            </div>-->
        </lay-carousel-item>
      </lay-carousel>
    </lay-col>
  </lay-row>
  <!--指标仪表盘-->
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
import {dashboard_cluster, dashboard_overview} from "../../api/module/api";
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

const convertClientData = function (data) {
  var res = [];
  for (var i = 0; i < data.length; i++) {
    var geoCoord = city[data[i].name];
    if (geoCoord) {
      // console.log(geoCoord.concat(data[i].value))
      let v = data[i].value
      let color
      if (v < 10) {
        color = '#ff85c0';
      } else if (v < 100) {
        color = '#1677ff';
      } else if (v < 1000) {
        color = '#1d39c4';
      } else if (v < 10000) {
        color = '#fa8c16';
      } else {
        color = '#f5222d';
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
let chinaChart: EChartsType;
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

    chinaChart = echarts.init(this.chinaRef);
    echarts.use([MapChart]);
    echarts.registerMap("chinaMap", china);
    chinaChart.setOption({
      geo: {
        type: 'map',
        map: 'chinaMap',
        // roam:true,
        zoom: 1.5,
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

    const loadClusterNodes = async () => {
      const {data} = await dashboard_cluster();
      // console.log("cluster", data)
      //更新集群节点信息
      // const nodes = [{
      //   ip: '192.168.1.1',
      //   color: '#d4d4d7',
      // }, {
      //   ip: '192.168.1.2',
      //   color: '#eeefee',
      // }];
      const nodes = data;
      nodes.map(node => {
        const preNode = this.clusterNodes?.filter(n => n.ip == node.ip)
        node.ref = node.localAddress + "_ref"
        if (preNode && preNode?.[0]?.chart) {
          console.log("aaa",node)
          node.chart = preNode[0].chart;
          node.chart.setOption({series: [
              {
                data: [
                  {
                    value: node.memUsage,
                    name: '内存',
                    title: {
                      offsetCenter: ['0%', '40%']
                    },
                    detail: {
                      valueAnimation: true,
                      offsetCenter: ['0%', '20%']
                    }
                  },
                  {
                    value: node.cpuUsage,
                    name: 'CPU',
                    title: {
                      offsetCenter: ['0%', '-10%']
                    },
                    detail: {
                      valueAnimation: true,
                      offsetCenter: ['0%', '-30%']
                    }
                  },
                ],
              }]

          })
        } else {
          console.log("refs", this.$refs)
          if (this.$refs[node.ref] && !node.chart) {
            node.chart = echarts.init(this.$refs[node.ref][0])
            node.chart.setOption({
              series: [
                {
                  type: 'gauge',
                  startAngle: 90,
                  endAngle: -270,
                  pointer: {
                    show: false
                  },
                  progress: {
                    show: true,
                    overlap: false,
                    roundCap: true,
                    clip: false,
                    itemStyle: {
                      borderWidth: 1,
                      borderColor: '#464646'
                    }
                  },
                  axisLine: {
                    lineStyle: {
                      width: 40
                    }
                  },
                  splitLine: {
                    show: false,
                    distance: 0,
                    length: 10
                  },
                  axisTick: {
                    show: false
                  },
                  axisLabel: {
                    show: false,
                    distance: 50
                  },
                  data: [
                  ],
                  title: {
                    fontSize: 14
                  },
                  detail: {
                    width: 30,
                    height: 10,
                    fontSize: 13,
                    color: 'inherit',
                    borderColor: 'inherit',
                    borderRadius: 20,
                    borderWidth: 1,
                    formatter: '{value}%'
                  }
                }
              ]
            })
          }
        }
        return node
      })
      this.clusterNodes = nodes
    }
    loadClusterNodes()
    let timer = setInterval(() => {
      loadClusterNodes()
    }, 1000)

  },
  setup() {
    //指标仪表盘
    const metrics: MetricModel[] = ['client_online', 'topic_count', 'packets_publish_received', 'packets_publish_sent', 'packets_received', 'packets_sent'].map(key => {
      return {key: key, chartRef: 'chart_' + key}
    })

    const clusterNodes = ref([])
    const activeNode = ref(0)
    const chinaRef = ref()

    const license = ref({
      username: "",
      password: "",
      desc: "",
      datetime: []

    });

    const metric = ref([{
      code: 'client_online',
      title: '连接数',
      color: '#9370DB',
      avatar: 'Connection.svg',
      value: 0
    }, {
      code: 'topic_count',
      title: '主题数',
      color: '#00ff00',
      avatar: 'https://s3.us-east-2.amazonaws.com/template.appsmith.com/Group+9.svg',
      value: 0
    }, {
      code: 'subscribe_topic_count',
      title: '订阅数',
      color: '#ffff00',
      avatar: 'Connection.svg',
      value: 0
    }])

    const loadData = async () => {
      const {data} = await dashboard_overview();
      metric.value = metric.value.map(v => {
        v.value = data.metric[v.code].value;
        return v;
      });

      metrics.map(metric => {
        updateChart(metric, data.metric[metric.key])
      })

      //更新地图
      // console.log("r",data.group.clientRegions)
      chinaChart.setOption({
        series: [
          {
            name: '客户端',
            type: 'scatter',
            coordinateSystem: 'geo',
            data: convertClientData(data.group?.clientRegions?.map(m => {
              return {name: m.code, value: m.value};
            })),
            symbolSize: function (val) {
              return Math.min(Math.max(val[2], 10), 40);
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
                data?.group?.clusterNodes?.map(m => {
                  return {name: m.code, value: m.value};
                }).sort(function (a, b) {
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
      clusterNodes,
      activeNode,
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

.metricCard {
  border-radius: 5px;
  border: #1e9fff;
  background-color: #000D3D;
  color: #FFFFFF
}
</style>