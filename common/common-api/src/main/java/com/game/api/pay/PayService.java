package com.game.api.pay;

import com.game.common.result.Result;

import java.util.List;

/**
 * 支付服务接口 (Dubbo RPC)
 *
 * @author GameServer
 */
public interface PayService {
    
    /**
     * 创建订单
     *
     * @param request 创建订单请求
     * @return 订单信息
     */
    Result<OrderDTO> createOrder(CreateOrderRequest request);
    
    /**
     * 查询订单
     *
     * @param orderId 订单号
     * @return 订单信息
     */
    Result<OrderDTO> getOrder(String orderId);
    
    /**
     * 查询角色订单列表
     *
     * @param roleId 角色ID
     * @param page   页码
     * @param size   每页数量
     * @return 订单列表
     */
    Result<List<OrderDTO>> getOrders(long roleId, int page, int size);
    
    /**
     * 取消订单
     *
     * @param orderId 订单号
     * @param reason  取消原因
     * @return 操作结果
     */
    Result<Void> cancelOrder(String orderId, String reason);
    
    /**
     * 查询商品列表
     *
     * @param roleId 角色ID (用于判断限购等)
     * @return 商品列表
     */
    Result<List<ProductDTO>> getProducts(long roleId);
    
    /**
     * 查询商品详情
     *
     * @param productId 商品ID
     * @return 商品信息
     */
    Result<ProductDTO> getProduct(int productId);
    
    /**
     * 检查是否首充
     *
     * @param roleId 角色ID
     * @return 是否首充
     */
    Result<Boolean> isFirstPay(long roleId);
    
    /**
     * 查询角色累计充值金额 (分)
     *
     * @param roleId 角色ID
     * @return 累计充值金额
     */
    Result<Long> getTotalPayAmount(long roleId);
    
    /**
     * 补发订单道具 (GM用)
     *
     * @param orderId  订单号
     * @param operator 操作人
     * @return 操作结果
     */
    Result<Void> redeliverOrder(String orderId, String operator);
}
