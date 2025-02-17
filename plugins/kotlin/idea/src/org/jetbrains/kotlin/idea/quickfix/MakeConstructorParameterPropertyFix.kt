// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.codeinsight.api.classic.quickfixes.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinValVar
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.*

class MakeConstructorParameterPropertyFix(
    element: KtParameter, private val kotlinValVar: KotlinValVar, className: String?
) : KotlinQuickFixAction<KtParameter>(element) {
    override fun getFamilyName() = KotlinBundle.message("make.constructor.parameter.a.property.0", "")

    private val suffix = if (className != null) KotlinBundle.message("in.class.0", className) else ""

    override fun getText() = KotlinBundle.message("make.constructor.parameter.a.property.0", suffix)

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val element = element ?: return false
        return !element.hasValOrVar()
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        element.addBefore(kotlinValVar.createKeyword(KtPsiFactory(project))!!, element.nameIdentifier)
        element.addModifier(KtTokens.PRIVATE_KEYWORD)
        element.visibilityModifier()?.let { private ->
            editor?.apply {
                selectionModel.setSelection(private.startOffset, private.endOffset)
                caretModel.moveToOffset(private.endOffset)
            }
        }
    }

    companion object Factory : KotlinIntentionActionsFactory() {

        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val ktReference = Errors.UNRESOLVED_REFERENCE.cast(diagnostic).a as? KtNameReferenceExpression ?: return emptyList()

            val valOrVar = if (ktReference.getAssignmentByLHS() != null) KotlinValVar.Var else KotlinValVar.Val

            val ktParameter = ktReference.getPrimaryConstructorParameterWithSameName() ?: return emptyList()
            val containingClass = ktParameter.containingClass()!!
            val className = if (containingClass != ktReference.containingClass()) containingClass.nameAsSafeName.asString() else null

            return listOf(MakeConstructorParameterPropertyFix(ktParameter, valOrVar, className))
        }
    }
}

fun KtNameReferenceExpression.getPrimaryConstructorParameterWithSameName(): KtParameter? {
    return nonStaticOuterClasses()
        .mapNotNull { it.primaryConstructor?.valueParameters?.firstOrNull { it.name == getReferencedName() } }
        .firstOrNull()
}