package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CanvasJsonSerializer
import com.example.data.repository.ProjectRepository
import com.example.engine.CanvasRenderer
import com.example.engine.LanguageDetector
import com.example.model.BackgroundType
import com.example.model.CanvasBackgroundConfig
import com.example.model.CanvasDocument
import com.example.model.CanvasElement
import com.example.model.ElementKind
import com.example.model.ProjectType
import com.example.model.StudioFont
import com.example.model.TextDirectionMode
import com.example.model.TextEffectConfig
import com.example.model.TextFillType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var renderer: CanvasRenderer
    private lateinit var repo: ProjectRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        renderer = CanvasRenderer(context)
        repo = ProjectRepository(
            context = context,
            projectDao = db.projectDao(),
            exportRecordDao = db.exportRecordDao(),
            renderer = renderer
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app name matches iOSify`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("iOSify", appName)
    }

    @Test
    fun `language detector handles english, urdu, and auto rtl`() {
        assertFalse(LanguageDetector.resolveIsRtl("Hello iOSify Studio", TextDirectionMode.AUTO))
        assertTrue(LanguageDetector.resolveIsRtl("خودی کو کر بلند اتنا", TextDirectionMode.AUTO))
        assertTrue(LanguageDetector.resolveIsRtl("English text", TextDirectionMode.RTL))
        assertFalse(LanguageDetector.resolveIsRtl("اردو", TextDirectionMode.LTR))
    }

    @Test
    fun `canvas serializer preserves text effects`() {
        val doc = CanvasDocument(
            canvasWidth = 1080,
            canvasHeight = 1350,
            background = CanvasBackgroundConfig(type = BackgroundType.SOLID, solidColor = 0xFF000000L),
            elements = listOf(
                CanvasElement(
                    id = "el_1",
                    kind = ElementKind.CARD,
                    xFraction = 0.1f,
                    yFraction = 0.2f,
                    widthFraction = 0.8f,
                    heightFraction = 0.4f,
                    text = "سادگی میں خوبصورتی ہے",
                    font = StudioFont.NOTO_NASTALIQ_URDU,
                    directionMode = TextDirectionMode.RTL,
                    textEffect = TextEffectConfig(
                        fillType = TextFillType.GRADIENT,
                        gradientColors = listOf(0xFF007AFFL, 0xFFAF52DEL),
                        gradientAngleDeg = 60f,
                        outlineEnabled = true,
                        outlineColor = 0xFFFFFFFFL,
                        outlineWidthDp = 3f,
                        shadowEnabled = true,
                        shadowBlurRadius = 12f,
                        shadowOffsetX = 2f,
                        shadowOffsetY = 6f
                    )
                )
            )
        )

        val json = CanvasJsonSerializer.toJson(doc)
        val restored = CanvasJsonSerializer.fromJson(json)
        assertEquals(doc.elements.first().text, restored.elements.first().text)
        assertEquals(TextFillType.GRADIENT, restored.elements.first().textEffect.fillType)
        assertTrue(restored.elements.first().textEffect.outlineEnabled)
        assertTrue(restored.elements.first().textEffect.shadowEnabled)
    }

    @Test
    fun `canvas renderer produces bitmap for urdu rtl with text effects`() {
        val doc = CanvasDocument(
            canvasWidth = 540,
            canvasHeight = 540,
            elements = listOf(
                CanvasElement(
                    id = "ur_fx",
                    kind = ElementKind.TEXT,
                    xFraction = 0.1f,
                    yFraction = 0.2f,
                    widthFraction = 0.8f,
                    heightFraction = 0.5f,
                    text = "السلام علیکم iOSify ✨",
                    font = StudioFont.NOTO_NASTALIQ_URDU,
                    directionMode = TextDirectionMode.AUTO,
                    textEffect = TextEffectConfig(
                        fillType = TextFillType.GRADIENT,
                        outlineEnabled = true,
                        shadowEnabled = true
                    )
                )
            )
        )
        val bmp = renderer.renderToBitmap(doc, 540, 540)
        assertEquals(540, bmp.width)
        assertEquals(540, bmp.height)
        bmp.recycle()
    }

    @Test
    fun `project repository supports create, rename, favorite, duplicate, and delete`() = runTest {
        val created = repo.createProject(
            name = "My Urdu Quote",
            type = ProjectType.TEXT_CARD,
            document = CanvasDocument()
        )
        assertEquals("My Urdu Quote", created.name)

        repo.renameProject(created.id, "Renamed Quote")
        val renamed = repo.getProject(created.id)
        assertNotNull(renamed)
        assertEquals("Renamed Quote", renamed?.name)

        repo.toggleFavorite(created.id, false)
        assertTrue(repo.getProject(created.id)?.isFavorite == true)

        val copy = repo.duplicateProject(created.id)
        assertNotNull(copy)
        assertEquals("Renamed Quote Copy", copy?.name)
        assertEquals(2, repo.allProjects.first().size)

        repo.deleteProject(created.id)
        assertNull(repo.getProject(created.id))
        assertEquals(1, repo.allProjects.first().size)
    }
}
