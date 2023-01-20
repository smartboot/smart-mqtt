/**
 * 获取数据差异
 * 
 * @param arr1 数组
 * @param arr2 数组
 */
const diff = function (arr1: any[], arr2: any[]) {
    arr1 = Array.from(new Set(arr1)); 
    arr2 = Array.from(new Set(arr2)); 
    var mergeArr = arr1.concat(arr2);
    return mergeArr.filter((x) => !(arr1.includes(x) && arr2.includes(x)));
}

export { diff }