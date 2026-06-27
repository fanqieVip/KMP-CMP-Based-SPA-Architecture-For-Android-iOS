package com.basic.common.api

import com.basic.common.beans.Data
import com.basic.common.beans.UserInfo
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query

interface TestApi {
    /**
     * 根据账号获取用户信息
     */
    @GET("/user/getUserPublicInfo")
    suspend fun queryUserInfo(@Query("username") username: String): Data<UserInfo?>
}