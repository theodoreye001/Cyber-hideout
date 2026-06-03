package com.example

import android.os.Bundle
import android.widget.Toast
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.data.*
import com.example.ui.CyberScreen
import com.example.ui.CyberViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CyberHideoutTheme {
                val viewModel: CyberViewModel = viewModel()
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: CyberViewModel) {
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val usersMap by viewModel.usersMap.collectAsStateWithLifecycle()
    val isChinese by viewModel.isChinese.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showAddPostDialog by remember { mutableStateOf(false) }
    var showReportDialogUser by remember { mutableStateOf<String?>(null) }

    var tempPostContent by remember { mutableStateOf("") }
    var tempImageUrl by remember { mutableStateOf<String?>(null) }
    var tempImageDeclaration by remember { mutableStateOf<String?>(null) } // "self", "ai", "others"

    val textFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val text = stream.bufferedReader().use { r -> r.readText() }
                    if (text.isNotBlank()) {
                        tempPostContent = text
                        Toast.makeText(context, if (isChinese) "文档导入成功！" else "Text file loaded successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, if (isChinese) "选择的文件无文本内容" else "Selected file has no text contents", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, (if (isChinese) "读取文档错误: " else "Error reading document: ") + e.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            tempImageUrl = it.toString()
            Toast.makeText(context, if (isChinese) "本地图片加载成功！" else "Local image selected!", Toast.LENGTH_SHORT).show()
        }
    }

    val activeUser = usersMap[currentUserId] ?: UserEntity(currentUserId, currentUserId, currentUserId)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                // Header Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CYBER-HIDEOUT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isChinese) "虚拟剧场 • 幕后模式" else "Virtual Theater • Backstage Mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    }

                    // Quick User Emulator Switcher (Awesome for testing rules!)
                    Row(
                        modifier = Modifier
                            .background(StealthSlate, RoundedCornerShape(20.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("ariadne", "basil", "chloe").forEach { uid ->
                            val isSelected = currentUserId == uid
                            val nameInitial = uid.take(1).uppercase()
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable {
                                        viewModel.switchUser(uid)
                                        Toast.makeText(context, if (isChinese) "已切换至 ${uid.replaceFirstChar { it.uppercase() }}" else "Switched to ${uid.replaceFirstChar { it.uppercase() }}", Toast.LENGTH_SHORT).show()
                                    }
                                    .testTag("switch_user_$uid"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = nameInitial,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else SoftLavender
                                )
                            }
                        }
                    }
                }

                // Active Identity Toast indicator bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StealthSlate)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Identity",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isChinese) "当前身份: " else "Viewing as: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                    Text(
                        text = activeUser.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "@${activeUser.username}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = StealthSlate,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .border(BorderStroke(1.dp, SophisticatedBorder), RoundedCornerShape(0.dp))
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RadiantCyan,
                    selectedTextColor = RadiantCyan,
                    unselectedIconColor = MutedSlate,
                    unselectedTextColor = MutedSlate,
                    indicatorColor = DeepOceanBlue
                )
                NavigationBarItem(
                    selected = currentScreen == CyberScreen.FEED,
                    onClick = { viewModel.navigateTo(CyberScreen.FEED) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(if (isChinese) "剧场" else "Theater") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        tempPostContent = ""
                        tempImageUrl = null
                        tempImageDeclaration = null
                        showAddPostDialog = true
                    },
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "Post Idea", tint = RadiantCyan) },
                    label = { Text(if (isChinese) "发布创意" else "Cast Idea", color = RadiantCyan) },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = currentScreen == CyberScreen.PROFILE && viewModel.selectedProfileId.value == currentUserId,
                    onClick = { viewModel.viewProfile(currentUserId) },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
                    label = { Text(if (isChinese) "我的后台" else "My Backstage") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = currentScreen == CyberScreen.SETTINGS,
                    onClick = { viewModel.navigateTo(CyberScreen.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(if (isChinese) "控制台" else "Console") },
                    colors = navItemColors
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    CyberScreen.FEED -> FeedScreen(
                        viewModel = viewModel,
                        onReportUser = { showReportDialogUser = it }
                    )
                    CyberScreen.POST_DETAIL -> PostDetailScreen(
                        viewModel = viewModel,
                        onReportUser = { showReportDialogUser = it }
                    )
                    CyberScreen.PROFILE -> ProfileScreen(
                        viewModel = viewModel,
                        onReportUser = { showReportDialogUser = it }
                    )
                    CyberScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Modal dialogs
    if (showAddPostDialog) {
        AddPostDialog(
            content = tempPostContent,
            onContentChange = { tempPostContent = it },
            selectedImageUri = tempImageUrl,
            onSelectedImageUriChange = { tempImageUrl = it },
            imageDeclaration = tempImageDeclaration,
            onImageDeclarationChange = { tempImageDeclaration = it },
            onPickTextFile = {
                try {
                    textFileLauncher.launch("text/plain")
                } catch (e: Exception) {
                    Toast.makeText(context, if (isChinese) "设备上未找到支持选择文本文件的应用" else "No app found to handle text file selection", Toast.LENGTH_LONG).show()
                }
            },
            onPickImage = {
                try {
                    imagePickerLauncher.launch("image/*")
                } catch (e: Exception) {
                    Toast.makeText(context, if (isChinese) "找不到图片选择器" else "Image picker not found", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showAddPostDialog = false },
            onSubmit = { content, imgUrl, decl ->
                viewModel.writePost(content, imgUrl, decl) {
                    showAddPostDialog = false
                    Toast.makeText(context, if (isChinese) "剧场广播已发布！" else "Theater Broadcast Published!", Toast.LENGTH_SHORT).show()
                }
            },
            isChinese = isChinese
        )
    }

    showReportDialogUser?.let { targetId ->
        ReportUserDialog(
            userId = targetId,
            userName = usersMap[targetId]?.displayName ?: targetId,
            onDismiss = { showReportDialogUser = null },
            onSubmit = { reason ->
                viewModel.reportSpam(targetUserId = targetId, postId = null, commentId = null, reason = reason)
                showReportDialogUser = null
                Toast.makeText(context, if (isChinese) "安全报告直达管理员主控制台" else "Direct security report lodged to Admin.", Toast.LENGTH_LONG).show()
            },
            isChinese = isChinese
        )
    }
}

// -----------------------------------------------------------------------------
// FEED SCREEN
// -----------------------------------------------------------------------------
@Composable
fun FeedScreen(
    viewModel: CyberViewModel,
    onReportUser: (String) -> Unit
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val usersMap by viewModel.usersMap.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val isChinese by viewModel.isChinese.collectAsStateWithLifecycle()

    if (posts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Filled.Warning, "Empty Board", modifier = Modifier.size(64.dp), tint = MutedSlate)
                Spacer(modifier = Modifier.height(16.dp))
                Text(if (isChinese) "暂无动态看板。开启一场新演出吧！" else "No dynamic boards operating. Put on a new show!", color = SoftLavender, textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("feed_list"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = if (isChinese) "开放广场" else "THE OPEN SQUARE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedSlate,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = if (isChinese) "一个汇聚广播记录的舞台，零社区公开交流。所有讨论都在后台私下进行。" else "A stage with aggregate logs, but zero community cross-talk. All discussions happen backstage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            items(posts.size) { index ->
                val post = posts[index]
                val author = usersMap[post.authorId] ?: UserEntity(post.authorId, post.authorId, post.authorId)
                PostFeedCard(
                    post = post,
                    author = author,
                    currentUserId = currentUserId,
                    viewModel = viewModel,
                    onClick = { viewModel.viewPostDetail(post.id) },
                    onReportUser = onReportUser
                )
            }
        }
    }
}

@Composable
fun PostFeedCard(
    post: PostEntity,
    author: UserEntity,
    currentUserId: String,
    viewModel: CyberViewModel,
    onClick: () -> Unit,
    onReportUser: (String) -> Unit
) {
    var isLiked by remember { mutableStateOf(false) }
    val isChinese by viewModel.isChinese.collectAsStateWithLifecycle()
    val featuredCommentFlow = remember(post.id) {
        viewModel.getFeaturedCommentForPost(post.id)
    }
    val featuredComment by featuredCommentFlow.collectAsStateWithLifecycle(initialValue = null)
    
    // Quick refresh of liked status
    LaunchedEffect(currentUserId, post.id) {
        isLiked = viewModel.getIsLiked(post.id)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("post_card_${post.id}"),
        colors = CardDefaults.cardColors(containerColor = StealthSlate),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, if (post.authorId == currentUserId) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else SophisticatedBorder)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { viewModel.viewProfile(author.id) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            author.displayName.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = author.displayName,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "@${author.username}" + if(author.id == currentUserId) (if (isChinese) " (你)" else " (You)") else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    }
                }

                // Security Shield / Report trigger
                IconButton(
                    onClick = { onReportUser(post.authorId) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Flag Post",
                        tint = MutedSlate.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body text
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = SoftLavender,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )

            if (!post.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box {
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(model = post.imageUrl),
                            contentDescription = "Idea Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        
                        val declLabel = when(post.imageDeclaration) {
                            "self" -> if (isChinese) "作者原创" else "Original Art"
                            "ai" -> if (isChinese) "AI 生成" else "AI Generated"
                            "others" -> if (isChinese) "他人授权/摄影" else "Other's Photo"
                            else -> ""
                        }
                        if (declLabel.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(MidnightNavy.copy(alpha = 0.82f), RoundedCornerShape(topStart = 8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = declLabel,
                                    fontSize = 9.sp,
                                    color = RadiantCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Consenting featured quotation if available
            featuredComment?.let { fc ->
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MidnightNavy.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left border-l-4 style accent
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(RadiantCyan)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MutedSlate,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isChinese) "高亮隐私心声" else "FEATURED PRIVATE THOUGHT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedSlate,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "“${fc.content}”",
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = TextLightGrey
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isChinese) "— 匿名 (经用户同意引用)" else "— Anonymous (CITED WITH CONSENT)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = RadiantCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer parameters (Rule 1: Counts are public, lists are NOT)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Likes button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.toggleLike(post.id)
                            isLiked = !isLiked
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Likes count",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MutedSlate,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = post.likesCount.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isLiked) MaterialTheme.colorScheme.primary else MutedSlate
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Connection private Comments stats
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = "Backstage chats",
                        tint = MutedSlate,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = post.commentsCount.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MutedSlate
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Enter Backstage button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(0.12f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Backstage",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if(post.authorId == currentUserId) {
                            if (isChinese) "管理后台" else "Manage Backstage"
                        } else {
                            if (isChinese) "后台私聊" else "Backstage Chat"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// POST DETAIL SCREEN (SPOTLIGHT MAIN BOARD & BACKSTAGE 1-ON-1)
// -----------------------------------------------------------------------------
@Composable
fun PostDetailScreen(
    viewModel: CyberViewModel,
    onReportUser: (String) -> Unit
) {
    val selectedPostId by viewModel.selectedPostId.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val usersMap by viewModel.usersMap.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val comments by viewModel.selectedPostComments.collectAsStateWithLifecycle()
    val isChinese by viewModel.isChinese.collectAsStateWithLifecycle()

    val post = posts.find { it.id == selectedPostId }

    if (post == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (isChinese) "未找到该剧场广播。" else "Theater broadcast not found.", color = SoftLavender)
        }
        return
    }

    val author = usersMap[post.authorId] ?: UserEntity(post.authorId, post.authorId, post.authorId)
    val isAuthor = currentUserId == post.authorId

    var commentText by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<CommentUiModel?>(null) }
    var allowCitationCheckbox by remember { mutableStateOf(false) }
    val currentUserEntity = usersMap[currentUserId]
    LaunchedEffect(currentUserEntity) {
        if (currentUserEntity != null) {
            allowCitationCheckbox = currentUserEntity.allowAnonymousCitationDefault
        }
    }

    // Grouping comments by commenter thread (Room).
    // For Visitor: Only 1 list representing commenter's own messages.
    // For Creator: Can see distinct threads.
    val commentsByCommenter = remember(comments, post.authorId) {
        comments
            .filter { it.commenterId != post.authorId || it.targetCommenterId != null }
            .groupBy { item ->
                if (item.commenterId == post.authorId) {
                    item.targetCommenterId ?: ""
                } else {
                    item.commenterId
                }
            }
            .filterKeys { it.isNotEmpty() }
    }

    // Active thread selected by Creator
    var selectedBackstageCommenterId by remember { mutableStateOf<String?>(null) }

    // Automatically set default backstage thread for creator if not set
    LaunchedEffect(commentsByCommenter, isAuthor) {
        if (isAuthor && selectedBackstageCommenterId == null && commentsByCommenter.isNotEmpty()) {
            selectedBackstageCommenterId = commentsByCommenter.keys.first()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("post_detail_container")
    ) {
        // Back Button
        TextButton(
            onClick = { viewModel.navigateTo(CyberScreen.FEED) },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.testTag("back_button")
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isChinese) "返回开放广场" else "Return to Open Square", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Original Post Statement
        Card(
            colors = CardDefaults.cardColors(containerColor = StealthSlate),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(author.displayName.take(1).uppercase(), color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(author.displayName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("@${author.username}", fontSize = 11.sp, color = MutedSlate)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(post.content, style = MaterialTheme.typography.bodyLarge, color = SoftLavender, lineHeight = 24.sp)
                
                if (!post.imageUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box {
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(model = post.imageUrl),
                                contentDescription = "Idea Image Detail",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            
                            val declLabel = when(post.imageDeclaration) {
                                "self" -> if (isChinese) "作者原创" else "Original Art"
                                "ai" -> if (isChinese) "AI 生成" else "AI Generated"
                                "others" -> if (isChinese) "他人授权/摄影" else "Other's Photo"
                                else -> ""
                            }
                            if (declLabel.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(MidnightNavy.copy(alpha = 0.82f), RoundedCornerShape(topStart = 8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = declLabel,
                                        fontSize = 9.sp,
                                        color = RadiantCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ---------------------------------------------------------------------
        // RULE 3: DUAL-CONSENT SPOTLIGHT HIGHLIGHT BOARD (PUBLIC DECORATION SNIPPETS)
        // ---------------------------------------------------------------------
        val featuredComments = comments.filter { it.isFeatured && it.citizenConsentGiven }
        Text(
            text = if (isChinese) "高亮推荐看板 (公开看板)" else "SPOTLIGHT BOARD (PUBLIC BOARD)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.sp
        )
        Text(
            text = if (isChinese) "由作者精心挑选的匿名片段。您无法在片段下发表评论，只能阅读与思考。" else "Anonymous snippets curated by the author. You cannot comment under a snippet, only read and reflect.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedSlate,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (featuredComments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StealthSlate.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, MutedSlate.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isChinese) "该看板一侧暂无推荐引用。幕后私信内容绝对安全。" else "No citations spotlighted on this board yet. Backstage messages are strictly secure.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                featuredComments.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StealthSlate, RoundedCornerShape(20.dp))
                            .border(BorderStroke(1.dp, SophisticatedBorder), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left border-l-4 accent
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(52.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(RadiantCyan)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MutedSlate,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isChinese) "高亮隐私心声" else "FEATURED PRIVATE THOUGHT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedSlate,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "“${item.content}”",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = TextLightGrey
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isChinese) "— 匿名 (经用户同意引用)" else "— Anonymous (CITED WITH CONSENT)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = RadiantCyan
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ---------------------------------------------------------------------
        // RULE 2: BACKSTAGE CHAT INTERACTIVE PANEL (1-ON-1 ISOLATED DIALOGUES)
        // ---------------------------------------------------------------------
        Text(
            text = if (isChinese) "幕后交互模式 (1对1会话)" else "BACKSTAGE MODE (1-ON-1 SESSIONS)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )

        if (isAuthor) {
            // AUTHOR VIEW: Manage multiple lists of backstage "hand-raisers"
            Text(
                text = if (isChinese) "每位评论者在各自房间中完全隔离。在下方选择一间聊天室：" else "Each commenter is fully isolated in their own room. Select a room layout below:",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSlate,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (commentsByCommenter.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StealthSlate, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isChinese) "暂无幕后对话发起。" else "No backstage conversations started yet.", color = MutedSlate)
                }
            } else {
                // Room list tab layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    commentsByCommenter.keys.forEach { commenterId ->
                        val activeUserEnt = usersMap[commenterId] ?: UserEntity(commenterId, commenterId, commenterId)
                        val isRoomSelected = selectedBackstageCommenterId == commenterId

                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isRoomSelected) MaterialTheme.colorScheme.primary else StealthSlate,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedBackstageCommenterId = commenterId }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isChinese) "聊天室: ${activeUserEnt.displayName}" else "Room: " + activeUserEnt.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRoomSelected) MaterialTheme.colorScheme.onPrimary else SoftLavender
                            )
                        }
                    }
                }

                // Render active selected thread for post author
                selectedBackstageCommenterId?.let { targetCommenterId ->
                    val threadComments = commentsByCommenter[targetCommenterId] ?: emptyList()
                    val targetUser = usersMap[targetCommenterId] ?: UserEntity(targetCommenterId, targetCommenterId, targetCommenterId)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = StealthSlate),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Backstage thread header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, "Isolated", tint = SafetyGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isChinese) "与 ${targetUser.displayName} 的幕后对话" else "Backstage chat with ${targetUser.displayName}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SafetyGreen
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(
                                    onClick = { onReportUser(targetCommenterId) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(if (isChinese) "举报" else "Report", color = AlarmCrimson, fontSize = 11.sp)
                                }
                            }

                            Divider(color = MutedSlate.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

                            // Message stream
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                threadComments.forEach { item ->
                                    val isSelfComment = item.commenterId == currentUserId
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = if (isSelfComment) Alignment.End else Alignment.Start
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = if (isSelfComment) Arrangement.End else Arrangement.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (isSelfComment) {
                                                CommentOptionsButton(
                                                    item = item,
                                                    onReplySelect = { replyingToComment = it },
                                                    isChinese = isChinese
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = if (isSelfComment) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else StealthSlate.copy(alpha = 0.8f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .border(BorderStroke(1.dp, if(isSelfComment) MaterialTheme.colorScheme.primary else MutedSlate.copy(0.3f)), RoundedCornerShape(12.dp))
                                                    .padding(10.dp)
                                            ) {
                                                Column {
                                                    if (!item.parentCommentContent.isNullOrBlank()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .padding(bottom = 6.dp)
                                                                .background(MidnightNavy.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                                        ) {
                                                            Text(
                                                                text = "“${item.parentCommentContent}”",
                                                                fontSize = 11.sp,
                                                                color = MutedSlate,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        item.content,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = SoftLavender
                                                    )
                                                }
                                            }

                                            if (!isSelfComment) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                CommentOptionsButton(
                                                    item = item,
                                                    onReplySelect = { replyingToComment = it },
                                                    isChinese = isChinese
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Spotlight controls inside thread (Author only)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Status tag
                                            val spotlightStatusText = when {
                                                item.isFeatured && item.citizenConsentGiven -> if (isChinese) "推荐公开看板已启用" else "Spotlight Publicly Enabled"
                                                item.isFeatured && !item.citizenConsentGiven -> if (isChinese) "推荐悬而未决(等待创作者/用户同意)" else "Spotlight Pending Creator/User Approval"
                                                else -> if (isChinese) "直达幕后私聊" else "Direct Private Note"
                                            }
                                            Text(
                                                text = spotlightStatusText,
                                                fontSize = 9.sp,
                                                color = if (item.isFeatured && item.citizenConsentGiven) SafetyGreen else MutedSlate
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Curate button
                                            TextButton(
                                                onClick = { viewModel.toggleSpotlight(item.id, item.isFeatured) },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(
                                                    text = if (item.isFeatured) (if (isChinese) "取消高亮" else "Unspotlight") else (if (isChinese) "高亮至推荐看板" else "Publish to Spotlight"),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            
                                            // Emulating Basil consenting to show popup trigger if missing
                                            if (item.isFeatured && !item.citizenConsentGiven) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                TextButton(
                                                    onClick = { viewModel.toggleConsentForComment(item.id, true) },
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text(if (isChinese) "授权合规匿名高亮" else "Grant Citizen Approval", fontSize = 10.sp, color = SafetyGreen)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action write back
                            if (replyingToComment != null) {
                                replyingToComment?.let { parentComment ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MidnightNavy.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val commenterName = usersMap[parentComment.commenterId]?.displayName ?: parentComment.commenterName
                                        Text(
                                            text = if (isChinese) "回复 @${commenterName}: “${parentComment.content}”" else "Replying to @${commenterName}: \"${parentComment.content}\"",
                                            fontSize = 11.sp,
                                            color = RadiantCyan,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { replyingToComment = null },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cancel reply",
                                                tint = RadiantCyan,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    placeholder = { Text(if (isChinese) "在幕后写下私人回复..." else "Write private answer Backstage...", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MutedSlate.copy(0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (commentText.isNotBlank()) {
                                            // Author comments back. Comment matches recipient. We pass targetCommenterId
                                            viewModel.leaveComment(
                                                postId = post.id,
                                                content = commentText,
                                                preConsent = true,
                                                targetCommenterId = targetCommenterId,
                                                parentCommentId = replyingToComment?.id,
                                                parentCommentContent = replyingToComment?.content
                                            )
                                            replyingToComment = null
                                            commentText = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

        } else {
            // VISITOR VIEW: Show simple 1-on-1 private chat with the post creator
            Text(
                text = if (isChinese) "只有您和创作者 ${author.displayName} 可以看到此交流。不存在公开公共闲聊。" else "Only you and ${author.displayName} can see this conversation. There is no open public chat.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSlate,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = StealthSlate),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Chat stream representing just this commenter's conversation
                    val myThreadComments = comments.filter {
                        it.commenterId == currentUserId || (it.commenterId == post.authorId && it.targetCommenterId == currentUserId)
                    }

                    if (myThreadComments.isEmpty()) {
                        Text(
                            text = if (isChinese) "暂无往期幕后信件。在下方开启直接关联吧！" else "No previous backstage letters. Start the direct link below!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            myThreadComments.forEach { item ->
                                val isMyComment = item.commenterId == currentUserId
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (isMyComment) Alignment.End else Alignment.Start
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = if (isMyComment) Arrangement.End else Arrangement.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isMyComment) {
                                            CommentOptionsButton(
                                                item = item,
                                                onReplySelect = { replyingToComment = it },
                                                isChinese = isChinese
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = if (isMyComment) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else StealthSlate.copy(alpha = 0.8f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .border(BorderStroke(1.dp, if(isMyComment) MaterialTheme.colorScheme.primary else MutedSlate.copy(0.3f)), RoundedCornerShape(12.dp))
                                                .padding(10.dp)
                                        ) {
                                            Column {
                                                if (!item.parentCommentContent.isNullOrBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(bottom = 6.dp)
                                                            .background(MidnightNavy.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = "“${item.parentCommentContent}”",
                                                            fontSize = 11.sp,
                                                            color = MutedSlate,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                Text(
                                                    item.content,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = SoftLavender
                                                )
                                            }
                                        }

                                        if (!isMyComment) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            CommentOptionsButton(
                                                item = item,
                                                onReplySelect = { replyingToComment = it },
                                                isChinese = isChinese
                                            )
                                        }
                                    }
                                    
                                    // Citizen spotlight status info
                                    if (isMyComment) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (item.citizenConsentGiven) Icons.Default.CheckCircle else Icons.Default.Info,
                                                contentDescription = "Consent",
                                                tint = if (item.citizenConsentGiven) SafetyGreen else MutedSlate,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (item.citizenConsentGiven) {
                                                    if (isChinese) "已向作者授予匿名引用展示权" else "Consent to show anonymously granted"
                                                } else {
                                                    if (isChinese) "仅限私聊 (已取消授权)" else "Private-Only (Consent revoked)"
                                                },
                                                fontSize = 9.sp,
                                                color = if (item.citizenConsentGiven) SafetyGreen else MutedSlate
                                            )
                                            
                                            Spacer(modifier = Modifier.width(8.dp))
                                            // Toggle consent trigger
                                            TextButton(
                                                onClick = { viewModel.toggleConsentForComment(item.id, !item.citizenConsentGiven) },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(
                                                    text = if (item.citizenConsentGiven) {
                                                        if (isChinese) "撤销授权" else "Revoke Consent"
                                                    } else {
                                                        if (isChinese) "授予授权" else "Grant Consent"
                                                    },
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Form
                    if (replyingToComment != null) {
                        replyingToComment?.let { parentComment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MidnightNavy.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val commenterName = usersMap[parentComment.commenterId]?.displayName ?: parentComment.commenterName
                                Text(
                                    text = if (isChinese) "回复 @${commenterName}: “${parentComment.content}”" else "Replying to @${commenterName}: \"${parentComment.content}\"",
                                    fontSize = 11.sp,
                                    color = RadiantCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { replyingToComment = null },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel reply",
                                        tint = RadiantCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text(if (isChinese) "在幕后书写密语聊天..." else "Write private message backstage...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MutedSlate.copy(0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    viewModel.leaveComment(
                                        postId = post.id,
                                        content = commentText,
                                        preConsent = allowCitationCheckbox,
                                        parentCommentId = replyingToComment?.id,
                                        parentCommentContent = replyingToComment?.content
                                    )
                                    replyingToComment = null
                                    commentText = ""
                                }
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Comment",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Consent toggle before sending
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { allowCitationCheckbox = !allowCitationCheckbox }
                    ) {
                        Checkbox(
                            checked = allowCitationCheckbox,
                            onCheckedChange = { allowCitationCheckbox = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                        )
                        Column {
                            Text(
                                text = if (isChinese) "预先授予匿名引用特权" else "Pre-grant Anonymous Citation",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftLavender
                            )
                            Text(
                                text = if (isChinese) "允许作者在推荐看板发表此文本为匿名引用片段" else "Allow author to publish this as an anonymous Board citation snippet",
                                fontSize = 9.sp,
                                color = MutedSlate
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// PROFILE SCREEN
// -----------------------------------------------------------------------------
@Composable
fun ProfileScreen(
    viewModel: CyberViewModel,
    onReportUser: (String) -> Unit
) {
    val selectedProfileId by viewModel.selectedProfileId.collectAsStateWithLifecycle()
    val usersMap by viewModel.usersMap.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isChinese by viewModel.isChinese.collectAsStateWithLifecycle()

    val profileId = selectedProfileId ?: currentUserId
    val user = usersMap[profileId] ?: UserEntity(profileId, profileId, profileId)
    val isMe = currentUserId == profileId

    val followStatsFlow = remember(profileId) {
        viewModel.getProfileFollowStats(profileId)
    }
    val followStats by followStatsFlow.collectAsStateWithLifecycle(
        initialValue = FollowStats(0, 0, true)
    )

    var isFollowing by remember { mutableStateOf(false) }
    LaunchedEffect(currentUserId, profileId) {
        isFollowing = viewModel.checkIsFollowing(profileId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("profile_container")
    ) {
        // Back Button
        TextButton(
            onClick = { viewModel.navigateTo(CyberScreen.FEED) },
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isChinese) "返回广场舞台" else "Back to Stadium")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Avatar & info center
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.displayName.take(1).uppercase(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlate
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (!isMe) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.toggleFollow(profileId)
                                isFollowing = !isFollowing
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) StealthSlate else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = if (isFollowing) {
                                    if (isChinese) "取消关注" else "Unfollow"
                                } else {
                                    if (isChinese) "在幕后关注" else "Follow Backstage"
                                },
                                fontSize = 11.sp,
                                color = if (isFollowing) SoftLavender else MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        IconButton(
                            onClick = { onReportUser(profileId) },
                            modifier = Modifier.background(StealthSlate, CircleShape)
                        ) {
                            Icon(Icons.Default.Warning, "Report User", tint = AlarmCrimson, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bio Card
        user.bio?.let { bioText ->
            Card(
                colors = CardDefaults.cardColors(containerColor = StealthSlate),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(if (isChinese) "关于此位创作者" else "ABOUT THE CREATOR", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(bioText, style = MaterialTheme.typography.bodyMedium, color = SoftLavender)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RULE 1: THE NUMBERS EXIST, THE LISTS DO NOT
        Card(
            colors = CardDefaults.cardColors(containerColor = StealthSlate),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(if (isChinese) "剧场受众数据指标" else "THEATER AUDIENCE METRICS", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                Spacer(modifier = Modifier.height(12.dp))

                if (followStats.isVisibleToVisitor) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(followStats.followersCount.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            Text(if (isChinese) "受众粉丝" else "Followers", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                        }

                        Divider(modifier = Modifier.height(30.dp).width(1.dp), color = MutedSlate.copy(alpha = 0.3f))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(followStats.followingCount.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            Text(if (isChinese) "幕后关注" else "Following", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Lock, "Private Lists", tint = MutedSlate, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isChinese) "受众列表和数据已私有" else "Audience Lists and Counts are Private",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MutedSlate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isChinese) {
                        "核心政策：关注与被关注的实际受众真实身份被严格限制，仅供所有者本地查询。任何外部用户都无法随意窥探受众名单。"
                    } else {
                        "CRITICAL RULE: The actual identities of followers/following are strictly restricted and can only be queried by the owner. No external user can ever view followers lists."
                    },
                    fontSize = 9.sp,
                    color = MutedSlate,
                    lineHeight = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Author's post logs inside their theater
        Text(
            text = if (isChinese) {
                "${if (isMe) "你" else "其"}的广播记录"
            } else {
                "${if (isMe) "YOUR" else "THEIR"} BROADCAST LOGS"
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val authorPosts = posts.filter { it.authorId == profileId }
        if (authorPosts.isEmpty()) {
            Text(if (isChinese) "暂无公开广播记录。" else "No statement broadcasts logged.", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                authorPosts.forEach { post ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StealthSlate),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.viewPostDetail(post.id) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(post.content, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = SoftLavender)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Favorite, "Likes", tint = MutedSlate, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(post.likesCount.toString(), fontSize = 10.sp, color = MutedSlate)

                                Spacer(modifier = Modifier.width(12.dp))

                                Icon(Icons.Default.MailOutline, "Answers", tint = MutedSlate, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(post.commentsCount.toString(), fontSize = 10.sp, color = MutedSlate)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// SETTINGS SCREEN
// -----------------------------------------------------------------------------
@Composable
fun SettingsScreen(viewModel: CyberViewModel) {
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val usersMap by viewModel.usersMap.collectAsStateWithLifecycle()
    val isChinese by viewModel.isChinese.collectAsStateWithLifecycle()

    val user = usersMap[currentUserId] ?: UserEntity(currentUserId, currentUserId, currentUserId)

    var showFollows by remember(user) { mutableStateOf(user.showFollowCounts) }
    var allowCitationDefault by remember(user) { mutableStateOf(user.allowAnonymousCitationDefault) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("settings_container")
    ) {
        Text(
            text = if (isChinese) "隐私控制控制台" else "PRIVACY CONTROL CONSOLE",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (isChinese) "在您本地的安全配置文件数据库层中配置硬重写" else "Hardcode security overrides in your local profile database layer",
            style = MaterialTheme.typography.bodySmall,
            color = MutedSlate
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Switch 1: Follow Counts display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StealthSlate, RoundedCornerShape(12.dp))
                .clickable {
                    showFollows = !showFollows
                    viewModel.saveSettings(showFollows, allowCitationDefault)
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isChinese) "公开显示受众关注数量" else "Show Follower Counts Publicly", 
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
                    color = SoftLavender
                )
                Text(
                    text = if (isChinese) "若关闭，访客将看到“受众列表已私有”且粉丝数隐藏（所有者始终可见）" else "If toggled OFF, visitors see 'Audience Lists Private' and counts are hidden. (Owner can always see)", 
                    fontSize = 10.sp, 
                    color = MutedSlate
                )
            }
            Switch(
                checked = showFollows,
                onCheckedChange = {
                    showFollows = it
                    viewModel.saveSettings(showFollows, allowCitationDefault)
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Switch 2: Citation toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StealthSlate, RoundedCornerShape(12.dp))
                .clickable {
                    allowCitationDefault = !allowCitationDefault
                    viewModel.saveSettings(showFollows, allowCitationDefault)
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isChinese) "默认同意引用展示" else "Precheck Citation Permission", 
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
                    color = SoftLavender
                )
                Text(
                    text = if (isChinese) "勾选时，您发表的任何评论都会自动授权在推荐看板作为高亮匿名引用展示" else "When toggled ON, any comment you write automatically consents to anonymous showcasing on the Spotlight board.", 
                    fontSize = 10.sp, 
                    color = MutedSlate
                )
            }
            Switch(
                checked = allowCitationDefault,
                onCheckedChange = {
                    allowCitationDefault = it
                    viewModel.saveSettings(showFollows, allowCitationDefault)
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Language settings item (Chinese / English toggler)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StealthSlate, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isChinese) "系统语言设置" else "System Language Settings", 
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
                    color = SoftLavender
                )
                Text(
                    text = if (isChinese) "选择用户界面显示语言 (中文 / EN)" else "Choose display language (CN / EN)", 
                    fontSize = 10.sp, 
                    color = MutedSlate
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chinese Button
                TextButton(
                    onClick = { viewModel.setLanguage(true) },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (isChinese) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (isChinese) MaterialTheme.colorScheme.onPrimary else RadiantCyan
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("中文", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                
                // English Button
                TextButton(
                    onClick = { viewModel.setLanguage(false) },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (!isChinese) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (!isChinese) MaterialTheme.colorScheme.onPrimary else RadiantCyan
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("EN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // System Specs Overview card
        Card(
            colors = CardDefaults.cardColors(containerColor = StealthSlate),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isChinese) "CYBER-HIDEOUT 网络隐藏处后端政策" else "CYBER-HIDEOUT BACKEND POLICY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isChinese) {
                        "• 行行隔离保护（Row-Level Security）: 激活运行中\n• 单向隐私保护隔离屏（One-Way Sandbox）: 已启用\n• 双相合规匿名高亮（Dual-Consent Spotlighting）: 受保护\n• 行政人员一键通告/清理机制 (Admin Incidents): 支持"
                    } else {
                        "• Row-Level Security: Active\n• One-Way Backstage isolation: Engaged\n• Dual-Consent Citation popup triggers: Safe\n• Admin Incident dispatch links: 1-click active"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftLavender,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// SUBSIDIARY DIALOGS
// -----------------------------------------------------------------------------
@Composable
fun AddPostDialog(
    content: String,
    onContentChange: (String) -> Unit,
    selectedImageUri: String?,
    onSelectedImageUriChange: (String?) -> Unit,
    imageDeclaration: String?,
    onImageDeclarationChange: (String?) -> Unit,
    onPickTextFile: () -> Unit,
    onPickImage: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String, String?, String?) -> Unit,
    isChinese: Boolean
) {
    val context = LocalContext.current
    val presets = listOf(
        "https://images.unsplash.com/photo-1578894381163-e72c17f2d45f" to (if (isChinese) "霓虹街区" else "Neon Street"),
        "https://images.unsplash.com/photo-1549490349-8643362247b5" to (if (isChinese) "抽象赛博" else "Abstract"),
        "https://images.unsplash.com/photo-1509198397868-475647b2a1e5" to (if (isChinese) "全息数据" else "Hologram")
    )

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = StealthSlate),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isChinese) "发布新创意" else "CAST AN IDEA", 
                    style = MaterialTheme.typography.titleMedium, 
                    color = MaterialTheme.colorScheme.primary, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    placeholder = { 
                        Text(
                            text = if (isChinese) "畅所欲言... 你的数字空间里有什么在困扰你？" else "Speak freely... What has been clouding your digital clear-space?", 
                            fontSize = 13.sp
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MutedSlate.copy(0.4f)
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                // Text Upload Tool Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MidnightNavy.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable { onPickTextFile() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Share, 
                        contentDescription = "Upload Text", 
                        tint = RadiantCyan, 
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isChinese) "导入本地文本文件 (.txt)" else "Import local text file (.txt)",
                        fontSize = 11.sp,
                        color = RadiantCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isChinese) "附加配画创意 (可选，请选择本地或下方预设):" else "Attach Visual Artwork (Optional, select local/preset):",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftLavender,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Local Selector button
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(MidnightNavy.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, RadiantCyan.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                            .clickable { onPickImage() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, contentDescription = "Add image", tint = RadiantCyan, modifier = Modifier.size(14.dp))
                            Text(if (isChinese) "本地图片" else "Local", fontSize = 8.sp, color = RadiantCyan)
                        }
                    }

                    // Cyberpunk presets
                    presets.forEach { (url, label) ->
                        val isSelected = selectedImageUri == url
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StealthSlate)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MutedSlate.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onSelectedImageUriChange(url)
                                    if (imageDeclaration == null) {
                                        onImageDeclarationChange("others") // Default placeholder declaration
                                    }
                                },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(model = url),
                                contentDescription = label,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MidnightNavy.copy(alpha = 0.8f))
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, fontSize = 7.sp, color = SoftLavender)
                            }
                        }
                    }
                }

                // Image Source Copyright Declaration (MANDATORY if image is attached)
                if (selectedImageUri != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isChinese) "声明图片所属来源 (*必填):" else "Image Source Copyright (*Required):",
                        style = MaterialTheme.typography.labelSmall,
                        color = RadiantCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val declarations = listOf(
                            "self" to (if (isChinese) "作者自己" else "My Own/Original"),
                            "ai" to (if (isChinese) "AI生成" else "AI Generated"),
                            "others" to (if (isChinese) "他人照片" else "Other's Photo")
                        )
                        declarations.forEach { (type, label) ->
                            val isSelected = imageDeclaration == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else StealthSlate.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MutedSlate.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onImageDeclarationChange(type) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else SoftLavender,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    
                    // Button to clear image selection
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            onSelectedImageUriChange(null)
                            onImageDeclarationChange(null)
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (isChinese) "清除配图" else "Remove Image", color = AlarmCrimson, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text(if (isChinese) "取消" else "Cancel", color = MutedSlate)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val submitEnabled = content.isNotBlank() && (selectedImageUri == null || imageDeclaration != null)
                    Button(
                        onClick = { if (submitEnabled) onSubmit(content, selectedImageUri, imageDeclaration) },
                        enabled = submitEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MutedSlate.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = if (isChinese) "发布创意" else "Broadcast", 
                            color = if (submitEnabled) MaterialTheme.colorScheme.onPrimary else MutedSlate
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportUserDialog(
    userId: String,
    userName: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    isChinese: Boolean
) {
    var reason by remember { mutableStateOf("") }
    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = StealthSlate),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, "Moderation", tint = AlarmCrimson, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isChinese) "提交举报违规" else "REPORT INCIDENT", 
                        style = MaterialTheme.typography.titleMedium, 
                        color = AlarmCrimson, 
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isChinese) 
                        "正在举报 $userName (@$userId)。该事件将被立即分派至网络安全管理中心，用于核查与净化本剧场的异常交互表现。"
                        else "Filing a report against $userName (@$userId). Incidents are dispatched straight to the root administrator panel to terminate toxic participants instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftLavender
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { 
                        Text(
                            text = if (isChinese) "具体举报详情（如发布垃圾广告、骚扰、刷屏机器人等）..." else "Reason (e.g. cyberbullying, harassment, spam, bots)...", 
                            fontSize = 12.sp
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AlarmCrimson,
                        unfocusedBorderColor = MutedSlate.copy(0.4f)
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text(if (isChinese) "取消" else "Cancel", color = MutedSlate)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (reason.isNotBlank()) onSubmit(reason) },
                        colors = ButtonDefaults.buttonColors(containerColor = AlarmCrimson)
                    ) {
                        Text(if (isChinese) "提交" else "Submit Report", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CommentOptionsButton(
    item: com.example.data.CommentUiModel,
    onReplySelect: (com.example.data.CommentUiModel) -> Unit,
    isChinese: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = MutedSlate.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(StealthSlate)
        ) {
            DropdownMenuItem(
                text = { Text(if (isChinese) "选择并回复" else "Select & Reply", color = SoftLavender, fontSize = 12.sp) },
                onClick = {
                    onReplySelect(item)
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Reply",
                        tint = RadiantCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }
    }
}
