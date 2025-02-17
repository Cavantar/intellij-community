// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.warmup

import com.intellij.ide.startup.ServiceNotReadyException
import com.intellij.openapi.progress.impl.CoreProgressManager
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexEx
import com.intellij.warmup.util.ConsoleLog
import com.intellij.warmup.util.runTaskAndLogTime
import kotlinx.coroutines.time.delay
import java.time.Duration
import kotlin.system.exitProcess

suspend fun waitIndexInitialization() {
  val deferred = (FileBasedIndex.getInstance() as FileBasedIndexEx).untilIndicesAreInitialized()
                 ?: throw ServiceNotReadyException()
  deferred.join()
}

suspend fun waitUntilProgressTasksAreFinishedOrFail() {
  try {
    waitUntilProgressTasksAreFinished()
  }
  catch (e: IllegalStateException) {
    ConsoleLog.info(e.message ?: e.toString())
    exitProcess(2)
  }
}

private suspend fun waitUntilProgressTasksAreFinished() {
  runTaskAndLogTime("Awaiting for progress tasks") {
    val timeout = System.getProperty("ide.progress.tasks.awaiting.timeout.min", "60").toLongOrNull() ?: 60
    val startTime = System.currentTimeMillis()
    while (CoreProgressManager.getCurrentIndicators().isNotEmpty()) {
      if (System.currentTimeMillis() - startTime > Duration.ofMinutes(timeout).toMillis()) {
        val timeoutMessage = StringBuilder("Progress tasks awaiting timeout.\n")
        timeoutMessage.appendLine("Not finished tasks:")
        for (indicator in CoreProgressManager.getCurrentIndicators()) {
          timeoutMessage.appendLine("  - ${indicator.text}")
        }
        error(timeoutMessage)
      }
      delay(Duration.ofMillis(1000))
    }
  }
}