// @ts-ignore
import Mock from 'mockjs';
import user from './user';

Mock.mock(/\/user\/login/,'post',(req: any,res: any) =>{
    return user.getLogin(req,res)
});

Mock.mock(/\/user\/info/,'post',(req: any,res: any) =>{
    return user.getInfo(req,res)
});

Mock.mock(/\/user\/menu/,'get',(req: any,res: any) =>{
    return user.getMenu(req,res)
});

Mock.mock(/\/user\/permission/,'get',(req: any,res: any) =>{
    return user.getPermission(req,res)
});

Mock.mock(/\/file\/upload/,'post',(req: any,res: any) =>{
    return user.getUpload(req,res)
}); 

export default Mock;