package com.github.xepozz.php_dump.services

import com.github.xepozz.php_opcodes_language.language.PHPOpFileType
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile

@Service(Service.Level.PROJECT)
class EditorProvider(var project: Project) {
    companion object {
        val opcodesEditorId = Key.create<Boolean>("opcodes.editor.id")
    }

    val documentManager = PsiDocumentManager.getInstance(project)
    val editorFactory = EditorFactory.getInstance()

    var editors = mutableMapOf<Key<*>, EditorEx>()
    var editorsByFile = mutableMapOf<VirtualFile, EditorEx>()
    var virtualFilesByEditorId = mutableMapOf<Key<*>, VirtualFile>()

    fun getOrCreateEditor(editorId: Key<Boolean>): EditorEx =
        synchronized(this) {
            editors.getOrPut(editorId) {
                val virtualFile = virtualFilesByEditorId.getOrPut(editorId) {
                    LightVirtualFile("opcodes.phpop", PHPOpFileType.INSTANCE, "")
                }
                createEditor(virtualFile)
            }
        }

    fun getOrCreateEditorFor(virtualFile: VirtualFile): EditorEx =
        synchronized(this) {
            editorsByFile.getOrPut(virtualFile) { createEditor(virtualFile) }
        }

    private fun createEditor(virtualFile: VirtualFile): EditorEx {
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)!!
        val document = documentManager.getDocument(psiFile)!!

        return (editorFactory.createEditor(document, project, virtualFile, false) as EditorEx)
            .apply {
                settings.apply {
                    isBlinkCaret = true
                    isCaretRowShown = true
                    isBlockCursor = false
                    isLineMarkerAreaShown = true
                    isAutoCodeFoldingEnabled = true
                    isSmartHome = true
                    isShowIntentionBulb = true
                }

                setCaretEnabled(true)
            }
    }
}