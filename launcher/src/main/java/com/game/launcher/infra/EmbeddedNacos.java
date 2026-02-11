package com.game.launcher.infra;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 嵌入式 Nacos 管理器
 * <p>
 * 自动检测、下载、启动、健康等待和关闭 Nacos。
 * 在 LauncherApplication 中调用 {@link #ensureRunning()} 即可。
 * </p>
 *
 * <pre>
 * 逻辑流程:
 *   1. 检查 localhost:8848 是否已有 Nacos → 已运行则跳过
 *   2. 检查 tools/nacos/ 是否完整 → 不完整则自动下载解压
 *   3. 以 standalone 模式启动 Nacos 子进程
 *   4. 轮询健康接口直到就绪 (最多 60 秒)
 *   5. Launcher 退出时自动销毁子进程
 * </pre>
 *
 * @author GameServer
 */
public class EmbeddedNacos {

    private static final int NACOS_PORT = 8848;
    private static final String NACOS_VERSION = "2.3.0";
    private static final String DOWNLOAD_URL =
            "https://github.com/alibaba/nacos/releases/download/" + NACOS_VERSION + "/nacos-server-" + NACOS_VERSION + ".zip";

    private static final int HEALTH_CHECK_TIMEOUT_SECONDS = 60;
    private static final int CONNECT_TIMEOUT_MS = 500;

    /** Nacos 子进程 (为 null 表示外部已启动或未由本 Launcher 管理) */
    private static Process nacosProcess;

    /** 是否由本 Launcher 启动的 Nacos */
    private static volatile boolean managedByUs = false;

    /**
     * 获取项目根目录 (即 GameNew/)
     */
    private static Path getProjectRoot() {
        // 优先使用 user.dir (IDEA 中通常是项目根目录)
        return Paths.get(System.getProperty("user.dir"));
    }

    /**
     * 获取 Nacos 安装目录: {projectRoot}/tools/nacos/
     */
    private static Path getNacosHome() {
        return getProjectRoot().resolve("tools").resolve("nacos");
    }

    /**
     * 确保 Nacos 正在运行。
     * <p>
     * 如果已在运行则跳过；否则检查安装、必要时下载、然后启动。
     * </p>
     *
     * @throws RuntimeException 如果启动失败或健康检查超时
     */
    public static void ensureRunning() {
        // 1. 检查是否已在运行
        if (isPortOpen(NACOS_PORT)) {
            System.out.println("  [OK] Nacos              localhost:" + NACOS_PORT + " 已在运行 (外部实例)");
            return;
        }

        System.out.println("  Nacos 未运行，准备自动启动...");

        // 2. 检查安装
        Path nacosHome = getNacosHome();
        if (!isNacosInstalled(nacosHome)) {
            System.out.println("  Nacos 未安装，开始下载...");
            downloadAndExtract(nacosHome);
        }

        // 3. 启动 Nacos
        startNacos(nacosHome);

        // 4. 等待就绪
        waitForReady();

        System.out.println("  [OK] Nacos              localhost:" + NACOS_PORT + " 启动成功 (由 Launcher 管理)");
        managedByUs = true;
    }

    /**
     * 停止由 Launcher 管理的 Nacos 进程
     */
    public static void stop() {
        if (nacosProcess != null && nacosProcess.isAlive()) {
            System.out.println("  正在关闭 Nacos...");
            nacosProcess.destroy();
            try {
                // 等待最多 10 秒
                boolean exited = nacosProcess.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
                if (!exited) {
                    System.out.println("  Nacos 未在 10 秒内退出，强制终止...");
                    nacosProcess.destroyForcibly();
                    nacosProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                }
                System.out.println("  Nacos 已关闭");
            } catch (InterruptedException e) {
                nacosProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            nacosProcess = null;
            managedByUs = false;
        }
    }

    /**
     * @return Nacos 是否由本 Launcher 管理
     */
    public static boolean isManagedByUs() {
        return managedByUs;
    }

    /**
     * @return Nacos 子进程是否存活
     */
    public static boolean isAlive() {
        return nacosProcess != null && nacosProcess.isAlive();
    }

    // ======================== 安装检测 ========================

    /**
     * 检查 Nacos 是否已完整安装
     * <p>
     * 关键文件: bin/startup.cmd (Windows) 或 bin/startup.sh (Linux/Mac)
     *          target/nacos-server.jar
     * </p>
     */
    private static boolean isNacosInstalled(Path nacosHome) {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        Path startupScript = isWindows
                ? nacosHome.resolve("bin").resolve("startup.cmd")
                : nacosHome.resolve("bin").resolve("startup.sh");

        Path serverJar = nacosHome.resolve("target").resolve("nacos-server.jar");

        return Files.exists(startupScript) && Files.exists(serverJar);
    }

    // ======================== 下载解压 ========================

    /**
     * 下载 Nacos Server 并解压到 tools/ 目录
     */
    private static void downloadAndExtract(Path nacosHome) {
        Path toolsDir = nacosHome.getParent(); // tools/
        Path zipFile = toolsDir.resolve("nacos-server-" + NACOS_VERSION + ".zip");

        try {
            Files.createDirectories(toolsDir);

            // 下载
            if (!Files.exists(zipFile)) {
                downloadFile(DOWNLOAD_URL, zipFile);
            } else {
                System.out.println("  发现已下载的 ZIP: " + zipFile);
            }

            // 解压
            System.out.println("  正在解压...");
            unzip(zipFile, toolsDir);

            // 验证
            if (!isNacosInstalled(nacosHome)) {
                throw new RuntimeException("解压完成但 Nacos 安装不完整，请检查 " + nacosHome);
            }

            System.out.println("  Nacos " + NACOS_VERSION + " 安装完成");

            // 设置 Linux/Mac 上的执行权限
            if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
                Path startupSh = nacosHome.resolve("bin").resolve("startup.sh");
                if (Files.exists(startupSh)) {
                    startupSh.toFile().setExecutable(true);
                }
                Path shutdownSh = nacosHome.resolve("bin").resolve("shutdown.sh");
                if (Files.exists(shutdownSh)) {
                    shutdownSh.toFile().setExecutable(true);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Nacos 下载/解压失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 URL 下载文件，带进度显示
     */
    private static void downloadFile(String urlStr, Path target) throws IOException {
        System.out.println("  下载地址: " + urlStr);
        System.out.println("  保存位置: " + target);

        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(60_000);
        conn.setRequestProperty("User-Agent", "GameLauncher/1.0");

        // 处理 GitHub 302 重定向
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                || responseCode == 307 || responseCode == 308) {
            String newUrl = conn.getHeaderField("Location");
            conn.disconnect();
            conn = (HttpURLConnection) URI.create(newUrl).toURL().openConnection();
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(60_000);
            conn.setRequestProperty("User-Agent", "GameLauncher/1.0");
            responseCode = conn.getResponseCode();
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            conn.disconnect();
            throw new IOException("下载失败，HTTP 状态码: " + responseCode);
        }

        long totalSize = conn.getContentLengthLong();
        String totalSizeMB = totalSize > 0 ? String.format("%.1f MB", totalSize / 1024.0 / 1024.0) : "未知大小";
        System.out.println("  文件大小: " + totalSizeMB);

        Path tempFile = target.getParent().resolve(target.getFileName() + ".downloading");
        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(tempFile)) {

            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int lastPercent = -1;
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                downloaded += bytesRead;

                if (totalSize > 0) {
                    int percent = (int) (downloaded * 100 / totalSize);
                    if (percent != lastPercent && percent % 10 == 0) {
                        System.out.printf("  下载进度: %d%% (%.1f MB / %.1f MB)%n",
                                percent, downloaded / 1024.0 / 1024.0, totalSize / 1024.0 / 1024.0);
                        lastPercent = percent;
                    }
                }
            }
        }

        // 重命名
        Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        conn.disconnect();

        System.out.println("  下载完成!");
    }

    /**
     * 解压 ZIP 文件到目标目录
     */
    private static void unzip(Path zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFile)))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            int fileCount = 0;

            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();

                // 安全检查: 防止 Zip Slip 攻击
                if (!entryPath.startsWith(targetDir)) {
                    throw new IOException("ZIP entry 超出目标目录: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (OutputStream out = Files.newOutputStream(entryPath)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }
                    fileCount++;
                }
                zis.closeEntry();
            }
            System.out.println("  解压完成，共 " + fileCount + " 个文件");
        }
    }

    // ======================== 启动 ========================

    /**
     * 以 standalone 模式启动 Nacos
     */
    private static void startNacos(Path nacosHome) {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");

        System.out.println("  正在启动 Nacos (standalone 模式)...");

        try {
            ProcessBuilder pb;
            if (isWindows) {
                Path startupCmd = nacosHome.resolve("bin").resolve("startup.cmd");
                pb = new ProcessBuilder("cmd.exe", "/c", startupCmd.toString(), "-m", "standalone");
            } else {
                Path startupSh = nacosHome.resolve("bin").resolve("startup.sh");
                pb = new ProcessBuilder("bash", startupSh.toString(), "-m", "standalone");
            }

            // 设置工作目录
            pb.directory(nacosHome.toFile());

            // 将 Nacos 输出重定向到日志文件
            Path logsDir = nacosHome.resolve("logs");
            Files.createDirectories(logsDir);
            Path logFile = logsDir.resolve("nacos-launcher.log");
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
            pb.redirectErrorStream(true);

            // 继承环境变量 (特别是 JAVA_HOME)
            pb.environment().putAll(System.getenv());

            nacosProcess = pb.start();

            System.out.println("  Nacos 进程已启动 (PID: " + nacosProcess.pid() + ")");
            System.out.println("  日志文件: " + logFile);

        } catch (IOException e) {
            throw new RuntimeException("Nacos 启动失败: " + e.getMessage(), e);
        }
    }

    // ======================== 健康检查 ========================

    /**
     * 等待 Nacos 就绪 (轮询端口，最多 HEALTH_CHECK_TIMEOUT_SECONDS 秒)
     */
    private static void waitForReady() {
        System.out.print("  等待 Nacos 就绪");

        long startTime = System.currentTimeMillis();
        long deadline = startTime + HEALTH_CHECK_TIMEOUT_SECONDS * 1000L;

        while (System.currentTimeMillis() < deadline) {
            // 检查进程是否意外退出
            if (nacosProcess != null && !nacosProcess.isAlive()) {
                int exitCode = nacosProcess.exitValue();
                System.out.println();
                throw new RuntimeException("Nacos 进程意外退出，退出码: " + exitCode
                        + "。请检查日志: " + getNacosHome().resolve("logs").resolve("nacos-launcher.log"));
            }

            // 尝试连接端口
            if (isPortOpen(NACOS_PORT)) {
                // 端口开放后，再尝试 HTTP 健康检查
                if (isNacosHealthy()) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    System.out.printf(" 就绪! (%.1f 秒)%n", elapsed / 1000.0);
                    return;
                }
            }

            System.out.print(".");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("等待 Nacos 就绪时被中断");
            }
        }

        System.out.println();
        throw new RuntimeException("Nacos 在 " + HEALTH_CHECK_TIMEOUT_SECONDS + " 秒内未就绪，请检查日志");
    }

    /**
     * HTTP 健康检查
     */
    private static boolean isNacosHealthy() {
        try {
            URL url = URI.create("http://localhost:" + NACOS_PORT + "/nacos/v1/console/health/readiness").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // ======================== 工具方法 ========================

    private static boolean isPortOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", port), CONNECT_TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
