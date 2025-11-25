package com.github.xepozz.php_dump.actions

import com.github.xepozz.php_dump.toolWindow.tabs.CompositeWindowTabsState
import com.intellij.icons.AllIcons
import com.intellij.ide.scratch.ScratchFileActions
import com.intellij.ide.scratch.ScratchFileCreationHelper
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.readText

class EditPhpSnippetAction(
    val project: Project,
    val tabConfig: CompositeWindowTabsState.TabConfig,
    val onUpdate: (String) -> Unit,
) :
    AnAction("Edit PHP Snippet", "Open PHP snippet in a temporary file", AllIcons.General.ExternalTools) {
    @OptIn(IntellijInternalApi::class)
    override fun actionPerformed(e: AnActionEvent) {
        val context = ScratchFileCreationHelper.Context()
        context.text = tabConfig.snippet
        context.language = Language.findLanguageByID("InjectablePHP")
        context.createOption = ScratchFileService.Option.create_if_missing

        val psiFile = ScratchFileActions.doCreateNewScratch(project, context) ?: return
        val virtualFile = psiFile.virtualFile

        val virtualFileManager = VirtualFileManager.getInstance()

        virtualFileManager.addAsyncFileListener(object :AsyncFileListener{
            override fun prepareChange(events: List<VFileEvent>): AsyncFileListener.ChangeApplier? {
                events.find { it.file == virtualFile } ?: return null

                return object : AsyncFileListener.ChangeApplier {
                    override fun afterVfsChange() {
                        val newText= virtualFile.readText()
                        println("newText: $newText")
                        onUpdate(newText)
                    }
                }
            }
        }) {}
    }
}