package com.github.xepozz.php_dump.startup

import com.github.xepozz.php_dump.services.EditorProvider
import com.github.xepozz.php_opcodes_language.language.PHPOpFile
import com.github.xepozz.php_opcodes_language.language.psi.PHPOpBlockName
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

        PsiTreeUtil.findChildrenOfType(psiFile, PHPOpBlockName::class.java)
            .firstOrNull { it.methodName?.name == method.name }
            ?.let {
                opcodesEditor.scrollingModel.scrollTo(
                    opcodesEditor.offsetToLogicalPosition(it.textOffset), ScrollType.CENTER_DOWN
                )

                val block = it.parent

//                opcodesEditor.markupModel.apply {
//                    removeAllHighlighters()
//                    addRangeHighlighter(
//                        block.startOffset,
//                        block.endOffset,
//                        0,
//                        TextAttributes().apply {
//                            effectType = EffectType.BOXED
//                            effectColor = JBColor.RED
//                        },
//                        HighlighterTargetArea.EXACT_RANGE,
//                    )
//                }

                opcodesEditor.foldingModel.apply {
                    runBatchFoldingOperation {
                        clearFoldRegions()
                        createFoldRegion(
                            0,
                            block.startOffset - 1,
                            "...",
                            null,
                            true,
                        )
                        createFoldRegion(
                            block.endOffset,
                            block.containingFile.textLength,
                            "...",
                            null,
                            true,
                        )
                    }
                }
            }
    }
}