package com.github.xepozz.php_dump

import com.github.xepozz.php_dump.panel.OpcacheSettingsPanel
import com.github.xepozz.php_dump.panel.OpcodesTerminalPanel
import com.github.xepozz.php_dump.panel.TokenTreePanel
import com.github.xepozz.php_dump.panel.TokensTerminalPanel
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

open class CompositeWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val contentManager = toolWindow.contentManager

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
        }
    }
}