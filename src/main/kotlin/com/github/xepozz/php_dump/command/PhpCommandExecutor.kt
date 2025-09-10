package com.github.xepozz.php_dump.command

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.php.config.PhpProjectConfigurationFacade
import com.jetbrains.php.config.commandLine.PhpCommandSettingsBuilder
import com.jetbrains.php.config.interpreters.PhpInterpretersManagerImpl
import com.jetbrains.php.run.remote.PhpRemoteInterpreterManager
import kotlin.coroutines.suspendCoroutine

object PhpCommandExecutor {
    suspend fun execute(
        file: String,
        phpSnippet: String,
        project: Project,
        processListener: ProcessAdapter,
        processArguments: List<String> = emptyList()
    ) {
        val arguments = buildList {
            addAll(processArguments)
            add("-r")
            add(phpSnippet)

            add(file)
        }

        executeCommand(project, arguments, processListener)
    }

    private suspend fun executeCommand(project: Project, arguments: List<String>, processListener: ProcessAdapter) =
        suspendCoroutine<Int> { continuation ->
            val interpretersManager = PhpInterpretersManagerImpl.getInstance(project)
            val interpreter = PhpProjectConfigurationFacade.getInstance(project).interpreter
                ?: interpretersManager.interpreters.firstOrNull() ?: return@suspendCoroutine

            val executable = interpreter.pathToPhpExecutable!!

            val command = GeneralCommandLine()
                .withExePath(executable)
                .apply { addParameters(arguments) }

            val processHandler: ProcessHandler
            if (interpreter.isRemote) {
                val manager = PhpRemoteInterpreterManager.getInstance() ?: throw ExecutionException(
                    PhpRemoteInterpreterManager.getRemoteInterpreterPluginIsDisabledErrorMessage()
                )

                val data = interpreter.phpSdkAdditionalData
                val validate = data.validate(project, null)
                if (StringUtil.isNotEmpty(validate)) {
                    throw ExecutionException(validate)
                }

                val pathMapper = manager.createPathMapper(project, data)
                val phpCommandSettings = PhpCommandSettingsBuilder.create(executable, pathMapper, data)

                val command = phpCommandSettings.createGeneralCommandLine(false)
                    .withRedirectErrorStream(false)
                    .apply { addParameters(arguments) }

                thisLogger().info("cmd: ${command.commandLineString}")
                processHandler = manager.getRemoteProcessHandler(
                    project,
                    data,
                    command,
                    false,
                    *phpCommandSettings.additionalMappings
                )
            } else {
                thisLogger().info("cmd: ${command.commandLineString}")
                processHandler = KillableColoredProcessHandler.Silent(command)
                processHandler.setShouldKillProcessSoftly(false)
                processHandler.setShouldDestroyProcessRecursively(true)
            }

            processHandler.addProcessListener(processListener)
            processHandler.addProcessListener(object : ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    continuation.resumeWith(Result.success(event.exitCode))
                }

                override fun processNotStarted() {
                    continuation.resumeWith(Result.failure(Error("process was not started")))
                }
            })

            processHandler.startNotify()
        }
}