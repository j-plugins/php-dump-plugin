package com.github.xepozz.php_dump.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.jetbrains.php.config.PhpProjectConfigurable

class OpenPhpSettingsAction : AnAction("Open Settings", "Open plugin settings", AllIcons.General.Settings) {
    override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(e.project, PhpProjectConfigurable::class.java)
    }
}