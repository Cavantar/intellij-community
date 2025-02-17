// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.highlighter.markers

import com.intellij.codeInsight.daemon.GutterIconDescriptor
import com.intellij.icons.AllIcons
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinIcons

internal object KotlinLineMarkerOptions {
    val overriddenOption = GutterIconDescriptor.Option(
        "kotlin.overridden",
        KotlinBundle.message("highlighter.name.overridden.declaration"), AllIcons.Gutter.OverridenMethod
    )

    val implementedOption = GutterIconDescriptor.Option(
        "kotlin.implemented",
        KotlinBundle.message("highlighter.name.implemented.declaration"), AllIcons.Gutter.ImplementedMethod
    )

    val overridingOption = GutterIconDescriptor.Option(
        "kotlin.overriding",
        KotlinBundle.message("highlighter.name.overriding.declaration"), AllIcons.Gutter.OverridingMethod
    )

    val implementingOption =
        GutterIconDescriptor.Option(
            "kotlin.implementing",
            KotlinBundle.message("highlighter.name.implementing.declaration"),
            AllIcons.Gutter.ImplementingMethod
        )

    val actualOption = GutterIconDescriptor.Option(
        "kotlin.actual",
        KotlinBundle.message("highlighter.name.multiplatform.actual.declaration"), KotlinIcons.ACTUAL
    )

    val expectOption = GutterIconDescriptor.Option(
        "kotlin.expect",
        KotlinBundle.message("highlighter.name.multiplatform.expect.declaration"), KotlinIcons.EXPECT
    )

    val dslOption =
        GutterIconDescriptor.Option("kotlin.dsl", KotlinBundle.message("highlighter.name.dsl.markers"), KotlinIcons.DSL_MARKER_ANNOTATION)

    val options = arrayOf(
        overriddenOption, implementedOption,
        overridingOption, implementingOption,
        actualOption, expectOption,
        dslOption
    )
}