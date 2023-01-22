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
              <a class="agency">
                <h3>连接数</h3>
                <p>
                  <cite>
                    <lay-count-up :end-val="metric.connectCount" :duration="2000"></lay-count-up>
                  </cite>
                </p>
              </a>
            </lay-col>
            <lay-col :md="8">
              <a class="agency">
                <h3>主题数</h3>
                <p>
                  <cite>
                    <lay-count-up :end-val="metric.topicCount" :duration="2000"></lay-count-up>
                  </cite>
                </p>
              </a>
            </lay-col>
            <lay-col :md="8">
              <a class="agency">
                <h3>订阅数</h3>
                <p>
                  <cite>
                    <lay-count-up :end-val="metric.subscriberCount" :duration="2000"></lay-count-up>
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
        <template #title><p class="agency">消息流入速率： {{ inflowRate }} 条/秒</p></template>
        <div class="flowChart" ref="flowInRef"></div>
      </lay-card>
    </lay-col>
    <lay-col md="12" sm="24" xs="24">
      <lay-card>
        <template #title><p class="agency">消息流出速率： {{ outflowRate }} 条/秒</p></template>
        <div class="flowChart" ref="flowOutRef"></div>
      </lay-card>
    </lay-col>


  </lay-row>
  <lay-row space="10">
    <lay-col md="24" sm="24" xs="24">
      <div class="grid-demo" style="height: 200px">3</div>
    </lay-col>
  </lay-row>
  <lay-select v-model="value" placeholder="请选择">
    <lay-select-option :value="1" label="过去1小时"></lay-select-option>
    <lay-select-option :value="2" label="过去6小时"></lay-select-option>
    <lay-select-option :value="3" label="过去12小时"></lay-select-option>
    <lay-select-option :value="4" label="过去1天"></lay-select-option>
    <lay-select-option :value="5" label="过去3天"></lay-select-option>
    <lay-select-option :value="6" label="过去7天"></lay-select-option>
  </lay-select>
  <lay-row space="10">
    <lay-col md="12" sm="12" xs="24">
      <div class="grid-demo">1</div>
    </lay-col>
    <lay-col md="12" sm="12" xs="24">
      <div class="grid1">2</div>
    </lay-col>
    <lay-col md="12" sm="12" xs="24">
      <div class="grid1">2</div>
    </lay-col>
    <lay-col md="12" sm="12" xs="24">
      <div class="grid1">1</div>
    </lay-col>
    <lay-col md="8" sm="12" xs="24">
      <div class="grid1">2</div>
    </lay-col>
    <lay-col md="8" sm="12" xs="24">
      <div class="grid1">2</div>
    </lay-col>
  </lay-row>
</template>

<script>
import {onMounted, ref} from "vue";
import {dashboard_overview} from "../../api/module/api";
import {Chart} from '@antv/g2';

export default {
  setup() {
    const flowInRef = ref()
    const flowOutRef = ref()
    const inflowRate = ref()
    const outflowRate = ref()
    const metric = ref({})

    //加载 JVM 相关配置
    const loadJvm = async () => {
      const {data} = await dashboard_overview();
      console.log(data.metricTO)
      metric.value = data.metricTO;
      // console.log(metric.value)
    };
    loadJvm()

    onMounted(() => {
      const flowInChartDom = flowInRef.value;
      const flowOutChartDom = flowOutRef.value;
      // @ts-ignore
      const data = [
        {
          "Data": "2023-01-01 19:02:00",
          "flowBytes": 10
        },
        {
          "Data": "2023-01-01 19:02:01",
          "flowBytes": 12
        },
        {
          "Data": "2023-01-01 19:14:00",
          "flowBytes": 15
        },
        {
          "Data": "2023-01-01 20:03:00",
          "flowBytes": 14
        },
        {
          "Data": "2023-01-01 21:03:00",
          "flowBytes": 14
        },
        {
          "Data": "2023-01-01 22:03:00",
          "flowBytes": 24
        }
      ];

      function flowChart(dom, data) {
        var chart = new Chart({
          container: dom,
          // forceFit: true,
          height: 250,
          autoFit: true,
          padding: [20, 20, 40, 50]
        });
        chart.data(data);
        chart.scale('Data', {
          range: [0, 1],
          mask: "YYYY-MM-DD HH:mm:ss",
          tickCount: 50,
          type: 'timeCat'
        });
        chart.axis('Data', {
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
        chart.axis('flowBytes', {
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
        // chart.annotation().text({
        //   content:"aaaa"
        // })

        chart.line().position('Data*flowBytes');
        chart.area().position('Data*flowBytes');
        chart.render();
      }

      flowChart(flowInChartDom, data);
      flowChart(flowOutChartDom, data);
    });

    return {
      metric,
      flowInRef,
      flowOutRef,
      inflowRate,
      outflowRate
    }
  }
}
</script>
<style>
.flowChart {
  width: 100%;
  /*height: 150px;*/
}

.grid-demo {
  padding: 10px;
  line-height: 50px;
  border-radius: 2px;
  text-align: center;
  background-color: var(--global-checked-color);
  color: #fff;
}

.card-demo {
  padding: 10px;
  line-height: 50px;
  border-radius: 2px;
  border: #5fb878;
  text-align: center;
  color: #000;
}

.agency {
  display: block;
  padding: 10.5px 16px;
  background-color: #f8f8f8;
  color: #999;
  border-radius: 2px;
}

.agency h3 {
  padding-bottom: 10px;
  font-size: 12px;
}

.agency p cite {
  font-style: normal;
  font-size: 30px;
  font-weight: 300;
  color: #009688;
}

</style>