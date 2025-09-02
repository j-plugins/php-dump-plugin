package com.github.xepozz.php_dump.services

import com.github.xepozz.php_dump.StringBufferProcessAdapter
import com.github.xepozz.php_dump.command.PathMapper
import com.github.xepozz.php_dump.command.PhpCommandExecutor
import com.github.xepozz.php_dump.stubs.token_object.TokenParser
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
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
        val localFile = PathMapper.map(project, file)

        return withContext(Dispatchers.IO) {
            val output = StringBuilder()

            PhpCommandExecutor.execute(
                localFile,
                phpSnippet,
                project,
                StringBufferProcessAdapter(output),
            )


            val jsonString = output.toString()
//            println("jsonString: $jsonString")

            val tree = TokenParser.parseTokens(jsonString)
//            println("result tree: $tree")

            tree
        }
    }
}