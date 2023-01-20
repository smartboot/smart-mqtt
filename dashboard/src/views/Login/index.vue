<template>
  <div class="login-wrap">
    <div class="login-root">
      <div class="login-main">
        <img class="login-one-ball"
          src="https://assets.codehub.cn/micro-frontend/login/fca1d5960ccf0dfc8e32719d8a1d80d2.png" />
        <img class="login-two-ball"
          src="https://assets.codehub.cn/micro-frontend/login/4bcf705dad662b33a4fc24aaa67f6234.png" />
        <div class="login-container">
          <lay-tab type="brief" v-model="method">
            <lay-tab-item title="用户名" id="1">
              <lay-form-item :label-width="0">
                <lay-input placeholder="用户名" v-model="loginForm.account"></lay-input>
              </lay-form-item>
              <lay-form-item :label-width="0">
                <lay-input placeholder="密码" password type="password" v-model="loginForm.password"></lay-input>
              </lay-form-item>
              <lay-form-item :label-width="0">
                <lay-checkbox name="like" v-model="remember" skin="primary" label="1">记住密码</lay-checkbox>
              </lay-form-item>
              <lay-form-item :label-width="0">
                <lay-button type="primary" fluid="true" @click="loginSubmit">登录</lay-button>
              </lay-form-item>
            </lay-tab-item>
            <lay-tab-item title="二维码" id="2">
              <div>选项二</div>
            </lay-tab-item>
          </lay-tab>
          <lay-line>Other login methods</lay-line>
          <ul class="other-ways">
            <li>
              <div class="line-container">
                <img class="icon" src="../../assets/login/w.svg" />
                <p class="text">微信</p>
              </div>
            </li>
            <li>
              <div class="line-container">
                <img class="icon" src="../../assets/login/q.svg" />
                <p class="text">钉钉</p>
              </div>
            </li>
            <li>
              <div class="line-container">
                <img class="icon" src="../../assets/login/a.svg" />
                <p class="text">Gitee</p>
              </div>
            </li>
            <li>
              <div class="line-container">
                <img class="icon" src="../../assets/login/f.svg" />
                <p class="text">Github</p>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {login} from '../../api/module/user';
import {defineComponent, reactive, ref} from "vue";
import {useRouter} from "vue-router";
import {useUserStore} from '../../store/user';
import {layer} from "@layui/layer-vue";

export default defineComponent({
  setup() {
    const router = useRouter();
    const userStore = useUserStore();
    const method = ref("1");
    const remember = ref(false);
    const loginForm = reactive({account:"admin",password:"123456"})

    const loginSubmit = async () => {
      let { data, code, msg } = await login(loginForm);
      if (code == 200) {
        layer.msg(msg, { icon: 1 }, async () => {
          userStore.token = data.token;
          await userStore.loadMenus();
          await userStore.loadPermissions();
          router.push('/');
        })
      } else {
        layer.msg(msg, { icon: 2 })
      }
    }
    return {
      loginSubmit,
      loginForm,
      remember,
      method,
    }
  },
});
</script>

<style scoped>
.login-one-ball {
  opacity: 0.4;
  position: absolute;
  max-width: 568px;
  left: -400px;
  bottom: 0px;
}

.login-two-ball {
  opacity: 0.4;
  position: absolute;
  max-width: 320px;
  right: -200px;
  top: -60px;
}

.login-wrap {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  right: 0;
  overflow: auto;
  z-index: 9;
  background-image: url(https://assets.codehub.cn/micro-frontend/login/f7eeecbeccefe963298c23b54741d473.png);
  background-repeat: no-repeat;
  background-size: cover;
  min-height: 100vh;
}

.login-wrap :deep(.layui-input-block) {
  margin-left: 0!important;
}

.login-root {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  justify-content: center;
  width: 100%;
  min-width: 320px;
  background-color: initial;
}

.login-main {
  position: relative;
  display: block;
}

.logo-container {
  max-width: calc(100vw - 28px);
  margin-bottom: 40px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-container .logo {
  display: inline-block;
  height: 30px;
  width: 143px;
  background: url() no-repeat 50%;
  background-size: contain;
  cursor: pointer;
}

.login-container {
  padding: 30px;
  position: relative;
  overflow: hidden;
  width: 400px;
  max-width: calc(100vw - 28px);
  border: 1.5px solid #fff;
  border-radius: 8px;
  background: hsla(0, 0%, 100%, 0.5);
  backdrop-filter: blur(30px);
}

.login-container .layui-tab-head {
  background: transparent;
}

.login-container .layui-input-wrapper {
  margin-top: 10px;
  margin-bottom: 10px;
}

.login-container .layui-input-wrapper {
  margin-top: 12px;
  margin-bottom: 12px;
}

.login-container .assist {
  margin-top: 5px;
  margin-bottom: 5px;
  letter-spacing: 2px;
}

.login-container .layui-btn {
  margin: 10px 0px 10px 0px;
  letter-spacing: 2px;
  height: 40px;
}

.login-container .layui-line-horizontal { 
  letter-spacing: 2px;
  margin-bottom: 34px;
  margin-top: 24px;
}

.other-ways {
  display: flex;
  justify-content: space-between;
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 14px;
  font-weight: 400;
}

.other-ways li {
  width: 100%;
}

.line-container {
  justify-content: center;
  align-items: center;
  text-align: center;
  cursor: pointer;
}

.line-container .icon {
  height: 28px;
  width: 28px;
  margin-right: 0;
  vertical-align: middle;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 2px 0 rgb(9 30 66 / 4%), 0 1px 4px 0 rgb(9 30 66 / 10%), 0 0 1px 0 rgb(9 30 66 / 10%);
}

.line-container .text {
  display: block;
  margin: 12px 0 0;
  font-size: 12px;
  color: #8592a6;
}

:deep(.layui-tab-title .layui-this) {
  background-color: transparent;
}
</style>
