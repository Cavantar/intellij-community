package com.intellij.workspaceModel.test.api

import com.intellij.workspaceModel.storage.EntityInformation
import com.intellij.workspaceModel.storage.EntitySource
import com.intellij.workspaceModel.storage.EntityStorage
import com.intellij.workspaceModel.storage.GeneratedCodeApiVersion
import com.intellij.workspaceModel.storage.GeneratedCodeImplVersion
import com.intellij.workspaceModel.storage.ModifiableWorkspaceEntity
import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.WorkspaceEntity
import com.intellij.workspaceModel.storage.impl.ConnectionId
import com.intellij.workspaceModel.storage.impl.ModifiableWorkspaceEntityBase
import com.intellij.workspaceModel.storage.impl.WorkspaceEntityBase
import com.intellij.workspaceModel.storage.impl.WorkspaceEntityData

@GeneratedCodeApiVersion(1)
@GeneratedCodeImplVersion(1)
open class CollectionFieldEntityImpl : CollectionFieldEntity, WorkspaceEntityBase() {

  companion object {


    val connections = listOf<ConnectionId>(
    )

  }

  @JvmField
  var _versions: Set<Int>? = null
  override val versions: Set<Int>
    get() = _versions!!

  @JvmField
  var _names: List<String>? = null
  override val names: List<String>
    get() = _names!!

  override fun connectionIdList(): List<ConnectionId> {
    return connections
  }

  class Builder(val result: CollectionFieldEntityData?) : ModifiableWorkspaceEntityBase<CollectionFieldEntity>(), CollectionFieldEntity.Builder {
    constructor() : this(CollectionFieldEntityData())

    override fun applyToBuilder(builder: MutableEntityStorage) {
      if (this.diff != null) {
        if (existsInBuilder(builder)) {
          this.diff = builder
          return
        }
        else {
          error("Entity CollectionFieldEntity is already created in a different builder")
        }
      }

      this.diff = builder
      this.snapshot = builder
      addToBuilder()
      this.id = getEntityData().createEntityId()

      // Process linked entities that are connected without a builder
      processLinkedEntities(builder)
      checkInitialization() // TODO uncomment and check failed tests
    }

    fun checkInitialization() {
      val _diff = diff
      if (!getEntityData().isVersionsInitialized()) {
        error("Field CollectionFieldEntity#versions should be initialized")
      }
      if (!getEntityData().isEntitySourceInitialized()) {
        error("Field CollectionFieldEntity#entitySource should be initialized")
      }
      if (!getEntityData().isNamesInitialized()) {
        error("Field CollectionFieldEntity#names should be initialized")
      }
    }

    override fun connectionIdList(): List<ConnectionId> {
      return connections
    }


    override var versions: Set<Int>
      get() = getEntityData().versions
      set(value) {
        checkModificationAllowed()
        getEntityData().versions = value

        changedProperty.add("versions")
      }

    override var entitySource: EntitySource
      get() = getEntityData().entitySource
      set(value) {
        checkModificationAllowed()
        getEntityData().entitySource = value
        changedProperty.add("entitySource")

      }

    override var names: List<String>
      get() = getEntityData().names
      set(value) {
        checkModificationAllowed()
        getEntityData().names = value

        changedProperty.add("names")
      }

    override fun getEntityData(): CollectionFieldEntityData = result ?: super.getEntityData() as CollectionFieldEntityData
    override fun getEntityClass(): Class<CollectionFieldEntity> = CollectionFieldEntity::class.java
  }
}

class CollectionFieldEntityData : WorkspaceEntityData<CollectionFieldEntity>() {
  lateinit var versions: Set<Int>
  lateinit var names: List<String>

  fun isVersionsInitialized(): Boolean = ::versions.isInitialized
  fun isNamesInitialized(): Boolean = ::names.isInitialized

  override fun wrapAsModifiable(diff: MutableEntityStorage): ModifiableWorkspaceEntity<CollectionFieldEntity> {
    val modifiable = CollectionFieldEntityImpl.Builder(null)
    modifiable.allowModifications {
      modifiable.diff = diff
      modifiable.snapshot = diff
      modifiable.id = createEntityId()
      modifiable.entitySource = this.entitySource
    }
    modifiable.changedProperty.clear()
    return modifiable
  }

  override fun createEntity(snapshot: EntityStorage): CollectionFieldEntity {
    val entity = CollectionFieldEntityImpl()
    entity._versions = versions
    entity._names = names
    entity.entitySource = entitySource
    entity.snapshot = snapshot
    entity.id = createEntityId()
    return entity
  }

  override fun getEntityInterface(): Class<out WorkspaceEntity> {
    return CollectionFieldEntity::class.java
  }

  override fun serialize(ser: EntityInformation.Serializer) {
  }

  override fun deserialize(de: EntityInformation.Deserializer) {
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (this::class != other::class) return false

    other as CollectionFieldEntityData

    if (this.versions != other.versions) return false
    if (this.entitySource != other.entitySource) return false
    if (this.names != other.names) return false
    return true
  }

  override fun equalsIgnoringEntitySource(other: Any?): Boolean {
    if (other == null) return false
    if (this::class != other::class) return false

    other as CollectionFieldEntityData

    if (this.versions != other.versions) return false
    if (this.names != other.names) return false
    return true
  }

  override fun hashCode(): Int {
    var result = entitySource.hashCode()
    result = 31 * result + versions.hashCode()
    result = 31 * result + names.hashCode()
    return result
  }
}
