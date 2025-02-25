package org.smartboot.mqtt.plugin.openapi.to;

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
}
