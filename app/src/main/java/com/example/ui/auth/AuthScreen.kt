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
            // Apple-Style Minimalist Floating Symbol (Pure silhouette on white, zero boxy borders)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF5F5F7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Checkroom,
                    contentDescription = "Ateli 로고",
                    tint = ZigzagStrong,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "ATELI",
                style = ZigzagMetadata.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZigzagSecondary,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Apple SF Pro Display Style Heading
            Text(
                text = "Ateli",
                style = ZigzagHeading.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "내 옷장 정리부터\n아바타 피팅까지 한 번에",
                style = ZigzagBody.copy(color = ZigzagSecondary),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ZIGZAG Product-led Feature Cards (0px/8px geometry, 1px #ECEDEE hairline)
            FeatureHighlightCard(
                icon = Icons.Default.AutoAwesome,
                title = "사진 한 장으로 옷 등록",
                subtitle = "영수증이나 사진만 올리면 옷장에 쏙 넣어드려요"
            )
            Spacer(modifier = Modifier.height(10.dp))
            FeatureHighlightCard(
                icon = Icons.Default.Checkroom,
                title = "내 아바타에 입혀보기",
                subtitle = "키와 몸무게를 넣고 내 옷을 직접 입혀보세요"
            )
            Spacer(modifier = Modifier.height(10.dp))
            FeatureHighlightCard(
                icon = Icons.Default.WbSunny,
                title = "오늘 날씨 맞춤 추천",
                subtitle = "지금 날씨에 딱 어울리는 옷을 골라드려요"
            )
            Spacer(modifier = Modifier.height(10.dp))
            FeatureHighlightCard(
                icon = Icons.Default.Insights,
                title = "안 입는 옷 쏙 찾기",
                subtitle = "자주 입은 옷과 옷장에 박힌 옷을 알려줘요"
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ZIGZAG Main Buy-CTA Button (48~52dp height, 24dp pill rounded, #121314 Black)
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
                        text = "Google로 시작하기",
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
                        text = "둘러볼게요",
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
    // Apple-Style Clean Showcase Card: 18dp curvature, soft off-white surface, minimal elegance
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF9F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White),
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
                    style = ZigzagTabSelected.copy(fontWeight = FontWeight.Bold),
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
