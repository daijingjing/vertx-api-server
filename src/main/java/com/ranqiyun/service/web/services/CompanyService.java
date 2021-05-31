package com.ranqiyun.service.web.services;

import com.ranqiyun.service.web.annotation.AutowiredService;
import com.ranqiyun.service.web.annotation.Service;
import com.ranqiyun.service.web.common.ServiceBase;
import com.ranqiyun.service.web.util.UnifiedCreditCodeUtil;
import com.ranqiyun.service.web.util.Utils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Objects;

/**
 * Created by daijingjing on 2018/4/19.
 */
@Service
public class CompanyService extends ServiceBase {

    private static final String INSERT_COMPANY = "INSERT INTO `company`(`id`, `name`, `license_id`, `type`, `link_name`, `link_tel`, `is_external`, `remark`) VALUES (?,?,?,?,?,?,?,?)";
    public static final String SELECT_COMPANY = "SELECT `id`, `name`, `license_id`, `type`, `link_name`, `link_tel`, `is_external`, `create_date`, `remark` FROM `company`";
    public static final String UPDATE_COMPANY = "UPDATE `company` SET ";
    public static final String DELETE_COMPANY = "DELETE FROM `company` WHERE `id` = ?";
    public static final String SELECT_CUSTOMER_COMPANYS = "SELECT b.id, b.name, b.license_id, b.link_name, b.link_tel, a.tags, a.manager_employee_id FROM `company_customer` a, `company` b WHERE a.customer_company_id = b.id AND a.company_id = ?";
    public static final String SELECT_VERDOR_COMPANYS = "SELECT b.id, b.name, b.license_id, b.link_name, b.link_tel FROM `company_customer` a, `company` b WHERE a.company_id = b.id AND a.customer_company_id = ?";

    @AutowiredService
    private MySQL mySQL;

