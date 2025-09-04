package com.github.xepozz.php_dump.startup

import com.github.xepozz.php_dump.panel.RefreshType
import com.github.xepozz.php_dump.panel.RefreshablePanel
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener

class ProjectFileEditorListener(val project: Project) : FileEditorManagerListener, ToolWindowManagerListener {
    companion object {
        var active = false
    }

    override fun stateChanged(
        toolWindowManager: ToolWindowManager,
        changeType: ToolWindowManagerListener.ToolWindowManagerEventType
    ) {
        val phpDumpWindow = toolWindowManager.getToolWindow("PHP Dump") ?: return

        active = phpDumpWindow.isVisible
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        if (!active) return

        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("PHP Dump")

        toolWindow
            ?.component
            ?.components
            ?.mapNotNull { it as? RefreshablePanel }
            ?.forEach { it.refresh(project, RefreshType.AUTO) }
            ?: return

    }
}