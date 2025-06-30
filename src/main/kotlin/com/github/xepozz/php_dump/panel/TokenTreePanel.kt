package com.github.xepozz.php_dump.panel

import com.github.xepozz.php_dump.actions.RunDumpTokensCommandAction
import com.github.xepozz.php_dump.nonBlocking
import com.github.xepozz.php_dump.services.TokensObjectTreeDumperService
import com.github.xepozz.php_dump.stubs.token_object.TokensList
import com.github.xepozz.php_dump.tree.RootNode
import com.github.xepozz.php_dump.tree.TokenNode
import com.github.xepozz.php_dump.tree.TokensTreeStructure
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.JBColor
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import kotlinx.coroutines.runBlocking
import org.jdesktop.swingx.VerticalLayout
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class TokenTreePanel(private val project: Project) :
    SimpleToolWindowPanel(false, false),
    RefreshablePanel, Disposable {
    private val progressBar = JProgressBar()

    private val treeModel = StructureTreeModel(TokensTreeStructure(RootNode(null)), this)
    private val tree = Tree(DefaultTreeModel(DefaultMutableTreeNode())).apply {
        setModel(AsyncTreeModel(treeModel, this@TokenTreePanel))
        isRootVisible = true
        showsRootHandles = true

        TreeUIHelper.getInstance()
            .installTreeSpeedSearch(this, { path ->
                val treeNode = path.lastPathComponent as? DefaultMutableTreeNode
                val tokenNode = treeNode?.userObject as? TokenNode

                tokenNode?.node?.value
            }, true)
    }
    val service: TokensObjectTreeDumperService = project.getService(TokensObjectTreeDumperService::class.java)


    init {
        treeModel.invalidateAsync()

        createToolbar()
        createContent()

        addTreeListeners()

        SwingUtilities.invokeLater { refreshData() }
    }

    fun createToolbar() {
        val actionGroup = DefaultActionGroup().apply {
            add(RunDumpTokensCommandAction(service, "Dump Tree"))
            addSeparator()
        }

        val actionToolbar = ActionManager.getInstance().createActionToolbar("Tree Toolbar", actionGroup, false)
        actionToolbar.targetComponent = this

        val toolBarPanel = JPanel(VerticalLayout()).apply {
            add(
                JPanel(VerticalLayout()).apply {
                    add(createRefreshButton { refreshData() })
                    add(createExpandsAll(tree))
                    add(createCollapseAll(tree))
                }
            )

            add(actionToolbar.component)
        }
//        searchTextField.addDocumentListener(this)

        toolbar = toolBarPanel
    }

    private fun createContent() {
        val responsivePanel = JPanel(BorderLayout())
        responsivePanel.add(progressBar, BorderLayout.NORTH)
        responsivePanel.add(JBScrollPane(tree))

        setContent(responsivePanel)
    }

    private fun addTreeListeners() {
        tree.addTreeSelectionListener { event ->
            event.path?.let { navigation(it) }
        }
    }

    private fun navigation(closestPathForLocation: TreePath?) {
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
        progressBar.setIndeterminate(true)
        progressBar.isVisible = true
        tree.emptyText.text = "Loading..."


        project.nonBlocking({ getViewData() }) { result ->
            tree.emptyText.text = "Nothing to show"
            rebuildTree(result)

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

    private fun getViewData(): TokensList {
        val result = TokensList()
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return result
        val virtualFile = editor.virtualFile ?: return result

        val runBlocking = runBlocking {
            val a = service.dump(virtualFile)

            a
        }
        println("result is $runBlocking")

        return runBlocking as? TokensList ?: result
    }

    override fun refresh(project: Project, type: RefreshType) {
        refreshData()
    }

    override fun dispose() {
    }
}