package com.basic.common.repository

import com.basic.common.api.createTestApi
import com.basic.common.beans.Data
import com.basic.common.beans.UserInfo
import com.basic.common.net.ktorfit

/**
 * 测试模块数据仓库
 */
object TestRepository {
    private val testApi by lazy { ktorfit.createTestApi() }

    /**
     * 查询用户信息
     */
    suspend fun queryUserInfo(username: String): Data<UserInfo?> {
        return testApi.queryUserInfo(username)
    }
}