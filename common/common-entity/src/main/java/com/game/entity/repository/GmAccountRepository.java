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

    Optional<GmAccount> findByUsername(String username);

    boolean existsByUsername(String username);
}
