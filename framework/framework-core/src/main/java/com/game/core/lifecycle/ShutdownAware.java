package com.game.core.lifecycle;

/**
 * 停机感知接口
 * <p>
 * 实现此接口的组件会在服务停机时被调用
 * </p>
 *
 * @author GameServer
 */
public interface ShutdownAware {

    /**
     * 停机顺序
     * <p>
     * 数字越大越先执行。建议：
     * <ul>
     *     <li>1000+ : 网关层（停止接收新连接）</li>
     *     <li>500-999 : 业务层（保存玩家数据）</li>
     *     <li>100-499 : 框架层（停止定时任务）</li>
     *     <li>0-99 : 基础设施层（关闭数据库连接）</li>
     * </ul>
     * </p>
     *
     * @return 停机顺序
     */
    default int getShutdownOrder() {
        return 500;
    }

    /**
     * 停机时执行的操作
     */
    void onShutdown();
}
