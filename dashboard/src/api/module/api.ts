import Http from '../http';

export const dashboard_overview = function () {
    return Http.post('/dashboard/overview')
}
