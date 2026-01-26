package com.game.entity.repository;

import com.game.entity.document.GuildData;
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

    Optional<GuildData> findByGuildId(long guildId);

    Optional<GuildData> findByGuildName(String guildName);

    boolean existsByGuildName(String guildName);

    Optional<GuildData> findByLeaderId(long leaderId);

    List<GuildData> findTop100ByOrderByTotalCombatPowerDesc();

    List<GuildData> findTop100ByOrderByLevelDescExpDesc();

    List<GuildData> findByLevelBetween(int minLevel, int maxLevel);
}
