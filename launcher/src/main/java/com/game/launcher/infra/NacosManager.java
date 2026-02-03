package com.game.launcher.infra;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Nacos 本地管理器
 * <p>
 * 支持自动解压和启动本地 Nacos，适用于 Windows 本地开发环境
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public class NacosManager {

    private static final String NACOS_ZIP = "nacos-server-2.3.0.zip";
    private static final String NACOS_DIR = "nacos";
    private static final int DEFAULT_PORT = 8848;
    private static final int GRPC_PORT = 9848;

    private final Path projectRoot;
    private final Path toolsDir;
    private final Path nacosHome;
    private Process nacosProcess;

    public NacosManager(String projectRoot) {
        this.projectRoot = Paths.get(projectRoot).toAbsolutePath();
        this.toolsDir = this.projectRoot.resolve("tools");
        this.nacosHome = this.toolsDir.resolve(NACOS_DIR);
    }

    /**
     * 检查 Nacos 是否正在运行
     */
    public boolean isRunning() {
        return checkPort(DEFAULT_PORT) && checkPort(GRPC_PORT);
    }

    /**
     * 检查 Nacos 是否已安装（解压）
     */
    public boolean isInstalled() {
        Path startupScript = getStartupScript();
        return Files.exists(startupScript);
    }

    /**
     * 获取启动脚本路径
     */
    private Path getStartupScript() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String script = isWindows ? "startup.cmd" : "startup.sh";
        return nacosHome.resolve("bin").resolve(script);
    }

    /**
     * 获取停止脚本路径
     */
    private Path getShutdownScript() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String script = isWindows ? "shutdown.cmd" : "shutdown.sh";
        return nacosHome.resolve("bin").resolve(script);
    }

    /**
     * 安装 Nacos（解压）
     */
    public boolean install() {
        if (isInstalled()) {
            log.info("Nacos 已安装: {}", nacosHome);
            return true;
        }

        Path zipFile = toolsDir.resolve(NACOS_ZIP);
        if (!Files.exists(zipFile)) {
            log.error("Nacos 安装包不存在: {}", zipFile);
            log.error("请下载 Nacos 并放置到 tools 目录:");
            log.error("  下载地址: https://github.com/alibaba/nacos/releases/download/2.3.0/nacos-server-2.3.0.zip");
            return false;
        }

        log.info("正在解压 Nacos...");
        try {
            unzip(zipFile, toolsDir);
            log.info("Nacos 解压完成: {}", nacosHome);

            // 设置执行权限 (Linux/Mac)
            if (!System.getProperty("os.name").toLowerCase().contains("win")) {
                Path startupSh = nacosHome.resolve("bin").resolve("startup.sh");
                Path shutdownSh = nacosHome.resolve("bin").resolve("shutdown.sh");
                startupSh.toFile().setExecutable(true);
                shutdownSh.toFile().setExecutable(true);
            }

            return true;
        } catch (IOException e) {
            log.error("解压 Nacos 失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 启动 Nacos
     */
    public boolean start() {
        if (isRunning()) {
            log.info("Nacos 已在运行中");
            return true;
        }

        if (!isInstalled()) {
            log.info("Nacos 未安装，正在自动安装...");
            if (!install()) {
                return false;
            }
        }

        log.info("正在启动 Nacos (单机模式)...");

        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            Path binDir = nacosHome.resolve("bin");
            ProcessBuilder pb;

            if (isWindows) {
                // Windows: 使用 cmd /c 启动
                pb = new ProcessBuilder("cmd", "/c", "startup.cmd", "-m", "standalone");
            } else {
                // Linux/Mac: 使用 bash
                pb = new ProcessBuilder("bash", "startup.sh", "-m", "standalone");
            }

            pb.directory(binDir.toFile());
            pb.environment().put("MODE", "standalone");

            // 重定向输出到日志文件
            Path logDir = projectRoot.resolve("logs");
            Files.createDirectories(logDir);
            Path logFile = logDir.resolve("nacos-startup.log");
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
            pb.redirectError(ProcessBuilder.Redirect.appendTo(logFile.toFile()));

            nacosProcess = pb.start();

            // 等待 Nacos 启动
            return waitForReady(60);

        } catch (IOException e) {
            log.error("启动 Nacos 失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 停止 Nacos
     */
    public boolean stop() {
        if (!isRunning()) {
            log.info("Nacos 未在运行");
            return true;
        }

        log.info("正在停止 Nacos...");

        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            Path binDir = nacosHome.resolve("bin");

            if (isWindows) {
                // Windows: 使用 taskkill 杀死 Java 进程
                // 先尝试使用 shutdown.cmd
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "shutdown.cmd");
                pb.directory(binDir.toFile());
                Process p = pb.start();
                p.waitFor(10, TimeUnit.SECONDS);

                // 如果还在运行，强制杀死
                if (isRunning()) {
                    killNacosProcess();
                }
            } else {
                // Linux/Mac
                ProcessBuilder pb = new ProcessBuilder("bash", "shutdown.sh");
                pb.directory(binDir.toFile());
                Process p = pb.start();
                p.waitFor(10, TimeUnit.SECONDS);
            }

            // 等待停止
            for (int i = 0; i < 10; i++) {
                if (!isRunning()) {
                    log.info("Nacos 已停止");
                    return true;
                }
                TimeUnit.SECONDS.sleep(1);
            }

            log.warn("Nacos 停止超时");
            return false;

        } catch (Exception e) {
            log.error("停止 Nacos 失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 强制杀死 Nacos 进程 (Windows)
     */
    private void killNacosProcess() {
        try {
            // 查找并杀死监听 8848 端口的进程
            ProcessBuilder findPid = new ProcessBuilder("cmd", "/c",
                    "for /f \"tokens=5\" %a in ('netstat -aon ^| findstr :8848 ^| findstr LISTENING') do @echo %a");
            findPid.redirectErrorStream(true);
            Process p = findPid.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String pid = line.trim();
                    if (!pid.isEmpty() && pid.matches("\\d+")) {
                        new ProcessBuilder("taskkill", "/F", "/PID", pid).start().waitFor();
                        log.info("已终止 Nacos 进程 PID: {}", pid);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("强制终止 Nacos 进程失败: {}", e.getMessage());
        }
    }

    /**
     * 等待 Nacos 就绪
     */
    public boolean waitForReady(int timeoutSeconds) {
        log.info("等待 Nacos 就绪...");

        for (int i = 0; i < timeoutSeconds; i++) {
            if (checkHealth()) {
                log.info("✓ Nacos 已就绪 (http://localhost:8848/nacos)");
                log.info("  默认账号: nacos / nacos");
                return true;
            }

            // 每 5 秒打印一次进度
            if (i > 0 && i % 5 == 0) {
                log.info("  等待中... ({}/{}s)", i, timeoutSeconds);
            }

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        log.error("✗ Nacos 启动超时");
        return false;
    }

    /**
     * 检查 Nacos 健康状态
     */
    public boolean checkHealth() {
        try {
            URL url = new URL("http://localhost:8848/nacos/v1/console/health/readiness");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            return code >= 200 && code < 400;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 检查端口
     */
    private boolean checkPort(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 解压 ZIP 文件
     */
    private void unzip(Path zipFile, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                Path newPath = destDir.resolve(entry.getName());

                // 防止 Zip Slip 漏洞
                if (!newPath.normalize().startsWith(destDir.normalize())) {
                    throw new IOException("Invalid ZIP entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    try (OutputStream os = Files.newOutputStream(newPath)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * 打印状态
     */
    public void printStatus() {
        System.out.println();
        System.out.println("┌─────────────────────────────────────────────────────┐");
        System.out.println("│                   Nacos 状态                        │");
        System.out.println("├─────────────────────────────────────────────────────┤");
        System.out.printf("│  安装路径: %-39s │%n", nacosHome);
        System.out.printf("│  已安装  : %-39s │%n", isInstalled() ? "是" : "否");
        System.out.printf("│  运行状态: %-39s │%n", isRunning() ? "✓ 运行中" : "✗ 未运行");
        System.out.printf("│  HTTP端口: %-39s │%n", "8848 " + (checkPort(8848) ? "✓" : "✗"));
        System.out.printf("│  GRPC端口: %-39s │%n", "9848 " + (checkPort(9848) ? "✓" : "✗"));
        System.out.println("├─────────────────────────────────────────────────────┤");
        System.out.println("│  控制台  : http://localhost:8848/nacos              │");
        System.out.println("│  账号    : nacos / nacos                            │");
        System.out.println("└─────────────────────────────────────────────────────┘");
        System.out.println();
    }

    /**
     * 获取 Nacos 安装目录
     */
    public Path getNacosHome() {
        return nacosHome;
    }
}
