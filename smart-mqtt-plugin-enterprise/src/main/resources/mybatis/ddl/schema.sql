-- Broker集群节点
CREATE TABLE IF NOT EXISTS broker_node
(
    id               varchar(30) NOT NULL COMMENT '节点ID',
    node_type        varchar(20) NOT NULL COMMENT '节点类型：core:核心节点,worker:工作节点',
    core_node_id     varchar(30) COMMENT '核心节点ID',
    cluster_endpoint varchar(30) NOT NULL UNIQUE COMMENT '集群访问地址',
    process          text COMMENT '进程信息',
    config           text COMMENT '配置文件',
    status           varchar(30) NOT NULL COMMENT '状态',
    ip_address       varchar(20) NOT NULL COMMENT 'Broker IP地址',
    port             int         NOT NULL COMMENT 'Broker端口号',
    start_time       timestamp   NOT NULL COMMENT '启动时间',
    create_time      timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    edit_time        timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
);

-- 插件配置表
-- drop table plugin_config;
CREATE TABLE IF NOT EXISTS plugin_config
(
    id          int         NOT NULL COMMENT '主键ID' AUTO_INCREMENT,
    plugin_type varchar(32) NOT NULL COMMENT '插件类型',
    status      int         not null comment '插件状态：0:停止,1:启用',
    config      text        NOT NULL COMMENT '插件配置',
    create_time timestamp            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    edit_time   timestamp            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     tinyint(1)  NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS subscriptions
(
    clientId    varchar(32) NOT NULL COMMENT '客户端ID',
    topic       varchar(30) NOT NULL COMMENT '订阅主题',
    node_id     varchar(30) NOT NULL COMMENT 'Broker 节点ID',
    qos         int         NOT NULL COMMENT '消息质量',
    options     int         NULL COMMENT '订阅选项',
    create_time timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    edit_time   timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX       idx_topic (topic),
    INDEX       idx_node_id (node_id)
);

CREATE TABLE IF NOT EXISTS connection
(
    clientId     varchar(32) NOT NULL COMMENT '客户端ID',
    username     varchar(30) NULL COMMENT '用户名',
    status       varchar(30) NOT NULL COMMENT '状态',
    node_id      varchar(30) NOT NULL COMMENT 'Broker 节点ID',
    ip_address   varchar(20) NOT NULL COMMENT 'IP地址',
    keepalive    int         NOT NULL COMMENT '心跳',
    connect_time timestamp   NOT NULL COMMENT '连接时间',
    country      varchar(64) COMMENT '国家',
    region       varchar(64) COMMENT '区域',
    province     varchar(64) COMMENT '省份',
    city         varchar(64) COMMENT '城市',
    isp          varchar(64) COMMENT '运营商',
    create_time  timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    edit_time    timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX        idx_connection_node_id (node_id),
    INDEX        idx_country (country),
    INDEX        idx_region (region),
    INDEX        idx_province (province),
    INDEX        idx_city (city),
    INDEX        idx_connection_create_time(create_time),
    INDEX        idx_connection_edit_time (edit_time),
    INDEX        idx_isp (isp),
    PRIMARY KEY (clientId)
);

-- 运行指标
CREATE TABLE IF NOT EXISTS metric
(
    id          int         NOT NULL COMMENT '主键ID' AUTO_INCREMENT,
    node_name   varchar(30) NOT NULL COMMENT '节点名称',
    object_id   varchar(30) NOT NULL COMMENT '指标对象ID',
    object_type varchar(30) NOT NULL COMMENT '指标对象类型: Node,Client,Topic',
    code        varchar(30) NOT NULL COMMENT '指标编码',
    value       long         NOT NULL COMMENT '指标值',
    create_time timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    edit_time   timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX       idx_code (code),
    INDEX       idx_node_name (node_name),
    INDEX       idx_node_code (node_name, code),
    INDEX       idx_create_time (create_time)
);

-- 用户信息
CREATE TABLE IF NOT EXISTS user_info
(
    username    varchar(32)  NOT NULL COMMENT '用户名',
    password    varchar(128) NOT NULL COMMENT '密码',
    `desc`      varchar(256) COMMENT '备注',
    role        varchar(32) COMMENT '角色',
    create_time timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    edit_time   timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX       idx_user_password (username, password),
    PRIMARY KEY (username)
);

-- 系统配置信息
CREATE table if not exists system_config
(
    code        varchar(32) NOT NULL COMMENT '配置编码',
    value       text        NOT NULL COMMENT '配置值',
    create_time timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    edit_time   timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (code)
);

insert
ignore into user_info(username, password, role, `desc`)
values ('smart-mqtt', 'y+wTBgSKjuHynio1CCTNAU8Gdy0gEV9pbE0t6Cu1/zk=', 'admin', '超级账户');

insert
ignore into system_config(code,value)
values ('acl', '{"type":"none","version":1697441735422}');
insert
ignore into system_config(code,value)
values ('showMetrics', 'packets_publish_received,packets_publish_sent,topic_count,client_online,subscribe_relation,bytes_received,bytes_sent,packets_publish_rate');
insert
ignore into system_config(code,value)
values ('license',
        'c21hcnQtbGljZW5zZQAAAZRVLVqQAAABm6zdGlAAAAAgOGUwMTNmMzQ2NWZjYjQ0YjQ1OGMwMzcyY2E1ZmE5ZWUAAACiMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDTBUd3yy8G6A5zEEA40rPAYfDGbMsZpqmSy3Q0iMXANb5lhGDTaDu0jBM6lnCFakQroIiomTi1RZh4w4E3wgAvjydbWbZwoanAnEtm+pmNfprAHv/5Zw839mceuNIPyaFqSXCZmO6B7ZWHqkxcufWBmdUzkoHIu+xBuuu+Tmz+LQIDAQABAAAAFXNtYXJ0Ym9vdOW8gOa6kOe7hOe7hwAAABd6aGVuZ2p1bndlaW1haWxAMTYzLmNvbQAAAB6AUX8mUz4SycvMXsLb5DPy+5vuGpwofY96I3ESGg9oD5TpgDqK2IaYmJ6Hz2OmMk5MMEcormSRHLEUEn9P98hIJ64hdRd/GsLBolLQjlKpY/BdOK9MJNcfSnMx1Rh9i/24YRoLYiKPTfErsWQ6eLsAVoEdxbH2VMxWC4A3gHkNR4YAAAAAAAAAEICaGgqQChYKK8luIMiA13Lp2rx6s1iSut2lZromJc/n4jAV+8VnaWHIZ/o/L8ohVDR86DnEHrXLF4/exn1QUtM0jaun/pvNbtHrk661Pfc0ZvQoLJtMq4mt4vbDldQWLcIhuvddo+kPBoNbehq83N6CR/D2Yc8Ti+mV/6g3avAKAAAAAAAAAAAGgM2MPzhPWCjOciy/qv3jCZTGyo9TiFRhH5r4psXLZSPTGJpLIwl8cWGDOCACmyp1mO1TndCyHF0rgBiSQI8YAP6zVjbcGvBP1kpqHxZkWshzBx0RDfgtCEoyg7m6NT7XxjlyKzcC8HbFFpDVBw6Zplbd+vGmlU/wRMzMlAInZRQhAAAAAAAAABCAmaDNS/I3zmllX+CNja82xB/X7IK9gkue2Jw7ZAZMSg5RM/cPdzfmNd1IdIGY726iZyuGU9Z2fLoVOSXqg965EoCePb0axEbvqc18H0RZCoFNAawuCiSouvSyM+AEiIj+czE7z3BdyYlEJM3UufVRxOw8XZgutbPnMxvstoXAE2gAAAAAAAAAEIA0u0BTVA5nwmWKEzgikk5LNFnhgrwlU4mjkulHCfBjwWYVHHgPUWtzM5QSNY2tfC0eSBAXIGQyLCssE7hOAwpC6Kc6J2OQfOhrceK+no/HwOhcsUmHo77nflwwVxpVj3QxdG/Tk9dT5fstj1HMnMYNKC6IdnNxHXkqNOYlIMEokgAAAAAAAAAQgKqBLlyf1r/tnrRXCFFd8r5GzK+dIln4CPXB9PKoj9wTbPtwph8mn+A9iRO4HsT5O18jdLNdHYeW3GSiwdlSBhXSQdFbrDRdMQL382kMz1Th5cHZmz0hFkLhYNYAW21AypwWE/5A8SiEYt6lt0j0+U7vkXcXUJA989Jl2EchcZYaAAAAAAAAAAQA');
insert
ignore into system_config(code,value)
values ('connectRecord', 'db');
insert
ignore into system_config(code,value)
values ('subscribeRecord', 'db');
insert
ignore into system_config(code,value)
values ('metricRecord', 'db');