package com.example.util

/**
 * FormatUtils
 *
 * 클린 코드 & DRY 원칙 준수를 위한 전역 숫자 및 좌표 포맷팅 유틸리티.
 */

/**
 * 3자리 콤마 숫자 문자열 변환 (예: 15000 -> "15,000")
 */
fun Long.toFormattedNumber(): String = String.format("%,d", this)

/**
 * 3자리 콤마 숫자 문자열 변환 (예: 15000 -> "15,000")
 */
fun Int.toFormattedNumber(): String = String.format("%,d", this)

/**
 * GPS 좌표 소수점 4자리 표준 변환 (예: 37.566512 -> "37.5665")
 */
fun Double.toFormattedGps(): String = String.format("%.4f", this)
