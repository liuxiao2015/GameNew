package com.game.common.enums;

import lombok.Getter;

/**
 * 统一错误码枚举
 * <p>
 * 错误码规范：
 * - 0: 成功
 * - 1xxx: 系统级错误
 * - 2xxx: 登录/账号错误
 * - 3xxx: 玩家相关错误
 * - 4xxx: 背包/物品错误
 * - 5xxx: 公会相关错误
 * - 6xxx: 聊天相关错误
 * - 7xxx: 排行相关错误
 * - 8xxx: 任务相关错误
 * - 9xxx: GM 相关错误
 * </p>
 *
 * @author GameServer
 */
@Getter
public enum ErrorCode {

    // ==================== 成功 ====================
    SUCCESS(0, "成功"),

    // ==================== 系统级错误 1xxx ====================
    SYSTEM_ERROR(1000, "系统错误"),
    PARAM_ERROR(1001, "参数错误"),
    REQUEST_TIMEOUT(1002, "请求超时"),
    SERVICE_UNAVAILABLE(1003, "服务不可用"),
    RATE_LIMIT_EXCEEDED(1004, "请求过于频繁"),
    ILLEGAL_OPERATION(1005, "非法操作"),
    DATA_NOT_FOUND(1006, "数据不存在"),
    DATA_ALREADY_EXISTS(1007, "数据已存在"),
    CONCURRENT_MODIFY(1008, "数据已被修改"),
    PERMISSION_DENIED(1009, "权限不足"),
    RPC_ERROR(1010, "远程调用失败"),
    NOT_FOUND(1011, "数据不存在"),
    OPERATION_FORBIDDEN(1012, "操作被禁止"),
    LIMIT_EXCEEDED(1013, "超过限制"),

    // ==================== 登录/账号错误 2xxx ====================
    ACCOUNT_NOT_FOUND(2001, "账号不存在"),
    ACCOUNT_ALREADY_EXISTS(2002, "账号已存在"),
    PASSWORD_ERROR(2003, "密码错误"),
    TOKEN_INVALID(2004, "Token 无效"),
    TOKEN_EXPIRED(2005, "Token 已过期"),
    ACCOUNT_BANNED(2006, "账号已封禁"),
    ACCOUNT_DISABLED(2007, "账号已禁用"),
    ROLE_NOT_FOUND(2008, "角色不存在"),
    ROLE_NAME_EXISTS(2009, "角色名已存在"),
    ROLE_NAME_INVALID(2010, "角色名不合法"),
    SERVER_MAINTENANCE(2011, "服务器维护中"),
    ROLE_ALREADY_EXISTS(2012, "该服已有角色"),
    ROLE_NOT_BELONG(2013, "角色不属于此账号"),
    ROLE_DELETED(2014, "角色已删除"),
    UNAUTHORIZED(2015, "未授权"),
    FORBIDDEN(2016, "禁止访问"),
    ACCOUNT_EXISTS(2017, "账号已存在"),
    THIRD_PARTY_AUTH_FAILED(2018, "第三方登录验证失败"),
    THIRD_PARTY_ALREADY_BOUND(2019, "第三方账号已被绑定"),
    SERVER_NOT_FOUND(2020, "服务器不存在"),
    SERVER_FULL(2021, "服务器已满"),
    VERSION_TOO_LOW(2022, "版本过低"),

    // ==================== 玩家相关错误 3xxx ====================
    PLAYER_OFFLINE(3001, "玩家不在线"),
    LEVEL_NOT_ENOUGH(3002, "等级不足"),
    GOLD_NOT_ENOUGH(3003, "金币不足"),
    DIAMOND_NOT_ENOUGH(3004, "钻石不足"),
    VIP_LEVEL_NOT_ENOUGH(3005, "VIP 等级不足"),
    ENERGY_NOT_ENOUGH(3006, "体力不足"),
    PLAYER_BANNED(3007, "玩家已被封禁"),
    CURRENCY_NOT_ENOUGH(3008, "货币不足"),

