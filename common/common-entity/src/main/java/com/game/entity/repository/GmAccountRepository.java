package com.game.entity.repository;

import com.game.entity.document.GmAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * GM 账号仓库
 *
 * @author GameServer
 */
@Repository
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
