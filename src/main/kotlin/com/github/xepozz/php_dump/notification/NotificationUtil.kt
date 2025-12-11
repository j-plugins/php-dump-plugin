package com.github.xepozz.php_dump.notification

import com.github.xepozz.php_dump.PhpDumpIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project

object NotificationUtil {
    fun sendNotification(
        project: Project,
        title: String,
        message: String,
        actions: Collection<AnAction> = emptyList(),
    ) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("PHP Dump Errors")
            .createNotification(
                title,
                message,
                NotificationType.ERROR,
            )
        notification.isImportant = true
        notification.icon = PhpDumpIcons.POT

        notification.addActions(actions)

        notification.notify(project)
    }
}