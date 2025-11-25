package com.github.xepozz.php_dump.toolWindow.tabs

import com.github.xepozz.php_dump.toolWindow.panel.CustomTreePanel
import com.intellij.openapi.project.Project
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener

object TabsUtil {
    // language=injectablephp
    private val SNIPPET = $$"""
        /**
         * $argv[1] – File path 
         */
        
        echo json_encode(
            array_map(
                function (PhpToken $token) {
                    return [
                        'line' => $token->line,
                        'pos' => $token->pos,
                        'name' => $token->getTokenName(),
                        'value' => $token->text,
                    ];
                },
                \PhpToken::tokenize(file_get_contents($argv[1])),
            )
        );
        """.trimIndent()

    fun createTab(project: Project, contentManager: ContentManager) {
        val tabsState = CompositeWindowTabsState.getInstance(project)
        val tabNumber = tabsState.state.tabs.size + 1
        val tabName = "Dump $tabNumber"
        val tabConfig = CompositeWindowTabsState.TabConfig(
            name = tabName,
            snippet = SNIPPET,
        )

        createTab(project, contentManager, tabConfig)
    }

    fun createTab(project: Project, contentManager: ContentManager, tabConfig: CompositeWindowTabsState.TabConfig) {
        val contentFactory = ContentFactory.getInstance()
        val tabsState = CompositeWindowTabsState.getInstance(project)
        tabsState.state.tabs.add(tabConfig)

        val panel = CustomTreePanel(project, tabConfig)
        val content = contentFactory.createContent(panel, tabConfig.name, false)

        contentManager.addContent(content)
        contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun contentRemoved(event: ContentManagerEvent) {
                if (event.content === content) {
                    tabsState.state.tabs.remove(tabConfig)
                }
            }
        })
        contentManager.setSelectedContent(content)
    }
}