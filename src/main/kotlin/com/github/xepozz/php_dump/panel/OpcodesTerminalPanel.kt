package com.github.xepozz.php_dump.panel

import com.github.xepozz.php_dump.PhpDumpIcons
import com.github.xepozz.php_dump.actions.OpenPhpSettingsAction
import com.github.xepozz.php_dump.actions.RefreshAction
import com.github.xepozz.php_dump.configuration.PhpDumpSettingsService
import com.github.xepozz.php_dump.configuration.PhpOpcacheDebugLevel
import com.github.xepozz.php_dump.services.EditorProvider
import com.github.xepozz.php_dump.services.OpcodesDumperService
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.asSafely
import com.jetbrains.php.lang.PhpFileType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JPanel

class OpcodesTerminalPanel(
    val project: Project,
) : SimpleToolWindowPanel(false, false), RefreshablePanel, Disposable {

    val fileEditorManager = FileEditorManager.getInstance(project)
    val documentManager = PsiDocumentManager.getInstance(project)
    val editorFactory = EditorFactory.getInstance()

    private val service = project.getService(OpcodesDumperService::class.java)
    private val editorProvider = project.getService(EditorProvider::class.java)
    private val state = PhpDumpSettingsService.getInstance(project)

    val editor = editorProvider.getOrCreateEditor()
    val viewComponent = editor.component

    init {

        createToolBar()
        createContent()
    }


    private fun createToolBar() {
        val actionGroup = DefaultActionGroup().apply {
            add(RefreshAction { refresh(project, RefreshType.MANUAL) })
            add(object : AnAction("Clear Output", "Clear the output", AllIcons.Actions.GC) {
                override fun actionPerformed(e: AnActionEvent) {
                    setDocumentText(project, "")
                }

                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            })
            add(object : AnAction(
                "Enable Auto Refresh", "Turns on or off auto refresh of panel context",
                if (state.autoRefresh) PhpDumpIcons.RESTART_STOP else PhpDumpIcons.RERUN_AUTOMATICALLY
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    state.autoRefresh = !state.autoRefresh
                }

                override fun update(e: AnActionEvent) {
                    if (state.autoRefresh) {
                        e.presentation.text = "Disable Auto Refresh"
                        e.presentation.icon = PhpDumpIcons.RESTART_STOP
                    } else {
                        e.presentation.text = "Enable Auto Refresh"
                        e.presentation.icon = PhpDumpIcons.RERUN_AUTOMATICALLY
                    }
                }

                override fun getActionUpdateThread() = ActionUpdateThread.BGT
            })
            addSeparator()
            add(DefaultActionGroup("Debug Level", true).apply {
                mapOf(
                    PhpOpcacheDebugLevel.BEFORE_OPTIMIZATION to "Before Optimization (0x10000)",
                    PhpOpcacheDebugLevel.AFTER_OPTIMIZATION to "After Optimization (0x20000)",
                    PhpOpcacheDebugLevel.CONTEXT_FREE to "Context Free (0x40000)",
                    PhpOpcacheDebugLevel.SSA_FORM to "Static Single Assignment Form (0x200000)",
                )
                    .map { (level, label) ->
                        object : AnAction(label) {
                            override fun actionPerformed(e: AnActionEvent) {
                                state.debugLevel = level
                                refresh(project, RefreshType.MANUAL)
                            }

                            override fun update(e: AnActionEvent) {
                                e.presentation.icon = when (state.debugLevel) {
                                    level -> AllIcons.Actions.Checked
                                    else -> null
                                }
                            }

                            override fun getActionUpdateThread() = ActionUpdateThread.BGT

                        }
                    }
                    .also { addAll(it) }

                templatePresentation.icon = AllIcons.Actions.ToggleVisibility
            })

            add(object : AnAction("Select Preload File", "Choose a file", AllIcons.Actions.MenuOpen) {
                override fun actionPerformed(e: AnActionEvent) {

                    val fileChooserDescriptor =
                        FileChooserDescriptorFactory.createSingleFileDescriptor(PhpFileType.INSTANCE)
                            .withTitle("Select Preload File")
                            .withDescription("Choose a preload.php file")

                    FileChooser
                        .chooseFile(fileChooserDescriptor, project, null)
                        .let { file ->
                            state.preloadFile = file?.path
                        }
                    refresh(project, RefreshType.MANUAL)
                }

                override fun getActionUpdateThread() = ActionUpdateThread.BGT
            })
            add(OpenPhpSettingsAction())
        }

        val actionToolbar = ActionManager.getInstance().createActionToolbar("Opcodes Toolbar", actionGroup, false)
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
        if (type == RefreshType.AUTO && !state.autoRefresh) {
            return
        }
        val editor = fileEditorManager.selectedTextEditor ?: return
        val virtualFile = editor.virtualFile ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val result = service.dump(virtualFile)

            val content = result
                .asSafely<String>()
                ?.replace("\r\n", "\n")
                ?: "No output"

            withContext(Dispatchers.EDT) {
                setDocumentText(project, content)
            }
        }
    }

    private fun setDocumentText(project: Project, content: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.setText(content)
            documentManager.commitDocument(editor.document)
        }
    }

    override fun dispose() {
        editorFactory.releaseEditor(editor)
    }
}