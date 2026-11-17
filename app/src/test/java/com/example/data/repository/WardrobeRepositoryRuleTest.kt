package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Garment
import com.example.data.model.SavedOutfit
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WardrobeRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addAndObserveGarment_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = WardrobeRepository(firestore)

        val garment = Garment(
            name = "클래식 트렌치코트",
            brand = "무신사 스탠다드",
            category = "OUTER",
            subCategory = "트렌치코트",
            colorName = "베이지",
            colorHex = "#D2B48C",
            season = "SPRING_FALL",
            pattern = "솔리드",
            thickness = "MEDIUM",
            waterResistant = true,
            tpoTags = listOf("출근", "데이트"),
            purchasePrice = 139000L,
            wearCount = 3L,
            sourceType = "RECEIPT_AUTO",
            receiptStore = "MUSINSA"
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.addGarment(garment) }
        assertTrue(createResult.isSuccess)
        val garmentId = createResult.getOrThrow()
        assertTrue(garmentId.isNotEmpty())

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeGarments(uid).first { list -> list.any { it.id == garmentId } }
        }
        assertTrue(emitted.any { it.id == garmentId && it.name == "클래식 트렌치코트" })
    }

    @Test
    fun saveProfileAndOutfit_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = WardrobeRepository(firestore)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserProfile(
                UserProfile(
                    displayName = "앨리스",
                    heightCm = 166L,
                    weightKg = 54L,
                    shoulderWidthCm = 40L,
                    waistCm = 66L,
                    bodyShape = "HOURGLASS",
                    skinToneHex = "#F3D6C1",
                    preferredCity = "Seoul"
                )
            )
        }
        assertTrue(profileResult.isSuccess)

        val outfitResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveOutfit(
                SavedOutfit(
                    title = "비 오는 날 출근 코디",
                    tpoOccasion = "출근",
                    weatherSummary = "서울 15°C · 비",
                    garmentIds = listOf("g1", "g2"),
                    outerGarmentId = "g1",
                    topGarmentId = "g2",
                    aiStylingNote = "발수 코팅 트렌치코트로 비 오는 출근길 실용성과 핏을 모두 잡았습니다.",
                    fitScore = 95L
                )
            )
        }
        assertTrue(outfitResult.isSuccess)
    }

    @Test
    fun getGarmentById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = WardrobeRepository(firestore)
        val garmentId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addGarment(
                Garment(
                    name = "앨리스 블레이저",
                    brand = "쿠어",
                    category = "OUTER",
                    subCategory = "블레이저",
                    colorName = "네이비",
                    colorHex = "#1F2A44",
                    season = "SPRING_FALL",
                    pattern = "솔리드",
                    thickness = "MEDIUM",
                    waterResistant = false,
                    tpoTags = listOf("출근"),
                    purchasePrice = 200000L,
                    wearCount = 1L,
                    sourceType = "CAMERA_AI"
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = WardrobeRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getGarmentById(aliceUid, garmentId)
        }
        assertTrue(crossResult.isFailure)
        val fsEx = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeGarments_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = WardrobeRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeGarments(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull(caught)
        val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_smartcoordi@test.com"
        const val BOB_EMAIL = "bob_smartcoordi@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