    public CompanyService(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    protected Future<String> createCompany(String name, String license_id, String type, String link_name, String link_tel, boolean isExternal, String remark) {
        String company_id = newId();
        if (!UnifiedCreditCodeUtil.checkUnifiedCreditCode(license_id)) {
            return Future.failedFuture("统一社会信用代码输入有误，请先验证后重试");
        }

        return mySQL.executeNoResult(
            Utils.buildArray(company_id, name, license_id.toUpperCase(), type, link_name, link_tel, isExternal ? 1 : 0, remark),
            INSERT_COMPANY)
            .map(v -> company_id);
    }

    /**
     * 创建外部企业
     *
     * @param name
     * @param license_id
     * @param link_name
     * @param link_tel
     * @param remark
     * @return
     */
    public Future<String> createExternalCompany(String name, String license_id, String type, String link_name, String link_tel, String remark) {
        return createCompany(name, license_id, type, link_name, link_tel, true, remark);
    }

    /**
     * 创建内部企业
     *
     * @param name
     * @param license_id
     * @param type
     * @param link_name
     * @param link_tel
     * @param remark
     * @return
     */
    public Future<String> createCompany(String name, String license_id, String type, String link_name, String link_tel, String remark) {
        return createCompany(name, license_id, type, link_name, link_tel, false, remark);
    }

    /**
     * 获取内部企业列表
     *
     * @return
     */
    public Future<List<JsonObject>> getListInternal() {
        return mySQL.retrieveMany(SELECT_COMPANY + "WHERE `is_external` = 0");
    }

    /**
     * 获取外部企业列表
     *
     * @return
     */
    public Future<List<JsonObject>> getListExternal() {
        return mySQL.retrieveMany(SELECT_COMPANY + "WHERE `is_external` = 1");
    }

    /**
     * 按照企业类型获取企业列表
     *
     * @return
     */
    public Future<List<JsonObject>> getListByType(String type) {
        return mySQL.retrieveMany(Utils.buildArray(type), SELECT_COMPANY + "WHERE `type` = ?");
    }

    /**
     * 获取企业信息列表
     *
     * @return
     */
    public Future<List<JsonObject>> getList() {
        return mySQL.retrieveMany(SELECT_COMPANY);
    }

    /**
     * 通过ID获取企业信息
     *
     * @param company_id
     * @return
     */
    public Future<JsonObject> getById(String company_id) {
        return mySQL.retrieveOne(Utils.buildArray(company_id),
            SELECT_COMPANY + "WHERE `id` = ?")
            .map(v -> v.orElse(null));
    }

    /**
     * 通过企业名称获取企业信息
     *
     * @param name
     * @return
     */
    public Future<JsonObject> getByName(String name) {
        return mySQL.retrieveOne(Utils.buildArray(name),
            SELECT_COMPANY + "WHERE `name` = ?")
            .map(v -> v.orElse(null));
    }

    /**
     * 通过社会统一信用代码获取企业信息
     *
     * @param license_id
     * @return
     */
    public Future<JsonObject> getByLicenseId(String license_id) {
        return mySQL.retrieveOne(Utils.buildArray(license_id),
            SELECT_COMPANY + "WHERE `license_id` = ?")
            .map(v -> v.orElse(null));
    }

    /**
     * 更新联系人和联系电话
     *
     * @param company_id
     * @param link_name
     * @param link_tel
     * @return
     */
    public Future<Void> updateLink(String company_id, String link_name, String link_tel) {
        return mySQL.executeNoResult(Utils.buildArray(link_name, link_tel, company_id),
            UPDATE_COMPANY + "`link_name`= ?, `link_tel` = ? WHERE `id`=?");
    }

    /**
     * 更新企业备注信息
     *
     * @param company_id
     * @param remark
     * @return
     */
    public Future<Void> updateRemark(String company_id, String remark) {
        return mySQL.executeNoResult(Utils.buildArray(remark, company_id),
            UPDATE_COMPANY + "`remark`= ? WHERE `id`=?");
    }

    /**
     * 更新企业名称
     *
     * @param company_id
     * @param name
     * @return
     */
    public Future<Void> updateName(String company_id, String name) {
        return mySQL.executeNoResult(Utils.buildArray(name, company_id),
            UPDATE_COMPANY + "`name`=? WHERE `id`=?");
    }

    /**
     * 更新企业社会统一信用代码
     *
     * @param company_id 企业ID
     * @param license_id 社会统一信用代码
     * @return
     */
    public Future<Void> updateLicenseId(String company_id, String license_id) {
        return mySQL.executeNoResult(Utils.buildArray(license_id, company_id),
            UPDATE_COMPANY + "`license_id`=? WHERE `id`=?");
    }

    /**
     * 更新企业类型
     *
     * @param company_id 企业ID
     * @param type 类型
     * @return
     */
    public Future<Void> updateType(String company_id, String type) {
        return mySQL.executeNoResult(Utils.buildArray(type, company_id),
            UPDATE_COMPANY + "`type`=? WHERE `id`=?");
    }

    /**
     * 移除企业
     *
     * @param company_id 企业ID
     *
     */
    public Future<Void> remove(String company_id) {
        return mySQL.executeNoResult(Utils.buildArray(company_id), DELETE_COMPANY);
    }

    /**
     * 获取供应商企业
     *
     * @param company_id 本企业ID
     * @return 供应商企业列表{id, name, license_id}
     */
    public Future<List<JsonObject>> getVerdorCompanys(String company_id) {
        return mySQL.retrieveMany(Utils.buildArray(company_id), SELECT_VERDOR_COMPANYS);
    }

    /**
     * 添加关联的客户企业
     *
     * @param company_id 本企业ID
     * @param customer_company_id 客户企业ID
     * @param tags 客户企业标签
     * @param manager_employee_id 客户企业客户经理ID
     * @return
     */
    public Future<Void> addCustomerComany(String company_id, String customer_company_id, String tags, String manager_employee_id) {
        return addComanyRelation(company_id, customer_company_id, tags, manager_employee_id);
    }

    /**
     * 添加关联的客户企业
     *
     * @param company_id 本企业ID
     * @param verdor_company_id 供应商企业ID
     * @return
     */
    public Future<Void> addVerdorComany(String company_id, String verdor_company_id, String tags) {
        return addComanyRelation(verdor_company_id, company_id, tags, null);
    }

    protected Future<Void> addComanyRelation(String company_id, String customer_company_id, String tags, String manager_employee_id) {
        return mySQL.executeNoResult(Utils.buildArray(company_id, customer_company_id, tags, manager_employee_id),
            "INSERT INTO `company_customer`(`company_id`, `customer_company_id`, `tags`, `manager_employee_id`) VALUES (?,?,?,?)");
    }


    /**
     * 更新客户企业标签
     *
     * @param company_id 本企业ID
     * @param customer_company_id 客户企业ID
     * @param tags 客户企业标签
     * @return
     */
    public Future<Void> updateCustomerCompanyTags(String company_id, String customer_company_id, String tags) {
        return mySQL.executeNoResult(Utils.buildArray(tags, company_id, customer_company_id),
            "UPDATE `company_customer` SET `tags` = ? WHERE `company_id` = ? AND `customer_company_id` = ?");
    }

    /**
     * 更新客户企业客户经理
     *
     * @param company_id 本企业ID
     * @param customer_company_id 客户企业ID
     * @param manager_employee_id 客户企业客户经理ID
     * @return
     */
    public Future<Void> updateCustomerCompanyManager(String company_id, String customer_company_id, String manager_employee_id) {
        return mySQL.executeNoResult(Utils.buildArray(manager_employee_id, company_id, customer_company_id),
            "UPDATE `company_customer` SET `manager_employee_id` = ? WHERE `company_id` = ? AND `customer_company_id` = ?");
    }

    /**
     * 移除关联的客户企业
     *
     * @param company_id
     * @param customer_company_id
     * @return
     */
    public Future<Void> removeCustomerCompany(String company_id, String customer_company_id) {
        return mySQL.executeNoResult(Utils.buildArray(company_id, customer_company_id),
            "DELETE FROM `company_customer` WHERE `company_id` = ? AND `customer_company_id` = ?");
    }

    /**
     * 移除关联的供应商企业
     *
     * @param company_id 本企业ID
     * @param verdor_company_id 供应商企业ID
     * @return
     */
    public Future<Void> removeVerdorCompany(String company_id, String verdor_company_id) {
        return mySQL.executeNoResult(Utils.buildArray(verdor_company_id, company_id),
            "DELETE FROM `company_customer` WHERE `company_id` = ? AND `customer_company_id` = ?");
    }


    /**
     * 获取用户所在企业
     *
     * @param user_id
     * @return
     */
    public Future<JsonObject> getEmployeeCompany(String user_id) {
        return mySQL.retrieveOne(Utils.buildArray(user_id),
            "SELECT a.`company_id` as `id`, a.`name`, a.`is_admin`, a.`create_date`, b.`name` as `companyName`, b.`type` as `companyType` FROM `company_employee` a , `company` b WHERE a.`company_id` = b.`id` and `employee_id` = ? ORDER BY `create_date`")
            .map(v -> v.orElse(null));
    }

    /**
     * 获取用户所在企业ID
     *
     * @param user_id
     * @return
     */
    public Future<String> getEmployeeCompanyId(String user_id) {
        return getEmployeeCompany(user_id)
            .compose(v -> Objects.nonNull(v) ? Future.succeededFuture(v.getString("id")) : Future.failedFuture("不在任何企业中"));
    }

    /**
     * 更新企业信息
     *
     * @param company_id
     * @param name
     * @param license_id
     * @param type
     * @param is_external
     * @param link_name
     * @param link_tel
     * @param remark
     * @return
     */
    public Future<Void> updateCompany(String company_id, String name, String license_id, String type, Boolean is_external, String link_name, String link_tel, String remark) {
        return mySQL.executeNoResult(Utils.buildArray(name, license_id, type, is_external ? 1 : 0, link_name, link_tel, remark, company_id),
            "UPDATE `company` SET `name`=?,`license_id`=?,`type`=?,`is_external`=?,`link_name`=?,`link_tel`=?,`remark`= ? WHERE `id` = ?");
    }

    /**
     * 添加外部供应商企业
     * @param company_id 本企业ID
     * @param verdor_company_id 外部供应商企业ID
     * @param tags
     * @return
     */
    public Future<Void> addExternalVerdor(String company_id, String verdor_company_id, String tags) {
        return mySQL.executeNoResult(Utils.buildArray(company_id, verdor_company_id, tags),
            "INSERT INTO `company_external_verdor`(`company_id`, `verdor_company_id`, `tags`) VALUES (?,?,?)");
    }

    /**
     * 移除关联的外部供应商企业
     *
     * @param company_id 本企业ID
     * @param verdor_company_id 外部供应商企业ID
     * @return
     */
    public Future<Void> removeExternalVerdor(String company_id, String verdor_company_id) {
        return mySQL.executeNoResult(Utils.buildArray(company_id, verdor_company_id),
            "DELETE FROM `company_external_verdor` WHERE `company_id` = ? AND `verdor_company_id` = ?");
    }

    /**
     * 获取外部供应商企业列表
     *
     * @param company_id 本企业ID
     * @return
     */
    public Future<List<JsonObject>> getExternalVerdorCompanys(String company_id) {
        return mySQL.retrieveMany(Utils.buildArray(company_id),
            "SELECT b.id, b.name, b.license_id, b.link_name, b.link_tel FROM `company_external_verdor` a, `company` b WHERE a.verdor_company_id = b.id AND a.company_id = ?");
    }

    /**
     * 添加外部运输企业
     * @param company_id 本企业ID
     * @param transport_company_id 外部运输企业ID
     * @param tags
     * @return
     */
    public Future<Void> addExternalTransport(String company_id, String transport_company_id, String tags) {
        return mySQL.executeNoResult(Utils.buildArray(company_id, transport_company_id, tags),
            "INSERT INTO `company_external_transport`(`company_id`, `transport_company_id`, `tags`) VALUES (?,?,?)");
    }

    /**
     * 移除关联的外部运输企业
     *
     * @param company_id 本企业ID
     * @param transport_company_id 外部运输企业ID
     * @return
     */
    public Future<Void> removeExternalTransport(String company_id, String transport_company_id) {
        return mySQL.executeNoResult(Utils.buildArray(company_id, transport_company_id),
            "DELETE FROM `company_external_transport` WHERE `company_id` = ? AND `transport_company_id` = ?");
    }

    /**
     * 获取外部运输企业列表
     *
     * @param company_id 本企业ID
     * @return
     */
    public Future<List<JsonObject>> getExternalTransportCompanys(String company_id) {
        return mySQL.retrieveMany(Utils.buildArray(company_id),
            "SELECT b.id, b.name, b.license_id, b.link_name, b.link_tel FROM `company_external_transport` a, `company` b WHERE a.transport_company_id = b.id AND a.company_id = ?");
    }
}
