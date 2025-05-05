/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.openapi.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.InterceptorMapping;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.Session;
import tech.smartboot.feat.router.Interceptor;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.convert.UserConvert;
import tech.smartboot.mqtt.plugin.dao.mapper.SystemConfigMapper;
import tech.smartboot.mqtt.plugin.dao.mapper.UserMapper;
import tech.smartboot.mqtt.plugin.dao.model.UserDO;
import tech.smartboot.mqtt.plugin.dao.query.UserQuery;
import tech.smartboot.mqtt.plugin.openapi.OpenApi;
import tech.smartboot.mqtt.plugin.openapi.to.Pagination;
import tech.smartboot.mqtt.plugin.openapi.to.SettingsTO;
import tech.smartboot.mqtt.plugin.openapi.to.UserTO;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.utils.SecureUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 5/1/23
 */
@Controller
public class SystemController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemController.class);

    @Autowired
    private UserMapper userMapper;

    private boolean licenseExpire;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private BrokerContext brokerContext;

    @PostConstruct
    public void init() {
        brokerContext.getTimer().scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                String value = systemConfigMapper.getConfig("license");
                if (StringUtils.isBlank(value)) {
                    licenseExpire = true;
                }
            }
        }, 5, TimeUnit.SECONDS);

    }

//    @Interceptor(patterns = {"/**"})
//    public void cross(HttpRequest request, HttpResponse response) {
//        response.setHeader("Access-Control-Allow-Origin", "*");
//        response.setHeader("Access-Control-Allow-Headers", "*");
//    }

    @InterceptorMapping(OpenApi.BASE_API + "/*")
    public Interceptor sessionCheck() {
        return (context, completableFuture, chain) -> {
            String uri = context.Request.getRequestURI();
            if (StringUtils.equals(uri, OpenApi.USER_LOGIN)) {
                chain.proceed(context, completableFuture);
                return;
            }
            Session session = context.session();
            if (session.get("username") == null) {
                context.Response.setHttpStatus(HttpStatus.UNAUTHORIZED);
                completableFuture.complete(null);
            } else {
                chain.proceed(context, completableFuture);
            }
        };
    }

    @RequestMapping(OpenApi.BASE_API + "/user/login")
    public RestResult<UserTO> login(@Param("username") String username, @Param("password") String password, Session session, HttpResponse response) {
        UserDO userDO = userMapper.getUser(username, SecureUtil.shaEncrypt(password));
        if (userDO == null) {
            return RestResult.fail("无效账户名或密码");
        }
        UserTO userTO = new UserTO();
        userTO.setUsername(username);
        session.put("username", username);
        return RestResult.ok(userTO);
    }

    /**
     * 用户列表
     *
     * @return
     */
    @RequestMapping(OpenApi.SYSTEM_USER_LIST)
    public RestResult<Pagination<UserTO>> userList(UserQuery query) {
        PageHelper.offsetPage((query.getPageNo() - 1) * query.getPageSize(), query.getPageSize());
        Page<UserDO> list = (Page<UserDO>) userMapper.getUserList(query);
        Pagination<UserTO> pagination = new Pagination<>();
        pagination.setPageSize(query.getPageSize());
        pagination.setTotal(list.getTotal());
        pagination.setList(UserConvert.convert(list));
        return RestResult.ok(pagination);
    }

    /**
     * 新增用户
     *
     * @return
     */
    @RequestMapping(OpenApi.SYSTEM_USER_ADD)
    public RestResult<Void> userAdd(UserDO userDO) {
        ValidateUtils.notNull(userDO, "");
        userDO.setPassword(SecureUtil.shaEncrypt(userDO.getPassword()));
        userMapper.insert(userDO);
        return RestResult.ok(null);
    }

    /**
     * 删除用户
     *
     * @return
     */
    @RequestMapping(OpenApi.SYSTEM_USER_DELETE)
    public RestResult<Void> deleteUsers(@Param("users") List<String> users) {
        userMapper.deleteUsers(users);
        return RestResult.ok(null);
    }

    /**
     * 获取系统配置
     *
     * @return
     */
    @RequestMapping(OpenApi.SYSTEM_SETTINGS_GET)
    public RestResult<SettingsTO> getSettings() {
        SettingsTO settingsTO = new SettingsTO();
        JSONObject jsonObject = (JSONObject) JSON.toJSON(settingsTO);
        jsonObject.keySet().forEach(key -> {
            String v = systemConfigMapper.getConfig(key);
            jsonObject.put(key, v);
        });
        LOGGER.info("settings:{}", jsonObject);
        return RestResult.ok(jsonObject.to(SettingsTO.class));
    }

    /**
     * 保存系统配置
     *
     * @return
     */
    @RequestMapping(OpenApi.SYSTEM_SETTINGS_SAVE)
    public RestResult<Void> saveSettings(SettingsTO settings) {
        JSONObject jsonObject = (JSONObject) JSON.toJSON(settings);
        jsonObject.keySet().forEach(key -> {
            String oldValue = systemConfigMapper.getConfig(key);
            String newValue = jsonObject.getString(key);
            if (StringUtils.isBlank(jsonObject.getString(key))) {
                newValue = "";
            }
            if (oldValue == null) {
                systemConfigMapper.insert(key, newValue);
            } else {
                systemConfigMapper.update(key, newValue);
            }
        });
        LOGGER.info("save settings:{}", JSONObject.toJSONString(settings));
        return RestResult.ok(null);
    }

    public void setUserMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public void setSystemConfigMapper(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }
}
