/**
 * 结果集
 * 
 * @property code 状态码
 * @property success 是否成功
 * @property msg 提示信息
 * @property data 携带数据 
 */
export interface Result {
    code: number;
    success: boolean;
    msg: string;
    data?: any;
}