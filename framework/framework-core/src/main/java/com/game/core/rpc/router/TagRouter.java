package com.game.core.rpc.router;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签路由器 - 支持灰度发布
 * <p>
 * 基于标签进行服务路由，用于灰度发布、金丝雀发布等场景。
 * </p>
 *
 * <pre>
 * 使用示例:
 * 1. Provider 端设置标签:
 *    dubbo.provider.tag=gray
 *
 * 2. Consumer 端设置路由:
 *    TagRouter.setTag("gray");
 *    guildService.getGuildInfo(guildId);
 *    TagRouter.clearTag();
 *
 * 3. 或通过 HTTP Header 传递:
 *    X-Dubbo-Tag: gray
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Activate(order = 100)
public class TagRouter extends AbstractRouter {

    /**
     * 标签 Key
     */
    public static final String TAG_KEY = "dubbo.tag";
    
    /**
     * 线程级标签
     */
    private static final ThreadLocal<String> TAG_HOLDER = new ThreadLocal<>();

    /**
     * 是否强制标签匹配（不匹配时是否降级到无标签实例）
     */
    private static final ThreadLocal<Boolean> FORCE_HOLDER = ThreadLocal.withInitial(() -> false);

    public TagRouter(URL url) {
        super(url);
        // 使用 setter 设置优先级
        setPriority(150);  // 优先级高于默认路由
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, 
                                       Invocation invocation) throws RpcException {
        if (invokers == null || invokers.isEmpty()) {
            return invokers;
        }

        // 获取当前标签
        String tag = getTag();
        if (tag == null || tag.isEmpty()) {
            // 没有标签，返回无标签的实例或所有实例
            return filterNoTag(invokers);
        }

        // 按标签过滤
        List<Invoker<T>> result = new ArrayList<>();
        List<Invoker<T>> noTagInvokers = new ArrayList<>();

        for (Invoker<T> invoker : invokers) {
            String invokerTag = invoker.getUrl().getParameter(TAG_KEY);
            if (tag.equals(invokerTag)) {
                result.add(invoker);
            } else if (invokerTag == null || invokerTag.isEmpty()) {
                noTagInvokers.add(invoker);
            }
        }

        // 如果找到匹配标签的实例
        if (!result.isEmpty()) {
            log.debug("[TagRouter] 匹配标签 '{}': {}/{} 个实例", 
                    tag, result.size(), invokers.size());
            return result;
        }

        // 没有匹配的实例
        boolean force = FORCE_HOLDER.get();
        if (force) {
            // 强制模式：必须匹配标签
            log.warn("[TagRouter] 强制模式下无匹配标签 '{}' 的实例", tag);
            return result;  // 返回空，触发 NoInvoker 异常
        }

        // 非强制模式：降级到无标签实例
        if (!noTagInvokers.isEmpty()) {
            log.debug("[TagRouter] 降级到无标签实例: {}/{} 个", 
                    noTagInvokers.size(), invokers.size());
            return noTagInvokers;
        }

        // 最终降级：返回所有实例
        log.debug("[TagRouter] 最终降级，返回所有实例: {} 个", invokers.size());
        return invokers;
    }

    /**
     * 过滤无标签实例（用于无标签请求）
     */
    private <T> List<Invoker<T>> filterNoTag(List<Invoker<T>> invokers) {
        List<Invoker<T>> result = new ArrayList<>();
        for (Invoker<T> invoker : invokers) {
            String invokerTag = invoker.getUrl().getParameter(TAG_KEY);
            if (invokerTag == null || invokerTag.isEmpty()) {
                result.add(invoker);
            }
        }
        
        // 如果没有无标签实例，返回所有
        if (result.isEmpty()) {
            return invokers;
        }
        return result;
    }

    /**
     * 获取当前标签
     */
    private String getTag() {
        // 优先从线程本地获取
        String tag = TAG_HOLDER.get();
        if (tag != null && !tag.isEmpty()) {
            return tag;
        }

        // 从 RPC 上下文获取
        tag = RpcContext.getContext().getAttachment(TAG_KEY);
        if (tag != null && !tag.isEmpty()) {
            return tag;
        }

        return null;
    }

    // ==================== 静态方法供业务使用 ====================

    /**
     * 设置当前请求的路由标签
     *
     * @param tag 标签 (如: gray, canary, test)
     */
    public static void setTag(String tag) {
        TAG_HOLDER.set(tag);
        RpcContext.getContext().setAttachment(TAG_KEY, tag);
    }

    /**
     * 设置当前请求的路由标签（强制模式）
     *
     * @param tag   标签
     * @param force 是否强制匹配
     */
    public static void setTag(String tag, boolean force) {
        setTag(tag);
        FORCE_HOLDER.set(force);
    }

    /**
     * 清除标签
     */
    public static void clearTag() {
        TAG_HOLDER.remove();
        FORCE_HOLDER.remove();
        RpcContext.getContext().removeAttachment(TAG_KEY);
    }

    /**
     * 获取当前标签
     */
    public static String currentTag() {
        return TAG_HOLDER.get();
    }
}
