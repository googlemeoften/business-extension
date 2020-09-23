package com.sankuai.sjst.scm.extension.executor;

import com.google.common.collect.Lists;
import com.sankuai.sjst.scm.extension.ExtensionPointI;
import com.sankuai.sjst.scm.extension.interruptionstrategy.DefaultInterruptionStrategy;
import com.sankuai.sjst.scm.extension.interruptionstrategy.InterruptionStrategy;

import java.util.List;

/**
 * <p>扩展点抽象执行器</p>
 *
 * @author heyong04@meituan.com
 * @version AbstractComponentExecutor.class 2020-09-14 上午11:33
 * @since 1.0.0
 **/
public abstract class AbstractComponentExecutor {

    /**
     * Execute extension with Response
     *
     * @param targetClz 扩展点接口定义
     * @param context   扩展点上下文信息
     * @param <R>       扩展点接口入参类型
     * @param <T>       扩展点接口出参类型
     * @return 执行结果
     */
    public <R, T> R execute(Class<? extends ExtensionPointI<T, R>> targetClz, T context) {
        ExtensionPointI extensionPointI = locateComponent(targetClz, context);
        return (R) extensionPointI.invoke(context);
    }

    /**
     * Multi Execute extension with Response
     *
     * @param targetClz 扩展点接口
     * @param context   扩展点上下文信息
     * @param <R>       扩展点接口入参类型
     * @param <T>       扩展点接口出参类型
     * @return 执行结果, 使用list包装了每个扩展点实现的返回值
     */
    public <R, T> List<R> multiExecute(Class<? extends ExtensionPointI<T, R>> targetClz, T context) {
        return multiExecute(targetClz, context, new DefaultInterruptionStrategy<>());
    }

    /**
     * Multi Execute extension with Response
     *
     * @param targetClz            扩展点接口
     * @param context              扩展点上下文信息
     * @param <R>                  扩展点接口入参类型
     * @param <T>                  扩展点接口出参类型
     * @param interruptionStrategy 中断策略
     * @return 执行结果, 使用list包装了每个扩展点实现的返回值
     */
    public <R, T> List<R> multiExecute(Class<? extends ExtensionPointI<T, R>> targetClz, T context, InterruptionStrategy<R> interruptionStrategy) {
        List<ExtensionPointI> extensionPointIs = locateComponents(targetClz, context);

        List<R> combinationResult = Lists.newArrayListWithExpectedSize(extensionPointIs.size());
        for (ExtensionPointI extensionPointI : extensionPointIs) {
            R result = (R) extensionPointI.invoke(context);
            combinationResult.add(result);
            if (interruptionStrategy.interrupt(result)) {
                return combinationResult;
            }
        }

        return combinationResult;
    }

    /**
     * 加载扩展实现
     *
     * @param targetClz 扩展点接口
     * @param context   扩展点上下文信息
     * @param <T>       扩展点接口入参类型
     * @param <R>       扩展点接口出参类型
     * @return 扩展点实现
     */
    abstract <T, R> ExtensionPointI locateComponent(Class<? extends ExtensionPointI<T, R>> targetClz, T context);

    /**
     * 加载多个扩展点实现
     *
     * @param <T>       扩展点接口入参类型
     * @param <R>       扩展点接口出参类型
     * @param targetClz 扩展点接口
     * @param context   扩展点接口入参
     * @return 扩展点实现列表
     */
    abstract <T, R> List<ExtensionPointI> locateComponents(Class<? extends ExtensionPointI<T, R>> targetClz, T context);
}
