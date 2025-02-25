package org.smartboot.mqtt.plugin.openapi.controller;

import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.util.ValidateUtils;
import org.smartboot.mqtt.plugin.dao.mapper.SystemConfigMapper;
import org.smartboot.mqtt.plugin.openapi.OpenApi;
import org.smartboot.mqtt.plugin.openapi.enums.SystemConfigEnum;
import org.smartboot.mqtt.plugin.utils.SecureUtil;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 5/1/23
 */
@Controller(async = true)
public class AclController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AclController.class);

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @RequestMapping(OpenApi.BASE_API + "/acl/get")
    public RestResult<JSONObject> getAclConfig() {
        String value = systemConfigMapper.getConfig(SystemConfigEnum.ACL.getCode());
        if (StringUtils.isBlank(value)) {
            return RestResult.ok(null);
        }
        return RestResult.ok(JSONObject.parseObject(value));
    }

    @RequestMapping(OpenApi.BASE_API + "/acl/save")
    public RestResult<Void> saveAclConfig(JSONObject object) {
        System.out.println(object);
        String value = systemConfigMapper.getConfig(SystemConfigEnum.ACL.getCode());
        if (StringUtils.isBlank(value)) {
            systemConfigMapper.insert(SystemConfigEnum.ACL.getCode(), object.toString());
        } else {
            systemConfigMapper.update(SystemConfigEnum.ACL.getCode(), object.toString());
        }
        return RestResult.ok(null);
    }

    @RequestMapping(OpenApi.BASE_API + "/acl/secret")
    public RestResult<String> secret(@Param("password") String password, @Param("alg") String alg) {
        ValidateUtils.notBlank(password, "密码不能为空");
        ValidateUtils.notBlank(alg, "加密方式不能为空");
        if (StringUtils.equals("md5", alg)) {
            return RestResult.ok(SecureUtil.md5(password));
        } else if (StringUtils.equals("sha256", alg)) {
            return RestResult.ok(SecureUtil.shaEncrypt(password));
        }
        return RestResult.fail("无效加密方式");
    }

    @RequestMapping(OpenApi.BASE_API + "/acl/test/ok")
    public RestResult<Void> testOk(HttpRequest request) {
        LOGGER.info("ack test: ok,headers");
        request.getHeaderNames().forEach(name -> {
            LOGGER.info("header name:{} ,value:{}", name, request.getHeader(name));
        });
        request.getParameters().keySet().forEach(key -> {
            LOGGER.info("param name:{} ,value:{}", key, request.getParameter(key));
        });

        return RestResult.ok(null);
    }

    @RequestMapping(OpenApi.BASE_API + "/acl/test/fail")
    public RestResult<Void> testFail(HttpResponse response) {
        response.setHttpStatus(HttpStatus.UNAUTHORIZED);
        LOGGER.warn("ack test: fail");
        return RestResult.fail("ackFail");
    }

    @RequestMapping(OpenApi.BASE_API + "/acl/test/404")
    public RestResult<Void> testNotFound(HttpResponse response) {
        response.setHttpStatus(HttpStatus.NOT_FOUND);
        LOGGER.warn("ack test: not found");
        return RestResult.fail("notFound");
    }

    public void setSystemConfigMapper(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }
}
