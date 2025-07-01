package com.github.xepozz.php_dump.panel

import com.github.xepozz.php_dump.PhpDumpIcons
import com.github.xepozz.php_dump.actions.ClearConsoleViewAction
import com.github.xepozz.php_dump.actions.RunDumpTokensCommandAction
import com.github.xepozz.php_dump.configuration.PhpDumpSettingsService
import com.github.xepozz.php_dump.services.TokensDumperService
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JComponent
import javax.swing.JPanel

class TokensTerminalPanel(
    val project: Project,
) : SimpleToolWindowPanel(false, false), RefreshablePanel {
    var viewComponent: JComponent
    val state = PhpDumpSettingsService.getInstance(project)
    var service: TokensDumperService
    val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console

    init {
        viewComponent = consoleView.component

        service = project.getService(TokensDumperService::class.java)
        service.consoleView = consoleView

        createToolBar()
        createContent()
    }

    private fun createToolBar() {
        val actionGroup = DefaultActionGroup().apply {
            add(RunDumpTokensCommandAction(service))
            add(ClearConsoleViewAction(consoleView))
            addSeparator()
            add(object : AnAction(
                "Use Object Tokens", "Switches engine to dump lexical tokens",
                if (state.tokensObject) PhpDumpIcons.TEXT else PhpDumpIcons.SHOW_AS_TREE
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    state.tokensObject = !state.tokensObject
                    refresh(project, RefreshType.MANUAL)
                }

                override fun update(e: AnActionEvent) {
                    if (state.tokensObject) {
                        e.presentation.text = "Use Plain Tokens"
                        e.presentation.icon = PhpDumpIcons.TEXT
                    } else {
                        e.presentation.text = "Use Object Tokens"
                        e.presentation.icon = PhpDumpIcons.SHOW_AS_TREE
                    }
                }
            })
        }

        val actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLWINDOW_TOOLBAR_BAR, actionGroup, false)
        actionToolbar.targetComponent = this

        val toolBarPanel = JPanel(GridLayout())
        toolBarPanel.add(actionToolbar.component)

        toolbar = toolBarPanel
    }

    private fun createContent() {
        val responsivePanel = JPanel(BorderLayout())
        responsivePanel.add(viewComponent, BorderLayout.CENTER)
        responsivePanel.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                viewComponent.revalidate()
                viewComponent.repaint()
            }
        })

        setContent(responsivePanel)
    }

    override fun refresh(project: Project, type: RefreshType) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val virtualFile = editor.virtualFile ?: return

        runBlocking { service.dump(virtualFile) }
    }
}