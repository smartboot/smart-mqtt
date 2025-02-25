package org.smartboot.mqtt.plugin.utils;

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
