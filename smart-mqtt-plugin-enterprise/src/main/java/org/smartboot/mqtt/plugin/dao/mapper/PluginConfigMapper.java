package org.smartboot.mqtt.plugin.dao.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.smartboot.mqtt.plugin.dao.model.PluginConfigDO;

import java.util.List;

@Mapper
public interface PluginConfigMapper {
    /**
     * 新增插件配置
     */
    @Insert("insert into plugin_config(plugin_type,status,config) values(#{pluginType},0,#{config})")
    int insert(PluginConfigDO configDO);

    @Update("update plugin_config set deleted=1 where id=#{id}")
    int deleteById(@Param("id") int id);

    @Update("update plugin_config set config=#{config.config} where id=#{config.id}")
    int updateById(@Param("config") PluginConfigDO configDO);

    @Update("update plugin_config set status=#{status} where id=#{id}")
    int updateStatusById(@Param("id") int id, @Param("status") int status);

    @Select("select * from plugin_config where deleted=0 order by create_time desc")
    @Result(property = "pluginType", column = "plugin_type")
    List<PluginConfigDO> selectAll();

    @Select("select * from plugin_config where deleted=0 and id=#{id}")
    @Result(property = "pluginType", column = "plugin_type")
    PluginConfigDO selectById(@Param("id") int id);
}
