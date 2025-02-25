package org.smartboot.mqtt.plugin.convert;

import org.smartboot.mqtt.plugin.dao.model.UserDO;
import org.smartboot.mqtt.plugin.openapi.to.UserTO;

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
