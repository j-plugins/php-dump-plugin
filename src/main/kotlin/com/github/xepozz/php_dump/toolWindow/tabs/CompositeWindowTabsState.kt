package com.github.xepozz.php_dump.toolWindow.tabs

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "CompositeWindowTabs",
    storages = [Storage("php-dump.tabs.xml")]
)
class CompositeWindowTabsState : PersistentStateComponent<CompositeWindowTabsState.State> {
    data class State(
        var tabs: MutableList<TabConfig> = mutableListOf()
    )

    data class TabConfig(
        var name: String = "",
        var snippet: String = ""
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(project: Project) = project.getService(CompositeWindowTabsState::class.java)
    }
}