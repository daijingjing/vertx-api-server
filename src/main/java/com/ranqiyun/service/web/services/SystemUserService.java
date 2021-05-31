/*
 * Create By Dai Jingjing (jjyyis@qq.com) at 2021/2/25 上午11:44
 */

package com.ranqiyun.service.web.services;

import com.ranqiyun.service.web.annotation.AutowiredService;
import com.ranqiyun.service.web.annotation.Service;
import com.ranqiyun.service.web.common.ServiceBase;
import com.ranqiyun.service.web.util.Utils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.List;

@Service
public class SystemUserService extends ServiceBase {


    public static final String SELECT_SYS_USER = "SELECT `employee_id`, `name`, `create_date`, `remark` FROM `sys_user` ";

    public static final String INSERT_SYS_USER = "INSERT INTO `sys_user`(`employee_id`, `name`, `remark`) VALUES (?,?,?) ";

    public static final String DELETE_SYS_USER = "DELETE FROM `sys_user` WHERE `employee_id` = ? ";

    @AutowiredService
    MySQL mySQL;

    public SystemUserService(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    public Future<List<JsonObject>> getSysUserList() {
        return mySQL.retrieveMany("SELECT b.id, b.mobile, b.nickname, b.remark, a.name FROM sys_user AS a LEFT JOIN employee AS b ON a.employee_id = b.id");
    }

    public Future<Void> create(String employee_id, String name, String remark) {
        return mySQL.executeNoResult(Utils.buildArray(employee_id, name, remark), INSERT_SYS_USER);
    }

    public Future<Void> remove(String employee_id) {
        return mySQL.executeNoResult(Utils.buildArray(employee_id), DELETE_SYS_USER);
    }

    public Future<JsonObject> getSysUser(String employee_id) {
        return mySQL.retrieveOne(Utils.buildArray(employee_id), SELECT_SYS_USER + " WHERE `employee_id` = ?")
            .map(v -> v.orElse(null));
    }

}
