// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.gradleJava.configuration

import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.openapi.externalSystem.model.project.ProjectId
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalModuleBuilder
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCoreUtil
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.configuration.toKotlinRepositorySnippet
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.gradleCodeInsightCommon.GradleBuildScriptSupport
import org.jetbrains.kotlin.idea.gradleCodeInsightCommon.SettingsScriptBuilder
import org.jetbrains.kotlin.idea.projectConfiguration.RepositoryDescription
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.plugins.gradle.frameworkSupport.GradleFrameworkSupportProvider
import org.jetbrains.plugins.gradle.service.project.wizard.AbstractGradleModuleBuilder
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

internal var Module.gradleModuleBuilder: AbstractExternalModuleBuilder<*>? by UserDataProperty(Key.create("GRADLE_MODULE_BUILDER"))
private var Module.settingsScriptBuilder: SettingsScriptBuilder<out PsiFile>? by UserDataProperty(Key.create("SETTINGS_SCRIPT_BUILDER"))

internal fun findSettingsGradleFile(module: Module): VirtualFile? {
    val contentEntryPath = module.gradleModuleBuilder?.contentEntryPath ?: return null
    if (contentEntryPath.isEmpty()) return null
    val contentRootDir = File(contentEntryPath)
    val modelContentRootDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(contentRootDir) ?: return null
    return modelContentRootDir.findChild(GradleConstants.SETTINGS_FILE_NAME)
        ?: modelContentRootDir.findChild(GradleConstants.KOTLIN_DSL_SETTINGS_FILE_NAME)
        ?: module.project.baseDir.findChild(GradleConstants.SETTINGS_FILE_NAME)
        ?: module.project.baseDir.findChild(GradleConstants.KOTLIN_DSL_SETTINGS_FILE_NAME)
}

class KotlinSettingsScriptBuilder(scriptFile: KtFile): SettingsScriptBuilder<KtFile>(scriptFile) {
    override fun addPluginRepository(repository: RepositoryDescription) {
        addPluginRepositoryExpression(repository.toKotlinRepositorySnippet())
    }

    override fun buildPsiFile(project: Project): KtFile {
        return KtPsiFactory(project).createFile(build())
    }
}

// Circumvent write actions and modify the file directly
// TODO: Get rid of this hack when IDEA API allows manipulation of settings script similarly to the main script itself
internal fun updateSettingsScript(module: Module, updater: (SettingsScriptBuilder<out PsiFile>) -> Unit) {
    fun createScriptBuilder(module: Module): SettingsScriptBuilder<*>? {
        val settingsGradleFile = findSettingsGradleFile(module)?.toPsiFile(module.project) ?: return null
        for (extension in GradleBuildScriptSupport.EP_NAME.extensionList) {
            return extension.createScriptBuilder(settingsGradleFile) ?: continue
        }

        return null
    }

    val storedSettingsBuilder = module.settingsScriptBuilder
    val settingsBuilder = storedSettingsBuilder ?: createScriptBuilder(module) ?: return
    if (storedSettingsBuilder == null) {
        module.settingsScriptBuilder = settingsBuilder
    }
    updater(settingsBuilder)
}

internal fun flushSettingsGradleCopy(module: Module) {
    try {
        val settingsFile = findSettingsGradleFile(module)
        val settingsScriptBuilder = module.settingsScriptBuilder
        if (settingsScriptBuilder != null && settingsFile != null) {
            // The module.project is not opened yet.
            // Due to optimization in ASTDelegatePsiElement.getManager() and relevant ones,
            // we have to take theOnlyOpenProject() for manipulations with tmp file
            // (otherwise file will have one parent project and its elements will have other parent project,
            // and we will get KT-29333 problem).
            // TODO: get rid of file manipulations until project is opened
            val project = ProjectCoreUtil.theOnlyOpenProject() ?: module.project
            val tmpFile = settingsScriptBuilder.buildPsiFile(project)
            CodeStyleManager.getInstance(project).reformat(tmpFile)
            VfsUtil.saveText(settingsFile, tmpFile.text)
        }
    } finally {
        module.gradleModuleBuilder = null
        module.settingsScriptBuilder = null
    }
}

class KotlinGradleFrameworkSupportInModuleConfigurable(
    private val model: FrameworkSupportModel,
    private val supportProvider: GradleFrameworkSupportProvider
) : FrameworkSupportInModuleConfigurable() {
    override fun createComponent() = supportProvider.createComponent()

    override fun addSupport(
        module: Module,
        rootModel: ModifiableRootModel,
        modifiableModelsProvider: ModifiableModelsProvider
    ) {
        val buildScriptData = AbstractGradleModuleBuilder.getBuildScriptData(module)
        if (buildScriptData != null) {
            val builder = model.moduleBuilder
            val projectId = (builder as? AbstractGradleModuleBuilder)?.projectId ?: ProjectId(null, module.name, null)
            try {
                module.gradleModuleBuilder = builder as? AbstractExternalModuleBuilder<*>
                supportProvider.addSupport(projectId, module, rootModel, modifiableModelsProvider, buildScriptData)
            } finally {
                flushSettingsGradleCopy(module)
            }
        }
    }
}