package org.smartboot.mqtt.data.persistence.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
* @Description: 雪花算法生成ID
 * @Author: learnhope
 * @Date: 2023/9/18
 */
public class IdUtils {
    
    private static Snowflake snowflake = IdUtil.getSnowflake();
    
    /**
     * 生成long 类型的ID
     *
     * @return
     */
    public static Long getId() {
        return snowflake.nextId();
    }
    
    /**
     * 生成String 类型的ID
     *
     * @return
     */
    public static String getIdStr() {
        return snowflake.nextIdStr();
    }
}