package com.intellij.workspaceModel.codegen

import com.intellij.workspaceModel.codegen.deft.meta.ObjClass
import com.intellij.workspaceModel.codegen.utils.QualifiedName
import com.intellij.workspaceModel.codegen.utils.fqn
import org.jetbrains.deft.Type

val Type<*, *>.javaFullName
  get() = fqn(packageName, name)

val ObjClass<*>.javaFullName: QualifiedName
  get() = fqn(module.name, name)

val ObjClass<*>.javaBuilderName
  get() = "$name.Builder"

val ObjClass<*>.javaImplName
  get() = "${name.replace(".", "")}Impl"

val ObjClass<*>.javaImplBuilderName
  get() = "${javaImplName}.Builder"
