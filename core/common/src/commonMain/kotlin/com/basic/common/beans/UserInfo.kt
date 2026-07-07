package com.basic.common.beans

import kotlinx.serialization.Serializable

/**
 * 用户资料
 * @param tenantName 租户名称(所属公司)
 * @param userLogo 用户头像
 * @param realName 名字
 * @param departmentName 所在部门名称
 */
@Serializable
data class UserInfo(
    val tenantName: String?,
    val userLogo: String?,
    val realName: String?,
    val departmentName: String?
)
