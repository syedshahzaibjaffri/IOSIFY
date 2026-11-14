package com.example.engine

import com.example.model.TextDirectionMode

object LanguageDetector {

    fun containsUrduOrArabic(text: String): Boolean {
        for (ch in text) {
            val code = ch.code
            if (
                code in 0x0600..0x06FF ||
                code in 0x0750..0x077F ||
                code in 0x08A0..0x08FF ||
                code in 0xFB50..0xFDFF ||
                code in 0xFE70..0xFEFF
            ) {
                return true
            }
        }
        return false
    }

    fun isPredominantlyRtl(text: String): Boolean {
        var rtlCount = 0
        var ltrCount = 0
        for (ch in text) {
            val code = ch.code
            when {
                code in 0x0600..0x06FF ||
                    code in 0x0750..0x077F ||
                    code in 0x08A0..0x08FF ||
                    code in 0xFB50..0xFDFF ||
                    code in 0xFE70..0xFEFF -> rtlCount++
                ch.isLetter() -> ltrCount++
            }
        }
        if (rtlCount == 0) return false
        return rtlCount >= ltrCount || (rtlCount > 0 && ltrCount == 0)
    }

    fun resolveIsRtl(text: String, mode: TextDirectionMode): Boolean {
        return when (mode) {
            TextDirectionMode.LTR -> false
            TextDirectionMode.RTL -> true
            TextDirectionMode.AUTO -> containsUrduOrArabic(text) && isPredominantlyRtl(text)
        }
    }
}
