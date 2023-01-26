import {Result} from "../types/result";
import {User} from "../types/user";

let user: User = {
    'userId': '1992',
    'username': 'admin',
}

const menus = [
  {
    id: "/dashboard",
    icon: "layui-icon-chart-screen",
    title: "仪表盘",
    children:[
      {
        id: "/dashboard/overview",
        title: "概览"
      },
      {
        id: "/dashboard/nodes",
        title: "节点"
      },
      {
        id: "/dashboard/metrics",
        title: "指标"
      },
    ]
  },
  {
    id: "/connections",
    icon: "layui-icon-senior",
    title: "连接管理"
  },
  {
    id: "/subscriptions",
    icon: "layui-icon-note",
    title: "订阅管理",
    children:[
      {
        id: "/subscriptions/subscription",
        title: "订阅"
      },
      {
        id: "/subscriptions/topics",
        title: "主题"
      },
    ]
  },
  {
    id: "https://www.baidu.com",
    icon: "layui-icon-help",
    title: "帮助",
    type: "blank"
  },{
    id: "/page",
    icon: "layui-icon-home",
    title: "外链页面",
    children: [
      {
        id: "http://www.bing.com",
        icon: "layui-icon-home",
        title: "弹层外链",
        type: "modal"
      },
      {
        id: "http://www.baidu.com",
        icon: "layui-icon-home",
        title: "原生跳转",
        type: "blank"
      }
    ]
  }
]

const getInfo = (req: any, res: any)=> {
    let item = JSON.parse(req.body);
    let token = item ? item.token : null;
    let result:Result = {
      code: 200,
      msg: "操作成功",
      data: user,
      success: true
    }
    if(item || token){
      result.code = 99998;
      result.msg = "请重新登录";
      result.success = false;
    }
    return result;
}

const getPermission = (req: any, res: any)=> {
  let item = JSON.parse(req.body);
  let token = item ? item.token : null;
  let result:Result = {
    code: 200,
    msg: "操作成功",
    data: ['sys:user:add','sys:user:edit','sys:user:delete','sys:user:import','sys:user:export'],
    success: true
  }
  if(item || token){
    result.code = 99998;
    result.msg = "请重新登录";
    result.success = false;
  }
  return result;
}

const getMenu = (req: any, res: any)=> {
  let item = JSON.parse(req.body);
  let token = item ? item.token : null;
  let result:Result = {
    code: 200,
    msg: "操作成功",
    data: menus,
    success: true
  }
  if(item || token){
    result.code = 99998;
    result.msg = "请重新登录";
    result.success = false;
  }
  return result;
}

const getLogin = (req: any, res: any)=> {
  let item = JSON.parse(req.body);
  let account = item.account;
  let password = item.password;
  if(account === 'admin' && password === '123456'){
    return {
      'code': 200,
      'msg':'登陆成功',
      'data':{
        'userId':'35002',
        'token':'eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOiJhZG1pbiIsInVzZXJOYW1lIjoiYWRtaW4iLCJvcmdDb2RlIjoiMzUwMDAiLCJkZXB0Q29kZSI6IjM1MDAwIiwiYXVkIjoiYWRtaW4iLCJpc3MiOiJhZG1pbiIsImV4cCI6MTU5MzUzNTU5OH0.0pJAojRtT5lx6PS2gH_Q9BmBxeNlgBL37ABX22HyDlebbr66cCjVYZ0v0zbLO_9241FX9-FZpCkEqE98MQOyWw',
      }
    }
  }else{
    return {
      'code': 500,
      'msg':'登陆失败,账号密码不正确'
    }
  }
}

const getUpload = (req: any, res: any)=> {
  return {
    'code': 200,
    'msg':'上传成功',
    'success': true
  }
}

export default{
  getInfo, getMenu, getLogin, getPermission, getUpload
}