package com.sankuai.sjst.scm.extension.repository;

import com.google.common.collect.Lists;
import com.sankuai.sjst.scm.extension.ExtensionPointI;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>扩展点仓库，使用map存放扩展点接口以及对应实现类</p>
 *
 * @author heyong04@meituan.com
 * @version ExtensionRepository.class 2020-09-14 上午11:33
 * @since 1.0.0
 **/
@Component
public class ExtensionRepository {
    private static final int DEFAULT_CAPACITY_SIZE = 128;

    /**
     * 扩展类接口名称
     */
    private Map<Class<? extends ExtensionPointI>, List<ExtensionPointI>> extensionRepo = new ConcurrentHashMap<>(DEFAULT_CAPACITY_SIZE);

    /**
     * 获取扩展点实现类
     *
     * @param clazz 类
     * @return 实现类列表
     */
    public List<ExtensionPointI> get(Class<? extends ExtensionPointI> clazz) {
        return extensionRepo.getOrDefault(clazz, Lists.newCopyOnWriteArrayList());
    }

    /**
     * 添加扩展点实现类
     *
     * @param clazz           类
     * @param extensionPointI 实现类
     */
    public void put(Class<? extends ExtensionPointI> clazz, ExtensionPointI extensionPointI) {
        List<ExtensionPointI> extensionPointIs = extensionRepo.getOrDefault(clazz, Lists.newCopyOnWriteArrayList());
        extensionPointIs.add(extensionPointI);

        extensionRepo.put(clazz, extensionPointIs);
    }
}
