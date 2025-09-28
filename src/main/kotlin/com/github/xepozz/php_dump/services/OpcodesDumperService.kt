package com.github.xepozz.php_dump.services

import com.github.xepozz.php_dump.SeparateStringBufferProcessAdapter
import com.github.xepozz.php_dump.command.PathMapper
import com.github.xepozz.php_dump.command.PhpCommandExecutor
import com.github.xepozz.php_dump.configuration.PhpDumpSettingsService
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class OpcodesDumperService(var project: Project) : DumperServiceInterface {
    val state = PhpDumpSettingsService.getInstance(project)

    override suspend fun dump(file: String): Any {
        val debugLevel = state.debugLevel.value
        val preloadFile = state.preloadFile

        val localFile = PathMapper.map(project, file)
        val command = GeneralCommandLine(buildList {
            add("-l")
            addAll("-d display_errors=0".split(" "))
            addAll("-d error_reporting=0".split(" "))

            addAll("-d opcache.enable_cli=1".split(" "))
            addAll("-d opcache.save_comments=1".split(" "))
            addAll("-d opcache.opt_debug_level=${debugLevel}".split(" "))
            if (preloadFile != null) {
                addAll("-d opcache.preload=${preloadFile}".split(" "))
            }
            add(localFile)
        }).commandLineString

        // language=injectablephp
        val phpSnippet = $$"""
        opcache_compile_file($argv[1]); passthru(PHP_BINARY . ' $$command');
        """.trimIndent()

//        println("command: $command")
//        println("phpSnippet: $phpSnippet")
        return withContext(Dispatchers.IO) {
            val opcodes = StringBuilder()
            val errors = StringBuilder()

            PhpCommandExecutor.execute(
                localFile,
                phpSnippet,
                project,
                SeparateStringBufferProcessAdapter(stderr = opcodes, stdout = errors),
                ("-d opcache.enable_cli=1".split(" ")),
            )

            if (opcodes.isEmpty()) {
                errors.toString()
            } else {
                opcodes.toString()
            }
        }
    }
}