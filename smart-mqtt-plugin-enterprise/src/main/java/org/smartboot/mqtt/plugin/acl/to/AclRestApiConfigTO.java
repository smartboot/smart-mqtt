package org.smartboot.mqtt.plugin.acl.to;

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
