// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.codeinsight.utils


import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager

fun PsiFile.adjustLineIndent(startOffset: Int, endOffset: Int) {
    if (!commitAndUnblockDocument()) return
    CodeStyleManager.getInstance(project).adjustLineIndent(this, TextRange(startOffset, endOffset))
}