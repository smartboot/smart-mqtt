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

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 5/1/23
 */
@Mapper
public interface SystemConfigMapper {

    @Select("select value from system_config where code=#{code}")
    @ResultType(String.class)
    String getConfig(@Param("code") String code);

    /**
     * 新增系统配置
     */
    @Insert("insert into system_config(code,value) values(#{code},#{value})")
    void insert(@Param("code") String code, @Param("value") String value);

    /**
     * 新增系统配置
     */
    @Update("update system_config set value=#{value} where code=#{code}")
    int update(@Param("code") String code, @Param("value") String value);
}
