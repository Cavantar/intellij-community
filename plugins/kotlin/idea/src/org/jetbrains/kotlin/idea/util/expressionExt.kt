// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.util

import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.idea.base.psi.textRangeIn as _textRangeIn

fun KtCallElement.replaceOrCreateTypeArgumentList(newTypeArgumentList: KtTypeArgumentList) {
    if (typeArgumentList != null) typeArgumentList?.replace(newTypeArgumentList)
    else addAfter(
        newTypeArgumentList,
        calleeExpression,
    )
}

fun KtModifierListOwner.hasInlineModifier() = hasModifier(KtTokens.INLINE_KEYWORD)

fun KtModifierListOwner.hasPrivateModifier() = hasModifier(KtTokens.PRIVATE_KEYWORD)

fun KtPrimaryConstructor.mustHaveValOrVar(): Boolean = containingClass()?.let {
    it.isAnnotation() || it.hasInlineModifier()
} ?: false

// TODO: add cases
fun KtExpression.hasNoSideEffects(): Boolean = when (this) {
    is KtStringTemplateExpression -> !hasInterpolation()
    is KtConstantExpression -> true
    else -> ConstantExpressionEvaluator.getConstant(this, analyze(BodyResolveMode.PARTIAL)) != null
}

@Deprecated("Please use org.jetbrains.kotlin.idea.base.psi.textRangeIn")
fun PsiElement.textRangeIn(other: PsiElement): TextRange = _textRangeIn(other)

fun KtDotQualifiedExpression.calleeTextRangeInThis(): TextRange? = callExpression?.calleeExpression?.textRangeIn(this)

fun KtNamedDeclaration.nameIdentifierTextRangeInThis(): TextRange? = nameIdentifier?.textRangeIn(this)

fun PsiElement.hasComments(): Boolean = anyDescendantOfType<PsiComment>()

fun KtDotQualifiedExpression.hasNotReceiver(): Boolean {
    val element = getQualifiedElementSelector()?.mainReference?.resolve() ?: return false
    return element is KtClassOrObject ||
            element is KtConstructor<*> ||
            element is KtCallableDeclaration && element.receiverTypeReference == null && (element.containingClassOrObject is KtObjectDeclaration?) ||
            element is PsiMember && element.hasModifier(JvmModifier.STATIC) ||
            element is PsiMethod && element.isConstructor
}

val KtExpression.isUnitLiteral: Boolean
    get() = StandardNames.FqNames.unit.shortName() == (this as? KtNameReferenceExpression)?.getReferencedNameAsName()

val PsiElement.isAnonymousFunction: Boolean get() = this is KtNamedFunction && isAnonymousFunction

val KtNamedFunction.isAnonymousFunction: Boolean get() = nameIdentifier == null

val DeclarationDescriptor.isPrimaryConstructorOfDataClass: Boolean
    get() = this is ConstructorDescriptor && this.isPrimary && this.constructedClass.isData
