package com.github.xepozz.php_dump.toolWindow.panel

import com.github.xepozz.php_dump.actions.CollapseTreeAction
import com.github.xepozz.php_dump.actions.ExpandTreeAction
import com.github.xepozz.php_dump.actions.OpenPhpSettingsAction
import com.github.xepozz.php_dump.actions.RefreshAction
import com.github.xepozz.php_dump.services.OpcacheSettingsTreeDumperService
import com.github.xepozz.php_dump.stubs.any_tree.AnyNodeList
import com.github.xepozz.php_dump.stubs.any_tree.AnyRootNode
import com.github.xepozz.php_dump.stubs.any_tree.AnyTreeStructure
import com.github.xepozz.php_dump.stubs.any_tree.LeafNode
import com.github.xepozz.php_dump.tree.RootNode
import com.github.xepozz.php_dump.tree.TokensTreeStructure
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
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
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class OpcacheSettingsPanel(private val project: Project) :
    SimpleToolWindowPanel(false, false),
    RefreshablePanel, Disposable {
    val fileEditorManager = FileEditorManager.getInstance(project)
    private val progressBar = JProgressBar()

    private val treeModel = StructureTreeModel(TokensTreeStructure(RootNode(null)), this)
    private val tree = Tree(DefaultTreeModel(DefaultMutableTreeNode())).apply {
        setModel(AsyncTreeModel(treeModel, this@OpcacheSettingsPanel))
        isRootVisible = true
        showsRootHandles = true

        TreeUIHelper.getInstance()
            .installTreeSpeedSearch(this, { path ->
                val treeNode = path.lastPathComponent as? DefaultMutableTreeNode
                val tokenNode = treeNode?.userObject as? LeafNode

                tokenNode?.node?.value
            }, true)
    }
    val service: OpcacheSettingsTreeDumperService = project.getService(OpcacheSettingsTreeDumperService::class.java)


    init {
        treeModel.invalidateAsync()

        createToolbar()
        createContent()

        SwingUtilities.invokeLater { refreshData() }
    }

    fun createToolbar() {
        val actionGroup = DefaultActionGroup().apply {
            add(RefreshAction { refresh(project, RefreshType.MANUAL) })
            addSeparator()
            add(ExpandTreeAction(tree))
            add(CollapseTreeAction(tree))
            addSeparator()
            add(OpenPhpSettingsAction())
        }

        val actionToolbar = ActionManager.getInstance().createActionToolbar("Opcache Toolbar", actionGroup, false)
        actionToolbar.targetComponent = this

        val toolBarPanel = JPanel(GridLayout())
        toolBarPanel.add(actionToolbar.component)

        toolbar = toolBarPanel
    }

    private fun createContent() {
        val responsivePanel = JPanel(BorderLayout())
        responsivePanel.add(progressBar, BorderLayout.NORTH)
        responsivePanel.add(JBScrollPane(tree), BorderLayout.CENTER)

        setContent(responsivePanel)
    }

    private fun refreshData() {
        CoroutineScope(Dispatchers.EDT).launch {
            progressBar.setIndeterminate(true)
            progressBar.isVisible = true
            tree.emptyText.text = "Loading..."

            val result = getViewData()
            tree.emptyText.text = "Nothing to show"
            rebuildTree(result)

            progressBar.setIndeterminate(false)
            progressBar.isVisible = false
        }
    }

    private fun rebuildTree(list: AnyNodeList?) {
        val treeModel = StructureTreeModel<AbstractTreeStructure>(AnyTreeStructure(AnyRootNode(list)), this)
        tree.setModel(AsyncTreeModel(treeModel, this))
        tree.setRootVisible(false)
        treeModel.invalidateAsync()

        TreeUtil.expandAll(tree)
    }

    private suspend fun getViewData(): AnyNodeList {
        val result = AnyNodeList()
        val editor = fileEditorManager.selectedTextEditor ?: return result
        val virtualFile = editor.virtualFile ?: return result

        return service.dump(virtualFile) as? AnyNodeList ?: result
    }

    override fun refresh(project: Project, type: RefreshType) {
        refreshData()
    }

    override fun dispose() {
    }
}