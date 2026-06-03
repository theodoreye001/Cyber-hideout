package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.UUID

// Data class that represents the visible data of a comment
data class CommentUiModel(
    val id: String,
    val postId: String,
    val commenterId: String,
    val commenterName: String,
    val commenterAvatar: String?,
    val content: String,
    val isFeatured: Boolean,
    val citizenConsentGiven: Boolean,
    val timestamp: Long,
    val isVisibleToCurrentUser: Boolean,
    val targetCommenterId: String? = null,
    val parentCommentId: String? = null,
    val parentCommentContent: String? = null
)

// Data class representing Follow counts and public visibility
data class FollowStats(
    val followersCount: Int,
    val followingCount: Int,
    val isVisibleToVisitor: Boolean
)

// Main repository
class CyberRepository(private val cyberDao: CyberDao) {

    // Manage active simulated session user
    private val _currentUserId = MutableStateFlow("ariadne")
    val currentUserId: StateFlow<String> = _currentUserId

    fun switchUser(userId: String) {
        _currentUserId.value = userId
    }

    suspend fun getActiveUser(): UserEntity {
        val uid = _currentUserId.value
        return cyberDao.getUser(uid) ?: UserEntity(uid, uid, uid)
    }

    // --- Seed Data function to populate database on startup ---
    suspend fun seedInitialDataIfEmpty() {
        val allUsers = cyberDao.getAllUsersFlow().first()
        if (allUsers.isEmpty()) {
            // 1. Instantiating 3 key personas
            val ariadne = UserEntity(
                id = "ariadne",
                username = "ari_creative",
                displayName = "Ariadne Vance",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330",
                bio = "Poet & Interface Designer. Building digital shelters. My theater is transparent but one-way.",
                showFollowCounts = true,
                allowAnonymousCitationDefault = false
            )
            val basil = UserEntity(
                id = "basil",
                username = "basil_s",
                displayName = "Basil Sterling",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d",
                bio = "Independent essayist and coder. Exploring cyber-hermitages. Anti-opinion-aggregate.",
                showFollowCounts = true,
                allowAnonymousCitationDefault = true // Basil pre-approves citations!
            )
            val chloe = UserEntity(
                id = "chloe",
                username = "chloe_m",
                displayName = "Chloe Mercer",
                avatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80",
                bio = "Mental health advocate. Seeking calm digital spaces without numbers and public judgment.",
                showFollowCounts = false, // Hides followers count from others!
                allowAnonymousCitationDefault = false
            )

            cyberDao.insertUser(ariadne)
            cyberDao.insertUser(basil)
            cyberDao.insertUser(chloe)

            // 2. Add some exemplary posts
            val post1 = PostEntity(
                id = "post_1",
                authorId = "ariadne",
                content = "The modern web has become a constant feedback loop of anxiety. Every review is polarized, every list aggregated, every opinion gamified. Cyber-Hideout is an experiment: what if we communicate, but we can't see who agrees with whom? What if opinions are only discussed 1-on-1?",
                likesCount = 2,
                commentsCount = 3,
                timestamp = System.currentTimeMillis() - 3600000 * 5
            )
            val post2 = PostEntity(
                id = "post_2",
                authorId = "basil",
                content = "I've been thinking about the 'Spotlight Rule'. When a creator highlights a comment, it becomes an anonymous snippet. There are no general replies underneath that snippet—it's like putting a letter on a theater wall. It keeps comments clean from trolling cascades.",
                likesCount = 1,
                commentsCount = 1,
                timestamp = System.currentTimeMillis() - 3600000 * 2
            )

            cyberDao.insertPost(post1)
            cyberDao.insertPost(post2)

            // 3. Add internal private comments to Post 1
            // Basil comments on Ariadne's post
            val comment1 = CommentEntity(
                id = "comment_basil_1",
                postId = "post_1",
                commenterId = "basil",
                content = "Absolutely stunning philosophy, Ariadne. It feels like we are re-humanizing communication.",
                isFeatured = true, // Featured! Since Basil allows citations by default, it's public.
                citizenConsentGiven = true,
                timestamp = System.currentTimeMillis() - 3600000 * 4
            )
            val comment2 = CommentEntity(
                id = "comment_basil_2", // Basil's private follow-up thread (Not featured yet)
                postId = "post_1",
                commenterId = "basil",
                content = "Also, how do you manage notifications in this structure?",
                isFeatured = false,
                citizenConsentGiven = true,
                timestamp = System.currentTimeMillis() - 3600000 * 3
            )
            // Chloe comments privately on Ariadne's post (No citation, no consent)
            val comment3 = CommentEntity(
                id = "comment_chloe_1",
                postId = "post_1",
                commenterId = "chloe",
                content = "This was the first time in years I read an article without feeling defensive or self-conscious. Thank you.",
                isFeatured = false,
                citizenConsentGiven = false,
                timestamp = System.currentTimeMillis() - 3600000 * 1
            )

            cyberDao.insertComment(comment1)
            cyberDao.insertComment(comment2)
            cyberDao.insertComment(comment3)

            // Seed likes
            cyberDao.insertLike(LikeEntity("basil", "post_1"))
            cyberDao.insertLike(LikeEntity("chloe", "post_1"))
            cyberDao.insertLike(LikeEntity("ariadne", "post_2"))

            // Seed follows relationships
            // Ariadne follows Basil
            cyberDao.insertFollow(FollowEntity("ariadne", "basil"))
            // Basil follows Ariadne
            cyberDao.insertFollow(FollowEntity("basil", "ariadne"))
            // Chloe follows Ariadne
            cyberDao.insertFollow(FollowEntity("chloe", "ariadne"))
        }
    }

