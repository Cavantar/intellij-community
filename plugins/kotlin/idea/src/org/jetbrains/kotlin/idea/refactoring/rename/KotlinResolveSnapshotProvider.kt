// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.refactoring.rename.ResolveSnapshotProvider
import com.intellij.util.containers.HashMap
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.base.psi.replaced
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.refactoring.explicateAsTextForReceiver
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector

class KotlinResolveSnapshotProvider : ResolveSnapshotProvider() {
    override fun createSnapshot(scope: PsiElement) = object : ResolveSnapshot() {
        private val project = scope.project
        private val document = PsiDocumentManager.getInstance(project).getDocument(scope.containingFile)!!
        private val refExpressionToDescriptor = HashMap<SmartPsiElementPointer<*>, PropertyDescriptor>()

        init {
            scope.accept(
                object : KtTreeVisitorVoid() {
                    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                        if (expression.getQualifiedExpressionForSelector() != null) return super.visitSimpleNameExpression(expression)
                        val targetDescriptor = expression.resolveMainReferenceToDescriptors().singleOrNull() ?: return
                        if (targetDescriptor !is PropertyDescriptor) return
                        refExpressionToDescriptor[expression.createSmartPointer()] = targetDescriptor
                    }
                }
            )
        }

        override fun apply(name: String) {
            PsiDocumentManager.getInstance(project).commitDocument(document)

            val elementsToShorten = ArrayList<KtElement>()
            for ((refExprPointer, targetDescriptor) in refExpressionToDescriptor) {
                val refExpr = refExprPointer.element ?: continue
                if (refExpr.text != name) continue
                val containingDescriptor = targetDescriptor.containingDeclaration
                val qualifiedRefText = if (containingDescriptor is ClassDescriptor) {
                    "${containingDescriptor.explicateAsTextForReceiver()}.${targetDescriptor.name.asString()}"
                } else {
                    targetDescriptor.importableFqName?.asString() ?: continue
                }
                val qualifiedRefExpr = KtPsiFactory(project).createExpression(qualifiedRefText)
                elementsToShorten += refExpr.replaced(qualifiedRefExpr)
            }
            ShortenReferences { ShortenReferences.Options.ALL_ENABLED }.process(elementsToShorten)
        }
    }
}