02_扩展点设计

# 一、业务举例

供应链的业务中，一个业务流程涉及到多个节点，并且每个节点的实现逻辑不同，如下图所示

![image.png](https://upload-images.jianshu.io/upload_images/6763914-4acac163d6478ef5.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240) 

每一个节点都可能存在不同的实现，有时候需要从多个实现中选择一个(互斥)，有时候需要选择多个（组合）。如果不对各种实现进行良好的管理，带来的问题是：

*   代码圈复杂度高。if-else，switch分支多，影响代码主干流程。阅读性差，新人学习成本高

*   分支之间没有做隔离，改了一个地方可能影响其他分支

*   随着时间推移，需求增多，代码越来越复杂，慢慢形成祖传代码，之前看到的一张图，就比较好的形容这种祖传代码

![image.png](https://upload-images.jianshu.io/upload_images/6763914-976dc4d46294096c.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

# 二、场景收集&分析

![image.png](https://upload-images.jianshu.io/upload_images/6763914-72c29f95c263bf54.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

1.  节点管理：节点管理本质上就是代码隔离，即将一个节点的不同实现分散到不同的类里面。

2.  互斥：不同分支实现相互隔离，根据条件选择唯一的实现

3.  组合：一个节点的多个实现同时执行

4.  优先级管理：在组合模式下，调用节点的多个实现，但是实现有优先级顺序

5.  中断策略：在组合模式下，调用节点的多个实现，根据节点返回结果判断是否继续向下执行

# 三、方案调研

## (一) Java SPI调研

针对于上一节中提到的节点多种实现的问题，Java的SPI可以解决我们的问题。

![image.png](https://upload-images.jianshu.io/upload_images/6763914-2cdbf136014b4b59.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

Java SPI使用约定：

1、当服务提供者提供了接口的一种具体实现后，在jar包的META-INF/services目录下创建一个以“接口全限定名”为命名的文件，内容为实现类的全限定名；

2、接口实现类所在的jar包放在主程序的classpath中；

3、主程序通过java.util.ServiceLoder动态装载实现模块，它通过扫描META-INF/services目录下的配置文件找到实现类的全限定名，把类加载到JVM；

4、SPI的实现类必须携带一个不带参数的构造方法；

## (二) Cola 框架 & Halo框架调研

扩展点（ExtensionPoint）必须通过接口申明，扩展实现（Extension）是通过Annotation的方式标注的，Extension里面使用BizCode和TenantId两个属性用来标识身份，

框架的Bootstrap类会在Spring启动的时候做类扫描，进行Extension注册，在Runtime的时候，通过TenantContext来选择要使用的Extension。TenantContext是通过Interceptor在调用业务逻辑之前进行初始化的。整个过程如下图所示：

![image.png](https://upload-images.jianshu.io/upload_images/6763914-21487fc85d5bcc6e.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

扩展点实现路由

![image.png](https://upload-images.jianshu.io/upload_images/6763914-fb02077f2b5e8ae0.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


比如在一个CRM系统里，客户要添加联系人Contact是一个，但是在添加联系人之前，我们要判断这个Contact是不是已经存在了，如果存在那么就不能添加了。不过在一个支持多业务的系统里面，可能每个业务的冲突检查都不一样，这是一个典型的可以扩展的场景。

那么在SOFA框架中，我们可以这样去做。

```
public interface ContactConflictRuleExtPt extends RuleI, ExtensionPointI {
   /**
    * 查询联系人冲突
    *
    * @param contact 冲突条件，不同业务会有不同的判断规则
    * @return 冲突结果
    */
   public boolean queryContactConflict(ContactE contact);
}
```

2、实现业务的扩展实现
```
@Extension(bizCode = BizCode.ICBU)
public class IcbuContactConflictRuleExt implements ContactConflictRuleExtPt {

    @Autowired
    private RepeatCheckServiceI repeatCheckService;
    @Autowired
    private MemberMappingQueryTunnel memberMappingQueryTunnel;
    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 查询联系人冲突
     *
     * @param contact 冲突条件，不同业务会有不同的判断规则
     * @return 冲突结果
     */
    @Override
    public boolean queryContactConflict(ContactE contact) {

        Set<String> emails = contact.getEmail();

        //具体的业务逻辑
        
        return false;
    }
```
3、在领域实体中调用扩展实现
```
@ToString
@Getter
@Setter
public class CustomerE extends Entity {
	/**
	 * 公司ID
	 */
	private String companyId;
	/**
	 * 公司(客户)名字
	 */
	private String companyName;
	/**
	 * 公司(客户)英文名字
	 */
	private String companyNameEn;
          /**
	 * 给客户添加联系人
	 * @param contact
	 */
	public void addContact(ContactE contact,boolean checkConflict){
		// 业务检查
		if (checkConflict) {
			ruleExecutor.execute(ContactConflictRuleExtPt.class, p -> p.queryContactConflict(contact));
		}
		contact.setCustomerId(this.getId());
		contactRepository.create(contact);
	}
}
```
## (三) 我们对于扩展点的需求

![image.png](https://upload-images.jianshu.io/upload_images/6763914-3130df5536c19766.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

cola扩展点的缺陷：

*   cola扩展点不支持组合场景

*   cola框架的Bootstrap类会在Spring启动的时候做类扫描，进行Extension注册，在Runtime的时候，通过TenantContext（身份标识信息）来选择要使用的Extension。TenantContext是通过Interceptor在调用业务逻辑之前进行初始化的，在供应链场景中，现在无法抽象出身份标识信息；或者执行扩展点的时候传参包含身份标识信息，如果业务场景比较复杂，构造身份标识信息会比较麻烦，因此考虑把扩展点的路由交个具体实现类处理，通过调用扩展点实现类的condition方案，判断是否执行该扩展点，扩展点实现：[02_扩展点设计](https://km.sankuai.com/page/377222237#id-%E5%9B%9B%E3%80%81%E4%B8%9A%E5%8A%A1%E6%89%A9%E5%B1%95%E7%82%B9%E5%8E%9F%E7%90%86)

# 三、业务扩展点使用

### 1、xml配置
```
<context:component-scan base-package="com.sankuai.sjst"/>
```
### 2、扩展点接口定义

扩展点必须以ExtPt结尾，通过ExtPt明显标识这是一个扩展点,扩展点实现类以Ext结尾

### 3、扩展点互斥场景实现

*   定义业务扩展点接口

```
public interface AgreementGoodsBOBuilderExtPt extends ExtensionPointI<ScmIntelligentQueryGoodsContext, List<ScmPurchaseGoodsWithSuppliersBO>> {
}
```
*   扩展点实现类-1
```
@Extension(name = "通过查询主数据es索引构建GoodsUnitBO")
public class AgreementGoodsBOBuilderByESQueryExt implements AgreementGoodsBOBuilderExtPt {
    @Resource
    private RemoteMainDataQueryService remoteMainDataQueryService;

    @Override
    public boolean condition(ScmIntelligentQueryGoodsContext context) {

        ScmIntelligentQueryGoodsConditionTO queryGoodsConditionTO = context.getQueryGoodsConditionTO();
        // GoodsUnitTO为空 且goodsIds不存在
        return queryGoodsConditionTO.getGoodsUnitTO() == null && queryGoodsConditionTO.getGoodsIdsSize() == 0;
    }

    @Override
    public List<ScmPurchaseGoodsWithSuppliersBO> invoke(ScmIntelligentQueryGoodsContext context) {
         // 业务逻辑
    }
}
```

*   扩展点实现类-2

```
@Extension(name = "通过goodsIds参数构建GoodsUnitBO")
public class AgreementGoodsBOBuilderByGoodsIdsExt implements AgreementGoodsBOBuilderExtPt {
   	// spring 依赖注入
    @Resource
    private RemoteBaseService remoteBaseService;

    @Override
    public boolean condition(ScmIntelligentQueryGoodsContext context) {
        List<Long> goodsIds = context.getQueryGoodsConditionTO().getGoodsIds();
        return CollectionUtils.isNotEmpty(goodsIds);
    }

    @Override
    public List<ScmPurchaseGoodsWithSuppliersBO> invoke(ScmIntelligentQueryGoodsContext context) {
        // 业务逻辑
    }
}
```
*   调用扩展点
```
List<ScmPurchaseGoodsWithSuppliersBO> purchaseAgreementGoodsBOs = extensionExecutor.execute(AgreementGoodsBOBuilderExtPt.class, context);
```

### 4、扩展点组合+优先级管理 + 中断策略实现

*   扩展点接口定义
```
/**
 * 智能采购物品协议校验扩展点
 */
public interface IntelligentPurchaseGoodsAgreementCheckExtPt extends ExtensionPointI<ScmIntelligentPurchaseCheckContext, ErrorItemAndStatus> {
}
```

*   扩展点实现-1
```
@Order(1)
@Extension(name = "智能采购-配送中心配送物品校验")
public class DistributionGoodsAgreementCheckExtPt implements IntelligentPurchaseGoodsAgreementCheckExtPt {
    @Resource
    private ScmSupplierCheckService scmSupplierCheckService;

    @Override
    public boolean condition(ScmIntelligentPurchaseCheckContext context) {
        return CollectionUtils.isNotEmpty(context.getGoodsAndDistributionOrgBOs());
    }

    @Override
    public ErrorItemAndStatus invoke(ScmIntelligentPurchaseCheckContext checkContext) {
        // 业务逻辑
        return new ErrorItemAndStatus();
    }
}
```

*   扩展点实现-2
```
@Order(2)
@Extension(name = "智能采购-供应商采购物品校验")
public class SupplierGoodsAgreementCheckExtPt implements IntelligentPurchaseGoodsAgreementCheckExtPt {
    @Override
    public boolean condition(ScmIntelligentPurchaseCheckContext context) {
        return CollectionUtils.isNotEmpty(context.getGoodsAndSupplierBOs());
    }

    @Override
    public ErrorItemAndStatus invoke(ScmIntelligentPurchaseCheckContext context) {
       	// 业务逻辑
        return new ErrorItemAndStatus();
    }
}

```

*   扩展点执行

```
// 智能采购-配送物品协议校验
List<ErrorItemAndStatus> errorItemAndStatuses =
        extensionExecutor.multiExecute(
                IntelligentPurchaseGoodsAgreementCheckExtPt.class,//扩展点接口
                intelligentPurchaseCheckContext,// 参数
                errorItemAndStatus -> ThriftStatusHelper.iserrorItemAndStatus.getStatus()));// 中断策略
```

# 四、业务扩展点原理

## (一)、原理

![image.png](https://upload-images.jianshu.io/upload_images/6763914-fa32f440422b89ee.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

*   spring在容器在启动的时候，会调用getBean方法实例化&初始化对象
```
public void refresh() throws BeansException, IllegalStateException {

        synchronized (this.startupShutdownMonitor) {

            // Prepare this context for refreshing.

            prepareRefresh();

            // Tell the subclass to refresh the internal bean factory.

            ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

            // Prepare the bean factory for use in this context.

            prepareBeanFactory(beanFactory);

            try {

                // Allows post-processing of the bean factory in context subclasses.

                postProcessBeanFactory(beanFactory);

                // 调用 factory processors registered as beans in the context.

                invokeBeanFactoryPostProcessors(beanFactory);

                // Register bean processors that intercept bean creation.

                registerBeanPostProcessors(beanFactory);

                // Initialize message source for this context.

                initMessageSource();

                // Initialize event multicaster for this context.

                initApplicationEventMulticaster();

                // Initialize other special beans in specific context subclasses.

                onRefresh();

                // Check for listener beans and register them.

                registerListeners();

                // 初始化所有单例对象

                finishBeanFactoryInitialization(beanFactory);

                // Last step: publish corresponding event.

                finishRefresh();

            }catch (BeansException ex) {

                if (logger.isWarnEnabled()) {

                    logger.warn("Exception encountered during context initialization - " +

                            "cancelling refresh attempt: " + ex);

                }

                // Destroy already created singletons to avoid dangling resources.

                destroyBeans();

                // Reset 'active' flag.

                cancelRefresh(ex);

                // Propagate exception to caller.

                throw ex;

            }finally {

                // Reset common introspection caches in Spring's core, since we

                // might not ever need metadata for singleton beans anymore...

                resetCommonCaches();

            }

        }

    }
```

*   初始化过程中会执行spring开发出来的扩展点，我们的业务扩展点框架实现了BeanPostProcessor接口，判断对象的class是否有Extension注解，如果存在组件，将对象添加到ExtensionRepository中，其内部接口是Map<String, List<ExtensionPointI>>结果，key是扩展点接口的类名称，value是实现类列表

*   当要执行扩展点时，通过调用ExtensionExecutor.execute方法，实现选择一个扩展点实现类，来进行调用；调用ExtensionExecutor.multiExecute方法，按扩展点实现类的优先级先后进行调用，如果设置了中断策略，在执行下一个扩展点实现类之前会先判断是否中断

## (二)、核心模型

### 1、扩展点接口：

```
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
```

### 2、扩展点注解

用在扩展点实现类上，使用该注解，会将实现类注入到spring容器中

```
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Component
public @interface Extension {
    String name() default "";
}
```

### 3、Spring BeanPostProcessor扩展点实现
```
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
 * ExtensionRegister
 *
 * @author heyong
 */
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
     * @param targetClz 子类
     * @return
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
```
### 4、扩展点执行器
```
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
```

### 5、中断策略
```
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
     * @return
     */
    boolean interrupt(R extensionPointResult);
}
```

# 五、使用规范
![image.png](https://upload-images.jianshu.io/upload_images/6763914-9d2883a97276b25d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


# 六、扩展点相对于策略模式优势

1、基于Strategy Pattern的扩展，没有找到一个很好的固化到框架中的方法

2、使用Strategy Pattern，没有规范的限制，编码相对随意

# 七、参考文档

[https://blog.csdn.net/significantfrank/article/details/85785565](https://blog.csdn.net/significantfrank/article/details/85785565)
