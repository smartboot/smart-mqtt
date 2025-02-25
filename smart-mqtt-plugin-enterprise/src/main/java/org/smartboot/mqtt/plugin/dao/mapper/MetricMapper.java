package org.smartboot.mqtt.plugin.dao.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.smartboot.mqtt.plugin.dao.model.MetricDO;

import java.util.Date;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/4
 */
@Mapper
public interface MetricMapper {

    /**
     * 新增指标值
     */
    @Insert("insert into metric(node_name,object_id,object_type,code,value,create_time) values(#{nodeName},#{objectId},#{objectType},#{code},#{value},now())")
    int insert(MetricDO metricDO);

    /**
     * 清理指定时间之前的数据
     */
    @Delete("delete from metric where create_time<#{createTime}")
    int deleteBefore(@Param("createTime") Date createTime);

    @Delete("delete from metric where create_time<#{createTime} and value=0")
    int clearBefore(@Param("createTime") Date createTime);

    @Select("<script>select * from METRIC where id in (select  MAX(id) time from METRIC where NODE_NAME=#{nodeName} and CODE in" +
            "   <foreach collection='codes' item='code' index='index'" +
            "          open='(' close=')' separator=','>" +
            "      #{code}" +
            "   </foreach> and create_time> #{createTime}" +
            "group by  CODE)" +
            "</script>")
    @ResultType(MetricDO.class)
    @Results({
            @Result(column = "node_name", property = "nodeName"),
            @Result(column = "object_type", property = "objectType"),
            @Result(column = "object_id", property = "objectId"),
            @Result(column = "create_time", property = "createTime")
    })
    List<MetricDO> selectLatest(@Param("nodeName") String nodeName, @Param("codes") List<String> codes, @Param("createTime") Date createTime);

    @Select("<script>SELECT node_name, DATE_SUB(create_time,INTERVAL unix_timestamp(create_time)%#{step} SECOND) as metric_time, max(value) as value , code FROM metric where code in" +
            "   <foreach collection='codes' item='code' index='index'" +
            "          open='(' close=')' separator=','>" +
            "      #{code}" +
            "   </foreach> and node_name=#{nodeName} and create_time> #{beginCreateTime} and create_time &lt; #{endCreateTime}  " +
            "group by code,node_name,metric_time" +
            "</script>")
    // h2数据库
//    @Select("<script>SELECT node_name, DATEADD('SECOND',-unix_timestamp(create_time)%#{step} ,create_time) as metric_time, max(value) as value , code FROM metric where code in" +
//            "   <foreach collection='codes' item='code' index='index'" +
//            "          open='(' close=')' separator=','>" +
//            "      #{code}" +
//            "   </foreach> and create_time> #{beginCreateTime} and create_time &lt; #{endCreateTime}  " +
//            "group by code,node_name,metric_time" +
//            "</script>")
    @Result(property = "nodeName", column = "node_name")
    @Result(property = "objectType", column = "object_type")
    @Result(property = "objectId", column = "object_id")
    @Result(property = "createTime", column = "metric_time")
    List<MetricDO> selectMetrics(@Param("nodeName") String nodeName, @Param("codes") List<String> codes, @Param("step") long step, @Param("beginCreateTime") Date beginCreateTime, @Param("endCreateTime") Date endCreateTime);


    // h2数据库
    @Select("<script>SELECT node_name, DATEADD('SECOND',-unix_timestamp(create_time)%#{step} ,create_time) as metric_time, max(value) as value , code FROM metric where code in" +
            "   <foreach collection='codes' item='code' index='index'" +
            "          open='(' close=')' separator=','>" +
            "      #{code}" +
            "   </foreach> and node_name=#{nodeName} and create_time> #{beginCreateTime} and create_time &lt; #{endCreateTime}  " +
            "group by code,node_name,metric_time" +
            "</script>")
    @Result(property = "nodeName", column = "node_name")
    @Result(property = "objectType", column = "object_type")
    @Result(property = "objectId", column = "object_id")
    @Result(property = "createTime", column = "metric_time")
    List<MetricDO> selectH2Metrics(@Param("nodeName") String nodeName, @Param("codes") List<String> codes, @Param("step") long step, @Param("beginCreateTime") Date beginCreateTime, @Param("endCreateTime") Date endCreateTime);
}
