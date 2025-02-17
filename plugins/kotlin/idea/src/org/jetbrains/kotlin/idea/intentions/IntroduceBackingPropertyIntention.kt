// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.codeinsight.api.classic.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.idea.util.hasJvmFieldAnnotation
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext

class IntroduceBackingPropertyIntention : SelfTargetingIntention<KtProperty>(
    KtProperty::class.java,
    KotlinBundle.lazyMessage("introduce.backing.property")
) {
    override fun isApplicableTo(element: KtProperty, caretOffset: Int): Boolean {
        if (!canIntroduceBackingProperty(element)) return false
        return element.nameIdentifier?.textRange?.containsOffset(caretOffset) == true
    }

    override fun applyTo(element: KtProperty, editor: Editor?) = introduceBackingProperty(element)

    companion object {
        fun canIntroduceBackingProperty(property: KtProperty): Boolean {
            val name = property.name ?: return false
            if (property.hasModifier(KtTokens.CONST_KEYWORD)) return false
            if (property.hasJvmFieldAnnotation()) return false

            val bindingContext = property.getResolutionFacade().analyzeWithAllCompilerChecks(property).bindingContext
            val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property) as? PropertyDescriptor ?: return false
            if (bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor) != true) return false

            val containingClass = property.getStrictParentOfType<KtClassOrObject>() ?: return false
            if (containingClass.isExpectDeclaration()) return false
            return containingClass.declarations.none { it is KtProperty && it.name == "_$name" }
        }

        fun introduceBackingProperty(property: KtProperty) {
            createBackingProperty(property)

            property.removeModifier(KtTokens.LATEINIT_KEYWORD)

            if (property.typeReference == null) {
                val type = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(property)
                SpecifyTypeExplicitlyIntention.addTypeAnnotation(null, property, type)
            }

            val getter = property.getter
            if (getter == null) {
                createGetter(property)
            } else {
                replaceFieldReferences(getter, property.name!!)
            }

            if (property.isVar) {
                val setter = property.setter
                if (setter == null) {
                    createSetter(property)
                } else {
                    replaceFieldReferences(setter, property.name!!)
                }
            }

            property.initializer = null
        }

        private fun createGetter(element: KtProperty) {
            val body = "get() = ${backingName(element)}"
            val newGetter = KtPsiFactory(element).createProperty("val x $body").getter!!
            element.addAccessor(newGetter)
        }

        private fun createSetter(element: KtProperty) {
            val body = "set(value) { ${backingName(element)} = value }"
            val newSetter = KtPsiFactory(element).createProperty("val x $body").setter!!
            element.addAccessor(newSetter)
        }

        private fun KtProperty.addAccessor(newAccessor: KtPropertyAccessor) {
            val semicolon = node.findChildByType(KtTokens.SEMICOLON)
            addBefore(newAccessor, semicolon?.psi)
        }

        private fun createBackingProperty(property: KtProperty) {
            val backingProperty = KtPsiFactory(property).buildDeclaration {
                appendFixedText("private ")
                appendFixedText(property.valOrVarKeyword.text)
                appendFixedText(" ")
                appendNonFormattedText(backingName(property))
                if (property.typeReference != null) {
                    appendFixedText(": ")
                    appendTypeReference(property.typeReference)
                }
                if (property.initializer != null) {
                    appendFixedText(" = ")
                    appendExpression(property.initializer)
                }
            }

            if (property.hasModifier(KtTokens.LATEINIT_KEYWORD)) {
                backingProperty.addModifier(KtTokens.LATEINIT_KEYWORD)
            }

            property.parent.addBefore(backingProperty, property)
        }

        private fun backingName(property: KtProperty): String {
            return if (property.nameIdentifier?.text?.startsWith('`') == true) "`_${property.name}`" else "_${property.name}"
        }

        private fun replaceFieldReferences(element: KtElement, propertyName: String) {
            element.acceptChildren(object : KtTreeVisitorVoid() {
                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    val target = expression.resolveToCall()?.resultingDescriptor
                    if (target is SyntheticFieldDescriptor) {
                        expression.replace(KtPsiFactory(element).createSimpleName("_$propertyName"))
                    }
                }

                override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
                    // don't go into accessors of properties in local classes because 'field' will mean something different in them
                }
            })
        }
    }
}