    // --- Post API implementation ---
    fun getAllPosts(): Flow<List<PostEntity>> = cyberDao.getAllPostsFlow()

    suspend fun createPost(content: String, imageUrl: String? = null, imageDeclaration: String? = null) {
        val authorId = _currentUserId.value
        val newPost = PostEntity(
            id = UUID.randomUUID().toString(),
            authorId = authorId,
            content = content,
            imageUrl = imageUrl,
            imageDeclaration = imageDeclaration
        )
        cyberDao.insertPost(newPost)
    }

    suspend fun toggleLike(postId: String): Boolean {
        val uid = _currentUserId.value
        val existingLike = cyberDao.getLike(uid, postId)
        val post = cyberDao.getPost(postId) ?: return false

        var isLikedNow = false
        if (existingLike != null) {
            cyberDao.deleteLike(uid, postId)
            // Decrement
            cyberDao.updatePostCounters(postId, (post.likesCount - 1).coerceAtLeast(0), post.commentsCount)
        } else {
            cyberDao.insertLike(LikeEntity(uid, postId))
            // Increment
            cyberDao.updatePostCounters(postId, post.likesCount + 1, post.commentsCount)
            isLikedNow = true
        }
        return isLikedNow
    }

    suspend fun getIsLiked(postId: String): Boolean {
        val uid = _currentUserId.value
        return cyberDao.getLike(uid, postId) != null
    }

    // --- Rule 1: Secure Follow List Retrieval ---
    fun getFollowStats(profileId: String, viewerId: String): Flow<FollowStats> {
        return combine(
            cyberDao.getFollowersFlow(profileId),
            cyberDao.getFollowingFlow(profileId),
            cyberDao.getUserFlow(profileId)
        ) { followers, following, user ->
            val isOwner = profileId == viewerId
            val isVisible = isOwner || (user?.showFollowCounts ?: true)
            FollowStats(
                followersCount = followers.size,
                followingCount = following.size,
                isVisibleToVisitor = isVisible
            )
        }
    }

    suspend fun isFollowing(followerId: String, followingId: String): Boolean {
        return cyberDao.getFollowRelation(followerId, followingId) != null
    }

    suspend fun toggleFollow(targetUserId: String) {
        val uid = _currentUserId.value
        if (uid == targetUserId) return
        val relation = cyberDao.getFollowRelation(uid, targetUserId)
        if (relation != null) {
            cyberDao.deleteFollow(uid, targetUserId)
        } else {
            cyberDao.insertFollow(FollowEntity(uid, targetUserId))
        }
    }

