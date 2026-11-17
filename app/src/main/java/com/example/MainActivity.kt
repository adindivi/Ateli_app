package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.ZigzagCanvas
import com.example.ui.theme.ZigzagHairline
import com.example.ui.theme.ZigzagStrong
import com.example.ui.theme.ZigzagForeground
import com.example.ui.theme.ZigzagMuted
import com.example.ui.theme.ZigzagTabBarLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.configureAppCheck
import com.example.data.local.LocalFittingHistoryRepository
import com.example.data.local.SmartCoordiLocalDatabase
import com.example.data.repository.WardrobeRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AiStylistChatScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.TpoStylingScreen
import com.example.ui.screens.VirtualFittingScreen
import com.example.ui.screens.WardrobeScreen
import com.example.ui.theme.SmartCoordiTheme
import com.example.viewmodel.SmartCoordiViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        // STEP 2: Initialize Compose UI with Navigation-Level Auth Gating
        setContent {
            SmartCoordiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    var guestUserId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    val activeUid = user?.uid ?: guestUserId

    if (activeUid == null) {
        AuthScreen(
            onAuthSuccess = { },
            onGuestMode = { guestUserId = "guest_atelier_director" }
        )
    } else {
        SmartCoordiMainApp(currentUserId = activeUid)
    }
}

