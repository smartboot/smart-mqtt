import BaseLayout from '../../layouts/BaseLayout.vue';
import Login from '../../views/login/index.vue';


export default [
    {
        path: '/',
        redirect: '/dashboard'
    },
    {
        path: '/login',
        component: Login,
        meta: {title: '登录页面'},
    },
    {
        path: '/dashboard',
        redirect: "/dashboard/overview",
        component: BaseLayout,
        children: [
            {
                path: '/dashboard/overview',
                component: () => import('../../views/Dashboard/overview.vue'),
                meta: {title: '仪表盘', requireAuth: true},
            },
            {
                path: '/dashboard/nodes',
                component: () => import('../../views/Dashboard/nodes.vue'),
                meta: {title: '仪表盘', requireAuth: true},
            },
            {
                path: '/dashboard/metrics',
                component: () => import('../../views/Dashboard/metrics.vue'),
                meta: {title: '仪表盘', requireAuth: true},
            },
        ]

    },
    {
        path: '/connections',
        component: BaseLayout,
        children:[
            {
                path: '/connections',
                component: () => import('../../views/Connections/index.vue'),
                meta: {title: '连接管理', requireAuth: true},
            },
        ]
    },
    {
        path: '/subscriptions',
        redirect: "/subscriptions/subscription",
        component: BaseLayout,
        children: [
            {
                path: '/subscriptions/subscription',
                component: () => import('../../views/Subscriptions/subscription.vue'),
                meta: {title: '订阅', requireAuth: true},
            },
            {
                path: '/subscriptions/topics',
                component: () => import('../../views/Subscriptions/topics.vue'),
                meta: {title: '主题', requireAuth: true},
            },
        ]

    },{
        path: '/im',
        component: BaseLayout,
        children:[
            {
                path: '/im',
                component: () => import('../../views/im/index.vue'),
                meta: {title: 'ChatMQTT', requireAuth: true},
            },
        ]
    },
    // {
    //     path: '/chatGPT',
    //     component: BaseLayout,
    //     children:[
    //         {
    //             path: '/chatGPT',
    //             component: () => import('../../views/im/chatGPT.vue'),
    //             meta: {title: 'ChatGPT', requireAuth: true},
    //         },
    //     ]
    // }
]
