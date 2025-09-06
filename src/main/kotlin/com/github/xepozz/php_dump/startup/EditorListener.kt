package com.github.xepozz.php_dump.startup

import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.psi.PsiDocumentManager

class EditorListener : EditorMouseListener {
    override fun mouseClicked(event: EditorMouseEvent) {
        val editor = event.editor
        val project = editor.project ?: return
        val priDocumentManager = PsiDocumentManager.getInstance(project)
        val psiFile = priDocumentManager.getPsiFile(editor.document)
        val offset = editor.logicalPositionToOffset(event.logicalPosition)
        val element = psiFile?.findElementAt(offset) ?: return

        println("clicked on $element")

//        val toolWindowManager = ToolWindowManager.getInstance(project)
//        val toolWindow = toolWindowManager.getToolWindow("PHP Dump")
//
//        toolWindow
//            ?.component
//            ?.components
//            ?.mapNotNull { it as? RefreshablePanel }
//            ?.forEach { it.refresh(project, RefreshType.AUTO) }
//            ?: return

    }
}