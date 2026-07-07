package com.basic.common.beans

import kotlinx.serialization.Serializable

/**
 * 用户资料
 */
@Serializable
data class UserInfo(
    //租户名称(所属公司)
    val tenantName: String?,
    //用户头像
    val userLogo: String?,
    //名字
    val realName: String?,
    //所在部门名称
    val departmentName: String?
)
