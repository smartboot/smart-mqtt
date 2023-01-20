<template>
  <lay-container :fluid="true" style="padding: 10px">
    <lay-row :space="10">

    </lay-row>
  </lay-container>
  <lay-row space="10">
    <lay-col md="12" sm="12" xs="24">
      <lay-row space="10">
        <lay-col md="24" sm="24" xs="24">
          <p class="agency">消息流入速率： {{ inflowRate }} 条/秒</p>
        </lay-col>
        <lay-col md="24" sm="24" xs="24">
          <p class="agency">消息流出速率： {{ outflowRate }} 条/秒</p>
        </lay-col>
      </lay-row>

    </lay-col>
    <lay-col md="12" sm="12" xs="24">
      <lay-row space="10">
        <lay-card>
          <template #title> 资源指标 </template>
          <lay-row :space="10">
            <lay-col :md="8">
              <a class="agency">
                <h3>连接数</h3>
                <p>
                  <cite>
                    <lay-count-up :end-val="18" :duration="2000"></lay-count-up>
                  </cite>
                </p>
              </a>
            </lay-col>
            <lay-col :md="8">
              <a class="agency">
                <h3>主题数</h3>
                <p>
                  <cite>
                    <lay-count-up :end-val="18" :duration="2000"></lay-count-up>
                  </cite>
                </p>
              </a>
            </lay-col><lay-col :md="8">
            <a class="agency">
              <h3>订阅数</h3>
              <p>
                <cite>
                  <lay-count-up :end-val="18" :duration="2000"></lay-count-up>
                </cite>
              </p>
            </a>
          </lay-col>
          </lay-row>
        </lay-card>
      </lay-row>
    </lay-col>
  </lay-row>
  <lay-row space="10">
    <lay-col md="24" sm="24" xs="24"><div class="grid-demo" style="height: 200px">3</div></lay-col>
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
    <lay-col md="12" sm="12" xs="24"><div class="grid1">2</div></lay-col>
    <lay-col md="12" sm="12" xs="24"><div class="grid1">2</div></lay-col>
    <lay-col md="12" sm="12" xs="24"><div class="grid1">1</div></lay-col>
    <lay-col md="8" sm="12" xs="24"><div class="grid1">2</div></lay-col>
    <lay-col md="8" sm="12" xs="24"><div class="grid1">2</div></lay-col>
  </lay-row>
</template>

<script>
import {ref} from 'vue'

export default {
  setup() {

    const current3 = ref("1");
    const inflowRate = 10;
    const outflowRate = 20;

    return {
      current3,
      inflowRate,
      outflowRate
    }
  }
}
</script>
<style>
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
.agency  h3 {
    padding-bottom: 10px;
    font-size: 12px;
  }

.agency  p cite {
    font-style: normal;
    font-size: 30px;
    font-weight: 300;
    color: #009688;
  }

</style>