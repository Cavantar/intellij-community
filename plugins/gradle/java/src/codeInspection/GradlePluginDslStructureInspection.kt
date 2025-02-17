// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.gradle.codeInspection

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.util.castSafelyTo
import org.jetbrains.plugins.groovy.codeInspection.GroovyLocalInspectionTool
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase
import org.jetbrains.plugins.groovy.lang.psi.api.GrBlockLambdaBody
import org.jetbrains.plugins.groovy.lang.psi.api.GrExpressionLambdaBody
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression
import org.jetbrains.plugins.groovy.lang.psi.api.GrLambdaExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression

class GradlePluginDslStructureInspection : GroovyLocalInspectionTool() {

  override fun buildGroovyVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): GroovyElementVisitor = object: GroovyElementVisitor() {
    override fun visitFile(file: GroovyFileBase) {
      val statements = file.statements
      val lastPluginsStatement = statements.indexOfFirst { it is GrMethodCall && it.invokedExpression.text == "plugins" }
      if (lastPluginsStatement == -1) {
        return
      }
      val pluginsStatement = statements[lastPluginsStatement] as GrMethodCall
      checkPluginsStatement(holder, pluginsStatement)
      val statementsToCheck = statements.asList().subList(0, lastPluginsStatement)
      for (suspiciousStatement in statementsToCheck) {
        val psiToHighlight = getBadStatementHighlightingElement(suspiciousStatement) ?: continue
        holder.registerProblem(psiToHighlight, GradleInspectionBundle.message("inspection.message.incorrect.buildscript.structure"), ProblemHighlightType.GENERIC_ERROR)
      }
    }
  }

  private fun checkPluginsStatement(holder: ProblemsHolder, pluginsStatement: GrMethodCall) {
    val closure = pluginsStatement.closureArguments.singleOrNull() ?: pluginsStatement.expressionArguments.firstOrNull()?.castSafelyTo<GrFunctionalExpression>() ?: return
    val statements = getStatements(closure)
    for (statement in statements) {
      if (statement !is GrMethodCall) {
        holder.registerProblem(statement, GradleInspectionBundle.message("inspection.message.only.method.calls.to.id.alias.are.available.in.plugins.block"))
        continue
      }
      val (refElement) = decomposeCall(statement) ?: continue
      when (refElement?.text) {
        "apply" -> checkApply(holder, statement)
        "version" -> checkVersion(holder, statement)
        else -> if (checkIdAlias(holder, statement)) {
          holder.registerProblem(statement, GradleInspectionBundle.message("inspection.message.only.method.calls.to.id.alias.are.available.in.plugins.block"))
        }
      }
    }
  }

  private fun decomposeCall(call: GrMethodCall) : Triple<PsiElement?, GrExpression?, String?>? {
    val caller = call.invokedExpression.castSafelyTo<GrReferenceExpression>() ?: return null
    val qualifierCallName = caller.qualifierExpression.castSafelyTo<GrMethodCall>()?.invokedExpression.castSafelyTo<GrReferenceExpression>()?.referenceName
    return Triple(caller.referenceNameElement, caller.qualifierExpression, qualifierCallName)
  }

  private fun checkApply(holder: ProblemsHolder, method: GrExpression?) {
    if (method !is GrMethodCall) {
      return
    }
    val (refElement, qualifier, qualifierCallName) = decomposeCall(method) ?: return
    if (qualifierCallName == "version") {
      checkVersion(holder, qualifier)
    } else if (refElement != null && checkIdAlias(holder, qualifier)) {
      holder.registerProblem(refElement, GradleInspectionBundle.message("inspection.message.only.method.calls.to.id.alias.are.available.in.plugins.block"))
    }
  }

  private fun checkVersion(holder: ProblemsHolder, method: GrExpression?) {
    if (method !is GrMethodCall) {
      return
    }
    val (refElement, qualifier) = decomposeCall(method) ?: return
    if (refElement != null && checkIdAlias(holder, qualifier)) {
      holder.registerProblem(refElement, GradleInspectionBundle.message("inspection.message.only.method.calls.to.id.alias.are.available.in.plugins.block"))
    }
  }

  private fun checkIdAlias(holder: ProblemsHolder, method: GrExpression?) : Boolean {
    if (method !is GrMethodCall) {
      return true
    }
    val (refElement, _) = decomposeCall(method) ?: return true
    val methodName = refElement?.text
    if (methodName != "id" && methodName != "alias") {
      holder.registerProblem(refElement ?: method, GradleInspectionBundle.message("inspection.message.only.method.calls.to.id.alias.are.available.in.plugins.block"))
    }
    return false
  }

  private fun getStatements(funExpr: GrFunctionalExpression) : List<GrStatement> {
    return when (funExpr) {
      is GrClosableBlock -> return funExpr.statements.asList()
      is GrLambdaExpression -> when (val body = funExpr.body) {
        is GrBlockLambdaBody -> body.statements.asList()
        is GrExpressionLambdaBody -> listOf(body.expression)
        else -> emptyList()
      }
      else -> emptyList()
    }
  }

  private val allowedStrings = setOf("buildscript", "pluginManagement", "plugins")

  private fun getBadStatementHighlightingElement(suspiciousStatement: GrStatement): PsiElement? {
    if (suspiciousStatement !is GrMethodCall) {
      return suspiciousStatement
    }
    if (suspiciousStatement.invokedExpression.text !in allowedStrings) {
      return suspiciousStatement.invokedExpression
    }
    return null
  }


}