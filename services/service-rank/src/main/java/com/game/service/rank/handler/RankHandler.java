package com.game.service.rank.handler;

import com.game.api.common.ProtocolConstants;
import com.game.core.handler.BaseHandler;
import com.game.core.handler.annotation.Protocol;
import com.game.core.handler.annotation.ProtocolController;
import com.game.core.net.session.Session;
import com.game.proto.*;
import com.game.service.rank.service.RankBusinessService;
import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 排行榜协议处理器
 * <p>
 * 处理排行榜相关的客户端请求
 * </p>
 * 
 * <pre>
 * 框架能力使用：
 * - ProtocolController: 模块级协议处理器声明
 * - Protocol: 方法级协议处理声明
 * - RankService: 排行榜查询
 * - CacheService: 排行榜数据缓存
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@ProtocolController(moduleId = ProtocolConstants.PROTOCOL_RANK, value = "排行模块")
@RequiredArgsConstructor
public class RankHandler extends BaseHandler {

    private final RankBusinessService rankBusinessService;

    /**
     * 协议方法 ID
     */
    private static final int METHOD_GET_RANK_LIST = 0x01;
    private static final int METHOD_GET_MY_RANK = 0x02;

    /**
     * 获取排行榜列表
     */
    @Protocol(methodId = METHOD_GET_RANK_LIST, desc = "获取排行榜", requireLogin = true, requireRole = true)
    public Message getRankList(Session session, C2S_GetRankList request) {
        long roleId = session.getRoleId();
        int rankType = request.getRankType();
        int start = Math.max(1, request.getStart());
        int count = Math.min(Math.max(1, request.getCount()), 100);

        log.debug("获取排行榜: roleId={}, rankType={}, start={}, count={}", 
                roleId, rankType, start, count);

        List<RankEntry> entries = rankBusinessService.getRankList(rankType, start, count);
        long refreshTime = rankBusinessService.getRefreshTime(rankType);

        return S2C_GetRankList.newBuilder()
                .setResult(buildSuccessResult())
                .setRankType(rankType)
                .addAllEntries(entries)
                .setRefreshTime(refreshTime)
                .build();
    }

    /**
     * 获取我的排名
     */
    @Protocol(methodId = METHOD_GET_MY_RANK, desc = "获取我的排名", requireLogin = true, requireRole = true)
    public Message getMyRank(Session session, C2S_GetMyRank request) {
        long roleId = session.getRoleId();
        int rankType = request.getRankType();

        log.debug("获取我的排名: roleId={}, rankType={}", roleId, rankType);

        RankBusinessService.MyRankInfo myRank = rankBusinessService.getMyRank(roleId, rankType);

        return S2C_GetMyRank.newBuilder()
                .setResult(buildSuccessResult())
                .setRank(myRank.rank())
                .setScore(myRank.score())
                .build();
    }

    // ==================== 辅助方法 ====================

    private com.game.proto.Result buildSuccessResult() {
        return com.game.proto.Result.newBuilder()
                .setCode(0)
                .setMessage("success")
                .build();
    }
}
