package com.game.service.gm.repository;

import com.game.service.gm.entity.GmAccount;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * GM 账号仓库
 *
 * @author GameServer
 */
public interface GmAccountRepository extends MongoRepository<GmAccount, String> {

    /**
     * 根据用户名查找
     */
    Optional<GmAccount> findByUsername(String username);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
}
