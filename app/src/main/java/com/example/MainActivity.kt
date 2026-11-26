package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.launch
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
import com.example.data.local.AteliLocalDatabase
import com.example.data.model.Garment
import com.example.data.repository.WardrobeRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.FintechFloatingToast
import com.example.ui.screens.AiStylistChatScreen
import com.example.ui.screens.TpoStylingScreen
import com.example.ui.screens.VirtualFittingScreen
import com.example.ui.screens.WardrobeScreen
import com.example.ui.theme.AteliTheme
import com.example.viewmodel.AteliViewModel
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
            AteliTheme {
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
        AteliMainApp(currentUserId = activeUid)
    }
}

enum class AteliTab(
    val route: String,
    val labelKo: String,
    val icon: ImageVector
) {
    WARDROBE("wardrobe", "내 옷장", Icons.Default.Checkroom),
    TPO_STYLING("tpo_styling", "오늘 옷", Icons.Default.WbSunny),
    VIRTUAL_FITTING("fitting", "입어보기", Icons.Default.AccessibilityNew),
    AI_CHAT("ai_chat", "AI 코디", Icons.Default.ChatBubbleOutline)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AteliMainApp(
    currentUserId: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val viewModel: AteliViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val localDb = AteliLocalDatabase.getInstance(app)
                val localFittingRepo = LocalFittingHistoryRepository(localDb.fittingHistoryDao())
                AteliViewModel(WardrobeRepository(db), currentUserId, localFittingRepo)
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

    var currentTab by rememberSaveable { mutableStateOf(AteliTab.WARDROBE) }
    var pendingLookGarments by remember { mutableStateOf<List<Garment>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val showFintechToastAction: (String) -> Unit = { msg ->
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Mandatory BackHandler for secondary tabs when using custom state switching
    BackHandler(enabled = currentTab != AteliTab.WARDROBE) {
        currentTab = AteliTab.WARDROBE
    }

    LaunchedEffect(statusBannerMessage) {
        statusBannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    AteliTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        FintechFloatingToast(snackbarData = data)
                    }
                },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = currentTab.labelKo,
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
                },
            bottomBar = {
                if (!isExpandedScreen) {
                    AppleFloatingNavigationDock(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        onSearchClick = {
                            currentTab = AteliTab.WARDROBE
                        }
                    )
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
                        AteliTab.entries.forEach { tab ->
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

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (currentTab) {
                        AteliTab.WARDROBE -> WardrobeScreen(
                            garmentsState = garmentsState,
                            aiRegistrationState = aiRegistrationState,
                            cutoutBitmaps = cutoutBitmaps,
                            onSeedStarterWardrobe = viewModel::seedStarterWardrobe,
                            onParseReceipt = viewModel::parseReceiptAndRegister,
                            onProcessPhoto = viewModel::processGarmentPhotoAndRegister,
                            onQuickAddGarment = viewModel::addQuickManualGarment,
                            onPhotoCutoutRegistered = viewModel::addPhotoCutoutGarment,
                            onIncrementWear = viewModel::incrementGarmentWearCount,
                            onDeleteGarment = viewModel::deleteGarment,
                            onResetAiState = viewModel::resetAiRegistrationState,
                            onOpenVirtualFitting = { currentTab = AteliTab.VIRTUAL_FITTING },
                            onOpenVirtualFittingWithLook = { selectedLook ->
                                pendingLookGarments = selectedLook
                                currentTab = AteliTab.VIRTUAL_FITTING
                            },
                            userProfileState = profileState,
                            onSavePersonalColorTone = viewModel::savePersonalColorTone,
                            onShowToast = showFintechToastAction
                        )

                        AteliTab.VIRTUAL_FITTING -> VirtualFittingScreen(
                            profileState = profileState,
                            garmentsState = garmentsState,
                            cutoutBitmaps = cutoutBitmaps,
                            recentFittingHistory = recentFittingHistory,
                            preselectedLookGarments = pendingLookGarments,
                            onConsumePreselectedLook = { pendingLookGarments = emptyList() },
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
                            },
                            onShowToast = showFintechToastAction
                        )

                        AteliTab.TPO_STYLING -> TpoStylingScreen(
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
                            onDeleteSavedOutfit = viewModel::deleteSavedOutfit,
                            onShowToast = showFintechToastAction
                        )

                        AteliTab.AI_CHAT -> AiStylistChatScreen(
                            messages = chatMessages,
                            isStreaming = isChatStreaming,
                            selectedModelName = selectedChatModel,
                            profileState = profileState,
                            garmentsState = garmentsState,
                            weather = weatherState,
                            onSelectModel = viewModel::selectChatModel,
                            onSendMessage = viewModel::sendChatMessage,
                            onClearChat = viewModel::clearChatHistory,
                            onShowToast = showFintechToastAction
                        )
                    }
                }
            }
        }
    }
    }
}

/**
 * Apple Store (iOS 18) Signature Floating Island Navigation Dock.
 * Features a frosted/translucent glass pill capsule on the left and a dedicated circular FAB on the right.
 */
@Composable
fun AppleFloatingNavigationDock(
    currentTab: AteliTab,
    onTabSelected: (AteliTab) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Floating Pill Navigation Capsule
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(62.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color.Black.copy(alpha = 0.14f),
                    ambientColor = Color.Black.copy(alpha = 0.06f)
                ),
            shape = RoundedCornerShape(32.dp),
            color = Color.White.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AteliTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                if (isSelected) Color(0xFF0071E3).copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable { onTabSelected(tab) }
                            .padding(vertical = 4.dp)
                            .testTag("apple_dock_tab_${tab.route}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.labelKo,
                                tint = if (isSelected) Color(0xFF0071E3) else Color(0xFF86868B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.labelKo,
                                style = TextStyle(
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF0071E3) else Color(0xFF86868B),
                                    platformStyle = @Suppress("DEPRECATION") androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // 2. Apple Store Floating Circular Search Action Button
        Surface(
            modifier = Modifier
                .size(62.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.14f),
                    ambientColor = Color.Black.copy(alpha = 0.06f)
                ),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onSearchClick() }
                    .testTag("apple_dock_search_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "검색",
                    tint = ZigzagStrong,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
