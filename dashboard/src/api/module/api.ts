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
