package com.game.entity.repository;

import com.game.entity.document.Account;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 账号数据仓库
 *
 * @author GameServer
 */
@Repository
public interface AccountRepository extends MongoRepository<Account, String> {

    Optional<Account> findByAccountId(long accountId);

    Optional<Account> findByUsername(String username);

    Optional<Account> findByEmail(String email);

    Optional<Account> findByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
