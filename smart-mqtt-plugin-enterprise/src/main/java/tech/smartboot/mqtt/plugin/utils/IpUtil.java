/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.utils;

import org.h2.util.IOUtils;
import org.lionsoul.ip2region.xdb.Searcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 6/22/23
 */
public class IpUtil {
    private static final Searcher SEARCHER;

    static {
        try {
            File file = File.createTempFile("ip2region", ".xdb");
            file.deleteOnExit();
            IOUtils.copyAndCloseInput(IpUtil.class.getClassLoader().getResourceAsStream("ip2region.xdb"), new FileOutputStream(file));
            SEARCHER = Searcher.newWithFileOnly(file.getPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String search(String ip) {
        try {
            return SEARCHER.search(ip);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println(search("101.71.39.169"));
    }
}
