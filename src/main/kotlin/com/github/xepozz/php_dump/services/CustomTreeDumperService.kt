package com.github.xepozz.php_dump.services

import com.github.xepozz.php_dump.StringBufferProcessAdapter
import com.github.xepozz.php_dump.command.PathMapper
import com.github.xepozz.php_dump.command.PhpCommandExecutor
import com.github.xepozz.php_dump.stubs.token_object.TokenParser
import com.github.xepozz.php_dump.stubs.token_object.TokensList
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class CustomTreeDumperService(var project: Project) : DumperServiceInterface {
    var phpSnippet: String? = null

    override suspend fun dump(file: String): Any {
        val phpSnippet = phpSnippet ?: return TokensList()

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