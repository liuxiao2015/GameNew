package com.game.api.chat;

import com.game.common.result.Result;

import java.util.List;

/**
 * 聊天服务接口
 *
 * @author GameServer
 */
public interface ChatService {

    /**
     * 发送聊天消息
     *
     * @param roleId   发送者角色 ID
     * @param channel  频道 (1:世界 2:公会 3:私聊)
     * @param content  消息内容
     * @param targetId 目标 ID (私聊时为对方角色 ID)
     * @return 消息 ID
     */
    Result<Long> sendMessage(long roleId, int channel, String content, long targetId);

    /**
     * 获取聊天记录
     *
     * @param roleId    角色 ID
     * @param channel   频道
     * @param targetId  目标 ID (私聊时)
     * @param lastMsgId 上一条消息 ID
     * @param count     获取数量
     * @return 消息列表
     */
    Result<List<ChatMessageDTO>> getHistory(long roleId, int channel, long targetId, long lastMsgId, int count);

    /**
     * 禁言
     *
     * @param roleId   被禁言角色 ID
     * @param duration 禁言时长 (秒，0 表示永久)
     * @param reason   禁言原因
     * @return 操作结果
     */
    Result<Void> mute(long roleId, long duration, String reason);

    /**
     * 解除禁言
     *
     * @param roleId 角色 ID
     * @return 操作结果
     */
    Result<Void> unmute(long roleId);

    /**
     * 发送系统公告
     *
     * @param noticeType 公告类型 (1:滚动 2:弹窗)
     * @param title      标题
     * @param content    内容
     * @return 操作结果
     */
    Result<Void> sendSystemNotice(int noticeType, String title, String content);
}
