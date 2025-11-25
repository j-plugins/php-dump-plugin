package com.github.xepozz.php_dump.actions

import com.github.xepozz.php_dump.panel.tabs.TabsUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.ui.content.ContentManager

class CreateTabAction(
    private val project: Project,
    private val contentManager: ContentManager
) : AnAction("Add Tab", "Add new tab", AllIcons.General.Add) {
    override fun actionPerformed(e: AnActionEvent) {
        TabsUtil.createTab(project, contentManager)
    }
}