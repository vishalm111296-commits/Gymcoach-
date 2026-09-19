package com.gymcoach.app.core.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutShareCardRenderer @Inject constructor() {

    companion object {
        const val CARD_WIDTH = 1080
        const val CARD_HEIGHT = 1350

        // GymCoach Color Palette
        val COLOR_BG = Color.parseColor("#0F131C")
        val COLOR_CARD_BG = Color.parseColor("#171D2B")
        val COLOR_CARD_BORDER = Color.parseColor("#273248")
        val COLOR_ELECTRIC_VIOLET = Color.parseColor("#6C63FF")
        val COLOR_CYAN = Color.parseColor("#00F2FE")
        val COLOR_GOLD = Color.parseColor("#FFB300")
        val COLOR_TEXT_PRIMARY = Color.parseColor("#FFFFFF")
        val COLOR_TEXT_SECONDARY = Color.parseColor("#8E9AA8")
        val COLOR_TEXT_MUTED = Color.parseColor("#5A677B")
        val COLOR_PILL_BG = Color.parseColor("#1F2739")

        fun renderToBitmap(data: WorkoutShareCardData): Bitmap =
            WorkoutShareCardRenderer().renderToBitmap(data)

        fun saveShareImage(context: Context, bitmap: Bitmap, workoutId: Long): Uri? =
            WorkoutShareCardRenderer().saveShareImage(context, bitmap, workoutId)
    }

    fun renderToBitmap(data: WorkoutShareCardData): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Background
        val bgPaint = Paint().apply {
            color = COLOR_BG
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), bgPaint)

        // Subtle gradient glow at top-left
        val glowPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, CARD_WIDTH.toFloat(), 400f,
                Color.parseColor("#256C63FF"), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), 400f, glowPaint)

        val marginX = 64f
        val contentWidth = CARD_WIDTH - (marginX * 2)

        // 2. Header: App Brand Badge
        var currentY = 70f

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ELECTRIC_VIOLET
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.15f
        }
        canvas.drawText("GYMCOACH", marginX, currentY, brandPaint)

        currentY += 48f

        // 3. Header: Workout Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val displayTitle = truncateText(data.workoutTitle, titlePaint, contentWidth)
        canvas.drawText(displayTitle, marginX, currentY, titlePaint)

        currentY += 36f

        // 4. Header: Date and Duration
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val metaText = "${data.dateFormatted}  •  ${data.durationFormatted}"
        canvas.drawText(metaText, marginX, currentY, metaPaint)

        currentY += 32f

        // 5. Motivational Quote Banner
        val quoteBannerRect = RectF(marginX, currentY, marginX + contentWidth, currentY + 68f)
        val quoteBannerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C2333")
            style = Paint.Style.FILL
        }
        val quoteBannerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ELECTRIC_VIOLET
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(quoteBannerRect, 14f, 14f, quoteBannerBgPaint)
        canvas.drawRoundRect(quoteBannerRect, 14f, 14f, quoteBannerBorderPaint)

        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ELECTRIC_VIOLET
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val quoteText = "\"${data.motivationalQuote}\""
        val truncatedQuote = truncateText(quoteText, quotePaint, contentWidth - 48f)
        val quoteX = marginX + 24f
        val quoteY = currentY + 44f
        canvas.drawText(truncatedQuote, quoteX, quoteY, quotePaint)

        currentY += 92f

        // 6. Stats Grid (2x2)
        val gridGap = 20f
        val colWidth = (contentWidth - gridGap) / 2f
        val rowHeight = 118f

        // Row 1: Volume & Sets
        drawStatCard(
            canvas = canvas,
            rect = RectF(marginX, currentY, marginX + colWidth, currentY + rowHeight),
            label = "TOTAL VOLUME",
            value = "${String.format(Locale.US, "%,.0f", data.totalVolumeKg)} kg",
            accentColor = COLOR_ELECTRIC_VIOLET
        )

        drawStatCard(
            canvas = canvas,
            rect = RectF(marginX + colWidth + gridGap, currentY, marginX + contentWidth, currentY + rowHeight),
            label = "TOTAL SETS",
            value = "${data.totalSets}",
            accentColor = COLOR_ELECTRIC_VIOLET
        )

        currentY += rowHeight + gridGap

        // Row 2: Reps & PRs
        drawStatCard(
            canvas = canvas,
            rect = RectF(marginX, currentY, marginX + colWidth, currentY + rowHeight),
            label = "TOTAL REPS",
            value = "${data.totalReps}",
            accentColor = COLOR_CYAN
        )

        val prCardAccent = if (data.prCount > 0) COLOR_GOLD else COLOR_TEXT_SECONDARY
        val prValueText = if (data.prCount > 0) "${data.prCount} PRs ★" else "${data.prCount}"
        drawStatCard(
            canvas = canvas,
            rect = RectF(marginX + colWidth + gridGap, currentY, marginX + contentWidth, currentY + rowHeight),
            label = "PERSONAL RECORDS",
            value = prValueText,
            accentColor = prCardAccent
        )

        currentY += rowHeight + 36f

        // 7. Target Muscle Chips
        if (data.topMuscles.isNotEmpty()) {
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.08f
            }
            canvas.drawText("TARGET MUSCLES", marginX, currentY, sectionPaint)
            currentY += 20f

            var chipX = marginX
            val chipHeight = 44f
            val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_PILL_BG
                style = Paint.Style.FILL
            }
            val chipBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_CARD_BORDER
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            val chipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_CYAN
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            for (muscle in data.topMuscles) {
                val textWidth = chipTextPaint.measureText(muscle)
                val chipWidth = textWidth + 36f
                if (chipX + chipWidth <= marginX + contentWidth) {
                    val chipRect = RectF(chipX, currentY, chipX + chipWidth, currentY + chipHeight)
                    canvas.drawRoundRect(chipRect, 22f, 22f, chipPaint)
                    canvas.drawRoundRect(chipRect, 22f, 22f, chipBorderPaint)
                    canvas.drawText(muscle, chipX + 18f, currentY + 30f, chipTextPaint)
                    chipX += chipWidth + 14f
                }
            }

            currentY += chipHeight + 32f
        }

        // 8. Exercise Highlights
        if (data.exercises.isNotEmpty()) {
            val exerciseSectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.08f
            }
            canvas.drawText("EXERCISE HIGHLIGHTS", marginX, currentY, exerciseSectionPaint)
            currentY += 20f

            val exCardHeight = 84f
            val exCardGap = 12f
            val maxExercises = minOf(data.exercises.size, 4)

            for (i in 0 until maxExercises) {
                val ex = data.exercises[i]
                val exRect = RectF(marginX, currentY, marginX + contentWidth, currentY + exCardHeight)

                val exBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_CARD_BG
                    style = Paint.Style.FILL
                }
                val exBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (ex.isPr) COLOR_GOLD else COLOR_CARD_BORDER
                    style = Paint.Style.STROKE
                    strokeWidth = if (ex.isPr) 2.5f else 1.5f
                }
                canvas.drawRoundRect(exRect, 16f, 16f, exBgPaint)
                canvas.drawRoundRect(exRect, 16f, 16f, exBorderPaint)

                // Exercise Name
                val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_TEXT_PRIMARY
                    textSize = 26f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val maxNameWidth = contentWidth * 0.48f
                val safeName = truncateText(ex.exerciseName, namePaint, maxNameWidth)
                canvas.drawText(safeName, marginX + 24f, currentY + 36f, namePaint)

                // Best set summary
                val bestSetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_CYAN
                    textSize = 22f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(ex.bestSetSummary, marginX + 24f, currentY + 66f, bestSetPaint)

                // Sets count
                val setsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_TEXT_SECONDARY
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textAlign = Paint.Align.RIGHT
                }
                val setsText = "${ex.totalSetsCount} sets"
                val rightEdge = marginX + contentWidth - 24f
                val textRight = if (ex.isPr) rightEdge - 80f else rightEdge
                canvas.drawText(setsText, textRight, currentY + 48f, setsPaint)

                // PR Badge
                if (ex.isPr) {
                    val badgeRect = RectF(rightEdge - 68f, currentY + 24f, rightEdge, currentY + 60f)
                    val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = COLOR_GOLD
                        style = Paint.Style.FILL
                    }
                    canvas.drawRoundRect(badgeRect, 8f, 8f, badgeBg)

                    val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = COLOR_BG
                        textSize = 20f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText("PR", badgeRect.centerX(), badgeRect.centerY() + 7f, badgeTextPaint)
                }

                currentY += exCardHeight + exCardGap
            }
        }

        // 9. Footer Divider & Watermark
        val footerY = CARD_HEIGHT - 60f

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_CARD_BORDER
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(marginX, footerY - 24f, marginX + contentWidth, footerY - 24f, dividerPaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("GYMCOACH • TRACK. LIFT. CONQUER.", CARD_WIDTH / 2f, footerY + 8f, footerPaint)

        return bitmap
    }

    fun saveShareImage(context: Context, bitmap: Bitmap, workoutId: Long): Uri? {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val file = File(exportDir, "workout_share_${workoutId}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    private fun drawStatCard(
        canvas: Canvas,
        rect: RectF,
        label: String,
        value: String,
        accentColor: Int
    ) {
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_CARD_BG
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_CARD_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, 18f, 18f, cardBgPaint)
        canvas.drawRoundRect(rect, 18f, 18f, cardBorderPaint)

        // Accent pill on top-left of card
        val accentPill = RectF(rect.left + 20f, rect.top + 16f, rect.left + 26f, rect.top + 36f)
        val accentPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(accentPill, 3f, 3f, accentPillPaint)

        // Label
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        canvas.drawText(label, rect.left + 36f, rect.top + 33f, labelPaint)

        // Value
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val safeValue = truncateText(value, valuePaint, rect.width() - 40f)
        canvas.drawText(safeValue, rect.left + 20f, rect.top + 84f, valuePaint)
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        var end = text.length - 1
        while (end > 0 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
            end--
        }
        return if (end > 0) text.substring(0, end) + ellipsis else text
    }
}
