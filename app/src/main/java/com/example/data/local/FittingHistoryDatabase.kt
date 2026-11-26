package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Garment
import com.example.ui.components.AvatarBodyMetrics
import com.example.ui.components.ColorHarmonyAnalyzer
import com.example.ui.components.TailoringFitSpecEngine
import kotlinx.coroutines.flow.Flow

/**
 * Room Entity storing each outfit combination tried on the Virtual Fitting Canvas.
 * Supports chronological timeline display, pinning favorite combinations, and 1-tap restoration.
 */
@Entity(tableName = "recent_fitting_history")
data class FittingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val combinationSignature: String,
    val sessionTitle: String,
    val canvasModeLabel: String,
    val triggerSourceLabel: String,
    val outerId: String? = null,
    val outerName: String? = null,
    val outerBrand: String? = null,
    val outerColorName: String? = null,
    val outerColorHex: String? = null,
    val outerSubCategory: String? = null,
    val topId: String? = null,
    val topName: String? = null,
    val topBrand: String? = null,
    val topColorName: String? = null,
    val topColorHex: String? = null,
    val topSubCategory: String? = null,
    val bottomId: String? = null,
    val bottomName: String? = null,
    val bottomBrand: String? = null,
    val bottomColorName: String? = null,
    val bottomColorHex: String? = null,
    val bottomSubCategory: String? = null,
    val shoesId: String? = null,
    val shoesName: String? = null,
    val shoesBrand: String? = null,
    val shoesColorName: String? = null,
    val shoesColorHex: String? = null,
    val shoesSubCategory: String? = null,
    val harmonyScore: Int = 92,
    val harmonyFormulaKo: String = "아틀리에 톤온톤 하모니",
    val cadFitGrade: String = "S+ (Bespoke Master Fit)",
    val recommendedKrSize: String = "KR 100 (L)",
    val garmentScale: Float = 1.0f,
    val isPinned: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    fun toPreviewGarments(): List<Garment> {
        val list = mutableListOf<Garment>()
        if (!outerName.isNullOrBlank()) {
            list.add(
                Garment(
                    id = outerId.orEmpty(),
                    name = outerName,
                    brand = outerBrand.orEmpty(),
                    category = "OUTER",
                    subCategory = outerSubCategory ?: "아우터",
                    colorName = outerColorName ?: "블랙",
                    colorHex = outerColorHex ?: "#2C3E50"
                )
            )
        }
        if (!topName.isNullOrBlank()) {
            list.add(
                Garment(
                    id = topId.orEmpty(),
                    name = topName,
                    brand = topBrand.orEmpty(),
                    category = "TOP",
                    subCategory = topSubCategory ?: "상의",
                    colorName = topColorName ?: "네이비",
                    colorHex = topColorHex ?: "#1F2A44"
                )
            )
        }
        if (!bottomName.isNullOrBlank()) {
            list.add(
                Garment(
                    id = bottomId.orEmpty(),
                    name = bottomName,
                    brand = bottomBrand.orEmpty(),
                    category = "BOTTOM",
                    subCategory = bottomSubCategory ?: "하의",
                    colorName = bottomColorName ?: "베이지",
                    colorHex = bottomColorHex ?: "#D2B48C"
                )
            )
        }
        if (!shoesName.isNullOrBlank()) {
            list.add(
                Garment(
                    id = shoesId.orEmpty(),
                    name = shoesName,
                    brand = shoesBrand.orEmpty(),
                    category = "SHOES",
                    subCategory = shoesSubCategory ?: "신발",
                    colorName = shoesColorName ?: "화이트",
                    colorHex = shoesColorHex ?: "#F5F5F0"
                )
            )
        }
        return list
    }
}

@Dao
interface FittingHistoryDao {
    @Query("SELECT * FROM recent_fitting_history ORDER BY isPinned DESC, timestampMillis DESC LIMIT 60")
    fun observeAllHistory(): Flow<List<FittingHistoryEntity>>

