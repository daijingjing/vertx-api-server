package com.ranqiyun.service.web;

import com.ranqiyun.service.web.common.DispatchHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    private HttpServer httpServer = null;

    @Override
    public void start(Promise<Void> startFuture) throws Exception {
        // create http server
        JsonObject httpConfig = config().getJsonObject("http", new JsonObject());
        httpServer = vertx
            .createHttpServer(new HttpServerOptions(httpConfig))
            .requestHandler(DispatchHandler.create(vertx, httpConfig, this.getClass().getPackage().getName()))
            .listen(ar -> {
                if (ar.succeeded()) {
                    startFuture.complete();
                    logger.info("API Server is running on port " + ar.result().actualPort());
                } else {
                    startFuture.fail(ar.cause());
                }
            });
    }

    @Override
    public void stop(Promise<Void> stopFuture) throws Exception {
        logger.info("API Server shutdown...");
        httpServer.close(ar -> {
            stopFuture.complete();
            logger.info("API Server is shutdown!");
        });
    }
}
