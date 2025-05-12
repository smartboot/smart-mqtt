/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.openapi.to;

public class LicenseTO {
    private final long startTime = System.currentTimeMillis();
    private String sn;
    /**
     * 试用时长
     */
    private int trialDuration;
    /**
     * 授权对象
     */
    private String applicant;

    /**
     * 过期时间
     */
    private long expireTime;
    /**
     * 可用额度
     */
    private String limit;
    /**
     * 剩余额度
     */
    private String available;

    public String getApplicant() {
        return applicant;
    }

    public void setApplicant(String applicant) {
        this.applicant = applicant;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public int getTrialDuration() {
        return trialDuration;
    }

    public void setTrialDuration(int trialDuration) {
        this.trialDuration = trialDuration;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getLimit() {
        return limit;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    public String getAvailable() {
        return available;
    }

    public void setAvailable(String available) {
        this.available = available;
    }
}
