// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.codeinsight.api.classic.quickfixes.KotlinQuickFixAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList

class RemoveArgumentFix(argument: KtValueArgument) : KotlinQuickFixAction<KtValueArgument>(argument) {
    override fun getText(): String = KotlinBundle.message("fix.remove.argument.text")

    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val argument = element ?: return
        if (argument is KtLambdaArgument) {
            argument.delete()
        } else {
            (argument.parent as? KtValueArgumentList)?.removeArgument(argument)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtValueArgument>? {
            val argument = diagnostic.psiElement.parent as? KtValueArgument ?: return null
            return RemoveArgumentFix(argument)
        }
    }
}
