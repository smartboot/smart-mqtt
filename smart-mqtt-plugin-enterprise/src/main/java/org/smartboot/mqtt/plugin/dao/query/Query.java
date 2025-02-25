package org.smartboot.mqtt.plugin.dao.query;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/11
 */
public class Query {
    private int pageSize = 10;

    private int pageNo = 1;

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }
}
