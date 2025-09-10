package com.github.xepozz.php_dump

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.Key


class SeparateStringBufferProcessAdapter(val stderr: StringBuilder?, val stdout: StringBuilder?) : ProcessAdapter() {
    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        when (outputType) {
            ProcessOutputTypes.STDERR -> stderr?.append(event.text)
            ProcessOutputTypes.STDOUT -> stdout?.append(event.text)
        }
    }
}