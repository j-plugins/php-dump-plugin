package com.github.xepozz.php_dump.services

import com.github.xepozz.php_dump.StringBufferProcessAdapter
import com.github.xepozz.php_dump.command.PathMapper
import com.github.xepozz.php_dump.command.PhpCommandExecutor
import com.github.xepozz.php_dump.configuration.PhpDumpSettingsService
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jetbrains.php.config.PhpProjectConfigurationFacade
import com.jetbrains.php.config.interpreters.PhpInterpretersManagerImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class OpcodesDumperService(var project: Project) : DumperServiceInterface {
    val state = PhpDumpSettingsService.getInstance(project)
    val interpreter = PhpProjectConfigurationFacade.getInstance(project).interpreter
        ?: PhpInterpretersManagerImpl.getInstance(project).interpreters.firstOrNull()

    override suspend fun dump(file: String): Any? {
        val interpreterPath = interpreter?.pathToPhpExecutable ?: return null
        val debugLevel = state.debugLevel.value
        val preloadFile = state.preloadFile

        val localFile = PathMapper.map(project, file)
        val command = GeneralCommandLine(buildList {
            add(interpreterPath)
            add("-l")
            add("-ddisplay_errors=0")
            add("-derror_reporting=0")

            add("-dopcache.enable_cli=1")
            add("-dopcache.save_comments=1")
            add("-dopcache.opt_debug_level=${debugLevel}")
            if (preloadFile != null) {
                add("-dopcache.preload=${preloadFile}")
            }

            add("1>/dev/null")
            add(localFile)
        }).commandLineString

        // language=injectablephp
        val phpSnippet = $$"""
        opcache_compile_file($argv[1]);
        passthru('$$command');
        """.trimIndent()

        return withContext(Dispatchers.IO) {
            val output = StringBuilder()

            PhpCommandExecutor.execute(
                localFile,
                phpSnippet,
                project,
                StringBufferProcessAdapter(output),
                listOf("-dopcache.enable_cli=1"),
            )


            output.toString()
        }
    }
}