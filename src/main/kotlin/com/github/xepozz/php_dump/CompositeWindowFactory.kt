package com.github.xepozz.php_dump

import com.github.xepozz.php_dump.actions.CreateTabAction
import com.github.xepozz.php_dump.panel.OpcacheSettingsPanel
import com.github.xepozz.php_dump.panel.OpcodesTerminalPanel
import com.github.xepozz.php_dump.panel.TokenTreePanel
import com.github.xepozz.php_dump.panel.TokensTerminalPanel
import com.github.xepozz.php_dump.panel.tabs.CompositeWindowTabsState
import com.github.xepozz.php_dump.panel.tabs.TabsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowContentUiType
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.impl.ContentManagerImpl

open class CompositeWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindow = toolWindow as ToolWindowEx
        val contentFactory = ContentFactory.getInstance()
        val contentManager = toolWindow.contentManager as ContentManagerImpl
        println("can close: ${contentManager.canCloseContents()}")
        val tabsState = CompositeWindowTabsState.getInstance(project)

        val opcodesTerminalLayout = OpcodesTerminalPanel(project)
        val opcodesSettingsLayout = OpcacheSettingsPanel(project)
        val tokensTerminalLayout = TokensTerminalPanel(project)
        val tokenTreeLayout = TokenTreePanel(project)

        contentFactory.apply {
            createContent(opcodesTerminalLayout, "Opcodes", false).apply {
                isPinnable = true
                isCloseable = false
                contentManager.addContent(this)
            }
            createContent(opcodesSettingsLayout, "Opcache", false).apply {
                isPinnable = true
                isCloseable = false
                contentManager.addContent(this)
            }
            createContent(tokensTerminalLayout, "Plain Tokens", false).apply {
                isPinnable = true
                isCloseable = false
                contentManager.addContent(this)
            }
            createContent(tokenTreeLayout.component, "Tokens Tree", false).apply {
                isPinnable = true
                isCloseable = false
                contentManager.addContent(this)
            }

            DumbService.getInstance(project).runWhenSmart {
                tabsState.state.tabs.forEach { tabConfig ->
                    TabsUtil.createTab(project, contentManager, tabConfig)
                }
            }
        }

        toolWindow.setTabActions(CreateTabAction(project, contentManager))
        toolWindow.setDefaultContentUiType(ToolWindowContentUiType.COMBO)
    }
}
