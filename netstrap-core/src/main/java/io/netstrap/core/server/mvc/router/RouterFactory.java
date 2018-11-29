package io.netstrap.core.server.mvc.router;

import io.netstrap.common.factory.ClassFactory;
import io.netstrap.core.server.http.HttpMethod;
import io.netstrap.core.server.mvc.controller.DefaultErrorController;
import io.netstrap.core.server.mvc.stereotype.*;
import lombok.Data;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 路由单例
 *
 * @author minghu.zhang
 * @date 2018/11/09
 */
public class RouterFactory {

    /**
     * 调用模型
     */
    private final static Map<String, Router> ROUTERS = new HashMap<>(8);

    /**
     * 类工厂
     */
    private ClassFactory factory;

    /**
     * 路由工厂
     */
    private static RouterFactory routerFactory;


    /**
     * Spring上下文
     */
    private ApplicationContext context;

    /**
     * 构造函数
     */
    private RouterFactory(ApplicationContext context, ClassFactory factory) {
        this.context = context;
        this.factory = factory;
    }

    /**
     * 获取路由工厂
     */
    public static RouterFactory get() {
        return routerFactory;
    }

    /**
     * 获取单例路由
     */
    public static RouterFactory of(ApplicationContext context, ClassFactory factory) {
        if (Objects.isNull(routerFactory)) {
            synchronized (RouterFactory.class) {
                if (Objects.isNull(routerFactory)) {
                    routerFactory = new RouterFactory(context, factory).init();
                }
            }
        }
        return routerFactory;
    }

    /**
     * 初始化路由信息
     */
    private RouterFactory init() {
        initDefault();
        initRouter();
        return this;
    }

    /**
     * 初始化默认路由
     */
    private void initDefault() {
        buildRouter(DefaultErrorController.class);
    }

    /**
     * 初始化控制器
     */
    private void initRouter() {
        List<Class<?>> controllers = factory.getClassByAnnotation(RestController.class);
        for (Class clz : controllers) {
            if (!clz.equals(DefaultErrorController.class)) {
                buildRouter(clz);
            }
        }
    }

    /**
     * 构建路由对象
     */
    private void buildRouter(Class<?> clz) {
        Object invoker = context.getBean(clz);

        String groupUri = "";
        if (clz.isAnnotationPresent(RequestMapping.class)) {
            groupUri = clz.getDeclaredAnnotation(RequestMapping.class).value();
        }

        String slash = "/";

        if (!StringUtils.isEmpty(groupUri) && !groupUri.startsWith(slash)) {
            groupUri = slash + groupUri;
        }
        Method[] declaredMethods = clz.getDeclaredMethods();
        for (Method method : declaredMethods) {
            buildMethod(invoker, method, groupUri, slash);
        }
    }

    /**
     * 构建路由对象
     */
    private void buildMethod(Object invoker, Method method, String groupUri, String slash) {
        Router router = new Router();
        router.setInvoker(invoker);
        method.setAccessible(true);
        HttpMethod[] httpMethods = {};
        String mappingUri = "";

        if(method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getDeclaredAnnotation(RequestMapping.class);
            httpMethods = mapping.method();
            mappingUri = mapping.value();
        } else if(method.isAnnotationPresent(GetMapping.class)) {
            GetMapping mapping = method.getDeclaredAnnotation(GetMapping.class);
            httpMethods = mapping.method();
            mappingUri = mapping.value();
        } else if(method.isAnnotationPresent(DeleteMapping.class)) {
            DeleteMapping mapping = method.getDeclaredAnnotation(DeleteMapping.class);
            httpMethods = mapping.method();
            mappingUri = mapping.value();
        } else if(method.isAnnotationPresent(PostMapping.class)) {
            PostMapping mapping = method.getDeclaredAnnotation(PostMapping.class);
            httpMethods = mapping.method();
            mappingUri = mapping.value();
        } else if(method.isAnnotationPresent(PutMapping.class)) {
            PutMapping mapping = method.getDeclaredAnnotation(PutMapping.class);
            httpMethods = mapping.method();
            mappingUri = mapping.value();
        } else if(method.isAnnotationPresent(PatchMapping.class)) {
            PatchMapping mapping = method.getDeclaredAnnotation(PatchMapping.class);
            httpMethods = mapping.method();
            mappingUri = mapping.value();
        }

        if (!StringUtils.isEmpty(mappingUri)) {
            mappingUri = (mappingUri.startsWith(slash) ? mappingUri : slash + mappingUri);
            router.setAction(method);
            router.setMethods(httpMethods);
            router.setUri(groupUri + mappingUri);
            put(router.getUri(), router);
        }
    }

    /**
     * 添加路由模型
     */
    private void put(String uri, Router router) {
        ROUTERS.put(uri, router);
    }

    /**
     * 获取路由
     */
    public Router get(String uri) {

        Router router;
        if (!ROUTERS.containsKey(uri)) {
            router = getNotFoundRouter();
        } else {
            router = ROUTERS.get(uri);
        }

        return router;
    }

    /**
     * 405
     */
    public Router getMethodNotAllowedRouter() {
        return get(DefaultErrorController.METHOD_NOT_ALLOWED);
    }

    /**
     * 500
     */
    public Router getInternalServiceErrorRouter() {
        return get(DefaultErrorController.INTERNAL_SERVICE_ERROR);
    }

    /**
     * 400
     */
    public Router getBadRequestRouter() {
        return get(DefaultErrorController.BAD_REQUEST);
    }

    /**
     * 404
     */
    public Router getNotFoundRouter() {
        return get(DefaultErrorController.NOT_FOUND);
    }

    /**
     * 403
     */
    public Router getForbiddenRouter() {
        return get(DefaultErrorController.FORBIDDEN);
    }

    /**
     * 401
     */
    public Router getUnauthorizedRouter() {
        return get(DefaultErrorController.UNAUTHORIZED);
    }

    /**
     * 路由数据模型
     */
    @Data
    public class Router {
        /**
         * 当前映射的URI
         */
        private String uri;
        /**
         * 调用对象
         */
        private Object invoker;
        /**
         * 调用方法
         */
        private Method action;
        /**
         * 支持的HTTP方法
         */
        private HttpMethod[] methods;
    }
}