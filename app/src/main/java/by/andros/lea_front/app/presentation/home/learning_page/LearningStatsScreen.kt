package by.andros.lea_front.app.presentation.home.learning_page

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import by.andros.lea_front.learning.LearningSessionStats
import kotlin.math.max

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun RatingHistogram(
    ratingCounts: Map<Int, Int>,
    modifier: Modifier = Modifier
) {
    val maxCount = ratingCounts.values.maxOrNull() ?: 0
    // Ensure all ratings from 1 to 5 are present, default to 0 if not rated
    val completeRatingCounts = (1..5).associateWith { rating -> ratingCounts.getOrDefault(rating, 0) }

    val barColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    val textPaint = android.graphics.Paint().apply {
        color = textColor.hashCode()
        textSize = with(density) { 12.sp.toPx() }
        textAlign = android.graphics.Paint.Align.CENTER
    }

    BoxWithConstraints(modifier = modifier) {
        val chartHeight = maxHeight * 0.8f // Use 80% of available height for bars
        val barWidthPx = (maxWidth / (completeRatingCounts.size * 2f)) // Bars occupy half the allocated space
        val spacingPx = barWidthPx

        Canvas(modifier = Modifier.fillMaxSize()) {
            completeRatingCounts.entries.sortedBy { it.key }.forEachIndexed { index, entry ->
                val (rating, count) = entry
                val barHeight = if (maxCount > 0) (count.toFloat() / maxCount) * chartHeight.toPx() else 0f

                val barX = (index * (barWidthPx + spacingPx).toPx()) + spacingPx.toPx() / 2f
                val barY = chartHeight.toPx() - barHeight

                // Draw bar
                drawRect(
                    color = barColor,
                    topLeft = Offset(barX, barY),
                    size = Size(barWidthPx.toPx(), barHeight)
                )

                // Draw count text above bar
                drawContext.canvas.nativeCanvas.drawText(
                    count.toString(),
                    barX + barWidthPx.toPx() / 2f,
                    barY - 5.dp.toPx(), // Small offset above the bar
                    textPaint
                )

                // Draw rating label below bar
                drawContext.canvas.nativeCanvas.drawText(
                    rating.toString(),
                    barX + barWidthPx.toPx() / 2f,
                    chartHeight.toPx() + 20.dp.toPx(), // Below the x-axis line placeholder
                    textPaint
                )
            }
        }
    }
}

@Composable
fun LearningStatsScreen(
    stats: LearningSessionStats,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), // Adjusted padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Session Complete!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text("Total cards reviewed: ${stats.totalCardsReviewed}", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Text("Your Scores:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        // Integrate the histogram
        RatingHistogram(
            ratingCounts = stats.ratingCounts,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // Specify a height for the histogram area
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(32.dp))
        Button(onClick = onBackToHome) {
            Text("Back to Deck Selection")
        }
    }
} 