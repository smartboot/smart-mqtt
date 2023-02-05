<template>
  <lay-row space="10">

    <lay-col sm="1" md="1">
    </lay-col>
    <lay-col sm="22" md="22">
      <lay-layout class="example">
        <lay-header></lay-header>
        <lay-body>
          <lay-card>
            <template v-slot:title>
              ChatMQTT
            </template>
            <template v-slot:extra>
              ...
            </template>
            <template v-slot:body>
              <lay-scroll height="400px" style="background-color: whitesmoke" thumbColor="#000000">
                <lay-container>
                  <lay-row>
                    <lay-col span="24">
                      <lay-panel
                          v-for="(n, index) in messages"
                          :key="n"
                          style="margin: 10px; padding: 10px"
                      >
                        <lay-avatar
                            src="https://portrait.gitee.com/uploads/avatars/user/117/351975_smartdms_1578921064.jpg!avatar60"></lay-avatar>
                        ：
                        {{ n }}
                      </lay-panel
                      >
                    </lay-col>
                  </lay-row>
                </lay-container>
              </lay-scroll>
            </template>
          </lay-card>
        </lay-body>
        <lay-footer>

        </lay-footer>
      </lay-layout>

    </lay-col>
    <lay-col sm="1" md="1">
    </lay-col>
  </lay-row>

  <div style="width:100%;height:100px">
    <lay-row space="10">
      <lay-col sm="2" md="2">
      </lay-col>
      <lay-col sm="18" md="18">
        <lay-affix :offset="0" position="bottom">
          <lay-textarea placeholder="请输入描述" :rows="2" :cols="10" v-model.trim="message"></lay-textarea>
        </lay-affix>
      </lay-col>
      <lay-col sm="2" md="2">
        <lay-button type="normal" @click="sendMessage">发送消息</lay-button>
      </lay-col>
      <lay-col sm="2" md="2">
      </lay-col>
    </lay-row>
  </div>
</template>

<script lang="ts">
import {onMounted, ref} from 'vue'
import {onUnmounted} from "@vue/runtime-core";
import * as mqtt from 'mqtt/dist/mqtt.min';
// import * as mqtt from 'mqtt';
import {layer} from "@layui/layer-vue";

export default {
  setup() {
    const message_array: any[] = []
    const messages = ref(message_array)
    const message = ref();


    let client: any
    const topic = "/im"
    const sendMessage = async () => {
      // let { data, code, msg } = await login(loginForm);
      if (message.value == "") {
        layer.notifiy({
          title: "Error",
          content: "无法发送空内容",
          icon: 2
        })
      } else {
        let payload = {
          clientId: client.options.clientId,
          message: message.value
        }
        client.publish(topic, JSON.stringify(payload));
      }
    }

    onMounted(() => {
      client = mqtt.connect('ws://82.157.162.230:1884');
      client.on('connect', function () {
        client.subscribe(topic, function () {

        });
        console.log("aaaa")
      })
      client.on("message", function (topic: string, payload: string) {
        console.log("topic:" + topic)
        console.log("payload:" + payload)
        messages.value.push(JSON.parse(payload))
      })
    })

    onUnmounted(() => {
      client.end()
    })

    return {
      messages,
      message,
      sendMessage
    }
  }
}
</script>