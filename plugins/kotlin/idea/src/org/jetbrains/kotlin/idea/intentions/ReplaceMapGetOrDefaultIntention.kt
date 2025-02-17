// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.base.psi.replaced
import org.jetbrains.kotlin.idea.codeinsight.api.classic.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.isNullable

class ReplaceMapGetOrDefaultIntention : SelfTargetingRangeIntention<KtDotQualifiedExpression>(
    KtDotQualifiedExpression::class.java, KotlinBundle.lazyMessage("replace.with.indexing.and.elvis.operator")
) {
    companion object {
        private val getOrDefaultFqName = FqName("kotlin.collections.Map.getOrDefault")
    }

    override fun applicabilityRange(element: KtDotQualifiedExpression): TextRange? {
        val callExpression = element.callExpression ?: return null
        val calleeExpression = callExpression.calleeExpression ?: return null
        if (calleeExpression.text != getOrDefaultFqName.shortName().asString()) return null
        val (firstArg, secondArg) = callExpression.arguments() ?: return null
        val context = element.analyze(BodyResolveMode.PARTIAL)
        if (callExpression.getResolvedCall(context)?.isCalling(getOrDefaultFqName) != true) return null
        if (element.receiverExpression.getType(context)?.arguments?.lastOrNull()?.type?.isNullable() == true) return null
        setTextGetter(KotlinBundle.lazyMessage("replace.with.0.1.2", element.receiverExpression.text, firstArg.text, secondArg.text))
        return calleeExpression.textRange
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        val callExpression = element.callExpression ?: return
        val (firstArg, secondArg) = callExpression.arguments() ?: return
        val replaced = element.replaced(
            KtPsiFactory(element).createExpressionByPattern("$0[$1] ?: $2", element.receiverExpression, firstArg, secondArg)
        )

        replaced.findDescendantOfType<KtArrayAccessExpression>()?.leftBracket?.startOffset?.let {
            editor?.caretModel?.moveToOffset(it)
        }
    }

    private fun KtCallExpression.arguments(): Pair<KtExpression, KtExpression>? {
        val args = valueArguments
        if (args.size != 2) return null
        val first = args[0].getArgumentExpression() ?: return null
        val second = args[1].getArgumentExpression() ?: return null
        return first to second
    }
}