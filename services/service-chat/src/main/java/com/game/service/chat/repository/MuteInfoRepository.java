package com.game.service.chat.repository;

import com.game.service.chat.entity.MuteInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 禁言信息仓库
 *
 * @author GameServer
 */
public interface MuteInfoRepository extends MongoRepository<MuteInfo, Long> {

}