    // --- Rule 2 & 3: One-Way Backstage Comments with Dual-Consent RLS Emulation ---
    fun getSecureCommentsForPost(postId: String): Flow<List<CommentUiModel>> {
        return combine(
            cyberDao.getCommentsForPostFlow(postId),
            cyberDao.getAllUsersFlow(),
            _currentUserId,
            cyberDao.getPostFlow(postId) // watch ONLY this single post instead of all posts
        ) { comments, users, currentUserId, post ->
            val postAuthorId = post?.authorId ?: ""
            val isPostAuthor = currentUserId == postAuthorId

            comments.mapNotNull { comment ->
                val commenter = users.find { it.id == comment.commenterId }
                val commenterName = commenter?.displayName ?: comment.commenterId
                val commenterAvatar = commenter?.avatarUrl

                // SECURE PRIVACY RULES (Local RLS engine check):
                // Direct visibility conditions:
                // 1. Current user is the author of this comment
                // 2. Current user is the author of the post (Backstage Manager)
                // 3. Comment has been featured on board and consent was given (Public Spotlight)
                // 4. Comment is a response from the post author to the current visitor (1-on-1 private connection)
                val isOwner = currentUserId == comment.commenterId
                val isSpotlitAndApproved = comment.isFeatured && comment.citizenConsentGiven
                val isTargetUser = (comment.commenterId == postAuthorId && comment.targetCommenterId == currentUserId)

                val isVisible = isOwner || isPostAuthor || isSpotlitAndApproved || isTargetUser

                if (isVisible) {
                    CommentUiModel(
                        id = comment.id,
                        postId = comment.postId,
                        commenterId = comment.commenterId,
                        commenterName = if (isSpotlitAndApproved && !isOwner && !isPostAuthor) "Anonymous Theater Guest" else commenterName,
                        commenterAvatar = if (isSpotlitAndApproved && !isOwner && !isPostAuthor) null else commenterAvatar,
                        content = comment.content,
                        isFeatured = comment.isFeatured,
                        citizenConsentGiven = comment.citizenConsentGiven,
                        timestamp = comment.timestamp,
                        isVisibleToCurrentUser = true,
                        targetCommenterId = comment.targetCommenterId,
                        parentCommentId = comment.parentCommentId,
                        parentCommentContent = comment.parentCommentContent
                    )
                } else {
                    // Completely hide if unauthorized, leaving no trace in lists
                    null
                }
            }
        }
    }

    suspend fun addComment(
        postId: String,
        content: String,
        preConsent: Boolean = false,
        targetCommenterId: String? = null,
        parentCommentId: String? = null,
        parentCommentContent: String? = null
    ) {
        val uid = _currentUserId.value
        val user = cyberDao.getUser(uid)
        val defaultConsent = user?.allowAnonymousCitationDefault ?: false
        
        val newComment = CommentEntity(
            id = UUID.randomUUID().toString(),
            postId = postId,
            commenterId = uid,
            content = content,
            isFeatured = false,
            citizenConsentGiven = preConsent || defaultConsent,
            targetCommenterId = targetCommenterId,
            parentCommentId = parentCommentId,
            parentCommentContent = parentCommentContent
        )
        cyberDao.insertComment(newComment)

        // Increment cached counts
        val post = cyberDao.getPost(postId)
        if (post != null) {
            cyberDao.updatePostCounters(postId, post.likesCount, post.commentsCount + 1)
        }
    }

    // Creator spotlights a comment (requires matching consent status or real-time popup toggle)
    suspend fun setCommentSpotlight(commentId: String, featured: Boolean): Boolean {
        val comment = cyberDao.getComment(commentId) ?: return false
        
        // Spotlight rule: if spotlight true, it needs citizenConsentGiven to show up publicly.
        // We set isFeatured in the database.
        cyberDao.updateCommentSpotlight(
            commentId = commentId,
            isFeatured = featured,
            consent = comment.citizenConsentGiven // keeps current consent
        )
        return true
    }

    suspend fun forceConsentToggle(commentId: String, consentGranted: Boolean) {
        val comment = cyberDao.getComment(commentId) ?: return
        cyberDao.updateCommentSpotlight(
            commentId = commentId,
            isFeatured = comment.isFeatured,
            consent = consentGranted
        )
    }

    suspend fun deleteComment(commentId: String, postId: String) {
        cyberDao.deleteComment(commentId)
        val post = cyberDao.getPost(postId) ?: return
        cyberDao.updatePostCounters(postId, post.likesCount, (post.commentsCount - 1).coerceAtLeast(0))
    }

    // --- Rule 4: Security Moderation Actions ---
    suspend fun fileReport(targetUserId: String, postId: String?, commentId: String?, reason: String) {
        val uid = _currentUserId.value
        val report = ReportEntity(
            id = UUID.randomUUID().toString(),
            reporterId = uid,
            targetUserId = targetUserId,
            postId = postId,
            commentId = commentId,
            reason = reason
        )
        cyberDao.insertReport(report)
    }

    suspend fun updateUserSettings(showFollows: Boolean, preConsent: Boolean) {
        val uid = _currentUserId.value
        val user = cyberDao.getUser(uid) ?: return
        val updated = user.copy(
            showFollowCounts = showFollows,
            allowAnonymousCitationDefault = preConsent
        )
        cyberDao.updateUser(updated)
    }
}
