// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.ChangeToLabeledReturnFix
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.returnExpressionVisitor

import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection

class UnlabeledReturnInsideLambdaInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        returnExpressionVisitor(fun(returnExpression: KtReturnExpression) {
            if (returnExpression.labelQualifier != null) return
            val lambda = returnExpression.getParentOfType<KtLambdaExpression>(true, KtNamedFunction::class.java) ?: return
            val parentFunction = lambda.getStrictParentOfType<KtNamedFunction>() ?: return
            if (returnExpression.analyze().diagnostics.forElement(returnExpression).any { it.severity == Severity.ERROR }) return
            holder.registerProblem(
                returnExpression.returnKeyword,
                KotlinBundle.message("unlabeled.return.inside.lambda"),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                IntentionWrapper(ChangeToLabeledReturnFix(returnExpression, labeledReturn = "return@${parentFunction.name}"))
            )
        })
}