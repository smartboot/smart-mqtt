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
import tech.smartboot.mqtt.plugin.dao.model.BrokerNodeDO;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/4
 */
@Mapper
public interface BrokerNodeMapper {


    /**
     * 查询指定节点信息
     */
    @Select("select * from broker_node where id=#{nodeId}")
    @ResultType(BrokerNodeDO.class)
    @Results({
            @Result(column = "id", property = "nodeId"),
            @Result(column = "node_type", property = "nodeType"),
            @Result(column = "core_node_id", property = "coreNodeId"),
            @Result(column = "cluster_endpoint", property = "clusterEndpoint"),
            @Result(column = "ip_address", property = "ipAddress"),
            @Result(column = "edit_time", property = "editTime"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "start_time", property = "startTime")
    })
    BrokerNodeDO selectById(@Param("nodeId") String nodeId);

    /**
     * 查询所有broker
     */
    @Select("select * from broker_node order by status")
    @ResultType(BrokerNodeDO.class)
    @Results({
            @Result(column = "id", property = "nodeId"),
            @Result(column = "node_type", property = "nodeType"),
            @Result(column = "core_node_id", property = "coreNodeId"),
            @Result(column = "cluster_endpoint", property = "clusterEndpoint"),
            @Result(column = "ip_address", property = "ipAddress"),
            @Result(column = "edit_time", property = "editTime"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "start_time", property = "startTime")
    })
    List<BrokerNodeDO> selectAll();

    @Select("select * from broker_node where node_type='core' or core_node_id=#{codeNodeId}")
    @ResultType(BrokerNodeDO.class)
    @Results({
            @Result(column = "id", property = "nodeId"),
            @Result(column = "node_type", property = "nodeType"),
            @Result(column = "core_node_id", property = "coreNodeId"),
            @Result(column = "cluster_endpoint", property = "clusterEndpoint"),
            @Result(column = "ip_address", property = "ipAddress"),
            @Result(column = "edit_time", property = "editTime"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "start_time", property = "startTime")
    })
    List<BrokerNodeDO> selectCoordinationConnections(@Param("codeNodeId") String codeNodeId);

    @Select("select * from broker_node where node_type='core' and core_node_id=#{coreNodeId}")
    @ResultType(BrokerNodeDO.class)
    @Results({
            @Result(column = "id", property = "nodeId"),
            @Result(column = "node_type", property = "nodeType"),
            @Result(column = "core_node_id", property = "coreNodeId"),
            @Result(column = "cluster_endpoint", property = "clusterEndpoint"),
            @Result(column = "ip_address", property = "ipAddress"),
            @Result(column = "edit_time", property = "editTime"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "start_time", property = "startTime")
    })
    BrokerNodeDO selectWorkerConnection(@Param("coreNodeId") String coreNodeId);

    @Select("select count(*) from broker_node")
    int count();

    /**
     * 删除指定节点信息
     */
    @Delete("delete from broker_node where id=#{nodeId}")
    void deleteById(@Param("nodeId") String id);


    /**
     * 新增节点信息
     */
    @Insert("insert into broker_node(id,node_type,core_node_id,config,cluster_endpoint,status,ip_address,port,start_time) " +
            "values(#{nodeId},#{nodeType},#{coreNodeId},#{config},#{clusterEndpoint},#{status},#{ipAddress},#{port},now())")
    int insert(BrokerNodeDO brokerNodeDO);

    @Update("<script>update broker_node <trim prefix='set' suffixOverrides=','> " +
            "<if test='status!=null'>status=#{status},</if>" +
            "<if test='config!=null'>config=#{config},</if>" +
            "<if test='process!=null'>process=#{process},</if>" +
            "<if test='coreNodeId!=null'>core_node_id=#{coreNodeId},</if>" +
            "<if test='clusterEndpoint!=null'>cluster_endpoint=#{clusterEndpoint},</if>" +
            "<if test='startTime!=null'>start_time=#{startTime},</if>" +
            "</trim> where id=#{nodeId}</script>")
    int update(BrokerNodeDO brokerNodeDO);
}
