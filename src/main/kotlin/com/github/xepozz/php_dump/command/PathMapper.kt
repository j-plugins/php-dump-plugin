package com.github.xepozz.php_dump.command

import com.intellij.execution.ExecutionException
import com.intellij.openapi.project.Project
import com.jetbrains.php.config.PhpProjectConfigurationFacade
import com.jetbrains.php.config.interpreters.PhpInterpreter
import com.jetbrains.php.config.interpreters.PhpInterpretersManagerImpl
import com.jetbrains.php.run.remote.PhpRemoteInterpreterManager

object PathMapper {
    fun map(project: Project, interpreter: PhpInterpreter, localPath: String): String {
        if (!interpreter.isRemote) return localPath

        val data = interpreter.phpSdkAdditionalData
        val manager = PhpRemoteInterpreterManager.getInstance() ?: throw ExecutionException(
            PhpRemoteInterpreterManager.getRemoteInterpreterPluginIsDisabledErrorMessage()
        )
        val pathMapper = manager.createPathMapper(project, data)
        return pathMapper.process(localPath)
    }

    fun map(project: Project, localPath: String): String =
        (PhpProjectConfigurationFacade.getInstance(project).interpreter
            ?: PhpInterpretersManagerImpl.getInstance(project).interpreters.firstOrNull())
            ?.let { map(project, it, localPath) }
            ?: localPath
}