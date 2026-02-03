/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.convert;

import tech.smartboot.mqtt.plugin.dao.model.UserDO;
import tech.smartboot.mqtt.plugin.openapi.to.UserTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 5/1/23
 */
public class UserConvert {
    public static UserTO convert(UserDO userDO) {
        if (userDO == null) {
            return null;
        }
        UserTO userTO = new UserTO();
        userTO.setUsername(userDO.getUsername());
        userTO.setRole(userDO.getRole());
        userTO.setDesc(userDO.getDesc());
        userTO.setEditTime(userDO.getEditTime());
        userTO.setCreateTime(userDO.getCreateTime());
        return userTO;
    }

    public static List<UserTO> convert(List<UserDO> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserTO> toList = new ArrayList<>(list.size());
        list.forEach(userDO -> toList.add(convert(userDO)));
        return toList;
    }
}
