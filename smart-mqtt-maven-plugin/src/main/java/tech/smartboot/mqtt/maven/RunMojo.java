/*
 *  Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2020/11/4
 */
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
@Execute(phase = LifecyclePhase.PACKAGE)
public class RunMojo extends AbstractMojo {
    /**
     * @since 1.0
     */
    @Parameter(defaultValue = "${plugin.artifacts}", required = true)
    private List<Artifact> pluginArtifacts;

    /**
     * 编译后的存放目录
     */
    @Parameter(defaultValue = "${project.build.directory}/")
    private File configurationDir;

    @Parameter(defaultValue = "${project.packaging}", required = true, readonly = true)
    private String packaging;

    @Parameter(defaultValue = "${project.artifactId}", required = true, readonly = true)
    private String artifactId;

    @Parameter(defaultValue = "${project.version}", required = true, readonly = true)
    private String version;

    @Parameter(defaultValue = "8080")
    private int port;

    @Parameter(defaultValue = "/")
    private String path;

    @Override
    public void execute() throws MojoExecutionException {
        long startTime = System.currentTimeMillis();
        try {
            //父容器为纯净的 smart-mqtt-broker
            List<URL> urlList = new ArrayList<>();
            pluginArtifacts.forEach(artifact -> {
                try {
                    System.out.println("plugin: " + artifact.getFile().getAbsolutePath());
                    urlList.add(artifact.getFile().toURI().toURL());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            });
            URL[] urls = new URL[urlList.size()];
            urlList.toArray(urls);
            URLClassLoader classLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
            Thread.currentThread().setContextClassLoader(classLoader);

            //将插件的target目录作为插件的存储目录
            System.setProperty("SMART_MQTT_PLUGINS", configurationDir.getAbsolutePath());
            //清理原有的jar文件，避免被当做插件加载
            File originalJar = new File(configurationDir, "original-" + artifactId + "-" + version + ".jar");
            originalJar.delete();
            Class<?> clazz = classLoader.loadClass("tech.smartboot.mqtt.maven.Start");
            clazz.getConstructor().newInstance();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new MojoExecutionException(e.getMessage());
        }
        getLog().info("smart-mqtt start success, cost: " + (System.currentTimeMillis() - startTime));
        Object lock = new Object();
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
