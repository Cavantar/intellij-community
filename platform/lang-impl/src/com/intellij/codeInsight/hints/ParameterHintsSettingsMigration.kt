// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.settings.ParameterNameHintsSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.extensions.ExtensionNotApplicableException
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectPostStartupActivity

internal class ParameterHintsSettingsMigration : ProjectPostStartupActivity {
  init {
    if (ApplicationManager.getApplication().isUnitTestMode) {
      throw ExtensionNotApplicableException.create()
    }
  }

  override suspend fun execute(project: Project) {
    val editorSettingsExternalizable = EditorSettingsExternalizable.getInstance()
    if (!editorSettingsExternalizable.isShowParameterNameHints) {
      editorSettingsExternalizable.isShowParameterNameHints = true
      for (language in getBaseLanguagesWithProviders()) {
        ParameterNameHintsSettings.getInstance().setIsEnabledForLanguage(false, language)
      }
    }
  }
}