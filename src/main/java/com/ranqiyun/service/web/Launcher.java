package com.ranqiyun.service.web;


import com.ranqiyun.service.web.annotation.Service;
import com.ranqiyun.service.web.common.ServiceManager;
import com.ranqiyun.service.web.util.ClassUtil;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher extends io.vertx.core.Launcher {
    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    public Launcher() {
    }

    public static void main(String[] args) {
        //Force to use slf4j
        System.setProperty("vertx.logger-delegate-factory-class-name",
            "io.vertx.core.logging.SLF4JLogDelegateFactory");

        new Launcher().dispatch(args);
    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
        logger.info("Initializing services ...");

        // 注册服务组件
        ClassUtil.getAllClassByAnnotation(Service.class, this.getClass().getPackageName())
            .forEach(service -> {
                Object instance = ClassUtil.newInstance(service, vertx, vertx.getOrCreateContext().config());
                if (instance != null) {
                    ServiceManager.registerService(instance);
                }
            });

        // 初始化所有服务组件
        ServiceManager.serviceAutowired();
    }

    @Override
    public void beforeStoppingVertx(Vertx vertx) {
        ServiceManager.shutdown();
    }

    @Override
    public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
    }
}
