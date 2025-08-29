package com.github.xepozz.php_dump.services

import com.github.xepozz.php_dump.command.PhpCommandExecutor
import com.github.xepozz.php_dump.stubs.token_object.TokenParser
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class TokensTreeDumperService(var project: Project) : DumperServiceInterface {
    override suspend fun dump(file: String): Any {
        // language=injectablephp
        val phpSnippet = $$"""
            echo json_encode(
                array_map(
                    function (PhpToken $token) {
                        return [
                            'line' => $token->line,
                            'pos' => $token->pos,
                            'name' => $token->getTokenName(),
                            'value' => $token->text,
                        ];
                    },
                    \PhpToken::tokenize(file_get_contents($argv[1])),
                )
            );
        """.trimIndent()

        return withContext(Dispatchers.IO) {
            val output = StringBuilder()

            PhpCommandExecutor.execute(file, phpSnippet, project, object : ProcessAdapter() {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    when (outputType) {
                        ProcessOutputTypes.STDERR -> output.append(event.text)
                        ProcessOutputTypes.STDOUT -> output.append(event.text)
                    }
                }
            })


            val jsonString = output.toString()
//            println("jsonString: $jsonString")

            val tree = TokenParser.parseTokens(jsonString)
//            println("result tree: $tree")

            tree
        }
    }
}