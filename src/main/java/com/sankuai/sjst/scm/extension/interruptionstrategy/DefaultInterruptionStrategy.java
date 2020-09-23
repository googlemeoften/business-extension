package com.sankuai.sjst.scm.extension.interruptionstrategy;

/**
 * <p>扩展点执行默认中断策略</p>
 *
 * @author heyong04@meituan.com
 * @version DefaultInterruptionStrategy.class 2020-09-14 上午11:33
 * @since 1.0.0
 **/
public class DefaultInterruptionStrategy<R> implements InterruptionStrategy<R> {
    @Override
    public boolean interrupt(R extensionPointResult) {
        return false;
    }
}
