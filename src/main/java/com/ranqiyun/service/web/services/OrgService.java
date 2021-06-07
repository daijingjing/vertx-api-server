package com.ranqiyun.service.web.services;

import com.google.common.base.Strings;
import com.ranqiyun.service.web.annotation.AutowiredService;
import com.ranqiyun.service.web.annotation.Service;
import com.ranqiyun.service.web.common.ServiceBase;
import com.ranqiyun.service.web.util.Utils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.ranqiyun.service.web.util.Utils.buildArray;
import static com.ranqiyun.service.web.util.Utils.transform;

/**
 * Created by daijingjing on 2018/4/19.
 */
@Service
public class OrgService extends ServiceBase {

    public static final String SELECT_ORG = "SELECT `id`, `pid`, `name`, `remark` FROM `org` ";
    public static final String SELECT_ALL = SELECT_ORG + "ORDER BY `pid`, `name`";
    public static final String SELECT_BY_PARENT = SELECT_ORG + "WHERE `pid` = ?";
    public static final String SELECT_BY_PARENT_NULL = SELECT_ORG + "WHERE `pid` is null";
    public static final String SELECT_BY_ID = SELECT_ORG + "WHERE `id` = ?";
    public static final String SELECT_BY_NAME = SELECT_ORG + "WHERE name = ?";
    public static final String UPDATE_ORG = "UPDATE `org` SET ";
    public static final String UPDATE_REMARK = UPDATE_ORG + "`remark` = ? WHERE `id`= ?";
    public static final String UPDATE_NAME = UPDATE_ORG + "`name` = ? WHERE `id`= ?";
    public static final String UPDATE_PARENT = UPDATE_ORG + "`pid` = ? WHERE `id`= ?";
    public static final String INSERT_ORG = "INSERT INTO `org` (`id`, `pid`, `name`, `remark`) VALUES (?,?,?,?)";
    public static final String DELETE_ORG = "DELETE FROM `org` WHERE `id` = ?";

    public static final String SELECT_USER_ORG = "SELECT  b.`id`, b.`name` FROM `org_user` a INNER JOIN `org` b ON a.`org_id` = b.`id` WHERE a.`user_id` = ?";
    public static final String SELECT_ORG_USER = "SELECT  `user_id` FROM `org_user` WHERE `org_id` = ?";
    public static final String INSERT_ORG_USER = "INSERT INTO `org_user` (`org_id`, `user_id`) VALUES (?,?)";
    public static final String REMOVE_ORG_USER = "DELETE FROM `org_user` WHERE `org_id` = ? AND `user_id` = ?";

    @AutowiredService
    private MySQL mySQL;

    public OrgService(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    public Future<String> create(String pid, String name, String remark) {
        String id = newId();
        return mySQL.executeNoResult(Utils.buildArray(id, pid, name, remark), INSERT_ORG)
            .map(v -> id);
    }

    public Future<JsonObject> getById(String id) {
        return mySQL.retrieveOne(Utils.buildArray(id), SELECT_BY_ID)
            .map(v -> v.orElse(null));
    }

    public Future<JsonObject> getByName(String name) {
        return mySQL.retrieveOne(Utils.buildArray(name), SELECT_BY_NAME)
            .map(v -> v.orElse(null));
    }

    public Future<List<JsonObject>> getByParent(String parent_id) {
        return Strings.isNullOrEmpty(parent_id)
            ? mySQL.retrieveMany(SELECT_BY_PARENT_NULL)
            : mySQL.retrieveMany(Utils.buildArray(parent_id), SELECT_BY_PARENT);
    }

    public Future<Void> updateRemark(String id, String remark) {
        return mySQL.executeNoResult(Utils.buildArray(remark, id), UPDATE_REMARK);
    }

    public Future<Void> updateName(String id, String name) {
        return mySQL.executeNoResult(Utils.buildArray(name, id), UPDATE_NAME);
    }

    public Future<Void> updateParent(String id, String parent_id) {
        return mySQL.executeNoResult(Utils.buildArray(parent_id, id), UPDATE_PARENT);
    }

    public Future<Void> remove(String id, Boolean contain_children) {
        if (contain_children) {
            return getChildren(id)
                .map(list -> list.stream().map(v -> v.getString("id")).collect(Collectors.toList()))
                .compose(children_id -> {
                    children_id.add(id);
                    return mySQL.batchExecuteNoResult(children_id.stream().map(Utils::buildArray).collect(Collectors.toList()), DELETE_ORG);
                });
        } else {
            return CompositeFuture.all(getById(id), getByParent(id))
                .compose(results -> {
                    if (results.failed())
                        return Future.failedFuture(results.cause());

                    JsonObject org = results.resultAt(0);
                    List<JsonObject> children = results.resultAt(1);

                    String pid = Objects.nonNull(org) ? org.getString("pid") : null;

                    return CompositeFuture.all(
                        mySQL.executeNoResult(buildArray(id), DELETE_ORG),
                        mySQL.batchExecuteNoResult(children.stream().map(c -> buildArray(pid, c.getString("id"))).collect(Collectors.toList()), UPDATE_PARENT))
                        .mapEmpty();
                });
        }
    }

    public Future<List<JsonObject>> getChildren(String id) {
        return getByParent(id).compose(children -> {
            if (children.size() == 0) {
                return Future.succeededFuture(new ArrayList<>());
            } else {
                return transform(children, c -> getChildren(c.getString("id")).map(cc -> {
                    cc.add(c);
                    return cc;
                })).map(item -> item.stream().flatMap(Collection::stream).collect(Collectors.toList()));
            }
        });
    }

    public Future<List<JsonObject>> list() {
        return mySQL.retrieveMany(SELECT_ALL);
    }

    public Future<Void> addUserOrg(String org_id, String user_id) {
        return mySQL.executeNoResult(Utils.buildArray(org_id, user_id), INSERT_ORG_USER);
    }

    public Future<Void> removeUserOrg(String org_id, String user_id) {
        return mySQL.executeNoResult(Utils.buildArray(org_id, user_id), REMOVE_ORG_USER);
    }

    public Future<List<JsonObject>> listUserOrgs(String user_id) {
        return mySQL.retrieveMany(Utils.buildArray(user_id), SELECT_USER_ORG);
    }

    public Future<List<String>> listOrgUsers(String org_id) {
        return mySQL.retrieveMany(Utils.buildArray(org_id), SELECT_ORG_USER)
            .map(v -> v.stream().map(row -> row.getString("user_id")).collect(Collectors.toList()));
    }
}
