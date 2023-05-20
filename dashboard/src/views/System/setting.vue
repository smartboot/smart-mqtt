<!--
  - Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
  -
  -  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
  -
  -  Enterprise users are required to use this project reasonably
  -  and legally in accordance with the AGPL-3.0 open source agreement
  -  without special permission from the smartboot organization.
  -->

<template>
    <lay-card>
        <lay-form :model="model">

            <lay-form-item label="语言" label-position="top" prop="language" >
                <lay-select v-model="model.language" placeholder="请选择">
                    <lay-select-option value="Chinese" label="简体中文"></lay-select-option>
                    <lay-select-option value="English" label="English"></lay-select-option>
                </lay-select>
                选择Dashboard页面内展示的语言
            </lay-form-item>
<!--            <lay-form-item label="主题" label-position="top" prop="theme" >-->
<!--                选择Dashboard的主题色-->
<!--                <lay-select v-model="model.theme" placeholder="请选择">-->
<!--                    <lay-select-option :value="1" label="中文"></lay-select-option>-->
<!--                    <lay-select-option :value="2" label="English"></lay-select-option>-->
<!--                </lay-select>-->
<!--            </lay-form-item>-->
            <lay-button type="primary" @click="submitClick">提交</lay-button>
            <lay-button type="default" @click="">重置</lay-button>
        </lay-form>
    </lay-card>
</template>

<script>
import {reactive} from 'vue'
import {layer} from '@layui/layer-vue'

export default {
    setup() {

        const model = reactive({
            language: "Chinese",
            theme: "1",
        })

        const submitClick = function () {
            layer.open({
                type: 1,
                title: "表单结果",
                content: `<div style="padding: 10px">${JSON.stringify(model)}</div>`,
                shade: false,
                isHtmlFragment: true,
                btn: [{
                    text: '确认', callback(index) {
                        layer.close(index)
                    }
                }],
                area: '500px'
            });
        };

        return {
            model,
            submitClick
        }
    }
}
</script>