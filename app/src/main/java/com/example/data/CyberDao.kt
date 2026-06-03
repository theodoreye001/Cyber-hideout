package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CyberDao {

    // --- User DAOs ---
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserFlow(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUser(userId: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    // --- Post DAOs ---
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPostsFlow(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :postId")
    fun getPostFlow(postId: String): Flow<PostEntity?>

    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPost(postId: String): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Delete
    suspend fun deletePost(post: PostEntity)

    @Query("UPDATE posts SET likesCount = :likesCount, commentsCount = :commentsCount WHERE id = :postId")
    suspend fun updatePostCounters(postId: String, likesCount: Int, commentsCount: Int)

    // --- Comment DAOs ---
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPostFlow(postId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    suspend fun getCommentsForPost(postId: String): List<CommentEntity>

    @Query("SELECT * FROM comments WHERE id = :commentId")
    suspend fun getComment(commentId: String): CommentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("UPDATE comments SET isFeatured = :isFeatured, citizenConsentGiven = :consent WHERE id = :commentId")
    suspend fun updateCommentSpotlight(commentId: String, isFeatured: Boolean, consent: Boolean)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: String)

    // --- Follow DAOs ---
    @Query("SELECT * FROM follows WHERE followingId = :userId")
    fun getFollowersFlow(userId: String): Flow<List<FollowEntity>>

    @Query("SELECT * FROM follows WHERE followerId = :userId")
    fun getFollowingFlow(userId: String): Flow<List<FollowEntity>>

    @Query("SELECT * FROM follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun getFollowRelation(followerId: String, followingId: String): FollowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun deleteFollow(followerId: String, followingId: String)

    // --- Like DAOs ---
    @Query("SELECT * FROM likes WHERE postId = :postId")
    suspend fun getLikesForPost(postId: String): List<LikeEntity>

    @Query("SELECT * FROM likes WHERE userId = :userId AND postId = :postId")
    suspend fun getLike(userId: String, postId: String): LikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity)

    @Query("DELETE FROM likes WHERE userId = :userId AND postId = :postId")
    suspend fun deleteLike(userId: String, postId: String)

    // --- Report DAOs ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReportsFlow(): Flow<List<ReportEntity>>
}
