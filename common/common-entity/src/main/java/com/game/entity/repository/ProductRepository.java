package com.game.entity.repository;

import com.game.entity.document.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 商品仓库
 *
 * @author GameServer
 */
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    Optional<Product> findByProductId(int productId);

    List<Product> findByEnabledTrueOrderBySortOrderAsc();

    List<Product> findByTypeAndEnabledTrueOrderBySortOrderAsc(int type);

    List<Product> findByCategoryAndEnabledTrueOrderBySortOrderAsc(String category);

    Optional<Product> findByAppleProductId(String appleProductId);

    Optional<Product> findByGoogleProductId(String googleProductId);
}
