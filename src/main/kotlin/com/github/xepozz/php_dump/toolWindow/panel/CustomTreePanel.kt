package com.github.xepozz.php_dump.toolWindow.panel

import com.github.xepozz.php_dump.actions.CollapseTreeAction
import com.github.xepozz.php_dump.actions.EditPhpSnippetAction
import com.github.xepozz.php_dump.actions.ExpandTreeAction
import com.github.xepozz.php_dump.actions.OpenPhpSettingsAction
import com.github.xepozz.php_dump.actions.RefreshAction
import com.github.xepozz.php_dump.notification.NotificationUtil
import com.github.xepozz.php_dump.services.CustomDumpResult
import com.github.xepozz.php_dump.services.CustomTreeDumperService
import com.github.xepozz.php_dump.services.EditorProvider
import com.github.xepozz.php_dump.stubs.token_object.TokensList
import com.github.xepozz.php_dump.toolWindow.tabs.CompositeWindowTabsState
import com.github.xepozz.php_dump.tree.RootNode
import com.github.xepozz.php_dump.tree.TokenNode
import com.github.xepozz.php_dump.tree.TokensTreeStructure
import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.findDocument
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class CustomTreePanel(
    private val project: Project,
    private val tabConfig: CompositeWindowTabsState.TabConfig,
) :
    SimpleToolWindowPanel(false, false),
    RefreshablePanel, Disposable {
    val fileEditorManager = FileEditorManager.getInstance(project)
    private val progressBar = JProgressBar()

    var rawOutput = ""
    private val treeModel = StructureTreeModel(TokensTreeStructure(RootNode(null)), this)
    private val tree = Tree(DefaultTreeModel(DefaultMutableTreeNode())).apply {
        setModel(AsyncTreeModel(treeModel, this@CustomTreePanel))
        isRootVisible = true
        showsRootHandles = true

        TreeUIHelper.getInstance()
            .installTreeSpeedSearch(this, { path ->
                val treeNode = path.lastPathComponent as? DefaultMutableTreeNode
                val tokenNode = treeNode?.userObject as? TokenNode

                tokenNode?.node?.value
            }, true)
    }
    val service: CustomTreeDumperService = project.getService(CustomTreeDumperService::class.java)
    val editorProvider: EditorProvider = project.getService(EditorProvider::class.java)

    val scrollPane = JBScrollPane(tree)
    var contentPanel: JComponent = JPanel(BorderLayout()).apply { add(scrollPane) }
    val rawVirtualFile = LightVirtualFile("Raw Output", "")
    val rawPanel = editorProvider.getOrCreateEditorFor(rawVirtualFile).component

    init {
        treeModel.invalidateAsync()

        createToolbar()
        createContent()

        addTreeListeners()

        SwingUtilities.invokeLater { refreshData() }
    }

    private var showRawOutput = false
    fun createToolbar() {
        val actionGroup = DefaultActionGroup().apply {
            add(RefreshAction { refreshData() })
            addSeparator()
            add(ExpandTreeAction(tree))
            add(CollapseTreeAction(tree))
            addSeparator()
            add(EditPhpSnippetAction(project, tabConfig) { snippet ->
                tabConfig.snippet = snippet
            })
            add(object :
                ToggleAction("Use Object Tokens", "Switches engine to dump lexical tokens", AllIcons.FileTypes.Json) {
                override fun isSelected(event: AnActionEvent): Boolean = showRawOutput

                override fun setSelected(event: AnActionEvent, value: Boolean) {
                    println("switch content")
                    event.presentation.icon = if (value) AllIcons.FileTypes.Json else AllIcons.FileTypes.Text
                    showRawOutput = value
                    contentPanel.components.forEach { contentPanel.remove(it) }
                    contentPanel.add(if (showRawOutput) rawPanel else scrollPane)
                    SwingUtilities.invokeLater { contentPanel.repaint() }
                }

            })
            add(OpenPhpSettingsAction())
        }

        val actionToolbar = ActionManager.getInstance().createActionToolbar("Tree Toolbar", actionGroup, false)
        actionToolbar.targetComponent = this

        val toolBarPanel = JPanel(GridLayout())
        toolBarPanel.add(actionToolbar.component)

        toolbar = toolBarPanel
    }

    private fun createContent() {
        val responsivePanel = JPanel(BorderLayout())
        responsivePanel.add(progressBar, BorderLayout.NORTH)
        responsivePanel.add(contentPanel, BorderLayout.CENTER)

        setContent(responsivePanel)
    }

    private fun addTreeListeners() {
        tree.addTreeSelectionListener { event ->
            event.path?.let { navigation(it) }
        }
    }

    private fun navigation(closestPathForLocation: TreePath) {
        val lastUserObject = TreeUtil.getLastUserObject(closestPathForLocation)
        if (lastUserObject is TokenNode) {
            val fileEditor = FileEditorManager.getInstance(project)
            val textEditor = fileEditor.selectedTextEditor!!
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(textEditor.document)
            val node = lastUserObject.node
            val element = psiFile?.findElementAt(node.pos)
            if (element is Navigatable) {
                val markupModel = textEditor.markupModel
                val textAttributes = TextAttributes().apply {
                    effectType = EffectType.BOXED
                    effectColor = JBColor.RED
                    backgroundColor = null
                }

                markupModel.removeAllHighlighters()
                markupModel.addRangeHighlighter(
                    node.pos,
                    node.endPos,
                    HighlighterLayer.SELECTION + 1,
                    textAttributes,
                    HighlighterTargetArea.EXACT_RANGE
                )
            }
        }
    }

    private fun refreshData() {
        CoroutineScope(Dispatchers.EDT).launch {
            progressBar.setIndeterminate(true)
            progressBar.isVisible = true
            tree.emptyText.text = "Loading..."

            val result = getViewData()
            tree.emptyText.text = "Nothing to show"
            rebuildTree(result.tokens)
            rawOutput = result.raw

            progressBar.setIndeterminate(false)
            progressBar.isVisible = false
        }
    }

    private fun rebuildTree(list: TokensList?) {
        val treeModel = StructureTreeModel<AbstractTreeStructure>(TokensTreeStructure(RootNode(list)), this)
        tree.setModel(AsyncTreeModel(treeModel, this))
        tree.setRootVisible(false)
        treeModel.invalidateAsync()

        TreeUtil.expandAll(tree)
    }

    private suspend fun getViewData(): CustomDumpResult {
        val result = CustomDumpResult()
        val editor = fileEditorManager.selectedTextEditor ?: return result
        val virtualFile = editor.virtualFile ?: return result

        service.phpSnippet = tabConfig.snippet

        val runResult = service.dump(virtualFile) as CustomDumpResult

        val error = runResult.error
        val document = rawVirtualFile.findDocument()
        if (document != null) {
            runWriteAction {
                document.setText(runResult.raw ?: "")
            }
        }

        if (error != null) {
            NotificationUtil
                .sendNotification(
                    project,
                    "Error ${error.javaClass}",
                    error.message ?: "Unknown error",
                )
        }

        return runResult
    }

    override fun refresh(project: Project, type: RefreshType) {
        refreshData()
    }

    override fun dispose() {
    }
}