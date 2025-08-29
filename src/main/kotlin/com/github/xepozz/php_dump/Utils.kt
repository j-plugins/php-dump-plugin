package com.github.xepozz.php_dump

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class DumpPluginDisposable : Disposable {
    companion object {
        @JvmStatic
        fun getInstance(project: Project): Disposable = project.getService(DumpPluginDisposable::class.java)
    }

    override fun dispose() {}
}
