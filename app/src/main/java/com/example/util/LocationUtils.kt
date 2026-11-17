package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * LocationUtils
 *
 * 단일 책임 원칙(SRP) 준수를 위한 Android 기기 위치 수신 및 지오코딩 전용 유틸리티.
 */
object LocationUtils {

    /**
     * GPS, Network, Passive 프로바이더 중 가장 오차가 적은 최신 위치 반환
     */
    @SuppressLint("MissingPermission")
    fun getBestLastKnownLocation(context: Context): Location? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            var bestLocation: Location? = null
            for (provider in providers) {
                val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
                if (loc != null) {
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                        bestLocation = loc
                    }
                }
            }
            bestLocation
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 위경도 좌표를 바탕으로 한국어 시/도/구 행정구역 라벨 추출
     */
    @Suppress("DEPRECATION")
    suspend fun resolveKoreanDistrictLabel(
        context: Context,
        lat: Double,
        lon: Double
    ): String? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.KOREA)
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull() ?: return@withContext null
            listOfNotNull(addr.adminArea, addr.locality ?: addr.subLocality)
                .distinct()
                .joinToString(" ")
                .takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
