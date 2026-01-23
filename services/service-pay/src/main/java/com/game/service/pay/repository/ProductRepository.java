package com.game.service.pay.repository;

import com.game.service.pay.entity.Product;
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
    
    /**
     * 根据商品ID查询
     */
    Optional<Product> findByProductId(int productId);
    
    /**
     * 查询所有上架商品
     */
    List<Product> findByEnabledTrueOrderBySortOrderAsc();
    
    /**
     * 根据类型查询商品
     */
    List<Product> findByTypeAndEnabledTrueOrderBySortOrderAsc(int type);
    
    /**
     * 根据分类查询商品
     */
    List<Product> findByCategoryAndEnabledTrueOrderBySortOrderAsc(String category);
    
    /**
     * 根据苹果商品ID查询
     */
    Optional<Product> findByAppleProductId(String appleProductId);
    
    /**
     * 根据Google商品ID查询
     */
    Optional<Product> findByGoogleProductId(String googleProductId);
}
