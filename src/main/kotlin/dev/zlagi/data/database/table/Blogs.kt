package dev.zlagi.data.database.table

import dev.zlagi.data.dao.BlogsDao
import dev.zlagi.data.entity.EntityBlog
import dev.zlagi.data.entity.EntityUser
import dev.zlagi.data.model.BlogDataModel
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object Blogs : UUIDTable(), BlogsDao {
    val user = reference("user", Users)
    var username = varchar("username", length = 128)
    var title = varchar("title", length = 128)
    var description = text("description")
    var created = varchar("created", length = 48)
    var updated = varchar("updated", length = 48).nullable()

    override suspend fun store(
        userId: String,
        username: String,
        title: String,
        description: String,
        created: String,
        updated: String?
    ): BlogDataModel =
        newSuspendedTransaction(Dispatchers.IO) {
            EntityBlog.new {
                this.user = EntityUser[UUID.fromString(userId)]
                this.username = username
                this.title = title
                this.description = description
                this.created = created
                this.updated = updated
            }.let { BlogDataModel.fromEntity(it) }
        }

    override suspend fun searchByQuery(query: String): List<BlogDataModel> = newSuspendedTransaction(Dispatchers.IO) {
        EntityBlog.find {
            title regexp query or (description regexp query)
        }.sortedByDescending { it.id }
            .map { BlogDataModel.fromEntity(it) }
    }

    override suspend fun update(blogId: String, title: String, description: String, updated: String): BlogDataModel =
        newSuspendedTransaction(Dispatchers.IO) {
            EntityBlog[UUID.fromString(blogId)].apply {
                this.title = title
                this.description = description
                this.updated = updated
            }.let { BlogDataModel.fromEntity(it) }
        }

    override suspend fun deleteById(blogId: String): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            val blog = EntityBlog.findById(UUID.fromString(blogId))
            blog?.run {
                delete()
                return@newSuspendedTransaction true
            }
            return@newSuspendedTransaction false
        }

    override suspend fun isBlogAuthor(blogId: String, userId: String): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            EntityBlog.find {
                (Blogs.id eq UUID.fromString(blogId)) and (user eq UUID.fromString(userId))
            }.firstOrNull() != null
        }

    override suspend fun exists(id: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        EntityBlog.findById(UUID.fromString(id)) != null
    }
}