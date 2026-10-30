package com.example.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.activity.compose.BackHandler
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ai.ui.theme.AppTheme
import com.example.ai.ui.theme.DarkColorScheme
import com.example.ai.ui.theme.LightColorScheme

internal val OfflineAiColorScheme = DarkColorScheme

class MainActivity : ComponentActivity() {

    private var initialRouteState by mutableStateOf("chat_list")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val newChat = intent?.getBooleanExtra("new_chat", false) ?: false
        val openChat = intent?.getBooleanExtra("open_chat", false) ?: false
        val targetSessionId = intent?.getStringExtra("session_id")

        initialRouteState = when {
            newChat -> "chat_detail/new"
            openChat && !targetSessionId.isNullOrBlank() -> "chat_detail/$targetSessionId"
            else -> "chat_list"
        }

        setContent {
            val context = LocalContext.current
            var currentAppTheme by remember { mutableStateOf(SettingsRepository.getAppTheme(context)) }
            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = when (currentAppTheme) {
                1 -> false
                2 -> true
                else -> isSystemDark
            }

            AppTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OfflineAiApp(
                        initialRoute = initialRouteState,
                        onThemeChanged = { newTheme ->
                            currentAppTheme = newTheme
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newChat = intent.getBooleanExtra("new_chat", false)
        val openChat = intent.getBooleanExtra("open_chat", false)
        val targetSessionId = intent.getStringExtra("session_id")
        initialRouteState = when {
            newChat -> "chat_detail/new"
            openChat && !targetSessionId.isNullOrBlank() -> "chat_detail/$targetSessionId"
            else -> "chat_list"
        }
    }
}

@Composable
fun OfflineAiApp(
    initialRoute: String = "chat_list",
    onThemeChanged: (Int) -> Unit = {},
    llamaViewModel: LlamaViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel()
) {
    val navController = rememberNavController()
    val llamaState by llamaViewModel.uiState.collectAsStateWithLifecycle()
    val listState by chatViewModel.listState.collectAsStateWithLifecycle()
    val detailState by chatViewModel.detailState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showModelDialog by rememberSaveable { mutableStateOf(false) }
    var showAdvancedSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var currentSettings by remember { mutableStateOf(SettingsRepository.load(context)) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            llamaViewModel.importModel(uri)
        }
    }

    NavHost(
        navController = navController,
        startDestination = initialRoute
    ) {
        composable("chat_list") {
            // Ketika di ChatListScreen, tekan back akan me-minimize aplikasi (home launcher behavior)
            BackHandler {
                val activity = context as? ComponentActivity
                activity?.moveTaskToBack(true)
            }

            LaunchedEffect(Unit) {
                chatViewModel.loadSessions()
            }

            ChatListScreen(
                listState = listState,
                llamaState = llamaState,
                onOpenModelDialog = { showModelDialog = true },
                onOpenAdvancedSettings = {
                    currentSettings = SettingsRepository.load(context)
                    showAdvancedSettingsDialog = true
                },
                onSetExecutionMode = { mode ->
                    chatViewModel.setExecutionMode(mode)
                },
                onSetProfile = { profile ->
                    chatViewModel.setProfile(profile)
                },
                onCreateNewSession = {
                    val newId = chatViewModel.createNewSession()
                    chatViewModel.openSession(newId)
                    navController.navigate("chat_detail/$newId")
                },
                onOpenSession = { sessionId ->
                    chatViewModel.openSession(sessionId)
                    navController.navigate("chat_detail/$sessionId")
                },
                onRenameSession = { sessionId, newTitle ->
                    chatViewModel.renameSession(sessionId, newTitle)
                },
                onDeleteSession = { sessionId ->
                    chatViewModel.deleteSession(sessionId)
                },
                onSessionsChanged = {
                    chatViewModel.loadSessions()
                }
            )
        }

        composable("chat_detail/new") {
            LaunchedEffect(Unit) {
                val newId = chatViewModel.createNewSession()
                chatViewModel.openSession(newId)
                navController.navigate("chat_detail/$newId") {
                    popUpTo("chat_detail/new") { inclusive = true }
                }
            }
        }

        composable(
            route = "chat_detail/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable

            // Ketika di ChatDetailScreen, tekan back kembali ke ChatListScreen
            BackHandler {
                navController.popBackStack()
            }

            LaunchedEffect(sessionId) {
                chatViewModel.openSession(sessionId)
            }

            ChatDetailScreen(
                sessionId = sessionId,
                viewModel = chatViewModel,
                llamaViewModel = llamaViewModel,
                detailState = detailState,
                llamaState = llamaState,
                onBack = { navController.popBackStack() },
                onOpenModelDialog = { showModelDialog = true },
                onOpenAdvancedSettings = {
                    currentSettings = SettingsRepository.load(context)
                    showAdvancedSettingsDialog = true
                },
                onThemeChanged = onThemeChanged,
                onSendMessage = { text ->
                    chatViewModel.sendMessage(sessionId, text)
                },
                onDismissError = {
                    chatViewModel.clearDetailError()
                }
            )
        }
    }

    if (showModelDialog) {
        ModelSettingsDialog(
            llamaState = llamaState,
            onDismiss = { showModelDialog = false },
            onPickFile = {
                filePickerLauncher.launch(
                    arrayOf(
                        "application/octet-stream",
                        "application/x-gguf",
                        "*/*"
                    )
                )
            },
            onLoadModel = { llamaViewModel.loadModel() },
            onFreeModel = { llamaViewModel.freeModel() },
            onClearModel = { llamaViewModel.clearModel() },
            onOpenAdvancedSettings = {
                showModelDialog = false
                currentSettings = SettingsRepository.load(context)
                showAdvancedSettingsDialog = true
            }
        )
    }

    if (showAdvancedSettingsDialog) {
        AdvancedSettingsDialog(
            settings = currentSettings,
            llamaViewModel = llamaViewModel,
            onSettingsChange = { updated ->
                currentSettings = updated
                SettingsRepository.save(context, updated)
                onThemeChanged(updated.appTheme)
                showAdvancedSettingsDialog = false
                Toast.makeText(context, "Pengaturan lanjutan disimpan", Toast.LENGTH_SHORT).show()
            },
            onThemePreviewChange = { previewTheme ->
                onThemeChanged(previewTheme)
            },
            onDismiss = {
                onThemeChanged(currentSettings.appTheme)
                chatViewModel.refreshAvailableKitabs()
                showAdvancedSettingsDialog = false
            },
            onReset = {
                val defaults = SettingsRepository.reset(context)
                currentSettings = defaults
                onThemeChanged(defaults.appTheme)
                Toast.makeText(context, "Pengaturan dikembalikan ke default", Toast.LENGTH_SHORT)
                    .show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatListScreen(
    listState: ChatListState,
    llamaState: UiState,
    onOpenModelDialog: () -> Unit,
    onOpenAdvancedSettings: () -> Unit,
    onSetExecutionMode: (String) -> Unit = {},
    onSetProfile: (String) -> Unit = {},
    onCreateNewSession: () -> Unit,
    onOpenSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onSessionsChanged: suspend () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedSessionForOptions by remember { mutableStateOf<ChatSession?>(null) }
    var sessionToRename by remember { mutableStateOf<ChatSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }
    var renameText by remember { mutableStateOf("") }
    
    // Hidden gesture: tap 5x pada judul "Offline AI"
    var titleTapCount by remember { mutableStateOf(0) }
    var lastTapTimestamp by remember { mutableStateOf(0L) }
    var showHiddenLauncherSafetyDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .widthIn(max = 330.dp)
                        .testTag("execution_mode_selector_card")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Mode Eksekusi:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = listState.selectedExecutionMode == "CUSTOM",
                                onClick = { onSetExecutionMode("CUSTOM") },
                                label = {
                                    Text(
                                        text = "🔧 Shell Custom",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("mode_chip_custom")
                            )
                            FilterChip(
                                selected = listState.selectedExecutionMode == "NATIVE",
                                onClick = { onSetExecutionMode("NATIVE") },
                                label = {
                                    Text(
                                        text = "⚡ Shell Android",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                modifier = Modifier.testTag("mode_chip_native")
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (listState.selectedExecutionMode == "CUSTOM") {
                                "Aman, selalu ada feedback, cocok untuk Agent Loop"
                            } else {
                                "Akses penuh, output kadang kosong, gunakan hati-hati"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Profile Command:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${listState.activeProfileTemplateCount} template",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("profile_selector_row")
                        ) {
                            items(listState.availableProfiles) { profile ->
                                val isSelected = listState.selectedProfile.equals(profile, ignoreCase = true)
                                val icon = when (profile.lowercase()) {
                                    "whatsapp" -> "💬"
                                    "facebook" -> "👥"
                                    else -> "📁"
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetProfile(profile) },
                                    label = {
                                        Text(
                                            text = "$icon ${profile.replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("profile_chip_$profile")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dictionary: ${listState.activeProfileTemplateCount} template dari profile ${listState.selectedProfile}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            modifier = Modifier.testTag("profile_template_count_info")
                        )
                    }
                }

                ExtendedFloatingActionButton(
                    onClick = onCreateNewSession,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text("+ Chat Baru", fontWeight = FontWeight.Bold)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_new_chat")
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val now = System.currentTimeMillis()
                                if (now - lastTapTimestamp > 2000L) {
                                    titleTapCount = 1
                                } else {
                                    titleTapCount++
                                }
                                lastTapTimestamp = now
                                if (titleTapCount >= 5) {
                                    titleTapCount = 0
                                    showHiddenLauncherSafetyDialog = true
                                }
                            }
                            .testTag("app_title_row")
                    ) {
                        Text(
                            text = "Offline AI",
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (llamaState.isModelLoaded) Color(0xFF34D399)
                                    else Color(0xFF64748B)
                                )
                        )
                    }
                },
                actions = {
                    // Tombol kanan 1: "Model" (Memory / gear icon)
                    IconButton(
                        onClick = onOpenModelDialog,
                        modifier = Modifier.testTag("top_bar_model_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Model",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Tombol kanan 2: "Pengaturan Lanjutan" (Settings icon)
                    IconButton(
                        onClick = onOpenAdvancedSettings,
                        modifier = Modifier.testTag("top_bar_advanced_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan Lanjutan",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Tombol kanan 3: "Index Kitab" (Document icon yang membuka pengaturan lanjutan / index)
                    IconButton(
                        onClick = onOpenAdvancedSettings,
                        modifier = Modifier.testTag("top_bar_index_kitab_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Index Kitab",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (listState.sessions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum ada percakapan. Tekan + untuk memulai.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("empty_sessions_text")
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("sessions_lazy_column")
                ) {
                    items(
                        items = listState.sessions,
                        key = { it.id }
                    ) { session ->
                        val lastMessage = session.messages.lastOrNull()
                        val previewText = when {
                            lastMessage == null -> "Belum ada pesan"
                            lastMessage.role == "user" -> "Anda: ${lastMessage.content.replace('\n', ' ')}"
                            else -> lastMessage.content.replace('\n', ' ')
                        }
                        val avatarChar = session.title.trim().firstOrNull()?.uppercaseChar()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onOpenSession(session.id) },
                                    onLongClick = { selectedSessionForOptions = session }
                                )
                                .testTag("session_item_${session.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (avatarChar != null && session.title != "Percakapan Baru") {
                                        Text(
                                            text = avatarChar.toString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.SmartToy,
                                            contentDescription = "Avatar AI",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = session.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = formatSessionTimestamp(session.updatedAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = previewText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 80.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    selectedSessionForOptions?.let { session ->
        AlertDialog(
            onDismissRequest = { selectedSessionForOptions = null },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Dibuat: ${formatTanggal(session.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("option_dialog_created_at")
                    )
                    Text(
                        text = "${session.messages.size} pesan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("option_dialog_message_count")
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            renameText = session.title
                            sessionToRename = session
                            selectedSessionForOptions = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("option_rename_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✏️ Rename")
                    }

                    OutlinedButton(
                        onClick = {
                            sessionToDelete = session
                            selectedSessionForOptions = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("option_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🗑️ Hapus")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { selectedSessionForOptions = null },
                    modifier = Modifier.testTag("option_cancel_button")
                ) {
                    Text("Batal")
                }
            }
        )
    }

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = {
                Text(
                    text = "Hapus Percakapan?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Semua pesan di percakapan \"${session.title}\" akan hilang permanen. Tindakan ini tidak dapat dibatalkan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { sessionToDelete = null },
                    modifier = Modifier.testTag("confirm_delete_cancel_button")
                ) {
                    Text("Batal")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val deletedBackup = session
                        sessionToDelete = null
                        onDeleteSession(deletedBackup.id)
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Percakapan dihapus",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                val currentList = ChatStorage.loadSessions(context)
                                val restoredList = (listOf(deletedBackup) + currentList)
                                    .distinctBy { it.id }
                                    .sortedByDescending { it.updatedAt }
                                ChatStorage.saveSessions(context, restoredList)
                                onSessionsChanged()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    sessionToRename?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = { Text("Ubah Judul Percakapan") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Judul baru") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_input_field")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            onRenameSession(session.id, renameText)
                        }
                        sessionToRename = null
                    },
                    modifier = Modifier.testTag("confirm_rename_button")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showHiddenLauncherSafetyDialog) {
        AlertDialog(
            onDismissRequest = { showHiddenLauncherSafetyDialog = false },
            title = {
                Text(
                    text = "Pengaturan Home & Launcher (Safety)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Menu darurat untuk mengelola launcher atau keluar dari aplikasi:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = {
                            showHiddenLauncherSafetyDialog = false
                            SystemHomeManager.requestHomeRole(context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Set sebagai Launcher Default")
                    }

                    OutlinedButton(
                        onClick = {
                            showHiddenLauncherSafetyDialog = false
                            SystemHomeManager.launchDefaultLauncher(context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Buka Launcher Lain")
                    }

                    Button(
                        onClick = {
                            showHiddenLauncherSafetyDialog = false
                            val activity = context as? ComponentActivity
                            activity?.finish()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Keluar Aplikasi")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showHiddenLauncherSafetyDialog = false },
                    modifier = Modifier.testTag("dismiss_safety_dialog_button")
                ) {
                    Text("Tutup")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    sessionId: String,
    viewModel: ChatViewModel,
    llamaViewModel: LlamaViewModel = viewModel(),
    detailState: ChatDetailState,
    llamaState: UiState,
    onBack: () -> Unit,
    onOpenModelDialog: () -> Unit,
    onOpenAdvancedSettings: () -> Unit = {},
    onThemeChanged: (Int) -> Unit = {},
    onSendMessage: (String) -> Unit,
    onDismissError: () -> Unit,
    onExitToHome: () -> Unit = {}
) {
    val context = LocalContext.current
    var inputText by rememberSaveable(sessionId) { mutableStateOf("") }
    var currentSettings by remember {
        mutableStateOf(SettingsRepository.load(context))
    }
    var showLocalAdvancedDialog by rememberSaveable { mutableStateOf(false) }
    var showTaskQueueSheet by rememberSaveable { mutableStateOf(false) }
    var showAttachmentMenu by rememberSaveable { mutableStateOf(false) }
    var localTaskInput by rememberSaveable { mutableStateOf("") }

    val messages = detailState.session?.messages ?: emptyList()
    val lazyListState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val maxBubbleWidth = (configuration.screenWidthDp * 0.8f).dp

    val isModelLoaded = llamaState.isModelLoaded || ModelHandleHolder.handle != 0L
    val isEmbeddingLoaded = llamaState.isEmbeddingLoaded || ModelHandleHolder.embeddingHandle != 0L
    val isRetrievalOnly = detailState.isRetrievalOnlyMode || (!isModelLoaded && isEmbeddingLoaded)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.attachBitmap(context, bitmap)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.attachImage(context, uri)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.attachFile(context, uri)
        }
    }

    val attachedBitmap = remember(detailState.attachedImageUri) {
        detailState.attachedImageUri?.let { uri ->
            try {
                if (uri.scheme == "file") {
                    BitmapFactory.decodeFile(uri.path)?.asImageBitmap()
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }
            } catch (_: Throwable) {
                null
            }
        }
    }

    LaunchedEffect(messages.size, detailState.isGenerating, detailState.streamingText?.length) {
        val hasExtraBubble = detailState.isGenerating || detailState.streamingText != null
        val totalItems = messages.size + if (hasExtraBubble) 1 else 0
        if (totalItems > 0) {
            lazyListState.animateScrollToItem(totalItems - 1)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = detailState.session?.title ?: "Percakapan Baru",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            val mode = detailState.session?.executionMode ?: "CUSTOM"
                            val profile = detailState.session?.profileName ?: "general"
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (mode == "CUSTOM") {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                                modifier = Modifier.testTag("detail_execution_mode_badge")
                            ) {
                                Text(
                                    text = if (mode == "CUSTOM") {
                                        "🔧 Custom · 📁 ${profile.replaceFirstChar { it.uppercase() }}"
                                    } else {
                                        "⚡ Native · 📁 ${profile.replaceFirstChar { it.uppercase() }}"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (mode == "CUSTOM") {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Offline AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showTaskQueueSheet = true },
                        modifier = Modifier.testTag("task_queue_button")
                    ) {
                        val session = detailState.session
                        val taskCount = session?.taskQueue?.size ?: 0
                        val completedCount = session?.taskQueue?.count { it.isCompleted } ?: 0
                        Box(contentAlignment = Alignment.Center) {
                            Text("📋", fontSize = 20.sp)
                            if (taskCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (detailState.isTaskRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(16.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$completedCount/$taskCount",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onOpenModelDialog,
                        modifier = Modifier.testTag("detail_model_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Model",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    IconButton(
                        onClick = {
                            currentSettings = SettingsRepository.load(context)
                            showLocalAdvancedDialog = true
                        },
                        modifier = Modifier.testTag("top_bar_advanced_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                if (!isModelLoaded && !isEmbeddingLoaded) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("model_not_loaded_banner"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Model belum dimuat. Buka pengaturan Model dulu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onOpenModelDialog) {
                                Text(
                                    text = "Model",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else if (!isModelLoaded && isEmbeddingLoaded) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("retrieval_only_banner"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📖 Mode Cari di Kitab (Model Embedding aktif tanpa Chat Model)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onOpenModelDialog) {
                                Text(
                                    text = "Model",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = !detailState.error.isNullOrBlank()) {
                    detailState.error?.let { err ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("chat_error_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onDismissError,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Tutup Error",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // FilterChip Row untuk memilih kitab & mode pencarian
                if (detailState.availableKitabs.isNotEmpty() || (isModelLoaded && isEmbeddingLoaded)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        if (detailState.selectedKitab != null) {
                            Text(
                                text = "🔍 Mencari di: ${detailState.selectedKitab}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                                    .testTag("active_kitab_filter_header")
                            )
                        }
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kitab_filter_row"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isModelLoaded && isEmbeddingLoaded) {
                                item(key = "__MODE_TOGGLE__") {
                                    FilterChip(
                                        selected = detailState.isRetrievalOnlyMode,
                                        onClick = { viewModel.toggleRetrievalOnlyMode() },
                                        label = {
                                            Text(if (detailState.isRetrievalOnlyMode) "📖 Cari di Kitab" else "💬 Chat AI")
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("toggle_retrieval_mode_chip")
                                    )
                                }
                            }
                            if (detailState.availableKitabs.isNotEmpty()) {
                                item(key = "__ALL_KITABS__") {
                                    val isAllSelected = detailState.selectedKitab == null
                                    FilterChip(
                                        selected = isAllSelected,
                                        onClick = { viewModel.setSelectedKitab(null) },
                                        label = { Text("Semua") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("filter_chip_all")
                                    )
                                }
                                items(
                                    items = detailState.availableKitabs,
                                    key = { it }
                                ) { kitabName ->
                                    val isSelected = detailState.selectedKitab == kitabName
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setSelectedKitab(kitabName) },
                                        label = { Text(kitabName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("filter_chip_$kitabName")
                                    )
                                }
                            }
                        }
                    }
                }

                detailState.attachedFileName?.let { fileName ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("attached_file_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "File Lampiran",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearAttachment() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("clear_attachment_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus Lampiran",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (detailState.attachedImageBytes != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("attached_image_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (attachedBitmap != null) {
                                Image(
                                    bitmap = attachedBitmap,
                                    contentDescription = "Thumbnail Gambar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .testTag("attached_image_thumbnail")
                                )
                            } else {
                                Text("📷", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gambar Terlampir",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${detailState.attachedImageWidth} x ${detailState.attachedImageHeight} px · RGB888",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearAttachedImage() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("clear_image_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus Gambar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Root Shell Agent Control Bar & Turbo Mode (Toggle Switch & Tombol STOP)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("root_agent_control_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (detailState.rootAgentEnabled)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        else if (detailState.isTurboModeEnabled)
                            Color(0xFFFF9800).copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Switch(
                                    checked = detailState.rootAgentEnabled,
                                    onCheckedChange = { viewModel.setRootAgentEnabled(it) },
                                    enabled = detailState.isRootAvailable,
                                    modifier = Modifier.testTag("root_agent_switch")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Root Agent",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (detailState.rootAgentEnabled)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (detailState.isRootAvailable) "ROOT OK" else "TIDAK ADA ROOT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (detailState.isRootAvailable)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = if (!detailState.isRootAvailable)
                                            "Perangkat tidak memiliki akses root (su)"
                                        else if (detailState.isTaskRunning)
                                            "⚡ Antrean tugas bertahap sedang berjalan..."
                                        else if (detailState.rootAgentEnabled)
                                            "Eksperimen kontrol layar aktif (otomatis jalankan [SHELL])"
                                        else
                                            "Kontrol layar via perintah shell",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val tQueue = detailState.session?.taskQueue ?: emptyList()
                                    if (tQueue.isNotEmpty()) {
                                        val done = tQueue.count { it.isCompleted }
                                        Text(
                                            text = "📋 Antrean: $done/${tQueue.size} tugas selesai",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (detailState.isTaskRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (detailState.rootAgentEnabled || detailState.isTaskRunning) {
                                Button(
                                    onClick = {
                                        viewModel.stopTaskQueue()
                                        viewModel.stopRootAgent()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .testTag("root_agent_stop_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "STOP",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "STOP",
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // Turbo Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "⚡ Turbo Mode",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (detailState.isTurboModeEnabled) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (detailState.isTurboModeEnabled) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFF9800).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "PERFORMANCE",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = Color(0xFFE65100),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "CPU Performance + Drop Cache (Root)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = detailState.isTurboModeEnabled,
                                onCheckedChange = { viewModel.toggleTurboMode() },
                                enabled = detailState.isRootAvailable,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFFF9800),
                                    checkedTrackColor = Color(0xFFFFE0B2)
                                ),
                                modifier = Modifier.testTag("turbo_mode_switch")
                            )
                        }
                    }
                }

                // Web Search & Turbo Quick Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = detailState.isWebSearchEnabled,
                        onClick = { viewModel.toggleWebSearch() },
                        label = {
                            Text(
                                text = "Cari",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (detailState.isWebSearchEnabled) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Web Search",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("toggle_web_search_chip")
                    )

                    FilterChip(
                        selected = detailState.isTurboModeEnabled,
                        onClick = { viewModel.toggleTurboMode() },
                        label = {
                            Text(
                                text = "⚡ Turbo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (detailState.isTurboModeEnabled) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF9800).copy(alpha = 0.22f),
                            selectedLabelColor = Color(0xFFE65100),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("toggle_turbo_mode_chip")
                    )

                    if (detailState.isSearchingWeb) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Mencari di internet...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { showTaskQueueSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("open_tasks_bottom_button")
                    ) {
                        Text("📋", fontSize = 20.sp)
                    }

                    IconButton(
                        onClick = { showAttachmentMenu = true },
                        enabled = !detailState.isGenerating,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("attach_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Lampirkan Media atau File",
                            tint = if (!detailState.isGenerating) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input"),
                        placeholder = {
                            Text(if (isRetrievalOnly) "Cari di kitab..." else "Ketik pesan...")
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    val canSend =
                        (inputText.isNotBlank() || detailState.attachedImageBytes != null) &&
                                !detailState.isGenerating &&
                                (isModelLoaded || isEmbeddingLoaded || (detailState.attachedImageBytes != null && ModelHandleHolder.visionHandle != 0L))

                    IconButton(
                        onClick = {
                            val textToSend = inputText.ifBlank {
                                if (detailState.attachedImageBytes != null) "Tolong jelaskan dan analisa gambar ini." else ""
                            }
                            inputText = ""
                            onSendMessage(textToSend)
                        },
                        enabled = canSend,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim Pesan",
                            tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("messages_lazy_column"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = messages,
                key = { it.id }
            ) { message ->
                val isUser = message.role == "user"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = maxBubbleWidth),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isUser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            if (!message.attachmentName.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isUser) {
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                                            } else {
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("message_attachment_badge_${message.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = message.attachmentName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            SelectionContainer {
                                if (isUser) {
                                    Text(
                                        text = message.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Markdown(
                                        content = message.content,
                                        colors = markdownColor(
                                            text = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        typography = markdownTypography(
                                            text = MaterialTheme.typography.bodyMedium,
                                            h1 = MaterialTheme.typography.headlineMedium,
                                            h2 = MaterialTheme.typography.headlineSmall,
                                            h3 = MaterialTheme.typography.titleLarge,
                                            h4 = MaterialTheme.typography.titleMedium,
                                            h5 = MaterialTheme.typography.titleSmall,
                                            h6 = MaterialTheme.typography.titleSmall,
                                            paragraph = MaterialTheme.typography.bodyMedium,
                                            code = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Monospace
                                            )
                                        ),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            if (!isUser && !message.shellOutput.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("message_shell_output_${message.id}"),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                ) {
                                    SelectionContainer {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "⚙️ Output Shell:",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = message.shellOutput,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            if (!isUser && message.sources.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "📚 Sumber: ${message.sources.joinToString(", ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .testTag("message_sources_${message.id}")
                                )
                            }
                            if (!isUser && !message.webSources.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                        .testTag("message_web_sources_${message.id}"),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Sumber Referensi Web:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    message.webSources.forEachIndexed { sIdx, source ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url)).apply {
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        context.startActivity(intent)
                                                    } catch (_: Throwable) {
                                                        Toast.makeText(
                                                            context,
                                                            "Tidak dapat membuka tautan",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                                .padding(vertical = 4.dp, horizontal = 4.dp)
                                                .testTag("web_source_link_${message.id}_$sIdx")
                                        ) {
                                            Text(
                                                text = "🌐",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = source.title.ifBlank { source.url },
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    textDecoration = TextDecoration.Underline,
                                                    color = MaterialTheme.colorScheme.primary
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatBubbleTime(message.timestamp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = if (isUser) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                },
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }

            val streamingContent = detailState.streamingText
            if (!streamingContent.isNullOrEmpty()) {
                item(key = "streaming_assistant_bubble") {
                    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
                    val cursorAlpha by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 500),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "cursor_alpha"
                    )
                    val cursorSymbol = if (cursorAlpha > 0.5f) " ▌" else "  "

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Surface(
                            modifier = Modifier
                                .widthIn(max = maxBubbleWidth)
                                .testTag("streaming_assistant_bubble"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                if (detailState.selectedKitab != null) {
                                    Text(
                                        text = "🔍 Mencari di: ${detailState.selectedKitab}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                SelectionContainer {
                                    Markdown(
                                        content = streamingContent + cursorSymbol,
                                        colors = markdownColor(
                                            text = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        typography = markdownTypography(
                                            text = MaterialTheme.typography.bodyMedium,
                                            h1 = MaterialTheme.typography.headlineMedium,
                                            h2 = MaterialTheme.typography.headlineSmall,
                                            h3 = MaterialTheme.typography.titleLarge,
                                            h4 = MaterialTheme.typography.titleMedium,
                                            h5 = MaterialTheme.typography.titleSmall,
                                            h6 = MaterialTheme.typography.titleSmall,
                                            paragraph = MaterialTheme.typography.bodyMedium,
                                            code = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Monospace
                                            )
                                        ),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (detailState.isGenerating) {
                item(key = "typing_indicator") {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Surface(
                            modifier = Modifier
                                .widthIn(max = maxBubbleWidth)
                                .testTag("typing_indicator_bubble"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (detailState.isSearchingWeb) "🌐 Mencari di internet... ▌" else "AI sedang mengetik... ▌",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLocalAdvancedDialog) {
        AdvancedSettingsDialog(
            settings = currentSettings,
            llamaViewModel = llamaViewModel,
            onSettingsChange = { updated ->
                currentSettings = updated
                SettingsRepository.save(context, updated)
                onThemeChanged(updated.appTheme)
                showLocalAdvancedDialog = false
                Toast.makeText(context, "Pengaturan lanjutan disimpan", Toast.LENGTH_SHORT).show()
            },
            onThemePreviewChange = { previewTheme ->
                onThemeChanged(previewTheme)
            },
            onDismiss = {
                onThemeChanged(currentSettings.appTheme)
                viewModel.refreshAvailableKitabs()
                showLocalAdvancedDialog = false
            },
            onReset = {
                val defaults = SettingsRepository.reset(context)
                currentSettings = defaults
                onThemeChanged(defaults.appTheme)
                Toast.makeText(context, "Pengaturan dikembalikan ke default", Toast.LENGTH_SHORT)
                    .show()
            }
        )
    }

    if (showAttachmentMenu) {
        val attachmentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAttachmentMenu = false },
            sheetState = attachmentSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("attachment_menu_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Lampirkan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Item 1: Kamera
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Kamera",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Ambil foto langsung dengan kamera",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Kamera",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showAttachmentMenu = false
                            cameraLauncher.launch(null)
                        }
                        .testTag("attachment_option_camera")
                )

                // Item 2: Foto
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Foto",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Pilih gambar dari galeri perangkat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Photo,
                                contentDescription = "Foto",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showAttachmentMenu = false
                            imagePickerLauncher.launch("image/*")
                        }
                        .testTag("attachment_option_photo")
                )

                // Item 3: File
                ListItem(
                    headlineContent = {
                        Text(
                            text = "File",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Pilih dokumen teks atau file lainnya",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "File",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showAttachmentMenu = false
                            filePickerLauncher.launch("*/*")
                        }
                        .testTag("attachment_option_file")
                )
            }
        }
    }

    if (showTaskQueueSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val session = detailState.session
        val currentTasks = session?.taskQueue ?: emptyList()
        val currentIndex = session?.currentTaskIndex ?: 0

        ModalBottomSheet(
            onDismissRequest = { showTaskQueueSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("task_queue_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "📋 Antrean Tugas Agent",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tugas akan dieksekusi berurutan via shell",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showTaskQueueSheet = false },
                        modifier = Modifier.testTag("close_task_queue_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (detailState.isTaskRunning) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Antrean Sedang Berjalan...",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                val runningDesc = currentTasks.getOrNull(currentIndex)?.description ?: ""
                                Text(
                                    text = "Tugas ${currentIndex + 1}/${currentTasks.size}: $runningDesc",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.stopTaskQueue()
                                    viewModel.stopRootAgent()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("stop_queue_in_sheet_button")
                            ) {
                                Text("Hentikan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Input tugas baru
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = localTaskInput,
                        onValueChange = { localTaskInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_task_input"),
                        placeholder = {
                            Text("Tambah tugas, misal: Buka WhatsApp", fontSize = 13.sp)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Button(
                        onClick = {
                            if (localTaskInput.isNotBlank()) {
                                viewModel.addTask(localTaskInput, sessionId)
                                localTaskInput = ""
                            }
                        },
                        enabled = localTaskInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_task_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Tugas",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Daftar Tugas
                if (currentTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada tugas dalam antrean.\nKetik deskripsi tugas di atas lalu tekan Tambah.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = currentTasks, key = { it.id }) { task ->
                            val taskPos = currentTasks.indexOf(task)
                            val isCurrentlyRunning = detailState.isTaskRunning && taskPos == currentIndex

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("task_card_${task.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        isCurrentlyRunning -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = when {
                                                task.isCompleted -> MaterialTheme.colorScheme.tertiary
                                                isCurrentlyRunning -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.outline
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${taskPos + 1}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    ),
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = when {
                                                    task.isCompleted -> "✅ Selesai"
                                                    isCurrentlyRunning -> "⏳ Sedang dieksekusi..."
                                                    else -> "⏳ Menunggu"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = when {
                                                    task.isCompleted -> MaterialTheme.colorScheme.tertiary
                                                    isCurrentlyRunning -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeTask(task.id, sessionId) },
                                        enabled = !detailState.isTaskRunning || !isCurrentlyRunning,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_task_${task.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus Tugas",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom actions: Hapus Semua & Mulai Eksekusi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearTasks(sessionId) },
                        enabled = currentTasks.isNotEmpty() && !detailState.isTaskRunning,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("clear_all_tasks_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Semua",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus Semua")
                    }

                    Button(
                        onClick = {
                            viewModel.startTaskQueue(sessionId)
                            showTaskQueueSheet = false
                        },
                        enabled = currentTasks.isNotEmpty() && !detailState.isTaskRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("start_task_queue_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Mulai Eksekusi",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentTasks.all { it.isCompleted }) "Ulangi Eksekusi" else "Mulai Eksekusi",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModelSettingsDialog(
    llamaState: UiState,
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
    onLoadModel: () -> Unit,
    onFreeModel: () -> Unit,
    onClearModel: () -> Unit,
    onOpenAdvancedSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val isModelReady = remember(llamaState.statusText, llamaState.isLoading, llamaState.isModelLoaded) {
        ModelRepository.isModelReady(context)
    }
    val currentSettings = remember(llamaState.statusText, llamaState.isModelLoaded) {
        SettingsRepository.load(context)
    }
    val modelSizeMb = remember(llamaState.statusText, llamaState.isModelLoaded) {
        ModelRepository.getModelFile(context)?.let { it.length() / (1024 * 1024) } ?: 0L
    }
    val handleHex = remember(llamaState.statusText, llamaState.isModelLoaded) {
        val h = ModelHandleHolder.handle
        if (h != 0L) "0x${h.toString(16)}" else "0x0"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Pengaturan Model",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (llamaState.isModelLoaded) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Model: ${if (isModelReady) "Siap" else "Belum diimpor"}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Handle: $handleHex • $modelSizeMb MB • ${currentSettings.nThreads} Threads • Ctx ${currentSettings.nCtx}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = llamaState.statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (llamaState.isLoading) {
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        llamaState.error?.let { err ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onPickFile,
                    enabled = !llamaState.isLoading && !llamaState.isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_pick_model_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih File Model (.gguf)")
                }

                Button(
                    onClick = onLoadModel,
                    enabled = !llamaState.isLoading && !llamaState.isModelLoaded,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_load_model_button")
                ) {
                    Text("Load Model")
                }

                OutlinedButton(
                    onClick = onFreeModel,
                    enabled = llamaState.isModelLoaded && !llamaState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_free_model_button")
                ) {
                    Text("Free Model")
                }

                OutlinedButton(
                    onClick = onClearModel,
                    enabled = (llamaState.isModelLoaded || isModelReady) && !llamaState.isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_clear_model_button")
                ) {
                    Text("Hapus Model")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                OutlinedButton(
                    onClick = onOpenAdvancedSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_advanced_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⚙️ Pengaturan Lanjutan")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_close_button")
            ) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun AdvancedSettingsDialog(
    settings: GenerationSettings,
    llamaViewModel: LlamaViewModel = viewModel(),
    onSettingsChange: (GenerationSettings) -> Unit,
    onThemePreviewChange: (Int) -> Unit = {},
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val llamaState by llamaViewModel.uiState.collectAsStateWithLifecycle()
    val indexingState by IndexingManager.indexingState.collectAsStateWithLifecycle()
    val commandTemplates by CommandDictionary.templates.collectAsStateWithLifecycle()

    val commandTemplatePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val res = CommandDictionary.importTemplateFile(context, uri)
                if (res.isSuccess) {
                    Toast.makeText(context, res.getOrNull() ?: "Template berhasil diimpor", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal impor: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    var selectedEmbeddingUri by remember { mutableStateOf<Uri?>(null) }
    var indexedChunkCount by remember(indexingState.isIndexing, indexingState.currentChunk) {
        mutableStateOf(
            try {
                VectorStore.init(context)
                VectorStore.getChunkCount()
            } catch (_: Throwable) {
                0
            }
        )
    }
    var indexedFilesWithCounts by remember(indexingState.isIndexing, indexingState.currentChunk, indexedChunkCount) {
        mutableStateOf<List<Pair<String, Int>>>(
            try {
                VectorStore.init(context)
                VectorStore.getIndexedFiles().map { kitab ->
                    kitab to VectorStore.getChunkCountByKitab(kitab)
                }
            } catch (_: Throwable) {
                emptyList()
            }
        )
    }

    val embeddingPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedEmbeddingUri = uri
            llamaViewModel.loadEmbeddingModel(uri)
        }
    }

    val indexKitabPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            llamaViewModel.startIndexingFiles(uris)
        }
    }

    var appThemeValue by remember(settings) { mutableStateOf(settings.appTheme) }
    var systemPromptText by remember(settings) { mutableStateOf(settings.systemPrompt) }
    var agentSystemPromptText by remember(settings) { mutableStateOf(settings.agentSystemPrompt) }
    var chatmlTemplateText by remember(settings) { mutableStateOf(settings.chatmlTemplate) }
    var maxTokensText by remember(settings) { mutableStateOf(settings.maxTokens.toString()) }
    var temperatureValue by remember(settings) { mutableStateOf(settings.temperature) }
    var topKValue by remember(settings) { mutableStateOf(settings.topK) }
    var nCtxText by remember(settings) { mutableStateOf(settings.nCtx.toString()) }
    var nThreadsText by remember(settings) { mutableStateOf(settings.nThreads.toString()) }
    var validationErrorMessage by remember { mutableStateOf<String?>(null) }

    val parsedNCtx = nCtxText.toIntOrNull()
    val isNCtxError = parsedNCtx == null || parsedNCtx < 128
    val nCtxSupportingText = when {
        nCtxText.isBlank() || parsedNCtx == null -> "Masukkan angka minimal 128"
        parsedNCtx < 128 -> "Minimum 128"
        parsedNCtx <= 32768 -> "Semakin besar, semakin banyak RAM. Perlu reload model."
        else -> "⚠️ Nilai sangat besar. Kemungkinan gagal load di HP ini."
    }

    val parsedNThreads = nThreadsText.toIntOrNull()
    val isNThreadsError = parsedNThreads == null || parsedNThreads < 1
    val nThreadsSupportingText = when {
        nThreadsText.isBlank() || parsedNThreads == null -> "Masukkan angka minimal 1"
        parsedNThreads < 1 -> "Minimum 1"
        parsedNThreads <= 16 -> "Sesuaikan dengan jumlah core CPU HP"
        else -> "⚠️ Nilai sangat besar, bisa memperlambat."
    }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pengaturan Lanjutan",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section: Tema Tampilan (Light / Dark Mode)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_selection_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Tema Tampilan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pilih mode terang, gelap, atau ikuti pengaturan sistem:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val themeList = listOf(
                            0 to "Sistem",
                            1 to "Terang",
                            2 to "Gelap"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            themeList.forEach { (mode, label) ->
                                val isSelected = appThemeValue == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        appThemeValue = mode
                                        onThemePreviewChange(mode)
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        val icon = when (mode) {
                                            1 -> "☀️"
                                            2 -> "🌙"
                                            else -> "⚙️"
                                        }
                                        Text(icon, fontSize = 11.sp)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_chip_$mode")
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Field 1: System Prompt (Persona AI)
                OutlinedTextField(
                    value = systemPromptText,
                    onValueChange = { systemPromptText = it },
                    label = { Text("System Prompt (Persona AI)") },
                    minLines = 4,
                    maxLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advanced_system_prompt_input")
                )

                // Field 1b: Agent Prompt (Instruksi Shell)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Agent Prompt (Instruksi Shell)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = {
                                agentSystemPromptText = GenerationSettingsDefaults.DEFAULT_AGENT_SYSTEM_PROMPT
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.testTag("reset_agent_prompt_button")
                        ) {
                            Text("Reset ke Default", fontSize = 11.sp)
                        }
                    }
                    OutlinedTextField(
                        value = agentSystemPromptText,
                        onValueChange = { agentSystemPromptText = it },
                        label = { Text("Agent Prompt (Instruksi Shell)") },
                        placeholder = {
                            Text("Instruksi perintah [SHELL] untuk mode Agent...")
                        },
                        supportingText = {
                            Text("Instruksi ini hanya dikirim ke AI jika mode Agent/Root diaktifkan.")
                        },
                        minLines = 5,
                        maxLines = 12,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("advanced_agent_system_prompt_input")
                    )
                }

                // Field 2: ChatML Template
                OutlinedTextField(
                    value = chatmlTemplateText,
                    onValueChange = { chatmlTemplateText = it },
                    label = { Text("ChatML Template") },
                    supportingText = {
                        Text("Gunakan {system} dan {user} sebagai placeholder")
                    },
                    minLines = 4,
                    maxLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advanced_chatml_template_input")
                )

                // Field 3: Max Tokens
                OutlinedTextField(
                    value = maxTokensText,
                    onValueChange = { newValue ->
                        val digitsOnly = newValue.filter { it.isDigit() }
                        maxTokensText = digitsOnly
                    },
                    label = { Text("Max Tokens (panjang output)") },
                    supportingText = {
                        Text("Range: 32 - 800512")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advanced_max_tokens_input")
                )

                // Field 4: Temperature
                Column(modifier = Modifier.fillMaxWidth()) {
                    val formattedTemp = String.format(Locale.US, "%.1f", temperatureValue)
                    Text(
                        text = "Temperature: $formattedTemp",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = temperatureValue,
                        onValueChange = { raw ->
                            temperatureValue = ((raw * 10f).roundToInt() / 10f).coerceIn(0.0f, 2.0f)
                        },
                        valueRange = 0.0f..2.0f,
                        steps = 19,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("advanced_temperature_slider")
                    )
                    Text(
                        text = "0.0 = faktual, 1.0 = seimbang, 2.0 = kreatif",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Field 5: Top K
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Top K: $topKValue",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = topKValue.toFloat(),
                        onValueChange = { raw ->
                            topKValue = raw.roundToInt().coerceIn(0, 100)
                        },
                        valueRange = 0f..100f,
                        steps = 99,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("advanced_topk_slider")
                    )
                    Text(
                        text = "0 = disabled, 40 = default",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Field A: Context Size (nCtx) - OutlinedTextField tanpa batas atas
                OutlinedTextField(
                    value = nCtxText,
                    onValueChange = {
                        nCtxText = it
                        validationErrorMessage = null
                    },
                    label = { Text("Context Size (nCtx)") },
                    isError = isNCtxError,
                    supportingText = {
                        Text(nCtxSupportingText)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advanced_nctx_input")
                )

                // Field B: Jumlah Thread (nThreads) - OutlinedTextField tanpa batas atas
                OutlinedTextField(
                    value = nThreadsText,
                    onValueChange = {
                        nThreadsText = it
                        validationErrorMessage = null
                    },
                    label = { Text("Jumlah Thread") },
                    isError = isNThreadsError,
                    supportingText = {
                        Text(nThreadsSupportingText)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advanced_nthreads_input")
                )

                // Nasihat & Warning untuk user
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "💡 Nilai terlalu besar bisa gagal load. Kalau gagal, kurangi nilainya. Setelah ubah nilai ini, tekan Free Model lalu Load Model.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // Section Baru: Model Embedding (untuk RAG)
                Text(
                    text = "Model Embedding (untuk RAG)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val isEmbeddingLoaded =
                    llamaState.isEmbeddingLoaded || ModelHandleHolder.embeddingHandle != 0L

                Text(
                    text = "Model Embedding: ${if (isEmbeddingLoaded) "Siap" else "Belum dimuat"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEmbeddingLoaded) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("embedding_status_text")
                )

                Text(
                    text = "Index: $indexedChunkCount chunks dari ${indexedFilesWithCounts.size} file",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("index_summary_text")
                )

                if (indexedFilesWithCounts.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("indexed_files_list_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            indexedFilesWithCounts.forEach { (kitabName, count) ->
                                Text(
                                    text = "📖 $kitabName ($count chunks)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        embeddingPickerLauncher.launch(
                            arrayOf("application/octet-stream", "application/x-gguf", "*/*")
                        )
                    },
                    enabled = !llamaState.isLoading && !indexingState.isIndexing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_embedding_model_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih File Model Embedding (.gguf)")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { llamaViewModel.loadEmbeddingModel(selectedEmbeddingUri) },
                        enabled = !llamaState.isLoading && !isEmbeddingLoaded,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("load_embedding_button")
                    ) {
                        Text("Load Embedding", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { llamaViewModel.freeEmbeddingModel() },
                        enabled = isEmbeddingLoaded && !indexingState.isIndexing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("free_embedding_button")
                    ) {
                        Text("Free Embedding", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = {
                        indexKitabPickerLauncher.launch(
                            arrayOf("text/plain", "text/*", "application/octet-stream")
                        )
                    },
                    enabled = isEmbeddingLoaded && !indexingState.isIndexing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("index_kitab_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Index File .txt")
                }

                if (indexingState.isIndexing) {
                    val progressFraction = if (indexingState.totalChunks > 0) {
                        (indexingState.currentChunk.toFloat() / indexingState.totalChunks.toFloat())
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("indexing_progress_bar")
                    )
                    Text(
                        text = "Memproses file ${indexingState.processedFiles}/${indexingState.totalFiles} — ${indexingState.currentFile} (chunk ${indexingState.currentChunk}/${indexingState.totalChunks})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.testTag("indexing_status_text")
                    )
                }

                indexingState.error?.let { idxErr ->
                    Text(
                        text = idxErr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedButton(
                    onClick = {
                        VectorStore.init(context)
                        VectorStore.clearAll()
                        indexedChunkCount = 0
                        indexedFilesWithCounts = emptyList()
                        Toast.makeText(context, "Index kitab dihapus", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !indexingState.isIndexing && indexedChunkCount > 0,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_index_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus Index")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // Section: Custom Command Dictionary (Agent Loop)
                Text(
                    text = "Custom Command Dictionary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Template aktif: ${commandTemplates.size} perintah",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (commandTemplates.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("command_templates_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            commandTemplates.take(5).forEach { template ->
                                Text(
                                    text = "⚡ `${template.commandPattern}`: ${template.description.ifEmpty { "Custom template" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (commandTemplates.size > 5) {
                                Text(
                                    text = "+ ${commandTemplates.size - 5} template lainnya",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        commandTemplatePickerLauncher.launch(
                            arrayOf("application/json", "text/plain", "*/*")
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_command_template_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Template JSON")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                validationErrorMessage?.let { errMsg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("advanced_validation_error_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = errMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Tombol: "Reset ke Default"
                OutlinedButton(
                    onClick = {
                        onReset()
                        systemPromptText = GenerationSettingsDefaults.DEFAULT_SYSTEM_PROMPT
                        agentSystemPromptText = GenerationSettingsDefaults.DEFAULT_AGENT_SYSTEM_PROMPT
                        chatmlTemplateText = GenerationSettingsDefaults.DEFAULT_CHATML_TEMPLATE
                        maxTokensText = GenerationSettingsDefaults.DEFAULT_MAX_TOKENS.toString()
                        temperatureValue = GenerationSettingsDefaults.DEFAULT_TEMPERATURE
                        topKValue = GenerationSettingsDefaults.DEFAULT_TOP_K
                        nCtxText = GenerationSettingsDefaults.DEFAULT_N_CTX.toString()
                        nThreadsText = GenerationSettingsDefaults.DEFAULT_N_THREADS.toString()
                        appThemeValue = GenerationSettingsDefaults.DEFAULT_APP_THEME
                        onThemePreviewChange(GenerationSettingsDefaults.DEFAULT_APP_THEME)
                        validationErrorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advanced_reset_button")
                ) {
                    Text("Reset ke Default")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validNCtx = nCtxText.trim().toIntOrNull()
                    val validNThreads = nThreadsText.trim().toIntOrNull()

                    if (validNCtx == null || validNCtx < 128) {
                        val msg = "Context Size (nCtx) harus berupa angka valid minimal 128"
                        validationErrorMessage = msg
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (validNThreads == null || validNThreads < 1) {
                        val msg = "Jumlah Thread harus berupa angka valid minimal 1"
                        validationErrorMessage = msg
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val parsedMaxTokens = maxTokensText.toIntOrNull()
                        ?.coerceIn(32, 800512)
                        ?: GenerationSettingsDefaults.DEFAULT_MAX_TOKENS

                    val finalSettings = GenerationSettings(
                        systemPrompt = systemPromptText.ifBlank {
                            GenerationSettingsDefaults.DEFAULT_SYSTEM_PROMPT
                        },
                        agentSystemPrompt = agentSystemPromptText.ifBlank {
                            GenerationSettingsDefaults.DEFAULT_AGENT_SYSTEM_PROMPT
                        },
                        chatmlTemplate = chatmlTemplateText.ifBlank {
                            GenerationSettingsDefaults.DEFAULT_CHATML_TEMPLATE
                        },
                        maxTokens = parsedMaxTokens,
                        temperature = ((temperatureValue * 10f).roundToInt() / 10f).coerceIn(0.0f, 2.0f),
                        topK = topKValue.coerceIn(0, 100),
                        nCtx = validNCtx,
                        nThreads = validNThreads,
                        appTheme = appThemeValue
                    )
                    onSettingsChange(finalSettings)
                },
                modifier = Modifier.testTag("advanced_save_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("advanced_cancel_button")
            ) {
                Text("Batal")
            }
        }
    )
}

private fun formatTanggal(timestamp: Long): String {
    return SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun formatSessionTimestamp(timestamp: Long): String {
    val nowCal = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val isSameDay = nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
        nowCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)

    val pattern = if (isSameDay) "HH:mm" else "dd/MM"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}

private fun formatBubbleTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
