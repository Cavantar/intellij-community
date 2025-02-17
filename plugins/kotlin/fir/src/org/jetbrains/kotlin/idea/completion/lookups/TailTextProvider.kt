// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.completion.lookups


import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KtFunctionalType
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.idea.completion.KotlinIdeaCompletionBundle
import org.jetbrains.kotlin.idea.completion.lookups.CompletionShortNamesRenderer.renderFunctionParameters
import org.jetbrains.kotlin.name.FqName

internal object TailTextProvider {
    fun KtAnalysisSession.getTailText(symbol: KtCallableSymbol, substitutor: KtSubstitutor): String = buildString {
        if (symbol is KtFunctionSymbol) {
            if (insertLambdaBraces(symbol)) {
                append(" {...}")
            } else {
                append(renderFunctionParameters(symbol, substitutor))
            }
        }

        symbol.callableIdIfNonLocal
            ?.takeIf { it.className == null }
            ?.let { callableId ->
                append(" (")
                append(callableId.packageName.asStringForTailText())
                append(")")
            }

        symbol.receiverType?.let { receiverType ->
            val renderedType = receiverType.render(CompletionShortNamesRenderer.TYPE_RENDERING_OPTIONS)
            append(KotlinIdeaCompletionBundle.message("presentation.tail.for.0", renderedType))
        }
    }

    fun KtAnalysisSession.getTailText(symbol: KtClassLikeSymbol): String = buildString {
        symbol.classIdIfNonLocal?.let { classId ->
            append(" (")
            append(classId.asSingleFqName().parent().asStringForTailText())
            append(")")
        }
    }

    private fun FqName.asStringForTailText(): String =
        if (isRoot) "<root>" else asString()

    fun KtAnalysisSession.insertLambdaBraces(symbol: KtFunctionSymbol): Boolean {
        val singleParam = symbol.valueParameters.singleOrNull()
        return singleParam != null && !singleParam.hasDefaultValue && singleParam.returnType is KtFunctionalType
    }

    fun KtAnalysisSession.insertLambdaBraces(symbol: KtFunctionalType): Boolean {
        val singleParam = symbol.parameterTypes.singleOrNull()
        return singleParam != null && singleParam is KtFunctionalType
    }
}