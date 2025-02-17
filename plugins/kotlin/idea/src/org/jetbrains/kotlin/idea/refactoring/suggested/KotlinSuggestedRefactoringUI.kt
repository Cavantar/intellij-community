// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.refactoring.suggested

import com.intellij.psi.PsiCodeFragment
import com.intellij.refactoring.suggested.SignaturePresentationBuilder
import com.intellij.refactoring.suggested.SuggestedChangeSignatureData
import com.intellij.refactoring.suggested.SuggestedRefactoringExecution.NewParameterValue
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Parameter
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Signature
import com.intellij.refactoring.suggested.SuggestedRefactoringUI
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.psi.KtExpressionCodeFragment
import org.jetbrains.kotlin.psi.KtPsiFactory

object KotlinSuggestedRefactoringUI : SuggestedRefactoringUI() {
    override fun createSignaturePresentationBuilder(
        signature: Signature,
        otherSignature: Signature,
        isOldSignature: Boolean
    ): SignaturePresentationBuilder {
        return KotlinSignaturePresentationBuilder(signature, otherSignature, isOldSignature)
    }

    override fun extractNewParameterData(data: SuggestedChangeSignatureData): List<NewParameterData> {
        val result = mutableListOf<NewParameterData>()

        val declaration = data.declaration
        val factory = KtPsiFactory(declaration.project)

        fun createCodeFragment() = factory.createExpressionCodeFragment("", declaration)

        if (data.newSignature.receiverType != null && data.oldSignature.receiverType == null) {
            result.add(NewParameterData(KotlinBundle.message("extract.new.parameter.name.receiver"), createCodeFragment(), false/*TODO*/))
        }

        fun isNewParameter(parameter: Parameter) = data.oldSignature.parameterById(parameter.id) == null

        val newParameters = data.newSignature.parameters
        val dropLastN = newParameters.reversed().count { isNewParameter(it) && it.defaultValue != null }
        for (parameter in newParameters.dropLast(dropLastN)) {
            if (isNewParameter(parameter)) {
                result.add(NewParameterData(parameter.name, createCodeFragment(), false/*TODO*/))
            }
        }

        return result
    }

    override fun extractValue(fragment: PsiCodeFragment): NewParameterValue.Expression? {
        return (fragment as KtExpressionCodeFragment).getContentElement()
            ?.let { NewParameterValue.Expression(it) }
    }
}