enum class SmartCoordiTab(
    val route: String,
    val labelKo: String,
    val icon: ImageVector
) {
    WARDROBE("wardrobe", "스마트 옷장", Icons.Default.Checkroom),
    VIRTUAL_FITTING("fitting", "3D 피팅", Icons.Default.AccessibilityNew),
    TPO_STYLING("tpo_styling", "TPO 추천", Icons.Default.AutoAwesome),
    AI_CHAT("ai_chat", "AI 스타일챗", Icons.Default.ChatBubbleOutline),
    ANALYTICS("analytics", "가성비 분석", Icons.Default.Insights)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCoordiMainApp(
    currentUserId: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val viewModel: SmartCoordiViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val localDb = SmartCoordiLocalDatabase.getInstance(app)
                val localFittingRepo = LocalFittingHistoryRepository(localDb.fittingHistoryDao())
                SmartCoordiViewModel(WardrobeRepository(db), currentUserId, localFittingRepo)
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val garmentsState by viewModel.garmentsState.collectAsStateWithLifecycle()
    val savedOutfitsState by viewModel.savedOutfitsState.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val isWeatherLoading by viewModel.isWeatherLoading.collectAsStateWithLifecycle()
    val aiRegistrationState by viewModel.aiRegistrationState.collectAsStateWithLifecycle()
    val aiCoordiState by viewModel.aiCoordiState.collectAsStateWithLifecycle()
    val cutoutBitmaps by viewModel.processedCutoutBitmaps.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatStreaming by viewModel.isChatStreaming.collectAsStateWithLifecycle()
    val selectedChatModel by viewModel.selectedChatModel.collectAsStateWithLifecycle()
    val recentFittingHistory by viewModel.recentFittingHistoryState.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(SmartCoordiTab.WARDROBE) }
    var isProEnterpriseMode by rememberSaveable { mutableStateOf(true) }
    var isProDarkMode by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Mandatory BackHandler for secondary tabs when using custom state switching
    BackHandler(enabled = currentTab != SmartCoordiTab.WARDROBE) {
        currentTab = SmartCoordiTab.WARDROBE
    }

    LaunchedEffect(statusBannerMessage) {
        statusBannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    val skuCount = (garmentsState as? com.example.viewmodel.DataUiState.Success)?.data?.size ?: 0

    SmartCoordiTheme(darkTheme = isProDarkMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                Text(
                                    text = if (isProEnterpriseMode) {
                                        "Ateli · ${currentTab.labelKo}"
                                    } else {
                                        "Ateli · ${currentTab.labelKo}"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ZigzagStrong
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        signOutUser(
                                            context = context,
                                            onSignOutComplete = {},
                                            scope = coroutineScope
                                        )
                                    },
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "로그아웃",
                                        tint = ZigzagForeground
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = ZigzagCanvas,
                                titleContentColor = ZigzagStrong,
                                actionIconContentColor = ZigzagForeground
                            )
                        )
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = ZigzagHairline
                        )
                        com.example.ui.components.EnterpriseProCommandBar(
                            isProEnterpriseMode = isProEnterpriseMode,
                            isProDarkMode = isProDarkMode,
                            skuCount = skuCount,
                            weatherSummary = "${weatherState.cityLabelKo} ${weatherState.temperatureC.toInt()}°C",
                            onToggleEnterpriseMode = { isProEnterpriseMode = !isProEnterpriseMode },
                            onToggleDarkMode = { isProDarkMode = !isProDarkMode }
                        )
                    }
                },
            bottomBar = {
                if (!isExpandedScreen) {
                    Column {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = ZigzagHairline
                        )
                        NavigationBar(
                            containerColor = ZigzagCanvas,
                            tonalElevation = 0.dp
                        ) {
                            SmartCoordiTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.labelKo
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.labelKo,
                                            style = ZigzagTabBarLink,
                                            color = if (currentTab == tab) ZigzagStrong else ZigzagMuted
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ZigzagStrong,
                                        selectedTextColor = ZigzagStrong,
                                        indicatorColor = ZigzagHairline.copy(alpha = 0.5f),
                                        unselectedIconColor = ZigzagMuted,
                                        unselectedTextColor = ZigzagMuted
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail {
                        SmartCoordiTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.labelKo
                                    )
                                },
                                label = { Text(tab.labelKo) },
                                modifier = Modifier.testTag("nav_rail_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        SmartCoordiTab.WARDROBE -> WardrobeScreen(
                            garmentsState = garmentsState,
                            aiRegistrationState = aiRegistrationState,
                            cutoutBitmaps = cutoutBitmaps,
                            onSeedStarterWardrobe = viewModel::seedStarterWardrobe,
                            onParseReceipt = viewModel::parseReceiptAndRegister,
                            onProcessPhoto = viewModel::processGarmentPhotoAndRegister,
                            onQuickAddGarment = viewModel::addQuickManualGarment,
                            onIncrementWear = viewModel::incrementGarmentWearCount,
                            onDeleteGarment = viewModel::deleteGarment,
                            onResetAiState = viewModel::resetAiRegistrationState,
                            onOpenVirtualFitting = { currentTab = SmartCoordiTab.VIRTUAL_FITTING }
                        )

                        SmartCoordiTab.VIRTUAL_FITTING -> VirtualFittingScreen(
                            profileState = profileState,
                            garmentsState = garmentsState,
                            cutoutBitmaps = cutoutBitmaps,
                            recentFittingHistory = recentFittingHistory,
                            onRecordFittingAttempt = viewModel::recordFittingAttempt,
                            onTogglePinFittingRecord = viewModel::togglePinFittingRecord,
                            onDeleteFittingRecord = viewModel::deleteFittingRecord,
                            onClearUnpinnedFittingHistory = viewModel::clearUnpinnedFittingHistory,
                            onSaveBodyProfile = viewModel::saveBodyProfile,
                            onSaveVirtualOutfit = { title, tpo, outer, top, bottom, shoes, note, score ->
                                viewModel.saveVirtualOutfit(
                                    title = title,
                                    tpoOccasion = tpo,
                                    outer = outer,
                                    top = top,
                                    bottom = bottom,
                                    shoes = shoes,
                                    aiStylingNote = note,
                                    fitScore = score
                                )
                            }
                        )

                        SmartCoordiTab.TPO_STYLING -> TpoStylingScreen(
                            weather = weatherState,
                            isWeatherLoading = isWeatherLoading,
                            profileState = profileState,
                            garmentsState = garmentsState,
                            savedOutfitsState = savedOutfitsState,
                            aiCoordiState = aiCoordiState,
                            cutoutBitmaps = cutoutBitmaps,
                            onRefreshWeather = viewModel::refreshWeather,
                            onRefreshWeatherByLocation = viewModel::refreshWeatherByLocation,
                            onGenerateRecommendation = viewModel::generateTpoCoordiRecommendation,
                            onSaveRecommendedOutfit = { rec ->
                                viewModel.saveVirtualOutfit(
                                    title = rec.title,
                                    tpoOccasion = rec.tpoOccasion,
                                    outer = rec.outerGarment,
                                    top = rec.topGarment,
                                    bottom = rec.bottomGarment,
                                    shoes = rec.shoesGarment,
                                    aiStylingNote = rec.aiStylingNote,
                                    fitScore = rec.fitScore
                                )
                            },
                            onIncrementWear = viewModel::incrementGarmentWearCount,
                            onDeleteSavedOutfit = viewModel::deleteSavedOutfit
                        )

                        SmartCoordiTab.AI_CHAT -> AiStylistChatScreen(
                            messages = chatMessages,
                            isStreaming = isChatStreaming,
                            selectedModelName = selectedChatModel,
                            profileState = profileState,
                            garmentsState = garmentsState,
                            weather = weatherState,
                            onSelectModel = viewModel::selectChatModel,
                            onSendMessage = viewModel::sendChatMessage,
                            onClearChat = viewModel::clearChatHistory
                        )

                        SmartCoordiTab.ANALYTICS -> AnalyticsScreen(
                            garmentsState = garmentsState,
                            cutoutBitmaps = cutoutBitmaps,
                            onIncrementWear = viewModel::incrementGarmentWearCount,
                            onAskAiStylist = { prompt ->
                                val profile = (profileState as? com.example.viewmodel.DataUiState.Success)?.data
                                    ?: com.example.data.model.UserProfile()
                                val garments = (garmentsState as? com.example.viewmodel.DataUiState.Success)?.data
                                    ?: emptyList()
                                currentTab = SmartCoordiTab.AI_CHAT
                                viewModel.sendChatMessage(prompt, profile, garments, weatherState)
                            }
                        )
                    }
                }
            }
        }
    }
    }
}
