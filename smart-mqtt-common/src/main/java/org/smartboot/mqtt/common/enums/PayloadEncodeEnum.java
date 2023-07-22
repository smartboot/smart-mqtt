/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.enums;

public enum PayloadEncodeEnum {
    NONE("none"), STRING("string"), BASE64("base64");
    private final String code;

    PayloadEncodeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PayloadEncodeEnum getEnumByCode(String code) {
        for (PayloadEncodeEnum payloadEncodeEnum : values()) {
            if (payloadEncodeEnum.code.equals(code)) {
                return payloadEncodeEnum;
            }
        }
        return null;
    }
}
