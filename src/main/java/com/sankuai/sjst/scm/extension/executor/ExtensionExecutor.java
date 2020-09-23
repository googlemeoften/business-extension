package com.sankuai.sjst.scm.extension.executor;

import com.sankuai.sjst.scm.exception.ExtensionException;
import com.sankuai.sjst.scm.extension.ExtensionPointI;
import com.sankuai.sjst.scm.extension.repository.ExtensionRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.core.annotation.AnnotationAwareOrderComparator.INSTANCE;

/**
 * <p>扩展点执行器</p>
 *
 * @author heyong04@meituan.com
 * @version ExtensionExecutor.class 2020-09-14 上午11:33
 * @since 1.0.0
 **/
@Component
public class ExtensionExecutor extends AbstractComponentExecutor {

    @Resource
    private ExtensionRepository extensionRepository;

    @Override
    <T, R> ExtensionPointI locateComponent(Class<? extends ExtensionPointI<T, R>> targetClz, T context) {
        return locateExtension(targetClz, context);
    }

    @Override
    <T, R> List<ExtensionPointI> locateComponents(Class<? extends ExtensionPointI<T, R>> targetClz, T context) {
        return locateExtensions(targetClz, context);
    }

    /**
     * 获取多个扩展点
     *
     * @param <T>       扩展点接口参数类型
     * @param <R>       扩展点接口出参类型
     * @param targetClz 扩展点接口
     * @param context   扩展点接口参数
     * @return 扩展点实现列表
     */
    private <T, R> List<ExtensionPointI> locateExtensions(Class<? extends ExtensionPointI<T, R>> targetClz, T context) {
        checkNull(context);

        List<ExtensionPointI> allExtensionPointIs = extensionRepository.get(targetClz);
        List<ExtensionPointI> extensionPointIs = allExtensionPointIs.stream()
                .filter(pointI -> pointI.condition(context))
                .sorted(INSTANCE)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(extensionPointIs)) {
            throw new ExtensionException("Can not find extension with ExtensionPoint: " + targetClz);
        }

        return extensionPointIs;
    }

    /**
     * 获取当扩展点
     *
     * @param targetClz 扩展点接口
     * @param context   参数
     * @param <T>       扩展点接口参数类型
     * @param <R>       扩展点接口出参类型
     * @return 扩展点实现
     */
    private <T, R> ExtensionPointI locateExtension(Class<? extends ExtensionPointI<T, R>> targetClz, T context) {
        checkNull(context);

        List<ExtensionPointI> allExtensionPointIs = extensionRepository.get(targetClz);
        return allExtensionPointIs.stream()
                .filter(pointI -> pointI.condition(context))
                .findFirst()
                .orElseThrow(() -> new ExtensionException("Can not find extension with ExtensionPoint: " + targetClz));
    }

    /**
     * 检查参数是否为空
     *
     * @param context 参数
     * @param <T>     扩展点入参类型
     */
    private <T> void checkNull(T context) {
        if (context == null) {
            throw new ExtensionException("context can not be null for extension");
        }
    }
}
