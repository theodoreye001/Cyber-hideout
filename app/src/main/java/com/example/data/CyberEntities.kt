package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val showFollowCounts: Boolean = true,
    val allowAnonymousCitationDefault: Boolean = false
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val content: String,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val imageDeclaration: String? = null // "self", "ai", "others"
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val commenterId: String,
    val content: String,
    val isFeatured: Boolean = false,
    val citizenConsentGiven: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val targetCommenterId: String? = null,
    val parentCommentId: String? = null,
    val parentCommentContent: String? = null
)

@Entity(tableName = "follows")
data class FollowEntity(
    val followerId: String,
    val followingId: String,
    @PrimaryKey val compositeId: String = "$followerId-$followingId" // Room composite primary key helper
)

@Entity(tableName = "likes")
data class LikeEntity(
    val userId: String,
    val postId: String,
    @PrimaryKey val compositeId: String = "$userId-$postId"
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val reporterId: String,
    val targetUserId: String,
    val postId: String? = null,
    val commentId: String? = null,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)
