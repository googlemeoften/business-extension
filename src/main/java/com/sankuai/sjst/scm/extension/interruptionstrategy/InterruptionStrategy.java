package com.sankuai.sjst.scm.extension.interruptionstrategy;

/**
 * <p>扩展点执行中断策略接口</p>
 *
 * @author heyong04@meituan.com
 * @version InterruptionStrategy.class 2020-09-14 上午11:33
 * @since 1.0.0
 **/
public interface InterruptionStrategy<R> {
    /**
     * 是否中断执行
     *
     * @param extensionPointResult 扩展点执行返回结果
     * @return true表示需要中断，false表示不需要中断
     */
    boolean interrupt(R extensionPointResult);
}
