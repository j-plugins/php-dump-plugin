package com.github.xepozz.php_dump.startup

import com.github.xepozz.php_dump.services.EditorProvider
import com.github.xepozz.php_opcodes_language.language.PHPOpFile
import com.github.xepozz.php_opcodes_language.language.psi.PHPOpBlock
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import com.jetbrains.php.lang.psi.elements.Method


class EditorListener : EditorMouseListener {
    override fun mouseClicked(event: EditorMouseEvent) {
        val editor = event.editor
        val project = editor.project ?: return
        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        val psiFile = psiDocumentManager.getPsiFile(editor.document)
        val offset = editor.logicalPositionToOffset(event.logicalPosition)
        val element = psiFile?.findElementAt(offset) ?: return

        val method = PsiTreeUtil.getParentOfType(element, Method::class.java) ?: return

        highlightElement(project, psiDocumentManager, method)
    }

    private fun highlightElement(
        project: Project,
        psiDocumentManager: PsiDocumentManager,
        method: Method
    ) {
        val editorProvider = project.getService(EditorProvider::class.java)
        val opcodesEditor = editorProvider.getOrCreateEditor()

        val psiFile = psiDocumentManager.getPsiFile(opcodesEditor.document) as? PHPOpFile ?: return

        val blocks = PsiTreeUtil.findChildrenOfType(psiFile, PHPOpBlock::class.java)

        val selectedBlock = blocks.firstOrNull { it.blockName.methodName?.name == method.name } ?: return


        opcodesEditor.foldingModel.apply {
            runBatchFoldingOperation {
                clearFoldRegions()

                blocks
                    .filter { it !== selectedBlock }
                    .forEach { block ->
                        createFoldRegion(
                            block.startOffset,
                            block.endOffset - 1,
                            block.blockName.name ?: "...",
                            FoldingGroup.newGroup("before block"),
                            false,
                        )?.apply { isExpanded = false }
                    }
            }
        }

        opcodesEditor.scrollingModel.scrollTo(
            opcodesEditor.offsetToLogicalPosition(selectedBlock.textOffset),
            ScrollType.CENTER_DOWN
        )
    }
}