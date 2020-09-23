package com.sankuai.sjst.scm.extension;

public interface RegisterI {
    /**
     * 扩展点注入接口
     *
     * @param clazz           扩展点class
     * @param extensionPointI 扩展点实现类
     */
    void doRegistration(Class<?> clazz, ExtensionPointI extensionPointI);
}