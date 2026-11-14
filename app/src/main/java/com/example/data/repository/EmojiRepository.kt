package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.local.CustomEmojiEntity
import com.example.data.local.EmojiDao
import com.example.data.local.EmojiUsageEntity
import com.example.model.EmojiCatalogItem
import com.example.model.EmojiCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class EmojiRepository(
    private val context: Context,
    private val emojiDao: EmojiDao
) {
    val customEmojis: Flow<List<CustomEmojiEntity>> = emojiDao.observeCustomEmojis()
    val emojiUsage: Flow<List<EmojiUsageEntity>> = emojiDao.observeEmojiUsage()

    suspend fun markUsed(emojiChar: String) {
        val existing = emojiDao.getUsage(emojiChar)
        emojiDao.upsertUsage(
            EmojiUsageEntity(
                emojiChar = emojiChar,
                isFavorite = existing?.isFavorite ?: false,
                lastUsedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleFavorite(emojiChar: String) {
        val existing = emojiDao.getUsage(emojiChar)
        emojiDao.upsertUsage(
            EmojiUsageEntity(
                emojiChar = emojiChar,
                isFavorite = !(existing?.isFavorite ?: false),
                lastUsedAt = existing?.lastUsedAt ?: System.currentTimeMillis()
            )
        )
    }

    suspend fun importCustomEmojiAsset(
        uri: Uri,
        emojiChar: String,
        shortName: String,
        packName: String
    ): CustomEmojiEntity? = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
        val decoded = input.use {
            BitmapFactory.decodeStream(
                it,
                null,
                BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            )
        } ?: return@withContext null

        val maxSide = 512
        val targetBitmap = if (decoded.width > maxSide || decoded.height > maxSide) {
            val scale = minOf(maxSide.toFloat() / decoded.width, maxSide.toFloat() / decoded.height)
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            decoded
        }

        val dir = File(context.filesDir, "custom_emojis").apply { mkdirs() }
        val file = File(dir, "emoji_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            targetBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (targetBitmap !== decoded) decoded.recycle()
        targetBitmap.recycle()

        val entity = CustomEmojiEntity(
            id = UUID.randomUUID().toString(),
            packName = packName.ifBlank { "Custom Pack" },
            emojiChar = emojiChar.ifBlank { "✨" },
            shortName = shortName.ifBlank { "Custom Emoji" },
            assetFilePath = file.absolutePath,
            isFavorite = false,
            createdAt = System.currentTimeMillis()
        )
        emojiDao.upsertCustomEmoji(entity)
        entity
    }

    suspend fun deleteCustomEmoji(entity: CustomEmojiEntity) = withContext(Dispatchers.IO) {
        runCatching { File(entity.assetFilePath).delete() }
        emojiDao.deleteCustomEmoji(entity.id)
    }

    val catalog: List<EmojiCatalogItem> = listOf(
        // Smileys & Emotions
        EmojiCatalogItem("😀", "Grinning Face", EmojiCategory.SMILEYS, listOf("happy", "smile", "joy")),
        EmojiCatalogItem("🥹", "Holding Back Tears", EmojiCategory.SMILEYS, listOf("aww", "emotional", "cute")),
        EmojiCatalogItem("😍", "Heart Eyes", EmojiCategory.SMILEYS, listOf("love", "crush", "heart")),
        EmojiCatalogItem("🥰", "Smiling with Hearts", EmojiCategory.SMILEYS, listOf("love", "warm", "affection")),
        EmojiCatalogItem("🤩", "Star-Struck", EmojiCategory.SMILEYS, listOf("wow", "amazing", "star")),
        EmojiCatalogItem("😎", "Sunglasses", EmojiCategory.SMILEYS, listOf("cool", "chill", "ios")),
        EmojiCatalogItem("🥳", "Partying Face", EmojiCategory.SMILEYS, listOf("party", "celebrate", "birthday")),
        EmojiCatalogItem("😂", "Tears of Joy", EmojiCategory.SMILEYS, listOf("laugh", "funny", "lol")),
        EmojiCatalogItem("😭", "Loudly Crying", EmojiCategory.SMILEYS, listOf("cry", "sad", "sob")),
        EmojiCatalogItem("🤔", "Thinking Face", EmojiCategory.SMILEYS, listOf("think", "hmm", "idea")),
        EmojiCatalogItem("🫡", "Saluting Face", EmojiCategory.SMILEYS, listOf("respect", "salute", "sir")),
        EmojiCatalogItem("🫠", "Melting Face", EmojiCategory.SMILEYS, listOf("melt", "hot", "awkward")),
        EmojiCatalogItem("😇", "Smiling Halo", EmojiCategory.SMILEYS, listOf("angel", "blessed", "dua")),
        EmojiCatalogItem("😌", "Relieved Face", EmojiCategory.SMILEYS, listOf("calm", "peace", "sukun")),
        EmojiCatalogItem("🤯", "Exploding Head", EmojiCategory.SMILEYS, listOf("mindblown", "shock", "wow")),

        // People & Gestures
        EmojiCatalogItem("👋", "Waving Hand", EmojiCategory.PEOPLE, listOf("hello", "hi", "salam")),
        EmojiCatalogItem("🫶", "Heart Hands", EmojiCategory.PEOPLE, listOf("love", "support", "care")),
        EmojiCatalogItem("🙏", "Folded Hands", EmojiCategory.PEOPLE, listOf("please", "thanks", "dua")),
        EmojiCatalogItem("👏", "Clapping Hands", EmojiCategory.PEOPLE, listOf("applause", "bravo", "wah")),
        EmojiCatalogItem("👍", "Thumbs Up", EmojiCategory.PEOPLE, listOf("ok", "good", "approve")),
        EmojiCatalogItem("✌️", "Victory Hand", EmojiCategory.PEOPLE, listOf("peace", "two", "win")),
        EmojiCatalogItem("🤌", "Pinched Fingers", EmojiCategory.PEOPLE, listOf("perfection", "chef", "exact")),
        EmojiCatalogItem("🤝", "Handshake", EmojiCategory.PEOPLE, listOf("deal", "partner", "agree")),
        EmojiCatalogItem("💪", "Flexed Biceps", EmojiCategory.PEOPLE, listOf("strong", "power", "fitness")),
        EmojiCatalogItem("✍️", "Writing Hand", EmojiCategory.PEOPLE, listOf("write", "poetry", "shayari")),

        // Animals & Nature
        EmojiCatalogItem("🌸", "Cherry Blossom", EmojiCategory.ANIMALS, listOf("flower", "pink", "spring")),
        EmojiCatalogItem("🌹", "Red Rose", EmojiCategory.ANIMALS, listOf("gulab", "love", "rose")),
        EmojiCatalogItem("🌙", "Crescent Moon", EmojiCategory.ANIMALS, listOf("chand", "night", "eid")),
        EmojiCatalogItem("⭐", "Star", EmojiCategory.ANIMALS, listOf("sitara", "night", "favorite")),
        EmojiCatalogItem("🌈", "Rainbow", EmojiCategory.ANIMALS, listOf("colors", "sky", "hope")),
        EmojiCatalogItem("🦋", "Butterfly", EmojiCategory.ANIMALS, listOf("titli", "nature", "aesthetic")),
        EmojiCatalogItem("🔥", "Fire", EmojiCategory.ANIMALS, listOf("lit", "trending", "hot")),
        EmojiCatalogItem("🌊", "Ocean Wave", EmojiCategory.ANIMALS, listOf("sea", "water", "calm")),
        EmojiCatalogItem("🕊️", "Dove", EmojiCategory.ANIMALS, listOf("peace", "aman", "bird")),
        EmojiCatalogItem("🌿", "Herb", EmojiCategory.ANIMALS, listOf("leaf", "green", "organic")),

        // Food & Drinks
        EmojiCatalogItem("☕", "Hot Beverage", EmojiCategory.FOOD, listOf("chai", "coffee", "morning")),
        EmojiCatalogItem("🫖", "Teapot", EmojiCategory.FOOD, listOf("tea", "chai", "hospitality")),
        EmojiCatalogItem("🍓", "Strawberry", EmojiCategory.FOOD, listOf("fruit", "berry", "sweet")),
        EmojiCatalogItem("🥐", "Croissant", EmojiCategory.FOOD, listOf("breakfast", "cafe", "bakery")),
        EmojiCatalogItem("🍕", "Pizza", EmojiCategory.FOOD, listOf("slice", "party", "food")),
        EmojiCatalogItem("🧁", "Cupcake", EmojiCategory.FOOD, listOf("dessert", "sweet", "celebration")),
        EmojiCatalogItem("🥭", "Mango", EmojiCategory.FOOD, listOf("aam", "summer", "fruit")),
        EmojiCatalogItem("🧊", "Ice Cube", EmojiCategory.FOOD, listOf("cold", "chill", "drink")),

        // Travel & Places
        EmojiCatalogItem("✈️", "Airplane", EmojiCategory.TRAVEL, listOf("flight", "travel", "trip")),
        EmojiCatalogItem("🏔️", "Snow Mountain", EmojiCategory.TRAVEL, listOf("north", "hunza", "peak")),
        EmojiCatalogItem("🕌", "Mosque", EmojiCategory.TRAVEL, listOf("masjid", "eid", "ramadan")),
        EmojiCatalogItem("🏙️", "Cityscape", EmojiCategory.TRAVEL, listOf("urban", "skyline", "city")),
        EmojiCatalogItem("🚗", "Automobile", EmojiCategory.TRAVEL, listOf("car", "drive", "road")),
        EmojiCatalogItem("🧭", "Compass", EmojiCategory.TRAVEL, listOf("explore", "direction", "safar")),
        EmojiCatalogItem("🌅", "Sunrise", EmojiCategory.TRAVEL, listOf("subah", "morning", "glow")),
        EmojiCatalogItem("🌌", "Milky Way", EmojiCategory.TRAVEL, listOf("stars", "galaxy", "night")),

        // Activities
        EmojiCatalogItem("🎨", "Artist Palette", EmojiCategory.ACTIVITIES, listOf("design", "art", "studio")),
        EmojiCatalogItem("📸", "Camera Flash", EmojiCategory.ACTIVITIES, listOf("photo", "shot", "capture")),
        EmojiCatalogItem("🎧", "Headphone", EmojiCategory.ACTIVITIES, listOf("music", "audio", "listen")),
        EmojiCatalogItem("🏆", "Trophy", EmojiCategory.ACTIVITIES, listOf("winner", "award", "best")),
        EmojiCatalogItem("🎬", "Clapper Board", EmojiCategory.ACTIVITIES, listOf("video", "reel", "cinema")),
        EmojiCatalogItem("⚽", "Soccer Ball", EmojiCategory.ACTIVITIES, listOf("sport", "football", "match")),
        EmojiCatalogItem("🏏", "Cricket Game", EmojiCategory.ACTIVITIES, listOf("cricket", "bat", "match")),
        EmojiCatalogItem("🎯", "Bullseye", EmojiCategory.ACTIVITIES, listOf("goal", "target", "focus")),

        // Objects
        EmojiCatalogItem("📱", "Mobile Phone", EmojiCategory.OBJECTS, listOf("iphone", "screen", "ios")),
        EmojiCatalogItem("💻", "Laptop", EmojiCategory.OBJECTS, listOf("mac", "work", "code")),
        EmojiCatalogItem("💡", "Light Bulb", EmojiCategory.OBJECTS, listOf("idea", "creative", "bright")),
        EmojiCatalogItem("💎", "Gem Stone", EmojiCategory.OBJECTS, listOf("premium", "luxury", "diamond")),
        EmojiCatalogItem("🪄", "Magic Wand", EmojiCategory.OBJECTS, listOf("ai", "transform", "iosify")),
        EmojiCatalogItem("🖋️", "Fountain Pen", EmojiCategory.OBJECTS, listOf("qalam", "urdu", "calligraphy")),
        EmojiCatalogItem("📖", "Open Book", EmojiCategory.OBJECTS, listOf("kitab", "read", "quote")),
        EmojiCatalogItem("🔔", "Bell", EmojiCategory.OBJECTS, listOf("notification", "alert", "ios")),

        // Symbols
        EmojiCatalogItem("✨", "Sparkles", EmojiCategory.SYMBOLS, listOf("ai", "shine", "magic")),
        EmojiCatalogItem("❤️", "Red Heart", EmojiCategory.SYMBOLS, listOf("love", "dil", "favorite")),
        EmojiCatalogItem("🩵", "Light Blue Heart", EmojiCategory.SYMBOLS, listOf("aesthetic", "blue", "calm")),
        EmojiCatalogItem("💜", "Purple Heart", EmojiCategory.SYMBOLS, listOf("violet", "iosify", "creative")),
        EmojiCatalogItem("✅", "Check Mark", EmojiCategory.SYMBOLS, listOf("done", "verified", "success")),
        EmojiCatalogItem("💬", "Speech Balloon", EmojiCategory.SYMBOLS, listOf("message", "chat", "imessage")),
        EmojiCatalogItem("⚡", "High Voltage", EmojiCategory.SYMBOLS, listOf("fast", "energy", "flash")),
        EmojiCatalogItem("♾️", "Infinity", EmojiCategory.SYMBOLS, listOf("forever", "loop", "symbol")),

        // Flags
        EmojiCatalogItem("🇵🇰", "Pakistan", EmojiCategory.FLAGS, listOf("pakistan", "urdu", "pk")),
        EmojiCatalogItem("🇺🇸", "United States", EmojiCategory.FLAGS, listOf("usa", "english", "us")),
        EmojiCatalogItem("🇬🇧", "United Kingdom", EmojiCategory.FLAGS, listOf("uk", "english", "gb")),
        EmojiCatalogItem("🇦🇪", "United Arab Emirates", EmojiCategory.FLAGS, listOf("uae", "dubai", "ae")),
        EmojiCatalogItem("🇸🇦", "Saudi Arabia", EmojiCategory.FLAGS, listOf("saudi", "ksa", "sa")),
        EmojiCatalogItem("🇹🇷", "Turkey", EmojiCategory.FLAGS, listOf("turkey", "tr", "istanbul")),
        EmojiCatalogItem("🇨🇦", "Canada", EmojiCategory.FLAGS, listOf("canada", "ca")),
        EmojiCatalogItem("🇯🇵", "Japan", EmojiCategory.FLAGS, listOf("japan", "jp", "tokyo"))
    )
}
