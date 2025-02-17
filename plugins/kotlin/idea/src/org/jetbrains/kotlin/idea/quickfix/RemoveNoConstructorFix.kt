// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.base.psi.replaced
import org.jetbrains.kotlin.idea.codeinsight.api.classic.quickfixes.KotlinQuickFixAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class RemoveNoConstructorFix(constructor: KtValueArgumentList) : KotlinQuickFixAction<KtValueArgumentList>(constructor) {

    override fun getText() = KotlinBundle.message("remove.constructor.call")

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val superTypeCallEntry = element?.getStrictParentOfType<KtSuperTypeCallEntry>() ?: return
        val superTypeEntry = KtPsiFactory(project).createSuperTypeEntry(superTypeCallEntry.firstChild.text)
        superTypeCallEntry.replaced(superTypeEntry)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtValueArgumentList>? =
            (diagnostic.psiElement as? KtValueArgumentList)?.let { RemoveNoConstructorFix(it) }
    }

}
