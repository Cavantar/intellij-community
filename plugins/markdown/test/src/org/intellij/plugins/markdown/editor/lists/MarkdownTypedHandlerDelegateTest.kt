// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.plugins.markdown.editor.lists

import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.intellij.testFramework.RegistryKeyRule
import org.intellij.plugins.markdown.MarkdownTestingUtil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MarkdownTypedHandlerDelegateTest: LightPlatformCodeInsightTestCase() {
  @Rule
  @JvmField
  val rule = RegistryKeyRule("markdown.lists.renumber.on.type.enable", true)

  override fun getTestDataPath(): String = MarkdownTestingUtil.TEST_DATA_PATH + "/editor/lists/typing/"

  @Test
  fun testRenumberOnItemCreation() = doTest()

  @Test
  fun testRenumberOnItemCreationInBlockQuote() = doTest()

  private fun doTest() {
    val testName = getTestName(true)
    configureByFile("$testName.md")
    type(' ')
    checkResultByFile("$testName-after.md")
  }
}