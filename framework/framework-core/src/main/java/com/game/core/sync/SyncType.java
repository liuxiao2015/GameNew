package com.game.core.sync;

/**
 * 数据同步类型
 *
 * @author GameServer
 */
public enum SyncType {
    /**
     * 全量同步
     */
    FULL,

    /**
     * 增量更新
     */
    UPDATE,

    /**
     * 删除
     */
    DELETE,

    /**
     * 新增
     */
    ADD
}
