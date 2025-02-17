// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.structuralsearch.search

import org.jetbrains.kotlin.idea.structuralsearch.KotlinStructuralSearchTest

class KotlinSSTypeAliasTest : KotlinStructuralSearchTest() {
    override fun getBasePath(): String = "typeAlias"

    fun testTypeAlias() { doTest("typealias '_ = Int") }

    fun testAnnotated() { doTest("@Ann typealias '_ = '_") }
}