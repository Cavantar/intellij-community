// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.codeinsight.api.classic.quickfixes.KotlinQuickFixAction
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class AddArrayOfTypeFix(expression: KtExpression, expectedType: KotlinType) : KotlinQuickFixAction<KtExpression>(expression) {

    private val prefix = if (KotlinBuiltIns.isArray(expectedType)) {
        "arrayOf"
    } else {
        val typeName = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(expectedType)
        "${typeName.decapitalize(Locale.US)}Of"

    }

    override fun getText() = KotlinBundle.message("fix.add.array.of.type.text", prefix)
    override fun getFamilyName() = KotlinBundle.message("fix.add.array.of.type.family")

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val arrayOfExpression = KtPsiFactory(project).createExpressionByPattern("$0($1)", prefix, element)
        element.replace(arrayOfExpression)
    }
}
