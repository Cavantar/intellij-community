package com.intellij.workspaceModel.test.api

import com.intellij.workspaceModel.storage.EntitySource
import com.intellij.workspaceModel.storage.GeneratedCodeApiVersion
import com.intellij.workspaceModel.storage.ModifiableWorkspaceEntity
import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.WorkspaceEntity
import org.jetbrains.deft.ObjBuilder
import org.jetbrains.deft.Type

interface CollectionFieldEntity : WorkspaceEntity {
  val versions: Set<Int>
  val names: List<String>

  //region generated code
  @GeneratedCodeApiVersion(1)
  interface Builder : CollectionFieldEntity, ModifiableWorkspaceEntity<CollectionFieldEntity>, ObjBuilder<CollectionFieldEntity> {
    override var versions: Set<Int>
    override var entitySource: EntitySource
    override var names: List<String>
  }

  companion object : Type<CollectionFieldEntity, Builder>() {
    operator fun invoke(versions: Set<Int>,
                        names: List<String>,
                        entitySource: EntitySource,
                        init: (Builder.() -> Unit)? = null): CollectionFieldEntity {
      val builder = builder()
      builder.versions = versions
      builder.entitySource = entitySource
      builder.names = names
      init?.invoke(builder)
      return builder
    }
  }
  //endregion
}

//region generated code
fun MutableEntityStorage.modifyEntity(entity: CollectionFieldEntity, modification: CollectionFieldEntity.Builder.() -> Unit) = modifyEntity(
  CollectionFieldEntity.Builder::class.java, entity, modification)
//endregion
