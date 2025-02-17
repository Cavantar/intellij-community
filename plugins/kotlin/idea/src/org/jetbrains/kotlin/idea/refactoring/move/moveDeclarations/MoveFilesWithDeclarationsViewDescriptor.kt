// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.usageView.UsageViewUtil
import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle

internal class MoveFilesWithDeclarationsViewDescriptor(
    private val myElementsToMove: Array<PsiElement>,
    newParent: PsiDirectory
) : UsageViewDescriptor {
    @Nls
    private val myProcessedElementsHeader: String
    @Nls
    private val myCodeReferencesText: String

    init {
        if (myElementsToMove.size == 1) {
            myProcessedElementsHeader = RefactoringBundle.message(
                "move.single.element.elements.header",
                UsageViewUtil.getType(myElementsToMove[0]),
                newParent.virtualFile.presentableUrl
            ).capitalize()
            myCodeReferencesText = KotlinBundle.message(
                "text.references.in.code.to.0.1.and.its.declarations",
                UsageViewUtil.getType(myElementsToMove[0]),
                UsageViewUtil.getLongName(myElementsToMove[0])
            )
        } else {
            myProcessedElementsHeader =
                StringUtil.capitalize(RefactoringBundle.message("move.files.elements.header", newParent.virtualFile.presentableUrl))
            myCodeReferencesText = RefactoringBundle.message("references.found.in.code")
        }
    }

    override fun getElements() = myElementsToMove

    override fun getProcessedElementsHeader() = myProcessedElementsHeader

    override fun getCodeReferencesText(usagesCount: Int, filesCount: Int): String {
        return myCodeReferencesText + UsageViewBundle.getReferencesString(usagesCount, filesCount)
    }

    override fun getCommentReferencesText(usagesCount: Int, filesCount: Int): String =
        RefactoringBundle.message("comments.elements.header", UsageViewBundle.getOccurencesString(usagesCount, filesCount))
}