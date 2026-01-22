package com.game.entity.guild;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 公会数据仓库
 *
 * @author GameServer
 */
@Repository
public interface GuildRepository extends MongoRepository<GuildData, String> {

    /**
     * 根据公会 ID 查询
     */
    Optional<GuildData> findByGuildId(long guildId);

    /**
     * 根据公会名查询
     */
    Optional<GuildData> findByGuildName(String guildName);

    /**
     * 检查公会名是否存在
     */
    boolean existsByGuildName(String guildName);

    /**
     * 根据会长 ID 查询
     */
    Optional<GuildData> findByLeaderId(long leaderId);

    /**
     * 查询公会战力排行
     */
    List<GuildData> findTop100ByOrderByTotalCombatPowerDesc();

    /**
     * 查询公会等级排行
     */
    List<GuildData> findTop100ByOrderByLevelDescExpDesc();

    /**
     * 根据等级范围查询公会
     */
    List<GuildData> findByLevelBetween(int minLevel, int maxLevel);
}
