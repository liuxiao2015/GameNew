package com.game.core.trace;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

/**
 * Dubbo 链路追踪过滤器
 * <p>
 * 自动在 Dubbo RPC 调用间传递 traceId，实现全链路追踪：
 * <ul>
 *     <li>Consumer 端：将当前 traceId 放入 RPC 上下文</li>
 *     <li>Provider 端：从 RPC 上下文恢复 traceId</li>
 * </ul>
 * </p>
 *
 * <p>
 * 自动激活，无需配置。
 * </p>
 *
 * @author GameServer
 */
@Activate(group = {CommonConstants.CONSUMER, CommonConstants.PROVIDER}, order = -10000)
public class DubboTraceFilter implements Filter {

    private static final String TRACE_ID = "traceId";
    private static final String ROLE_ID = "roleId";
    private static final String ACCOUNT_ID = "accountId";
    private static final String SERVER_ID = "serverId";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        RpcContext context = RpcContext.getContext();
        
        if (context.isConsumerSide()) {
            // Consumer 端：传递 traceId
            String traceId = TraceContext.getTraceId();
            if (traceId != null && !traceId.isEmpty()) {
                context.setAttachment(TRACE_ID, traceId);
            }
            
            String roleId = TraceContext.getRoleId();
            if (roleId != null && !roleId.isEmpty()) {
                context.setAttachment(ROLE_ID, roleId);
            }
            
            // 传递其他上下文
            var contextMap = TraceContext.getCopyOfContextMap();
            if (contextMap != null) {
                String accountId = contextMap.get(TraceContext.ACCOUNT_ID_KEY);
                if (accountId != null) {
                    context.setAttachment(ACCOUNT_ID, accountId);
                }
                String serverId = contextMap.get(TraceContext.SERVER_ID_KEY);
                if (serverId != null) {
                    context.setAttachment(SERVER_ID, serverId);
                }
            }
            
        } else if (context.isProviderSide()) {
            // Provider 端：恢复 traceId
            String traceId = context.getAttachment(TRACE_ID);
            if (traceId != null && !traceId.isEmpty()) {
                TraceContext.start(traceId);
            } else {
                // 没有传入则生成新的
                TraceContext.start();
            }
            
            // 恢复 roleId
            String roleId = context.getAttachment(ROLE_ID);
            if (roleId != null && !roleId.isEmpty()) {
                try {
                    TraceContext.setRoleId(Long.parseLong(roleId));
                } catch (NumberFormatException ignored) {}
            }
            
            // 恢复其他上下文
            String accountId = context.getAttachment(ACCOUNT_ID);
            if (accountId != null && !accountId.isEmpty()) {
                try {
                    TraceContext.setAccountId(Long.parseLong(accountId));
                } catch (NumberFormatException ignored) {}
            }
            
            String serverId = context.getAttachment(SERVER_ID);
            if (serverId != null && !serverId.isEmpty()) {
                try {
                    TraceContext.setServerId(Integer.parseInt(serverId));
                } catch (NumberFormatException ignored) {}
            }
        }
        
        try {
            return invoker.invoke(invocation);
        } finally {
            // Provider 端清理上下文
            if (context.isProviderSide()) {
                TraceContext.end();
            }
        }
    }
}
