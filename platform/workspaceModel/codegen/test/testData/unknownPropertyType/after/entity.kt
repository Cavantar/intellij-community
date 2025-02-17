package com.intellij.workspaceModel.test.api

import com.intellij.workspaceModel.storage.EntitySource
import com.intellij.workspaceModel.storage.GeneratedCodeApiVersion
import com.intellij.workspaceModel.storage.ModifiableWorkspaceEntity
import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.WorkspaceEntity
import java.util.Date
import org.jetbrains.deft.ObjBuilder
import org.jetbrains.deft.Type

interface UnknownPropertyTypeEntity : WorkspaceEntity {
  val date: Date

  //region generated code
  @GeneratedCodeApiVersion(1)
  interface Builder : UnknownPropertyTypeEntity, ModifiableWorkspaceEntity<UnknownPropertyTypeEntity>, ObjBuilder<UnknownPropertyTypeEntity> {
    override var date: Date
    override var entitySource: EntitySource
  }

  companion object : Type<UnknownPropertyTypeEntity, Builder>() {
    operator fun invoke(date: Date, entitySource: EntitySource, init: (Builder.() -> Unit)? = null): UnknownPropertyTypeEntity {
      val builder = builder()
      builder.date = date
      builder.entitySource = entitySource
      init?.invoke(builder)
      return builder
    }
  }
  //endregion
}

//region generated code
fun MutableEntityStorage.modifyEntity(entity: UnknownPropertyTypeEntity,
                                      modification: UnknownPropertyTypeEntity.Builder.() -> Unit) = modifyEntity(
  UnknownPropertyTypeEntity.Builder::class.java, entity, modification)
//endregion
