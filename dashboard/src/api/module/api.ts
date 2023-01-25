import Http from '../http';

export const dashboard_overview = function () {
    return Http.post('/dashboard/overview')
}

export const dashboard_nodes = function () {
    return Http.post('/dashboard/nodes')
}

export const connections = function () {
    return Http.post('/connections')
}

export const subscriptions_subscription = function () {
    return Http.post('/subscriptions/subscription')
}

export const subscriptions_topics = function () {
    return Http.post('/subscriptions/topics')
}