    // ==================== 背包/物品错误 4xxx ====================
    ITEM_NOT_FOUND(4001, "物品不存在"),
    ITEM_NOT_ENOUGH(4002, "物品数量不足"),
    BAG_FULL(4003, "背包已满"),
    ITEM_CANNOT_USE(4004, "物品无法使用"),
    ITEM_CANNOT_SELL(4005, "物品无法出售"),
    EQUIPMENT_CANNOT_WEAR(4006, "装备无法穿戴"),
    EQUIPMENT_SLOT_INVALID(4007, "装备槽位无效"),

    // ==================== 公会相关错误 5xxx ====================
    GUILD_NOT_FOUND(5001, "公会不存在"),
    GUILD_ALREADY_JOINED(5002, "已加入公会"),
    GUILD_NOT_JOINED(5003, "未加入公会"),
    GUILD_FULL(5004, "公会已满"),
    GUILD_NAME_EXISTS(5005, "公会名已存在"),
    GUILD_PERMISSION_DENIED(5006, "公会权限不足"),
    GUILD_CANNOT_KICK_SELF(5007, "不能踢出自己"),
    GUILD_LEADER_CANNOT_LEAVE(5008, "会长不能退出公会"),
    GUILD_APPLY_PENDING(5009, "申请审核中"),
    GUILD_LEVEL_NOT_ENOUGH(5010, "公会等级不足"),

    // ==================== 聊天相关错误 6xxx ====================
    CHAT_FORBIDDEN(6001, "禁言中"),
    CHAT_CONTENT_INVALID(6002, "聊天内容不合法"),
    CHAT_TARGET_OFFLINE(6003, "对方不在线"),
    CHAT_BLACKLIST(6004, "对方在黑名单中"),
    CHAT_COOLDOWN(6005, "发言冷却中"),
    CHAT_MUTED(6006, "您已被禁言"),
    CHAT_TOO_FREQUENT(6007, "发言过于频繁"),

    // ==================== 排行相关错误 7xxx ====================
    RANK_NOT_FOUND(7001, "排行榜不存在"),

    // ==================== 任务相关错误 8xxx ====================
    QUEST_NOT_FOUND(8001, "任务不存在"),
    QUEST_NOT_ACCEPTED(8002, "任务未接取"),
    QUEST_ALREADY_COMPLETED(8003, "任务已完成"),
    QUEST_CONDITION_NOT_MET(8004, "任务条件未满足"),

    // ==================== GM 相关错误 9xxx ====================
    GM_AUTH_FAILED(9001, "GM 认证失败"),
    GM_PERMISSION_DENIED(9002, "GM 权限不足"),
    GM_OPERATION_FAILED(9003, "GM 操作失败"),

    // ==================== 支付相关错误 10xxx ====================
    PAY_ORDER_NOT_FOUND(10001, "订单不存在"),
    PAY_ORDER_EXPIRED(10002, "订单已过期"),
    PAY_ORDER_COMPLETED(10003, "订单已完成"),
    PAY_ORDER_CANCELLED(10004, "订单已取消"),
    PAY_PRODUCT_NOT_FOUND(10005, "商品不存在"),
    PAY_PRODUCT_UNAVAILABLE(10006, "商品已下架"),
    PAY_CHANNEL_NOT_SUPPORTED(10007, "不支持的支付渠道"),
    PAY_CHANNEL_UNAVAILABLE(10008, "支付渠道不可用"),
    PAY_AMOUNT_MISMATCH(10009, "支付金额不匹配"),
    PAY_SIGNATURE_INVALID(10010, "签名验证失败"),
    PAY_DELIVER_FAILED(10011, "道具发放失败"),
    PAY_LIMIT_EXCEEDED(10012, "超过购买限制"),
    PAY_DAILY_LIMIT_EXCEEDED(10013, "超过每日购买限制"),

    ;

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 根据 code 获取 ErrorCode
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return SYSTEM_ERROR;
    }
}
