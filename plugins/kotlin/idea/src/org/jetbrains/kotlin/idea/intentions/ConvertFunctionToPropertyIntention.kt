// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.base.psi.replaced
import org.jetbrains.kotlin.idea.codeinsight.api.classic.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.idea.refactoring.*
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiFactory.CallableBuilder.Target.READ_ONLY_PROPERTY
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import org.jetbrains.kotlin.types.expressions.OperatorConventions

class ConvertFunctionToPropertyIntention :
  SelfTargetingIntention<KtNamedFunction>(KtNamedFunction::class.java, KotlinBundle.lazyMessage("convert.function.to.property")),
  LowPriorityAction {
    private inner class Converter(
        project: Project,
        private val file: KtFile,
        editor: Editor?,
        descriptor: FunctionDescriptor
    ) : CallableRefactoring<FunctionDescriptor>(project, editor, descriptor, text) {
        private val elementsToShorten = ArrayList<KtElement>()

        private val newName: String by lazy {
            val name = callableDescriptor.name
            (propertyNameByGetMethodName(name) ?: name).asString()
        }

        private fun convertFunction(originalFunction: KtNamedFunction, psiFactory: KtPsiFactory, moveCaret: Boolean) {
            val propertyString = KtPsiFactory.CallableBuilder(READ_ONLY_PROPERTY).apply {
                // make sure to capture all comments and line breaks
                modifier(originalFunction.text.substring(0, originalFunction.funKeyword!!.getStartOffsetIn(originalFunction)))
                typeParams(originalFunction.typeParameters.map { it.text })
                originalFunction.receiverTypeReference?.let { receiver(it.text) }
                name(newName)
                originalFunction.getReturnTypeReference()?.let { returnType(it.text) }
                typeConstraints(originalFunction.typeConstraints.map { it.text })

                if (originalFunction.equalsToken != null) {
                    getterExpression(originalFunction.bodyExpression!!.text, breakLine = originalFunction.typeReference != null)
                } else {
                    originalFunction.bodyBlockExpression?.let { body ->
                        transform {
                            append("\nget() ")
                            append(body.text)
                        }
                    }
                }
            }.asString()

            val replaced = originalFunction.replaced(psiFactory.createDeclaration<KtProperty>(propertyString))
            if (editor != null && moveCaret) editor.caretModel.moveToOffset(replaced.nameIdentifier!!.endOffset)
        }

        override fun performRefactoring(descriptorsForChange: Collection<CallableDescriptor>) {
            val conflicts = MultiMap<PsiElement, String>()
            val getterName = JvmAbi.getterName(callableDescriptor.name.asString())
            val callables = getAffectedCallables(project, descriptorsForChange)
            val mainElement = findMainElement(callables)
            val kotlinCalls = ArrayList<KtCallElement>()
            val kotlinRefsToRename = ArrayList<PsiReference>()
            val foreignRefs = ArrayList<PsiReference>()
            for (callable in callables) {
                if (callable !is PsiNamedElement) continue

                if (!checkModifiable(callable)) {
                    reportDeclarationConflict(conflicts, callable) { KotlinBundle.message("can.t.modify.0", it) }
                }

                if (callable is KtNamedFunction) {
                    callableDescriptor.getContainingScope()
                        ?.findVariable(callableDescriptor.name, NoLookupLocation.FROM_IDE)
                        ?.takeIf { it.receiverType() == callableDescriptor.receiverType() }
                        ?.let { DescriptorToSourceUtilsIde.getAnyDeclaration(project, it) }
                        ?.let { reportDeclarationConflict(conflicts, it) { s -> KotlinBundle.message("0.already.exists", s) } }
                }

                if (callable is PsiMethod) callable.checkDeclarationConflict(getterName, conflicts, callables)

                val usages = ReferencesSearch.search(callable)
                for (usage in usages) {
                    if (usage is KtSimpleNameReference) {
                        val expression = usage.expression
                        val callElement = expression.getParentOfTypeAndBranch<KtCallElement> { calleeExpression }
                        if (callElement != null && expression.getStrictParentOfType<KtCallableReferenceExpression>() == null) {
                            if (callElement.typeArguments.isNotEmpty()) {
                                conflicts.putValue(
                                    callElement,
                                    KotlinBundle.message(
                                        "type.arguments.will.be.lost.after.conversion.0",
                                        StringUtil.htmlEmphasize(callElement.text)
                                    )
                                )
                            }

                            if (callElement.valueArguments.isNotEmpty()) {
                                conflicts.putValue(
                                    callElement,
                                    KotlinBundle.message(
                                        "call.with.arguments.will.be.skipped.0",
                                        StringUtil.htmlEmphasize(callElement.text)
                                    )
                                )
                                continue
                            }

                            kotlinCalls.add(callElement)
                        } else {
                            kotlinRefsToRename.add(usage)
                        }
                    } else {
                        foreignRefs.add(usage)
                    }
                }
            }

            project.checkConflictsInteractively(conflicts) {
                project.executeWriteCommand(text) {
                    val psiFactory = KtPsiFactory(project)
                    val newGetterName = JvmAbi.getterName(newName)
                    val newRefExpr = psiFactory.createExpression(newName)

                    kotlinCalls.forEach { it.replace(newRefExpr) }
                    kotlinRefsToRename.forEach { it.handleElementRename(newName) }
                    foreignRefs.forEach { it.handleElementRename(newGetterName) }
                    callables.forEach {
                        when (it) {
                            is KtNamedFunction -> convertFunction(it, psiFactory, moveCaret = it == mainElement)
                            is PsiMethod -> it.name = newGetterName
                        }
                    }

                    ShortenReferences.DEFAULT.process(elementsToShorten)
                }
            }
        }

        private fun findMainElement(callables: Collection<PsiElement>): PsiElement? {
            if (editor == null) return null
            val offset = editor.caretModel.offset
            return callables.find { file == it.containingFile && offset in it.textRange }
        }
    }

    override fun startInWriteAction(): Boolean = false

    override fun isApplicableTo(element: KtNamedFunction, caretOffset: Int): Boolean {
        val funKeyword = element.funKeyword ?: return false
        val identifier = element.nameIdentifier ?: return false
        if (!TextRange(funKeyword.startOffset, identifier.endOffset).containsOffset(caretOffset)) return false

        if (element.valueParameters.isNotEmpty() || element.isLocal) return false

        val name = element.name!!
        if (name == "invoke" || name == "iterator" || Name.identifier(name) in OperatorConventions.UNARY_OPERATION_NAMES.inverse().keys) {
            return false
        }

        val descriptor = element.resolveToDescriptorIfAny() ?: return false
        val returnType = descriptor.returnType ?: return false
        return !KotlinBuiltIns.isUnit(returnType) && !KotlinBuiltIns.isNothing(returnType)
    }

    override fun applyTo(element: KtNamedFunction, editor: Editor?) {
        val descriptor = element.unsafeResolveToDescriptor(BodyResolveMode.PARTIAL) as FunctionDescriptor
        Converter(element.project, element.containingKtFile, editor, descriptor).run()
    }
}
