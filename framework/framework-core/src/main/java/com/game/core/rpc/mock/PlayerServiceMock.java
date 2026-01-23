package com.game.core.rpc.mock;

import com.game.api.player.PlayerDTO;
import com.game.api.player.PlayerService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import lombok.extern.slf4j.Slf4j;

/**
 * 玩家服务降级 Mock
 * <p>
 * 当 PlayerService 不可用时，提供降级响应。
 * 用法:
 * <pre>
 * {@code
 * @DubboReference(
 *     mock = "com.game.core.rpc.mock.PlayerServiceMock",
 *     timeout = 3000
 * )
 * private PlayerService playerService;
 * }
 * </pre>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public class PlayerServiceMock implements PlayerService {

    private static final String SERVICE_UNAVAILABLE = "玩家服务暂时不可用";

    @Override
    public Result<PlayerDTO> getPlayerInfo(long roleId) {
        log.warn("[降级] PlayerService.getPlayerInfo 服务不可用: roleId={}", roleId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> addExp(long roleId, long exp, String reason) {
        log.warn("[降级] PlayerService.addExp 服务不可用: roleId={}", roleId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> addGold(long roleId, long gold, String reason) {
        log.warn("[降级] PlayerService.addGold 服务不可用: roleId={}", roleId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> addDiamond(long roleId, long diamond, String reason) {
        log.warn("[降级] PlayerService.addDiamond 服务不可用: roleId={}", roleId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> addItem(long roleId, int itemId, long count, String reason) {
        log.warn("[降级] PlayerService.addItem 服务不可用: roleId={}", roleId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> changeName(long roleId, String newName) {
        log.warn("[降级] PlayerService.changeName 服务不可用: roleId={}", roleId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> changeAvatar(long roleId, int avatarId) {
        log.warn("[降级] PlayerService.changeAvatar 服务不可用: roleId={}", roleId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> banPlayer(long roleId, long duration, String reason) {
        log.warn("[降级] PlayerService.banPlayer 服务不可用: roleId={}", roleId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> unbanPlayer(long roleId) {
        log.warn("[降级] PlayerService.unbanPlayer 服务不可用: roleId={}", roleId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> setGuildInfo(long roleId, long guildId, String guildName, int position) {
        log.warn("[降级] PlayerService.setGuildInfo 服务不可用: roleId={}", roleId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> dailyReset() {
        log.warn("[降级] PlayerService.dailyReset 服务不可用");
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }
}
