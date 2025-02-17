// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.quickfix.ChangeVariableMutabilityFix
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.delegatedSuperTypeEntry

import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection

class DelegationToVarPropertyInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        delegatedSuperTypeEntry(fun(delegatedSuperTypeEntry) {
            val parameter = delegatedSuperTypeEntry.delegateExpression?.mainReference?.resolve() as? KtParameter ?: return
            if (parameter.valOrVarKeyword?.node?.elementType != KtTokens.VAR_KEYWORD) return
            holder.registerProblem(
                parameter,
                KotlinBundle.message("delegating.to.var.property.does.not.take.its.changes.into.account"),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                IntentionWrapper(ChangeVariableMutabilityFix(parameter, false)),
                RemoveVarKeyword()
            )
        })
}

private class RemoveVarKeyword : LocalQuickFix {
    override fun getName() = KotlinBundle.message("remove.var.keyword.text")

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        (descriptor.psiElement as? KtParameter)?.valOrVarKeyword?.delete()

    }
}