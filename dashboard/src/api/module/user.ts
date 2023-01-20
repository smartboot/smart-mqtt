import Http from '../http';

export const login = function(loginForm: any) {
    return Http.post('/user/login', loginForm)
}

export const menu = function() {
    return Http.get('/user/menu') 
}

export const permission = function() {
    return Http.get('/user/permission') 
}