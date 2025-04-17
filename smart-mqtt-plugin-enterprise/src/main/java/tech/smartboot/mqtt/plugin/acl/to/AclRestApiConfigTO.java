/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.acl.to;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class AclRestApiConfigTO {
    private String url;

    private String headers;

    private Map<String, String> headerMap;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public Map<String, String> headers() {
        if (headerMap == null) {
            headerMap = new HashMap<>();
            for (String header : StringUtils.split(headers, "\r\n")) {
                int index = StringUtils.indexOf(header, ':');
                if (index == -1) {
                    continue;
                }
                headerMap.put(StringUtils.substring(header, 0, index), StringUtils.substring(header, index + 1, header.length()));
            }
        }
        return headerMap;
    }
}
