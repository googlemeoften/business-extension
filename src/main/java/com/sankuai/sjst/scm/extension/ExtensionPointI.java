package com.sankuai.sjst.scm.extension;

/**
 * ExtensionPointI is the parent interface of all ExtensionPoints
 * 扩展点表示一块逻辑在不同的业务有不同的实现，使用扩展点做接口申明，然后用Extension（扩展）去实现扩展点。
 *
 * @author heyong04
 */
public interface ExtensionPointI<T, R> {

    /**
     * 是否执行当前实现的条件
     *
     * @param context 调用上下文
     * @return 是否满足条件
     */
    boolean condition(T context);

    /**
     * 扩展点实现的具体操作
     *
     * @param context 调用上下文
     * @return 执行结果
     */
    R invoke(T context);
}
