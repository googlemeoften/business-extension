package com.sankuai.sjst.scm.extension.register;

import com.sankuai.sjst.scm.constant.ExtensionConstant;
import com.sankuai.sjst.scm.exception.ExtensionException;
import com.sankuai.sjst.scm.extension.Extension;
import com.sankuai.sjst.scm.extension.ExtensionPointI;
import com.sankuai.sjst.scm.extension.RegisterI;
import com.sankuai.sjst.scm.extension.repository.ExtensionRepository;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * <p>扩展点注册类，扫描spring容器中的bean，并添加到ExtensionRepository中</p>
 *
 * @author heyong04@meituan.com
 * @version ExtensionRegister.class 2020-09-14 上午11:33
 * @since 1.0.0
 **/
@Component
public class ExtensionRegister implements RegisterI, BeanPostProcessor {
    // 防止bean重复添加到ExtensionRepository
    private static final ConcurrentSkipListSet<String> EXTENSION_BEAN_NAME_SET = new ConcurrentSkipListSet<>();

    @Autowired
    private ExtensionRepository extensionRepository;

    @Override
    public void doRegistration(Class<?> clazz, ExtensionPointI extensionPointI) {
        Class<? extends ExtensionPointI> extPtClass = calculateExtensionPoint(clazz);
        extensionRepository.put(extPtClass, extensionPointI);
    }

    /**
     * 获取扩展点接口
     *
     * @param targetClz 实现类class
     * @return 实现类继承的接口
     */
    private Class<? extends ExtensionPointI> calculateExtensionPoint(Class<?> targetClz) {

        Class[] interfaces = targetClz.getInterfaces();
        if (ArrayUtils.isEmpty(interfaces)) {
            throw new ExtensionException("Please assign a extension point interface for " + targetClz);
        }

        for (Class iface : interfaces) {
            String extensionPoint = iface.getSimpleName();
            if (StringUtils.contains(extensionPoint, ExtensionConstant.EXTENSION_EXTPT_NAMING)) {
                return iface;
            }
        }
        throw new ExtensionException("Your name of ExtensionPoint for " + targetClz + " is not valid, must be end of " + ExtensionConstant.EXTENSION_EXTPT_NAMING);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 已经处理过的扩展点类，不需要处理
        if (EXTENSION_BEAN_NAME_SET.contains(beanName)) {
            return bean;
        }

        Class<?> targetClass = AopUtils.getTargetClass(bean);
        Extension extension = AnnotationUtils.findAnnotation(targetClass, Extension.class);
        if (Objects.nonNull(extension)) {
            EXTENSION_BEAN_NAME_SET.add(beanName);
            doRegistration(targetClass, (ExtensionPointI) bean);
        }
        return bean;
    }

}