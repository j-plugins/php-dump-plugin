package com.github.xepozz.php_dump.actions

import com.github.xepozz.php_dump.toolWindow.tabs.CompositeWindowTabsState
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile

class EditPhpSnippetAction(
    val project: Project,
    val tabConfig: CompositeWindowTabsState.TabConfig,
    val onUpdate: (String) -> Unit,
) :
    AnAction("Edit PHP Snippet", "Open PHP snippet in a temporary file", AllIcons.General.ExternalTools) {

    val documentManager = PsiDocumentManager.getInstance(project)

    override fun actionPerformed(e: AnActionEvent) {
        val language = Language.findLanguageByID("InjectablePHP") ?: return

        val virtualFile = LightVirtualFile(
            tabConfig.name,
            language,
            tabConfig.snippet,
        )
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)!!

        val document = documentManager.getDocument(psiFile)!!
        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val newText = event.document.text
                onUpdate(newText)
            }
        })

        FileEditorManager.getInstance(project).openFile(virtualFile, true)
    }
}