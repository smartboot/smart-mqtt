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
    <lay-col md="4" sm="24" xs="24">adsfa</lay-col>
    <lay-col md="20" sm="24" xs="24">
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

    let chinaChart=echarts.init(this.chinaRef);
    echarts.use([MapChart]);
    echarts.registerMap("chinaMap",china);
    const data = [
      { name: '海门', value: 9 },
      { name: '鄂尔多斯', value: 12 },
      { name: '招远', value: 12 },
      { name: '舟山', value: 12 },
      { name: '齐齐哈尔', value: 14 },
      { name: '盐城', value: 15 },
      { name: '赤峰', value: 16 },
      { name: '青岛', value: 18 },
      { name: '乳山', value: 18 },
      { name: '金昌', value: 19 },
      { name: '泉州', value: 21 },
      { name: '莱西', value: 21 },
      { name: '日照', value: 21 },
      { name: '胶南', value: 22 },
      { name: '南通', value: 23 },
      { name: '拉萨', value: 24 },
      { name: '云浮', value: 24 },
      { name: '梅州', value: 25 },
      { name: '文登', value: 25 },
      { name: '上海', value: 25 },
      { name: '攀枝花', value: 25 },
      { name: '威海', value: 25 },
      { name: '承德', value: 25 },
      { name: '厦门', value: 26 },
      { name: '汕尾', value: 26 },
      { name: '潮州', value: 26 },
      { name: '丹东', value: 27 },
      { name: '太仓', value: 27 },
      { name: '曲靖', value: 27 },
      { name: '烟台', value: 28 },
      { name: '福州', value: 29 },
      { name: '瓦房店', value: 30 },
      { name: '即墨', value: 30 },
      { name: '抚顺', value: 31 },
      { name: '玉溪', value: 31 },
      { name: '张家口', value: 31 },
      { name: '阳泉', value: 31 },
      { name: '莱州', value: 32 },
      { name: '湖州', value: 32 },
      { name: '汕头', value: 32 },
      { name: '昆山', value: 33 },
      { name: '宁波', value: 33 },
      { name: '湛江', value: 33 },
      { name: '揭阳', value: 34 },
      { name: '荣成', value: 34 },
      { name: '连云港', value: 35 },
      { name: '葫芦岛', value: 35 },
      { name: '常熟', value: 36 },
      { name: '东莞', value: 36 },
      { name: '河源', value: 36 },
      { name: '淮安', value: 36 },
      { name: '泰州', value: 36 },
      { name: '南宁', value: 37 },
      { name: '营口', value: 37 },
      { name: '惠州', value: 37 },
      { name: '江阴', value: 37 },
      { name: '蓬莱', value: 37 },
      { name: '韶关', value: 38 },
      { name: '嘉峪关', value: 38 },
      { name: '广州', value: 38 },
      { name: '延安', value: 38 },
      { name: '太原', value: 39 },
      { name: '清远', value: 39 },
      { name: '中山', value: 39 },
      { name: '昆明', value: 39 },
      { name: '寿光', value: 40 },
      { name: '盘锦', value: 40 },
      { name: '长治', value: 41 },
      { name: '深圳', value: 41 },
      { name: '珠海', value: 42 },
      { name: '宿迁', value: 43 },
      { name: '咸阳', value: 43 },
      { name: '铜川', value: 44 },
      { name: '平度', value: 44 },
      { name: '佛山', value: 44 },
      { name: '海口', value: 44 },
      { name: '江门', value: 45 },
      { name: '章丘', value: 45 },
      { name: '肇庆', value: 46 },
      { name: '大连', value: 47 },
      { name: '临汾', value: 47 },
      { name: '吴江', value: 47 },
      { name: '石嘴山', value: 49 },
      { name: '沈阳', value: 50 },
      { name: '苏州', value: 50 },
      { name: '茂名', value: 50 },
      { name: '嘉兴', value: 51 },
      { name: '长春', value: 51 },
      { name: '胶州', value: 52 },
      { name: '银川', value: 52 },
      { name: '张家港', value: 52 },
      { name: '三门峡', value: 53 },
      { name: '锦州', value: 54 },
      { name: '南昌', value: 54 },
      { name: '柳州', value: 54 },
      { name: '三亚', value: 54 },
      { name: '自贡', value: 56 },
      { name: '吉林', value: 56 },
      { name: '阳江', value: 57 },
      { name: '泸州', value: 57 },
      { name: '西宁', value: 57 },
      { name: '宜宾', value: 58 },
      { name: '呼和浩特', value: 58 },
      { name: '成都', value: 58 },
      { name: '大同', value: 58 },
      { name: '镇江', value: 59 },
      { name: '桂林', value: 59 },
      { name: '张家界', value: 59 },
      { name: '宜兴', value: 59 },
      { name: '北海', value: 60 },
      { name: '西安', value: 61 },
      { name: '金坛', value: 62 },
      { name: '东营', value: 62 },
      { name: '牡丹江', value: 63 },
      { name: '遵义', value: 63 },
      { name: '绍兴', value: 63 },
      { name: '扬州', value: 64 },
      { name: '常州', value: 64 },
      { name: '潍坊', value: 65 },
      { name: '重庆', value: 66 },
      { name: '台州', value: 67 },
      { name: '南京', value: 67 },
      { name: '滨州', value: 70 },
      { name: '贵阳', value: 71 },
      { name: '无锡', value: 71 },
      { name: '本溪', value: 71 },
      { name: '克拉玛依', value: 72 },
      { name: '渭南', value: 72 },
      { name: '马鞍山', value: 72 },
      { name: '宝鸡', value: 72 },
      { name: '焦作', value: 75 },
      { name: '句容', value: 75 },
      { name: '北京', value: 79 },
      { name: '徐州', value: 79 },
      { name: '衡水', value: 80 },
      { name: '包头', value: 80 },
      { name: '绵阳', value: 80 },
      { name: '乌鲁木齐', value: 84 },
      { name: '枣庄', value: 84 },
      { name: '杭州', value: 84 },
      { name: '淄博', value: 85 },
      { name: '鞍山', value: 86 },
      { name: '溧阳', value: 86 },
      { name: '库尔勒', value: 86 },
      { name: '安阳', value: 90 },
      { name: '开封', value: 90 },
      { name: '济南', value: 92 },
      { name: '德阳', value: 93 },
      { name: '温州', value: 95 },
      { name: '九江', value: 96 },
      { name: '邯郸', value: 98 },
      { name: '临安', value: 99 },
      { name: '兰州', value: 99 },
      { name: '沧州', value: 100 },
      { name: '临沂', value: 103 },
      { name: '南充', value: 104 },
      { name: '天津', value: 105 },
      { name: '富阳', value: 106 },
      { name: '泰安', value: 112 },
      { name: '诸暨', value: 112 },
      { name: '郑州', value: 113 },
      { name: '哈尔滨', value: 114 },
      { name: '聊城', value: 116 },
      { name: '芜湖', value: 117 },
      { name: '唐山', value: 119 },
      { name: '平顶山', value: 119 },
      { name: '邢台', value: 119 },
      { name: '德州', value: 120 },
      { name: '济宁', value: 120 },
      { name: '荆州', value: 127 },
      { name: '宜昌', value: 130 },
      { name: '义乌', value: 132 },
      { name: '丽水', value: 133 },
      { name: '洛阳', value: 134 },
      { name: '秦皇岛', value: 136 },
      { name: '株洲', value: 143 },
      { name: '石家庄', value: 147 },
      { name: '莱芜', value: 148 },
      { name: '常德', value: 152 },
      { name: '保定', value: 153 },
      { name: '湘潭', value: 154 },
      { name: '金华', value: 157 },
      { name: '岳阳', value: 169 },
      { name: '长沙', value: 175 },
      { name: '衢州', value: 177 },
      { name: '廊坊', value: 193 },
      { name: '菏泽', value: 194 },
      { name: '合肥', value: 229 },
      { name: '武汉', value: 273 },
      { name: '大庆', value: 279 }
    ];
    const convertData = function (data) {
      var res = [];
      for (var i = 0; i < data.length; i++) {
        var geoCoord = city[data[i].name];
        if (geoCoord) {
          res.push({
            name: data[i].name,
            value: geoCoord.concat(data[i].value)
          });
        }
      }
      return res;
    };
    chinaChart.setOption({
      geo:{
        type:'map',
         map:'chinaMap',
        roam:true,
        zoom:2,
        center: [104.114129, 37.550339],
      },
      title: {
        text: 'smart-mqtt',
        subtext: '终端在线实时监控大屏',
        sublink: 'http://smartboot.tech',
        left: 'center'
      },
      tooltip: {
        trigger: 'item'
      },
      series: [
        {
          name: '客户端',
          type: 'scatter',
          coordinateSystem: 'geo',
          data: convertData(data),
          symbolSize: function (val) {
            return val[2] / 10;
          },
          encode: {
            value: 2
          },
          label: {
            formatter: '{b}',
            position: 'right',
            show: false
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
              data
                  .sort(function (a, b) {
                    return b.value - a.value;
                  })
                  .slice(0, 6)
          ),
          symbolSize: function (val) {
            return val[2] / 10;
          },
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