/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.dao.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tech.smartboot.mqtt.plugin.dao.model.ConnectionDO;
import tech.smartboot.mqtt.plugin.dao.query.ConnectionQuery;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/24
 */
@Mapper
public interface ConnectionMapper {
    /**
     * 条件查询订阅信息
     *
     * @return
     */
    @Select({"<script>",
            "select * from connection ",
            "WHERE 1=1",
            "<when test='clientId!=null and clientId!=\"\"'>",
            "AND clientId = #{clientId}",
            "</when>",
            "<when test='username!=null and username!=\"\"'>",
            "AND username = #{username}",
            "</when>",
            "<when test='ipAddress!=null and ipAddress!=\"\"'>",
            "AND ip_address = #{ipAddress}",
            "</when>",
            "<when test='connectEndTime!=null'>",
            "AND connect_time &lt; #{connectEndTime}",
            "</when>",
            "<when test='connectStartTime!=null'>",
            "AND connect_time > #{connectStartTime}",
            "</when>",
            "order by status,connect_time desc",
            "</script>"})
    @ResultType(ConnectionDO.class)
    @Results({
            @Result(column = "ip_address",property = "ipAddress"),
            @Result(column = "broker_ip",property = "brokerIp"),
            @Result(column = "connect_time",property = "connectTime")
    })
    List<ConnectionDO> select(ConnectionQuery query);

    /**
     * 新增订阅记录
     */
    @Insert("insert into connection(clientId,username,status,broker_ip,ip_address,keepalive,connect_time) values(#{clientId},#{username},#{status},#{brokerIp},#{ipAddress},#{keepalive},#{connectTime})")
    int insert(ConnectionDO connectionDO);

    @Update("update connection set status=#{status} where clientId=#{clientId}")
    int updateStatus(@Param("clientId") String clientId, @Param("status") String status);

    @Delete("delete from connection where clientId=#{clientId}")
    int deleteById(String clientId);
}
