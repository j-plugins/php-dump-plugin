package com.github.xepozz.php_dump.services

import com.github.xepozz.php_dump.command.PathMapper
import com.github.xepozz.php_dump.command.PhpCommandExecutor
import com.github.xepozz.php_dump.stubs.token_object.TokenParser
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class CustomTreeDumperService(var project: Project) : DumperServiceInterface {
    var phpSnippet: String? = null

    override suspend fun dump(file: String): CustomDumpResult {
        val phpSnippet = phpSnippet ?: return CustomDumpResult()

        val localFile = PathMapper.map(project, file)

        return withContext(Dispatchers.IO) {
            val capture = CapturingProcessAdapter()

            PhpCommandExecutor.execute(
                localFile,
                phpSnippet,
                project,
                capture,
            )

            val result = CustomDumpResult()

            result.raw = capture.output.stdout + "\n" + capture.output.stderr
//            println("jsonString: $jsonString")

            try {
                val tree = TokenParser.parseTokens(result.raw)
                result.tokens = tree
            } catch (e: Throwable) {
                result.error = e
            }

            result
        }
    }
}