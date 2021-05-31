package com.ranqiyun.service.web.services;

import com.ranqiyun.service.web.annotation.Service;
import com.ranqiyun.service.web.common.ServiceBase;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Helper and wrapper class for JDBC repository services.
 */
@Service
public class MySQL extends ServiceBase {

    private final MySQLPool pool;

    public MySQL(Vertx vertx, JsonObject config) {
        super(vertx, config);

        MySQLConnectOptions connectOptions = new MySQLConnectOptions(config.getJsonObject("mysql", new JsonObject()));
        PoolOptions poolOptions = new PoolOptions(config.getJsonObject("mysql-pool", new JsonObject()));

        // Create the client pool
        pool = MySQLPool.pool(vertx, connectOptions, poolOptions);
    }

    @Override
    public void destroy() {
        pool.close();
    }

    public Future<Void> executeNoResult(Object[] params, String sql) {
        return pool.preparedQuery(sql)
            .execute(Tuple.from(params))
            .mapEmpty();
    }

    public Future<Void> executeNoResult(String sql) {
        return pool.preparedQuery(sql)
            .execute()
            .mapEmpty();
    }

    public Future<List<JsonObject>> retrieveMany(JsonObject params, String sql) {
        return SqlTemplate
            .forQuery(pool, sql)
            .mapFrom(TupleMapper.jsonObject())
            .mapTo(Row::toJson)
            .execute(params)
            .map(rows -> StreamSupport.stream(rows.spliterator(), false).collect(Collectors.toList()));
    }

    public Future<Optional<JsonObject>> retrieveOne(JsonObject params, String sql) {
        return retrieveMany(params, sql).map(rows -> rows.stream().findFirst());
    }

    public Future<Optional<JsonObject>> retrieveOne(Object[] params, String sql) {
        return retrieveMany(params, sql).map(rows -> rows.stream().findFirst());
    }

    public Future<List<JsonObject>> retrieveMany(Object[] params, String sql) {
        return pool.preparedQuery(sql)
            .execute(Tuple.from(params))
            .map(rows -> StreamSupport.stream(rows.spliterator(), false).map(Row::toJson).collect(Collectors.toList()));
    }

    public Future<List<JsonObject>> retrieveMany(String sql) {
        return pool.query(sql)
            .execute()
            .map(rows -> StreamSupport.stream(rows.spliterator(), false).map(Row::toJson).collect(Collectors.toList()));
    }

    public Future<Void> batchExecuteNoResult(List<Map.Entry<Object[], String>> tasks) {
        return transaction(client -> CompositeFuture.all(
            tasks.stream()
                .map(task -> client
                    .preparedQuery(task.getValue())
                    .execute(Tuple.from(task.getKey()))
                    .mapEmpty())
                .collect(Collectors.toList())).mapEmpty());
    }

    public <T> Future<T> transaction(Function<SqlConnection, Future<T>> execute) {
        return pool.withTransaction(execute);
    }

}
