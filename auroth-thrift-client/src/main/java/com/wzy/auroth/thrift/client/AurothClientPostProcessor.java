package com.wzy.auroth.thrift.client;

import com.wzy.auroth.thrift.annotation.TReference;
import com.wzy.auroth.thrift.annotation.TReferenceMeta;
import com.wzy.auroth.thrift.client.advice.ThriftClientAdvice;
import com.wzy.auroth.thrift.utils.ThriftUtils;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.protocol.TProtocol;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AurothClientPostProcessor implements BeanPostProcessor {

    private Map<String, List<Class>> beansToProcess = new HashMap<>(256);

    @Setter
    private DefaultListableBeanFactory beanFactory;

    @Setter
    private ThriftClientAdvice[] advices;

    /**
     * 处理那些bean需要进行代理
     * <p>
     * 对需要进行代理的TServiceClient进行处理
     *
     * @param bean     bean
     * @param beanName beanName
     * @return bean
     * @throws BeansException BeansException
     */
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        do {
            for (Method method : clazz.getMethods()) {
                String name = method.getName();
                if (name.length() > 3 && name.startsWith("set")
                        && method.getParameterTypes().length == 1
                        && Modifier.isPublic(method.getModifiers())
                        && !Modifier.isStatic(method.getModifiers())) {
                    TReference annotation = AnnotationUtils.getAnnotation(method, TReference.class);
                    if (annotation != null) {
                        if (!beansToProcess.containsKey(beanName)) {
                            beansToProcess.put(beanName, new ArrayList<>());
                        }

                        beansToProcess.get(beanName).add(clazz);
                    }
                }
            }

            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(TReference.class)) {
                    if (!beansToProcess.containsKey(beanName)) {
                        beansToProcess.put(beanName, new ArrayList<>());
                    }

                    beansToProcess.get(beanName).add(clazz);
                }
            }


            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return bean;
    }

    /**
     * 代理处理
     *
     * @param bean     bean
     * @param beanName beanName
     * @return bean
     * @throws BeansException BeansException
     */
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (beansToProcess.containsKey(beanName)) {
            Object target = getTargetBean(bean);
            for (Class clazz : beansToProcess.get(beanName)) {
                for (Method method : clazz.getMethods()) {
                    TReference annotation = AnnotationUtils.getAnnotation(method, TReference.class);
                    if (annotation != null) {
                        String name = method.getName();
                        if (name.length() > 3 && name.startsWith("set")
                                && method.getParameterTypes().length == 1
                                && Modifier.isPublic(method.getModifiers())
                                && !Modifier.isStatic(method.getModifiers())) {
                            if (beanFactory.containsBean(getBeanName(method))) {
                                ReflectionUtils.makeAccessible(method);
                                ReflectionUtils.invokeMethod(method, target, beanFactory.getBean(getBeanName(method)));
                            } else {
                                Parameter parameter = method.getParameters()[0];
                                String realClassName = getRealClassName(parameter.getType());

                                TReferenceMeta referenceMeta = new TReferenceMeta(annotation, parameter.getType());
                                ThriftUtils.addReferenceMeta(referenceMeta);

                                ProxyFactory proxyFactory = getProxyFactoryForThriftClient(target, parameter.getType(), realClassName);
                                for (ThriftClientAdvice advice : advices) {
                                    proxyFactory.addAdvice(advice);
                                }
                                proxyFactory.setFrozen(true);
                                proxyFactory.setProxyTargetClass(true);

                                ReflectionUtils.makeAccessible(method);
                                ReflectionUtils.invokeMethod(method, target, proxyFactory.getProxy());
                            }

                        }
                    }
                }

                for (Field field : clazz.getDeclaredFields()) {
                    if (field.isAnnotationPresent(TReference.class)) {
                        if (beanFactory.containsBean(getBeanName(field))) {
                            ReflectionUtils.makeAccessible(field);
                            ReflectionUtils.setField(field, target, beanFactory.getBean(field.getName()));
                        } else {
                            String realClassName = getRealClassName(field.getType());

                            TReferenceMeta referenceMeta =
                                    new TReferenceMeta(field.getAnnotation(TReference.class), field.getType());
                            ThriftUtils.addReferenceMeta(referenceMeta);

                            ProxyFactory proxyFactory = getProxyFactoryForThriftClient(target, field.getType(), realClassName);
                            for (ThriftClientAdvice advisor : advices) {
                                proxyFactory.addAdvice(advisor);
                            }
                            proxyFactory.setFrozen(true);
                            proxyFactory.setProxyTargetClass(true);

                            ReflectionUtils.makeAccessible(field);
                            ReflectionUtils.setField(field, target, proxyFactory.getProxy());
                        }
                    }
                }
            }
        }
        return bean;
    }

    private String getBeanName(Field field) {
        return field.getName();
    }

    private String getRealClassName(Class<?> clazz) {
        String className = clazz.getCanonicalName();
        int lastComma = className.lastIndexOf(".");
        return className.substring(0, lastComma);
    }

    private String getBeanName(Method method) {
        String name = method.getName();
        if (name.startsWith("set")) {
            char[] chars = name.substring(3).toCharArray();
            chars[0] = String.valueOf(chars[0]).toLowerCase().toCharArray()[0];
            return String.valueOf(chars);
        }
        return "";
    }

    @SneakyThrows
    private Object getTargetBean(Object bean) {
        Object target = bean;
        while (AopUtils.isAopProxy(target)) {
            target = ((Advised) target).getTargetSource().getTarget();
        }
        return target;
    }

    private ProxyFactory getProxyFactoryForThriftClient(Object bean, Class<?> type, String name) {
        ProxyFactory proxyFactory;
        try {
            proxyFactory = new ProxyFactory(BeanUtils
                    .instantiateClass(type.getConstructor(TProtocol.class), (TProtocol) null));
        } catch (NoSuchMethodException e) {
            log.error(e.getMessage(), e);
            throw new InvalidPropertyException(bean.getClass(), name, e.getMessage());
        }
        return proxyFactory;
    }
}
