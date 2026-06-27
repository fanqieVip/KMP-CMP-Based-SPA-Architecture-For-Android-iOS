package com.basic.common.repository

import com.basic.common.api.createTestApi
import com.basic.common.beans.Data
import com.basic.common.beans.UserInfo
import com.basic.common.net.ktorfit

object TestRepository {
    private val testApi by lazy { ktorfit.createTestApi() }
    suspend fun queryUserInfo(username: String): Data<UserInfo?> {
        return testApi.queryUserInfo(username)
    }
}