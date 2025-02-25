package org.smartboot.mqtt.plugin.dao.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Select;
import org.smartboot.mqtt.plugin.dao.model.SubscriptionDO;
import org.smartboot.mqtt.plugin.dao.model.TopicStatisticsDO;
import org.smartboot.mqtt.plugin.dao.query.SubscriberQuery;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/24
 */
@Mapper
public interface SubscriberMapper {
    /**
     * 条件查询订阅信息
     *
     * @return
     */
    @Select({"<script>",
            "select * from subscriptions ",
            "WHERE 1=1",
            "<when test='clientId!=null and clientId!=\"\"'>",
            "AND clientId = #{clientId}",
            "</when>",
            "<when test='topic!=null and topic!=\"\"'>",
            "AND topic = #{topic}",
            "</when>",
            "order by clientId",
            "</script>"})
    @ResultType(SubscriptionDO.class)
    @Result(column = "node_id", property = "nodeId")
    List<SubscriptionDO> select(SubscriberQuery query);

    /**
     * 新增订阅记录
     */
    @Insert("insert into subscriptions(clientId,topic,node_id,qos,options) values(#{clientId},#{topic},#{nodeId},#{qos},#{options})")
    void insert(SubscriptionDO subscriptionDO);

    @Delete("delete from subscriptions where clientId=#{clientId}")
    int deleteById(String clientId);

    @Select({"<script>select topic, count(*) as clients from subscriptions WHERE 1=1 ",
            "<when test='topic!=null'>",
            "AND topic=#{topic}",
            "</when>",
            "<when test='endTime!=null'>",
            "AND create_time &lt; #{endTime}",
            "</when>",
            "<when test='startTime!=null'>",
            "AND create_time > #{startTime}",
            "</when>",
            " group by topic,node_id</script>"})
    @ResultType(TopicStatisticsDO.class)
    List<TopicStatisticsDO> selectGroupByTopic(SubscriberQuery query);
}
