package com.game.api.battle;

import com.game.common.result.Result;

import java.util.List;

/**
 * 战斗服务接口
 * <p>
 * 提供 PVE/PVP 战斗相关功能
 * </p>
 *
 * @author GameServer
 */
public interface BattleService {

    // ==================== PVE 战斗 ====================

    /**
     * 开始 PVE 战斗
     *
     * @param roleId     角色ID
     * @param dungeonId  副本ID
     * @param difficulty 难度 (1:普通 2:困难 3:地狱)
     * @return 战斗信息
     */
    Result<BattleDTO> startPveBattle(long roleId, int dungeonId, int difficulty);

    /**
     * 扫荡副本
     *
     * @param roleId     角色ID
     * @param dungeonId  副本ID
     * @param difficulty 难度
     * @param count      扫荡次数
     * @return 扫荡结果列表
     */
    Result<List<BattleResultDTO>> sweepDungeon(long roleId, int dungeonId, int difficulty, int count);

    // ==================== PVP 战斗 ====================

    /**
     * 开始 PVP 战斗
     *
     * @param roleId     角色ID
     * @param battleType 战斗类型 (1:竞技场 2:跨服竞技 3:公会战)
     * @return 战斗信息
     */
    Result<BattleDTO> startPvpBattle(long roleId, int battleType);

    /**
     * PVP 匹配
     *
     * @param roleId     角色ID
     * @param battleType 战斗类型
     * @return 匹配结果
     */
    Result<MatchResultDTO> matchPvp(long roleId, int battleType);

    /**
     * 取消 PVP 匹配
     *
     * @param roleId 角色ID
     * @return 操作结果
     */
    Result<Void> cancelMatch(long roleId);

    // ==================== 战斗操作 ====================

    /**
     * 提交战斗操作
     *
     * @param battleId 战斗ID
     * @param roleId   角色ID
     * @param action   操作指令
     * @return 操作结果
     */
    Result<Void> submitAction(long battleId, long roleId, BattleActionDTO action);

    /**
     * 获取战斗状态
     *
     * @param battleId 战斗ID
     * @return 战斗状态
     */
    Result<BattleDTO> getBattleInfo(long battleId);

    /**
     * 获取战斗结果
     *
     * @param battleId 战斗ID
     * @return 战斗结果
     */
    Result<BattleResultDTO> getBattleResult(long battleId);

    /**
     * 退出战斗
     *
     * @param battleId 战斗ID
     * @param roleId   角色ID
     * @return 操作结果
     */
    Result<Void> exitBattle(long battleId, long roleId);

    // ==================== 战斗记录 ====================

    /**
     * 获取战斗记录
     *
     * @param roleId 角色ID
     * @param type   记录类型 (0:全部 1:PVE 2:PVP)
     * @param page   页码
     * @param size   每页数量
     * @return 战斗记录列表
     */
    Result<List<BattleRecordDTO>> getBattleRecords(long roleId, int type, int page, int size);
}
