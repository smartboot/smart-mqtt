package org.smartboot.mqtt.plugin.dao.mapper;

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
