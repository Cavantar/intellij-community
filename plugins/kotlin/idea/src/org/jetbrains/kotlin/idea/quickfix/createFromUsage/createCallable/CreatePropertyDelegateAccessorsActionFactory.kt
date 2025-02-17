// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.VariableAccessorDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.base.projectStructure.languageVersionSettings
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

object CreatePropertyDelegateAccessorsActionFactory : CreateCallableMemberFromUsageFactory<KtExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtExpression? {
        return diagnostic.psiElement as? KtExpression
    }

    override fun extractFixData(element: KtExpression, diagnostic: Diagnostic): List<CallableInfo> {
        val context = element.analyze()

        fun isApplicableForAccessor(accessor: VariableAccessorDescriptor?): Boolean =
            accessor != null && context[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, accessor] == null

        val property = element.getNonStrictParentOfType<KtProperty>() ?: return emptyList()
        val propertyDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, property] as? VariableDescriptorWithAccessors
            ?: return emptyList()

        if (propertyDescriptor is LocalVariableDescriptor
            && !element.languageVersionSettings.supportsFeature(LanguageFeature.LocalDelegatedProperties)
        ) {
            return emptyList()
        }

        val propertyReceiver = propertyDescriptor.extensionReceiverParameter ?: propertyDescriptor.dispatchReceiverParameter
        val propertyType = propertyDescriptor.type

        val accessorReceiverType = TypeInfo(element, Variance.IN_VARIANCE)
        val builtIns = propertyDescriptor.builtIns
        val thisRefParam = ParameterInfo(TypeInfo(propertyReceiver?.type ?: builtIns.nullableNothingType, Variance.IN_VARIANCE))
        val kPropertyStarType = ReflectionTypes.createKPropertyStarType(propertyDescriptor.module) ?: return emptyList()
        val metadataParam = ParameterInfo(TypeInfo(kPropertyStarType, Variance.IN_VARIANCE), "property")

        val callableInfos = SmartList<CallableInfo>()

        val psiFactory = KtPsiFactory(element)

        if (isApplicableForAccessor(propertyDescriptor.getter)) {
            val getterInfo = FunctionInfo(
                name = OperatorNameConventions.GET_VALUE.asString(),
                receiverTypeInfo = accessorReceiverType,
                returnTypeInfo = TypeInfo(propertyType, Variance.OUT_VARIANCE),
                parameterInfos = listOf(thisRefParam, metadataParam),
                modifierList = psiFactory.createModifierList(KtTokens.OPERATOR_KEYWORD)
            )
            callableInfos.add(getterInfo)
        }

        if (propertyDescriptor.isVar && isApplicableForAccessor(propertyDescriptor.setter)) {
            val newValueParam = ParameterInfo(TypeInfo(propertyType, Variance.IN_VARIANCE))
            val setterInfo = FunctionInfo(
                name = OperatorNameConventions.SET_VALUE.asString(),
                receiverTypeInfo = accessorReceiverType,
                returnTypeInfo = TypeInfo(builtIns.unitType, Variance.OUT_VARIANCE),
                parameterInfos = listOf(thisRefParam, metadataParam, newValueParam),
                modifierList = psiFactory.createModifierList(KtTokens.OPERATOR_KEYWORD)
            )
            callableInfos.add(setterInfo)
        }

        return callableInfos
    }
}
