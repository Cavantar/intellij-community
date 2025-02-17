// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.workspaceModel.storage.entities.test.api

import org.jetbrains.deft.ObjBuilder
import org.jetbrains.deft.Type
import org.jetbrains.deft.annotations.Child
import com.intellij.workspaceModel.storage.EntitySource
import com.intellij.workspaceModel.storage.GeneratedCodeApiVersion
import com.intellij.workspaceModel.storage.ModifiableWorkspaceEntity
import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.WorkspaceEntity



interface SelfLinkedEntity : WorkspaceEntity {
  val parentEntity: SelfLinkedEntity?

  //region generated code
  @GeneratedCodeApiVersion(1)
  interface Builder : SelfLinkedEntity, ModifiableWorkspaceEntity<SelfLinkedEntity>, ObjBuilder<SelfLinkedEntity> {
    override var parentEntity: SelfLinkedEntity?
    override var entitySource: EntitySource
  }

  companion object : Type<SelfLinkedEntity, Builder>() {
    operator fun invoke(entitySource: EntitySource, init: (Builder.() -> Unit)? = null): SelfLinkedEntity {
      val builder = builder()
      builder.entitySource = entitySource
      init?.invoke(builder)
      return builder
    }
  }
  //endregion

}

//region generated code
fun MutableEntityStorage.modifyEntity(entity: SelfLinkedEntity, modification: SelfLinkedEntity.Builder.() -> Unit) = modifyEntity(
  SelfLinkedEntity.Builder::class.java, entity, modification)

var SelfLinkedEntity.Builder.children: @Child List<SelfLinkedEntity>
  by WorkspaceEntity.extension()
//endregion

val SelfLinkedEntity.children: List<@Child SelfLinkedEntity>
    by WorkspaceEntity.extension()
