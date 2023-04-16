import Http from '../http';

export const dashboard_overview = function () {
    return Http.post('/dashboard/overview')
}

export const dashboard_nodes = function () {
    return Http.post('/dashboard/nodes')
}

export const dashboard_metrics = function () {
    return Http.post('/dashboard/metrics')
}

export const connections = function (query: {}) {
    return Http.post('/connections', query)
}

/**
 * 获取所有mqtt broker
 */
export const brokers = function () {
    return Http.post('/brokers')
}

export const subscriptions_subscription = function (query: {}) {
    return Http.post('/subscriptions/subscription', query)
}

export const subscriptions_topics = function (query: {}) {
    return Http.post('/subscriptions/topics', query)
}
