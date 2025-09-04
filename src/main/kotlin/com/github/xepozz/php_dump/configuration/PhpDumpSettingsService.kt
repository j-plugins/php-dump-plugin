package com.github.xepozz.php_dump.configuration

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "com.github.xepozz.php_dump.services.PhpDumpSettingsService",
    storages = [Storage("PhpDump.xml")]
)
class PhpDumpSettingsService : SimplePersistentStateComponent<PhpDumpSettingsService.State>(State()) {
    class State : BaseState() {
        var debugLevel: String? by string(PhpOpcacheDebugLevel.BEFORE_OPTIMIZATION.value)
        var preloadFile: String? by string(null)
        var autoRefresh: Boolean by property(false)
        var tokensObject: Boolean by property(true)
    }

    var debugLevel: PhpOpcacheDebugLevel
        get() = PhpOpcacheDebugLevel.entries.firstOrNull { it.value == state.debugLevel } ?: PhpOpcacheDebugLevel.BEFORE_OPTIMIZATION
        set(value) {
            state.debugLevel = value.value
        }
    var preloadFile: String? by state::preloadFile
    var autoRefresh: Boolean by state::autoRefresh

    var tokensObject: Boolean by state::tokensObject

    companion object {
        fun getInstance(project: Project): PhpDumpSettingsService {
            return project.getService(PhpDumpSettingsService::class.java)
        }
    }
}