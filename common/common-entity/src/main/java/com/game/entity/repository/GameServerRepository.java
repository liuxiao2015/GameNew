package com.game.entity.repository;

import com.game.entity.document.GameServer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 游戏服务器仓库
 *
 * @author GameServer
 */
@Repository
public interface GameServerRepository extends MongoRepository<GameServer, Integer> {

    /**
     * 查询所有开放的服务器
     */
    List<GameServer> findByOpenTrueOrderByServerIdDesc();

    /**
     * 根据分组查询服务器
     */
    List<GameServer> findByGroupIdAndOpenTrueOrderByServerIdDesc(int groupId);

    /**
     * 查询推荐服务器
     */
    List<GameServer> findByRecommendedTrueAndOpenTrueOrderByServerIdDesc();

    /**
     * 根据状态查询服务器
     */
    List<GameServer> findByStatusAndOpenTrue(int status);
}
