package com.example.ui.auth

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.ui.theme.ZigzagAppBanner
import com.example.ui.theme.ZigzagBody
import com.example.ui.theme.ZigzagBuyCta
import com.example.ui.theme.ZigzagCanvas
import com.example.ui.theme.ZigzagCtaBlack
import com.example.ui.theme.ZigzagCtaText
import com.example.ui.theme.ZigzagForeground
import com.example.ui.theme.ZigzagHairline
import com.example.ui.theme.ZigzagHeading
import com.example.ui.theme.ZigzagMetadata
import com.example.ui.theme.ZigzagMoreButton
import com.example.ui.theme.ZigzagPillText
import com.example.ui.theme.ZigzagSecondary
import com.example.ui.theme.ZigzagStrong
import com.example.ui.theme.ZigzagTabSelected
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthScreen"

suspend fun signOut(credentialManager: CredentialManager, auth: FirebaseAuth = Firebase.auth) {
    try {
        auth.signOut()
    } catch (e: Exception) {
        Log.e(TAG, "FirebaseAuth sign out failed: ${e.message}", e)
    }

    try {
        val clearRequest = androidx.credentials.ClearCredentialStateRequest()
        credentialManager.clearCredentialState(clearRequest)
    } catch (e: Exception) {
        Log.e(TAG, "CredentialManager clear state failed: ${e.message}", e)
    }
}

fun signOutUser(
    context: Context,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope,
    credentialManager: CredentialManager = CredentialManager.create(context),
    auth: FirebaseAuth = Firebase.auth
) {
    scope.launch {
        try {
            signOut(credentialManager, auth)
        } finally {
            onSignOutComplete()
        }
    }
}

private suspend fun handleCredentialResponse(
    result: GetCredentialResponse,
    auth: FirebaseAuth,
    onAuthSuccess: () -> Unit,
    context: Context
) {
    when (val credential = result.credential) {
        is CustomCredential -> {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken.idToken, null)
                    auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase Auth failed with Google ID token: ${e.message}", e)
                    Toast.makeText(context, "인증 실패: ${e.localizedMessage ?: "알 수 없는 오류"}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e(TAG, "Unexpected custom credential type: ${credential.type}")
                Toast.makeText(context, "지원하지 않는 자격 증명 형식입니다.", Toast.LENGTH_SHORT).show()
            }
        }
        else -> {
            Log.e(TAG, "Unexpected credential class: ${credential.javaClass.name}")
            Toast.makeText(context, "예상치 못한 자격 증명입니다.", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onGuestMode: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    val defaultWebClientId = remember(context) {
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else ""
        } catch (_: Exception) {
            ""
        }
    }

    val onGoogleSignInClick: () -> Unit = {
        if (!isLoading) {
            isLoading = true
            coroutineScope.launch {
                try {
                    if (defaultWebClientId.isNotBlank()) {
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(defaultWebClientId)
                            .setAutoSelectEnabled(false)
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        val result = credentialManager.getCredential(
                            request = request,
                            context = context
                        )
                        handleCredentialResponse(result, Firebase.auth, onAuthSuccess, context)
                    } else {
                        Toast.makeText(context, "Google 로그인 준비 중입니다. 둘러보기로 시작해주세요.", Toast.LENGTH_SHORT).show()
                        onGuestMode?.invoke()
                    }
                } catch (e: GetCredentialCancellationException) {
                    Log.d(TAG, "CredentialManager sign in cancelled by user")
                } catch (e: Exception) {
                    Log.e(TAG, "CredentialManager sign in failed: ${e.message}", e)
                    Toast.makeText(context, "로그인 안내: 둘러보기 모드를 이용해주세요.", Toast.LENGTH_SHORT).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // =========================================================================
    // ZIGZAG (ZDS) Canvas & Product-Led Layout
    // =========================================================================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZigzagCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ZIGZAG Icon Badge (8px rounded, subtle #F1ECEA surface)
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(8.dp),
                color = ZigzagAppBanner,
                border = BorderStroke(1.dp, ZigzagHairline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Checkroom,
                        contentDescription = "Ateli 로고",
                        tint = ZigzagStrong,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "ATELI",
                style = ZigzagMetadata.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ZigzagSecondary,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ZIGZAG Heading (32px / 600 / 44.8px)
            Text(
                text = "Ateli",
                style = ZigzagHeading,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ZIGZAG Body (16px / 400 / #292B2B)
            Text(
                text = "취향 발견부터 체형 맞춤 3D 가상 피팅까지\n스마트 디지털 옷장 아틀리에",
                style = ZigzagBody,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ZIGZAG Product-led Feature Cards (0px/8px geometry, 1px #ECEDEE hairline)
            FeatureHighlightCard(
                icon = Icons.Default.AutoAwesome,
                title = "제로 에포트 옷장 (AI 영수증 & 누끼)",
                subtitle = "쇼핑몰 영수증이나 사진 한 장으로 배경 제거부터 카테고리·태깅까지 자동 완성"
            )
            Spacer(modifier = Modifier.height(10.dp))
            FeatureHighlightCard(
                icon = Icons.Default.Checkroom,
                title = "내 체형 맞춤 아바타 가상 피팅",
                subtitle = "내 신체 치수 기반 3D 마네킹에 Z-index 레이어드 드래그 & 코디 조합"
            )
            Spacer(modifier = Modifier.height(10.dp))
            FeatureHighlightCard(
                icon = Icons.Default.WbSunny,
                title = "실시간 날씨 & TPO 스마트 추천",
                subtitle = "오늘 기온과 출근·데이트·하객룩 일정을 결합한 Gemini AI 맞춤 제안"
            )
            Spacer(modifier = Modifier.height(10.dp))
            FeatureHighlightCard(
                icon = Icons.Default.Insights,
                title = "옷장 CPW 가성비 & 방치템 분석",
                subtitle = "1회 착용당 비용(Cost Per Wear) 계산 및 미착용 의류 관리 알림"
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ZIGZAG Main Buy-CTA Button (46~52dp height, 24dp pill rounded, #121314 Black)
            Button(
                onClick = onGoogleSignInClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(24.dp), // ZIGZAG control: 24px
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZigzagCtaBlack,
                    contentColor = ZigzagCtaText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = ZigzagCtaText,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign in with Google (구글 계정으로 시작하기)",
                        style = ZigzagBuyCta
                    )
                }
            }

            if (onGuestMode != null) {
                Spacer(modifier = Modifier.height(10.dp))
                // ZIGZAG More-Pill Button (24dp pill rounded, 1px #292B2B border, transparent bg)
                OutlinedButton(
                    onClick = onGuestMode,
                    shape = RoundedCornerShape(24.dp), // ZIGZAG control: 24px
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = ZigzagPillText
                    ),
                    border = BorderStroke(1.dp, ZigzagForeground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("guest_tour_button")
                ) {
                    Text(
                        text = "로그인 없이 둘러보기 (게스트 체험)",
                        style = ZigzagMoreButton
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    // ZIGZAG Card Pattern: 0px/8px modern geometry, #FFFFFF canvas, 1px #ECEDEE hairline
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp), // ZIGZAG icon/subtle corner
        colors = CardDefaults.cardColors(
            containerColor = ZigzagCanvas
        ),
        border = BorderStroke(1.dp, ZigzagHairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // ZIGZAG: box-shadow none
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ZigzagAppBanner),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ZigzagStrong,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = ZigzagTabSelected,
                    color = ZigzagStrong
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = ZigzagMetadata,
                    color = ZigzagSecondary
                )
            }
        }
    }
}
