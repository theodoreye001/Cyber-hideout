package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.SharedPreferences

// Screen navigation enum
enum class CyberScreen {
    FEED,
    POST_DETAIL,
    PROFILE,
    SETTINGS
}

class CyberViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CyberDatabase.getDatabase(application)
    private val repository = CyberRepository(database.cyberDao())
    private val sharedPrefs: SharedPreferences = application.getSharedPreferences("cyber_hideout_prefs", android.content.Context.MODE_PRIVATE)

    // --- State flows ---
    val currentUserId: StateFlow<String> = repository.currentUserId
    
    private val _isChinese = MutableStateFlow(sharedPrefs.getBoolean("is_chinese", false))
    val isChinese: StateFlow<Boolean> = _isChinese.asStateFlow()

    private val _currentScreen = MutableStateFlow(CyberScreen.FEED)
    val currentScreen: StateFlow<CyberScreen> = _currentScreen

    private val _selectedPostId = MutableStateFlow<String?>(null)
    val selectedPostId: StateFlow<String?> = _selectedPostId

    private val _selectedProfileId = MutableStateFlow<String?>(null)
    val selectedProfileId: StateFlow<String?> = _selectedProfileId

    // All posts
    val posts: StateFlow<List<PostEntity>> = repository.getAllPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All registered users mapped by ID
    val usersMap: StateFlow<Map<String, UserEntity>> = database.cyberDao().getAllUsersFlow()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Flow of secure comments for selected post
    val selectedPostComments: StateFlow<List<CommentUiModel>> = _selectedPostId
        .flatMapLatest { postId ->
            if (postId != null) {
                repository.getSecureCommentsForPost(postId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // Seed sample database values
            repository.seedInitialDataIfEmpty()
        }
    }

    // --- Actions ---
    fun switchUser(userId: String) {
        viewModelScope.launch {
            repository.switchUser(userId)
            // Re-apply/force state update
            _selectedPostId.value = _selectedPostId.value
        }
    }

    fun navigateTo(screen: CyberScreen) {
        _currentScreen.value = screen
    }

    fun viewPostDetail(postId: String) {
        _selectedPostId.value = postId
        _currentScreen.value = CyberScreen.POST_DETAIL
    }

    fun viewProfile(userId: String) {
        _selectedProfileId.value = userId
        _currentScreen.value = CyberScreen.PROFILE
    }

    fun writePost(content: String, imageUrl: String? = null, imageDeclaration: String? = null, onFinished: () -> Unit) {
        viewModelScope.launch {
            repository.createPost(content, imageUrl, imageDeclaration)
            onFinished()
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            repository.toggleLike(postId)
        }
    }

    suspend fun getIsLiked(postId: String): Boolean {
        return repository.getIsLiked(postId)
    }

    fun getFeaturedCommentForPost(postId: String): Flow<CommentUiModel?> {
        return repository.getSecureCommentsForPost(postId).map { comments ->
            comments.find { it.isFeatured && it.citizenConsentGiven }
        }
    }

    fun leaveComment(
        postId: String,
        content: String,
        preConsent: Boolean,
        targetCommenterId: String? = null,
        parentCommentId: String? = null,
        parentCommentContent: String? = null
    ) {
        viewModelScope.launch {
            repository.addComment(
                postId = postId,
                content = content,
                preConsent = preConsent,
                targetCommenterId = targetCommenterId,
                parentCommentId = parentCommentId,
                parentCommentContent = parentCommentContent
            )
        }
    }

    fun toggleSpotlight(commentId: String, currentFeatured: Boolean) {
        viewModelScope.launch {
            repository.setCommentSpotlight(commentId, !currentFeatured)
        }
    }

    fun toggleConsentForComment(commentId: String, consent: Boolean) {
        viewModelScope.launch {
            repository.forceConsentToggle(commentId, consent)
        }
    }

    fun deleteComment(commentId: String, postId: String) {
        viewModelScope.launch {
            repository.deleteComment(commentId, postId)
        }
    }

    fun reportSpam(targetUserId: String, postId: String?, commentId: String?, reason: String) {
        viewModelScope.launch {
            repository.fileReport(targetUserId, postId, commentId, reason)
        }
    }

    fun setLanguage(isChinese: Boolean) {
        _isChinese.value = isChinese
        sharedPrefs.edit().putBoolean("is_chinese", isChinese).apply()
    }

    fun saveSettings(showFollows: Boolean, preConsent: Boolean) {
        viewModelScope.launch {
            repository.updateUserSettings(showFollows, preConsent)
        }
    }

    // Follow stats helper for standard profile observation
    fun getProfileFollowStats(profileId: String): Flow<FollowStats> {
        return repository.getFollowStats(profileId, currentUserId.value)
    }

    suspend fun checkIsFollowing(followingId: String): Boolean {
        return repository.isFollowing(currentUserId.value, followingId)
    }

    fun toggleFollow(followingId: String) {
        viewModelScope.launch {
            repository.toggleFollow(followingId)
        }
    }
}