    @Query("SELECT * FROM recent_fitting_history ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getLatestRecord(): FittingHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFittingRecord(record: FittingHistoryEntity): Long

    @Query("UPDATE recent_fitting_history SET isPinned = :pinned WHERE id = :id")
    suspend fun updatePinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM recent_fitting_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recent_fitting_history WHERE isPinned = 0")
    suspend fun clearUnpinnedHistory()
}

@Database(
    entities = [FittingHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AteliLocalDatabase : RoomDatabase() {
    abstract fun fittingHistoryDao(): FittingHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AteliLocalDatabase? = null

        fun getInstance(context: Context): AteliLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AteliLocalDatabase::class.java,
                    "ateli_local_fitting_history.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

class LocalFittingHistoryRepository(
    private val dao: FittingHistoryDao
) {
    val allHistoryFlow: Flow<List<FittingHistoryEntity>> = dao.observeAllHistory()

    suspend fun recordFittingAttempt(
        outer: Garment?,
        top: Garment?,
        bottom: Garment?,
        shoes: Garment?,
        metrics: AvatarBodyMetrics,
        canvasModeLabel: String,
        triggerSourceLabel: String,
        garmentScale: Float,
        skipIfDuplicateOfLatest: Boolean = true
    ): FittingHistoryEntity? {
        val activeItems = listOfNotNull(outer, top, bottom, shoes)
        if (activeItems.isEmpty()) return null

        val signature = buildSignature(outer, top, bottom, shoes)
        if (skipIfDuplicateOfLatest) {
            val latest = dao.getLatestRecord()
            if (latest != null && latest.combinationSignature == signature) {
                return latest
            }
        }

        val entity = createEntityFromSelection(
            outer = outer,
            top = top,
            bottom = bottom,
            shoes = shoes,
            metrics = metrics,
            canvasModeLabel = canvasModeLabel,
            triggerSourceLabel = triggerSourceLabel,
            garmentScale = garmentScale
        )
        val insertedId = dao.insertFittingRecord(entity)
        return entity.copy(id = insertedId)
    }

    suspend fun togglePin(id: Long, currentPinned: Boolean) {
        dao.updatePinned(id, !currentPinned)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearUnpinned() {
        dao.clearUnpinnedHistory()
    }

    companion object {
        fun buildSignature(
            outer: Garment?,
            top: Garment?,
            bottom: Garment?,
            shoes: Garment?
        ): String {
            return listOf(
                outer?.id ?: outer?.name.orEmpty(),
                top?.id ?: top?.name.orEmpty(),
                bottom?.id ?: bottom?.name.orEmpty(),
                shoes?.id ?: shoes?.name.orEmpty()
            ).joinToString("|")
        }

        fun createEntityFromSelection(
            outer: Garment?,
            top: Garment?,
            bottom: Garment?,
            shoes: Garment?,
            metrics: AvatarBodyMetrics,
            canvasModeLabel: String,
            triggerSourceLabel: String,
            garmentScale: Float,
            timestampMillis: Long = System.currentTimeMillis()
        ): FittingHistoryEntity {
            val items = listOfNotNull(outer, top, bottom, shoes)
            val harmony = ColorHarmonyAnalyzer.evaluateOutfitHarmony(
                outer = outer,
                top = top,
                bottom = bottom,
                shoes = shoes,
                bodyShape = metrics.bodyShape
            )
            val cadSpec = TailoringFitSpecEngine.evaluateCadFitSpec(
                metrics = metrics,
                outer = outer,
                top = top,
                bottom = bottom,
                fitScaleMultiplier = garmentScale
            )
            val titleParts = listOfNotNull(
                outer?.name,
                top?.name,
                bottom?.name
            ).take(2)
            val sessionTitle = if (titleParts.isNotEmpty()) {
                titleParts.joinToString(" + ")
            } else {
                "Ateli 피팅 조합"
            }

            return FittingHistoryEntity(
                combinationSignature = buildSignature(outer, top, bottom, shoes),
                sessionTitle = sessionTitle,
                canvasModeLabel = canvasModeLabel,
                triggerSourceLabel = triggerSourceLabel,
                outerId = outer?.id,
                outerName = outer?.name,
                outerBrand = outer?.brand,
                outerColorName = outer?.colorName,
                outerColorHex = outer?.colorHex,
                outerSubCategory = outer?.subCategory,
                topId = top?.id,
                topName = top?.name,
                topBrand = top?.brand,
                topColorName = top?.colorName,
                topColorHex = top?.colorHex,
                topSubCategory = top?.subCategory,
                bottomId = bottom?.id,
                bottomName = bottom?.name,
                bottomBrand = bottom?.brand,
                bottomColorName = bottom?.colorName,
                bottomColorHex = bottom?.colorHex,
                bottomSubCategory = bottom?.subCategory,
                shoesId = shoes?.id,
                shoesName = shoes?.name,
                shoesBrand = shoes?.brand,
                shoesColorName = shoes?.colorName,
                shoesColorHex = shoes?.colorHex,
                shoesSubCategory = shoes?.subCategory,
                harmonyScore = harmony.totalHarmonyScore,
                harmonyFormulaKo = harmony.harmonyTypeKo,
                cadFitGrade = cadSpec.overallFitGrade,
                recommendedKrSize = cadSpec.recommendedKrSize,
                garmentScale = garmentScale,
                isPinned = false,
                timestampMillis = timestampMillis
            )
        }
    }
}
