package com.game.service.pay.service;

import com.game.api.pay.*;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.entity.document.*;
import com.game.entity.repository.*;
import com.game.service.pay.channel.PayChannelAdapter;
import com.game.service.pay.channel.PayChannelFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 支付服务实现
 *
 * @author GameServer
 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {
    
    private final PayOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PayRecordRepository recordRepository;
    private final PayChannelFactory channelFactory;
    private final DeliverService deliverService;
    
    @Value("${pay.order-expire-minutes:30}")
    private int orderExpireMinutes;
    
    @Override
    public Result<OrderDTO> createOrder(CreateOrderRequest request) {
        log.info("创建订单: roleId={}, productId={}, channel={}", 
                request.getRoleId(), request.getProductId(), request.getChannel());
        
        // 1. 验证商品
        Product product = productRepository.findByProductId(request.getProductId()).orElse(null);
        if (product == null || !product.isEnabled()) {
            return Result.fail(ErrorCode.PARAM_ERROR, "商品不存在或已下架");
        }
        
        // 2. 验证限购
        Result<Void> limitResult = checkBuyLimit(request.getRoleId(), product);
        if (!limitResult.isSuccess()) {
            return Result.fail(limitResult.getCode(), limitResult.getMessage());
        }
        
        // 3. 验证支付渠道
        PayChannel channel = PayChannel.of(request.getChannel());
        if (channel == null) {
            return Result.fail(ErrorCode.PARAM_ERROR, "不支持的支付渠道");
        }
        if (!channelFactory.isChannelAvailable(channel)) {
            return Result.fail(ErrorCode.PARAM_ERROR, "支付渠道不可用");
        }
        
        // 4. 生成订单号
        String orderId = generateOrderId(request.getServerId());
        
        // 5. 创建订单
        PayOrder order = new PayOrder();
        order.setOrderId(orderId);
        order.setRoleId(request.getRoleId());
        order.setAccountId(request.getAccountId());
        order.setServerId(request.getServerId());
        order.setProductId(product.getProductId());
        order.setProductName(product.getName());
        order.setAmount(product.getPrice());
        order.setCurrency(product.getCurrency());
        order.setChannel(channel);
        order.setStatus(OrderStatus.PENDING);
        order.setClientIp(request.getClientIp());
        order.setDeviceId(request.getDeviceId());
        order.setPlatform(request.getPlatform());
        order.setExpireTime(LocalDateTime.now().plusMinutes(orderExpireMinutes));
        order.setExtra(request.getExtra());
        
        orderRepository.save(order);
        log.info("订单创建成功: orderId={}", orderId);
        
        // 6. 创建支付请求
        PayChannelAdapter adapter = channelFactory.getAdapter(channel);
        OrderDTO.PayParams payParams = adapter.createPayRequest(order);
        
        // 7. 更新订单状态
        order.setStatus(OrderStatus.PAYING);
        orderRepository.save(order);
        
        // 8. 返回订单信息
        return Result.success(toOrderDTO(order, payParams));
    }
    
    @Override
    public Result<OrderDTO> getOrder(String orderId) {
        PayOrder order = orderRepository.findByOrderId(orderId).orElse(null);
        if (order == null) {
            return Result.fail(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return Result.success(toOrderDTO(order, null));
    }
    
    @Override
    public Result<List<OrderDTO>> getOrders(long roleId, int page, int size) {
        // 简单分页，实际可使用 Spring Data 分页
        List<PayOrder> orders = orderRepository.findByRoleIdOrderByCreateTimeDesc(roleId);
        int start = Math.min(page * size, orders.size());
        int end = Math.min(start + size, orders.size());
        
        List<OrderDTO> list = orders.subList(start, end).stream()
                .map(o -> toOrderDTO(o, null))
                .collect(Collectors.toList());
        
        return Result.success(list);
    }
    
    @Override
    public Result<Void> cancelOrder(String orderId, String reason) {
        PayOrder order = orderRepository.findByOrderId(orderId).orElse(null);
        if (order == null) {
            return Result.fail(ErrorCode.NOT_FOUND, "订单不存在");
        }
        
        if (!order.getStatus().canCancel()) {
            return Result.fail(ErrorCode.OPERATION_FORBIDDEN, "当前状态不可取消");
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        order.setRemark(reason);
        orderRepository.save(order);
        
        log.info("订单已取消: orderId={}, reason={}", orderId, reason);
        return Result.success();
    }
    
    @Override
    public Result<List<ProductDTO>> getProducts(long roleId) {
        List<Product> products = productRepository.findByEnabledTrueOrderBySortOrderAsc();
        
        List<ProductDTO> list = products.stream()
                .filter(p -> isProductAvailable(p, roleId))
                .map(p -> toProductDTO(p, roleId))
                .collect(Collectors.toList());
        
        return Result.success(list);
    }
    
    @Override
    public Result<ProductDTO> getProduct(int productId) {
        Product product = productRepository.findByProductId(productId).orElse(null);
        if (product == null) {
            return Result.fail(ErrorCode.NOT_FOUND, "商品不存在");
        }
        return Result.success(toProductDTO(product, 0));
    }
    
    @Override
    public Result<Boolean> isFirstPay(long roleId) {
        boolean hasPaid = recordRepository.existsByRoleId(roleId);
        return Result.success(!hasPaid);
    }
    
    @Override
    public Result<Long> getTotalPayAmount(long roleId) {
        // TODO: 实现累计充值金额查询
        return Result.success(0L);
    }
    
    @Override
    public Result<Void> redeliverOrder(String orderId, String operator) {
        log.info("补发订单道具: orderId={}, operator={}", orderId, operator);
        
        PayOrder order = orderRepository.findByOrderId(orderId).orElse(null);
        if (order == null) {
            return Result.fail(ErrorCode.NOT_FOUND, "订单不存在");
        }
        
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.DELIVERING) {
            return Result.fail(ErrorCode.OPERATION_FORBIDDEN, "订单状态不正确");
        }
        
        // 重置重试次数并触发发放
        order.setDeliverRetryCount(0);
        order.setRemark("GM补发: " + operator);
        orderRepository.save(order);
        
        deliverService.deliverOrder(order);
        
        return Result.success();
    }
    
    /**
     * 生成订单号
     */
    private String generateOrderId(int serverId) {
        // 格式: 服务器ID(4位) + 日期(8位) + 时间戳后6位 + 随机数(4位)
        String date = LocalDate.now().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis() % 1000000);
        String random = String.valueOf((int) (Math.random() * 10000));
        return String.format("%04d%s%06d%04d", serverId, date, Long.parseLong(timestamp), Integer.parseInt(random));
    }
    
    /**
     * 检查限购
     */
    private Result<Void> checkBuyLimit(long roleId, Product product) {
        if (product.getBuyLimit() > 0) {
            long count = recordRepository.countByRoleIdAndProductId(roleId, product.getProductId());
            if (count >= product.getBuyLimit()) {
                return Result.fail(ErrorCode.LIMIT_EXCEEDED, "已达到购买上限");
            }
        }
        
        if (product.getDailyLimit() > 0) {
            String today = LocalDate.now().toString();
            long todayCount = recordRepository.countByRoleIdAndProductIdAndPayDate(roleId, product.getProductId(), today);
            if (todayCount >= product.getDailyLimit()) {
                return Result.fail(ErrorCode.LIMIT_EXCEEDED, "已达到今日购买上限");
            }
        }
        
        return Result.success();
    }
    
    /**
     * 检查商品是否可用
     */
    private boolean isProductAvailable(Product product, long roleId) {
        long now = System.currentTimeMillis();
        
        if (product.getStartTime() != null && now < product.getStartTime()) {
            return false;
        }
        if (product.getEndTime() != null && now > product.getEndTime()) {
            return false;
        }
        
        // TODO: 检查等级、VIP等级要求
        
        return true;
    }
    
    /**
     * 转换为OrderDTO
     */
    private OrderDTO toOrderDTO(PayOrder order, OrderDTO.PayParams payParams) {
        return OrderDTO.builder()
                .orderId(order.getOrderId())
                .tradeNo(order.getTradeNo())
                .roleId(order.getRoleId())
                .productId(order.getProductId())
                .productName(order.getProductName())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .channel(order.getChannel().getCode())
                .status(order.getStatus().getCode())
                .statusDesc(order.getStatus().getDesc())
                .createTime(order.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .payTime(order.getPayTime() != null ? order.getPayTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0)
                .expireTime(order.getExpireTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .payParams(payParams)
                .build();
    }
    
    /**
     * 转换为ProductDTO
     */
    private ProductDTO toProductDTO(Product product, long roleId) {
        int remainBuyCount = -1;
        int remainDailyBuyCount = -1;
        
        if (product.getBuyLimit() > 0 && roleId > 0) {
            long count = recordRepository.countByRoleIdAndProductId(roleId, product.getProductId());
            remainBuyCount = (int) (product.getBuyLimit() - count);
        }
        
        if (product.getDailyLimit() > 0 && roleId > 0) {
            String today = LocalDate.now().toString();
            long todayCount = recordRepository.countByRoleIdAndProductIdAndPayDate(roleId, product.getProductId(), today);
            remainDailyBuyCount = (int) (product.getDailyLimit() - todayCount);
        }
        
        List<ProductDTO.RewardDTO> rewards = null;
        if (product.getRewards() != null) {
            rewards = product.getRewards().stream()
                    .map(r -> ProductDTO.RewardDTO.builder()
                            .type(r.getType())
                            .itemId(r.getItemId())
                            .count(r.getCount())
                            .build())
                    .collect(Collectors.toList());
        }
        
        List<ProductDTO.RewardDTO> firstBuyRewards = null;
        if (product.getFirstBuyRewards() != null) {
            firstBuyRewards = product.getFirstBuyRewards().stream()
                    .map(r -> ProductDTO.RewardDTO.builder()
                            .type(r.getType())
                            .itemId(r.getItemId())
                            .count(r.getCount())
                            .build())
                    .collect(Collectors.toList());
        }
        
        return ProductDTO.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .currency(product.getCurrency())
                .type(product.getType())
                .icon(product.getIcon())
                .tags(product.getTags())
                .appleProductId(product.getAppleProductId())
                .googleProductId(product.getGoogleProductId())
                .rewards(rewards)
                .firstBuyRewards(firstBuyRewards)
                .remainBuyCount(remainBuyCount)
                .remainDailyBuyCount(remainDailyBuyCount)
                .build();
    }
}
