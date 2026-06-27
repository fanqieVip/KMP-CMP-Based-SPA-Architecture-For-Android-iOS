package com.basic.project.di.impl

import com.basic.base.utils.toastShort
import com.basic.common.di.service.ProjectService

class ProjectServiceImpl : ProjectService {
    override fun sayHello(text: String) {
        toastShort(text)
    }
}