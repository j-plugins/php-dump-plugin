package com.github.xepozz.php_dump.services

import com.github.xepozz.php_dump.command.PhpCommandExecutor
import com.github.xepozz.php_dump.stubs.any_tree.AnyNodeList
import com.github.xepozz.php_dump.stubs.any_tree.AnyNodeParser
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class OpcacheSettingsTreeDumperService(var project: Project) : DumperServiceInterface {
    override suspend fun dump(file: String): Any {
        // language=injectablephp
        val phpSnippet = $$"""
            function dump(array $config)
            {
                $result = [];
                foreach ($config as $key => $value) {
                    if (is_array($value)) {
                        $result[] = [
                            'name' => $key,
                            'value' => '',
                            'children' => dump($value),
                        ];
                    } else {
                        $result[] = [
                            'name' => $key,
                            'value' => $value,
                            'children' => [],
                        ];
                    }
                }
                return $result;
            }
            
            echo json_encode(dump([
                'configuration' => opcache_get_configuration(), 
                'status' => opcache_get_status(true),
            ]));
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
            }, listOf("-dopcache.enable_cli=1"))


            val jsonString = output.toString()
//            println("jsonString: $jsonString")

            val tree: AnyNodeList = try {
                AnyNodeParser.parseAnyNode(jsonString)
            } catch (e: Throwable) {
                AnyNodeList()
            }
//            println("result tree: $tree")

            tree
        }
    }
}