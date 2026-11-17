package com.example.util

/**
 * FormatUtils
 * 
 * 클린 코드 & DRY 원칙 준수를 위한 전역 통화(KRW) 및 숫자 포맷팅 유틸리티.
 */

/**
 * 금액을 한국 원화 단위 포맷으로 변환 (예: 15000 -> "₩15,000")
 */
fun Long.toFormattedKrw(): String = "₩${String.format("%,d", this)}"

/**
 * 금액을 한국 원화 단위 포맷으로 변환 (예: 15000 -> "₩15,000")
 */
fun Int.toFormattedKrw(): String = "₩${String.format("%,d", this)}"

/**
 * 금액에 '원' 접미사를 붙여 변환 (예: 15000 -> "15,000원")
 */
fun Long.toFormattedWon(): String = "${String.format("%,d", this)}원"

/**
 * 금액에 '원' 접미사를 붙여 변환 (예: 15000 -> "15,000원")
 */
fun Int.toFormattedWon(): String = "${String.format("%,d", this)}원"

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
