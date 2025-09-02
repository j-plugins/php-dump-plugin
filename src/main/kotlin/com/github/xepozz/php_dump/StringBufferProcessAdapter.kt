package com.github.xepozz.php_dump

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.Key

class StringBufferProcessAdapter(val output: StringBuilder) : ProcessAdapter() {
    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        when (outputType) {
            ProcessOutputTypes.STDERR -> output.append(event.text)
            ProcessOutputTypes.STDOUT -> output.append(event.text)
        }
    }
}