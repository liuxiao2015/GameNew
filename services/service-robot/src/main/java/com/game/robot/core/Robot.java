package com.game.robot.core;

import com.game.core.net.codec.GameMessage;
import com.game.robot.client.GameClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * 机器人
 * <p>
 * 模拟一个游戏客户端，用于压测和自动化测试
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Getter
public class Robot implements GameClient.MessageListener {

    private final int robotId;
    private final String username;
    private final String password;
    private final GameClient client;

    // 状态
    private volatile long accountId;
    private volatile long roleId;
    private volatile String roleName;
    private volatile boolean loggedIn = false;

    public Robot(int robotId, String host, int port) {
        this.robotId = robotId;
        this.username = "robot_" + robotId;
        this.password = "password_" + robotId;
        this.client = new GameClient(host, port);
        this.client.setMessageListener(this);
    }

    /**
     * 启动机器人
     */
    public CompletableFuture<Void> start() {
        return client.connect()
                .thenCompose(v -> login())
                .thenAccept(v -> log.info("机器人 {} 启动成功", robotId));
    }

    /**
     * 停止机器人
     */
    public void stop() {
        client.shutdown();
        log.info("机器人 {} 已停止", robotId);
    }

    /**
     * 登录
     */
    public CompletableFuture<Void> login() {
        log.info("机器人 {} 开始登录: {}", robotId, username);

        // TODO: 构建登录请求 Protobuf
        byte[] requestData = new byte[0]; // LoginRequest

        return client.sendRequest(0x0100, 0x01, requestData)
                .thenAccept(response -> {
                    if (response.getErrorCode() == 0) {
                        // TODO: 解析登录响应
                        loggedIn = true;
                        log.info("机器人 {} 登录成功", robotId);
                    } else {
                        log.warn("机器人 {} 登录失败: {}", robotId, response.getErrorCode());
                    }
                });
    }

    /**
     * 选择角色
     */
    public CompletableFuture<Void> selectRole(long roleId) {
        log.info("机器人 {} 选择角色: {}", robotId, roleId);

        byte[] requestData = new byte[0]; // SelectRoleRequest

        return client.sendRequest(0x0100, 0x03, requestData)
                .thenAccept(response -> {
                    if (response.getErrorCode() == 0) {
                        this.roleId = roleId;
                        log.info("机器人 {} 选角成功", robotId);
                    }
                });
    }

    /**
     * 发送心跳
     */
    public void sendHeartbeat() {
        if (client.isConnected()) {
            client.send(GameMessage.createRequest(0, 0x0001, 0x01, null));
        }
    }

    /**
     * 执行动作
     */
    public CompletableFuture<GameMessage> action(int protocolId, int methodId, byte[] data) {
        return client.sendRequest(protocolId, methodId, data);
    }

    @Override
    public void onPush(GameMessage message) {
        log.debug("机器人 {} 收到推送: {}", robotId, message);
        // TODO: 处理推送消息
    }
}
