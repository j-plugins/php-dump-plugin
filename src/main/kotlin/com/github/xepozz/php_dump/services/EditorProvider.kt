package com.github.xepozz.php_dump.services

import com.github.xepozz.php_opcodes_language.language.PHPOpFileType
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile

@Service(Service.Level.PROJECT)
class EditorProvider(var project: Project) {
    companion object {
        val editorId = Key.create<Boolean>("opcodes.editor.id")
    }

    val documentManager = PsiDocumentManager.getInstance(project)
    val editorFactory = EditorFactory.getInstance()

    var editor: EditorEx? = null

    fun getOrCreateEditor(): EditorEx = synchronized(this) {
        editor?.let { return it }

        val virtualFile = LightVirtualFile(
            "opcodes.phpop",
            PHPOpFileType.INSTANCE,
            ""
        )
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)!!
        val document = documentManager.getDocument(psiFile)!!

        return@synchronized (editorFactory.createEditor(document, project, virtualFile, false) as EditorEx)
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
                putUserData(editorId, true)

                editor = this
            }
    }
}