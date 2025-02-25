package org.smartboot.mqtt.plugin.openapi.controller;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.license.client.License;
import org.smartboot.license.client.LicenseEntity;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.plugin.dao.mapper.SystemConfigMapper;
import org.smartboot.mqtt.plugin.openapi.OpenApi;
import org.smartboot.mqtt.plugin.openapi.enums.SystemConfigEnum;
import org.smartboot.mqtt.plugin.openapi.to.LicenseTO;
import org.smartboot.mqtt.plugin.utils.Streams;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.multipart.Part;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;

@Controller(async = true)
public class LicenseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseController.class);

    @Autowired
    private BrokerContext brokerContext;

    @Autowired
    private SystemConfigMapper systemConfigMapper;
    @Autowired
    private License license;

    private LicenseTO licenseTO;

    /**
     * 初始化LICENSE实例
     */
    @Bean
    public License license() {
        return new License(entity -> {
            LOGGER.info("企业版License已过期,停止进程后将无法启动成功，请先联系smart-mqtt团队更新授权.");
        }, entity -> {
            if (entity == license.getEntity()) {
                LOGGER.info("The trial version License has expired.");
                systemConfigMapper.update(SystemConfigEnum.LICENSE.getCode(), "");
                licenseTO = null;
            }
        }, 10000);
    }

    @PostConstruct
    public void init() {
        String value = systemConfigMapper.getConfig(SystemConfigEnum.LICENSE.getCode());
        if (StringUtils.isBlank(value)) {
            LOGGER.error("none valid license");
            return;
        }

        try {
            LicenseEntity entity = license.loadLicense(Base64.getDecoder().decode(value));
            LicenseTO licenseTO = loadLicense(entity);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            System.out.println("::  Licensed to \033[4m" + licenseTO.getApplicant() + ConsoleColors.RESET + " until \033[4m" + sdf.format(new Date(licenseTO.getExpireTime())) + ConsoleColors.RESET);
            System.out.println("::  License issuer: " + entity.getApplicant() + " contact: " + entity.getContact());
        } catch (Exception e) {
            LOGGER.error("load license exception", e);
        }
    }

    private LicenseTO loadLicense(LicenseEntity entity) throws IOException {
        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(entity.getData()));
        licenseTO = new LicenseTO();
        licenseTO.setApplicant(properties.getProperty("enterprise.license.user"));
        licenseTO.setSn(properties.getProperty("enterprise.license.number"));
        licenseTO.setExpireTime(entity.getExpireTime());
        licenseTO.setTrialDuration(entity.getTrialDuration());
        return licenseTO;
    }


    @RequestMapping(OpenApi.LICENSE_IMPORT)
    public RestResult<Void> importLicense(HttpRequest request, HttpResponse response) throws IOException {
        try {
            for (Part part : request.getParts()) {
                if (part.getSubmittedFileName() == null) {
                    System.out.println("Form field " + part.getName() + " with value " + Streams.asString(part.getInputStream()) + " detected.");
                } else {
                    System.out.println("File field " + part.getName() + " with file name " + part.getSubmittedFileName() + " detected.");
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    Streams.copy(part.getInputStream(), byteArrayOutputStream, false);
                    byte[] bytes = byteArrayOutputStream.toByteArray();
                    loadLicense(license.getEntity() == null ? license.loadLicense(byteArrayOutputStream.toByteArray()) : license.replace(byteArrayOutputStream.toByteArray()));
                    String value = systemConfigMapper.getConfig(SystemConfigEnum.LICENSE.getCode());
                    if (value == null) {
                        systemConfigMapper.insert(SystemConfigEnum.LICENSE.getCode(), Base64.getEncoder().encodeToString(bytes));
                    } else {
                        systemConfigMapper.update(SystemConfigEnum.LICENSE.getCode(), Base64.getEncoder().encodeToString(bytes));
                    }

                }
            }


        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return RestResult.fail(throwable.getMessage());
        }
        return RestResult.ok(null);
    }

    @RequestMapping(OpenApi.LICENSE_GET)
    public RestResult<LicenseTO> getLicense(HttpResponse response) {
        //校验Licensee
        if (licenseTO == null || licenseTO.getExpireTime() < System.currentTimeMillis()) {
            response.setHttpStatus(HttpStatus.FORBIDDEN);
            return RestResult.fail("license expire");
        } else {
            return RestResult.ok(licenseTO);
        }
    }


    static class ConsoleColors {
        /**
         * 重置颜色
         */
        public static final String RESET = "\033[0m";
        /**
         * 蓝色
         */
        public static final String BLUE = "\033[34m";

        /**
         * 红色
         */
        public static final String RED = "\033[31m";

        /**
         * 绿色
         */
        public static final String GREEN = "\033[32m";

    }

    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    public void setSystemConfigMapper(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    public void setLicense(License license) {
        this.license = license;
    }
}
