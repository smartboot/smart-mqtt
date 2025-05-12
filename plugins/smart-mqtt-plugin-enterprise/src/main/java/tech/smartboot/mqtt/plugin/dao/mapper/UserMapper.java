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
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Select;
import tech.smartboot.mqtt.plugin.dao.model.UserDO;
import tech.smartboot.mqtt.plugin.dao.query.UserQuery;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 5/1/23
 */
@Mapper
public interface UserMapper {

    @Select({"<script>",
            "select * from user_info ",
            "WHERE 1=1",
//            "<when test='clientId!=null and clientId!=\"\"'>",
//            "AND clientId = #{clientId}",
//            "</when>",
//            "<when test='topic!=null and topic!=\"\"'>",
//            "AND topic = #{topic}",
//            "</when>",
            "order by username",
            "</script>"})
    @ResultType(UserDO.class)
    List<UserDO> getUserList(UserQuery query);

    @Select("select * from user_info where username=#{username} and password=#{password}")
    @ResultType(UserDO.class)
    UserDO getUser(@Param("username") String username, @Param("password") String password);

    /**
     * 新增订阅记录
     */
    @Insert("insert into user_info(username,password,role,`desc`) values(#{username},#{password},#{role},#{desc})")
    void insert(UserDO userDO);

    @Delete("<script>delete from user_info where username in" +
            "   <foreach collection='users' item='username' index='index'" +
            "          open='(' close=')' separator=','>" +
            "      #{username}" +
            "   </foreach> " +
            "</script>")
    int deleteUsers(@Param("users") List<String> usernames);
}
