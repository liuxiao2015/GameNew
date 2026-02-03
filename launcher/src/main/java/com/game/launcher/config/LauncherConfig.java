package com.game.launcher.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动器配置
 *
 * @author GameServer
 */
@Data
public class LauncherConfig {

    /**
     * 全局配置
     */
    private GlobalConfig global = new GlobalConfig();

    /**
     * 服务组配置
     */
    private Map<String, ServiceGroup> groups = new HashMap<>();

    /**
     * 服务配置列表
     */
    private List<ServiceConfig> services = new ArrayList<>();

    /**
     * 全局配置
     */
    @Data
    public static class GlobalConfig {
        /**
         * 项目根目录
         */
        private String projectRoot = ".";

        /**
         * Java 路径
         */
        private String javaHome = System.getenv("JAVA_HOME");

        /**
         * JVM 默认参数
         */
        private String jvmArgs = "-Xms512m -Xmx1g";

        /**
         * 启动超时时间 (秒)
         */
        private int startupTimeout = 60;

        /**
         * 停止超时时间 (秒)
         */
        private int shutdownTimeout = 30;

        /**
         * 日志目录
         */
        private String logDir = "logs";

        /**
         * PID 文件目录
         */
        private String pidDir = ".pids";

        /**
         * 环境变量
         */
        private Map<String, String> env = new HashMap<>();
    }

    /**
     * 服务组配置
     */
    @Data
    public static class ServiceGroup {
        /**
         * 组名
         */
        private String name;

        /**
         * 组描述
         */
        private String description;

        /**
         * 启动顺序 (越小越先启动)
         */
        private int order = 100;

        /**
         * 组内服务 (服务名列表)
         */
        private List<String> services = new ArrayList<>();
    }

    /**
     * 服务配置
     */
    @Data
    public static class ServiceConfig {
        /**
         * 服务名称
         */
        private String name;

        /**
         * 服务描述
         */
        private String description;

        /**
         * 服务模块目录 (相对于 projectRoot)
         */
        private String module;

        /**
         * JAR 文件路径 (相对于模块目录)
         */
        private String jar = "target/${name}-1.0.0-SNAPSHOT.jar";

        /**
         * 主类 (如果不使用 JAR 启动)
         */
        private String mainClass;

        /**
         * 服务端口
         */
        private int port;

        /**
         * Dubbo 端口
         */
        private int dubboPort;

        /**
         * JVM 参数 (覆盖全局配置)
         */
        private String jvmArgs;

        /**
         * 应用参数
         */
        private String appArgs;

        /**
         * 环境变量 (覆盖全局配置)
         */
        private Map<String, String> env = new HashMap<>();

        /**
         * 启动顺序 (越小越先启动)
         */
        private int order = 100;

        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 所属组
         */
        private String group;

        /**
         * 依赖的服务 (会等待依赖服务启动完成)
         */
        private List<String> dependsOn = new ArrayList<>();

        /**
         * 健康检查 URL
         */
        private String healthCheckUrl;

        /**
         * 健康检查间隔 (秒)
         */
        private int healthCheckInterval = 5;

        /**
         * 实例数量
         */
        private int instances = 1;

        /**
         * Spring Profiles
         */
        private String profiles = "dev";
    }
}
