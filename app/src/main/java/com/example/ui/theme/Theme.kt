package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class LedgerColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val brass: Color,
    val brassContainer: Color,
    val ruledLine: Color,
    val marginLine: Color,
    val paperCard: Color
)

private val LightLedgerColors = LedgerColors(
    income = IncomeGreen,
    incomeContainer = IncomeGreenContainer,
    expense = ExpenseRed,
    expenseContainer = ExpenseRedContainer,
    brass = AntiqueBrass,
    brassContainer = AntiqueBrassContainer,
    ruledLine = PaperRuledLine,
    marginLine = MarginTerracottaLine,
    paperCard = WarmPaperCard
)

private val DarkLedgerColors = LedgerColors(
    income = DarkIncomeGreen,
    incomeContainer = DarkIncomeGreenContainer,
    expense = DarkExpenseRed,
    expenseContainer = DarkExpenseRedContainer,
    brass = DarkBrass,
    brassContainer = DarkBrassContainer,
    ruledLine = DarkRuledLine,
    marginLine = DarkMarginLine,
    paperCard = DarkLeatherCard
)

val LocalLedgerColors = staticCompositionLocalOf { LightLedgerColors }

private val LightColorScheme = lightColorScheme(
    primary = ForestEmerald,
    onPrimary = Color.White,
    primaryContainer = ForestEmeraldContainer,
    onPrimaryContainer = OnForestEmeraldContainer,
    secondary = AntiqueBrass,
    onSecondary = Color.White,
    secondaryContainer = AntiqueBrassContainer,
    onSecondaryContainer = OnAntiqueBrassContainer,
    tertiary = ForestEmeraldLight,
    onTertiary = Color.White,
    background = WarmPaperBackground,
    onBackground = InkCharcoal,
    surface = WarmPaperCard,
    onSurface = InkCharcoal,
    surfaceVariant = WarmPaperElevated,
    onSurfaceVariant = InkMutedSepia,
    outline = PaperOutline,
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = ExpenseRedContainer,
    onErrorContainer = Color(0xFF5F140D)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkForestPrimary,
    onPrimary = Color(0xFF0A2418),
    primaryContainer = DarkForestContainer,
    onPrimaryContainer = Color(0xFFC8EBD7),
    secondary = DarkBrass,
    onSecondary = Color(0xFF2B1C00),
    secondaryContainer = DarkBrassContainer,
    onSecondaryContainer = Color(0xFFFCE2B0),
    tertiary = DarkForestPrimary,
    background = DarkLeatherBackground,
    onBackground = DarkInkCream,
    surface = DarkLeatherCard,
    onSurface = DarkInkCream,
    surfaceVariant = DarkLeatherElevated,
    onSurfaceVariant = DarkInkMuted,
    outline = DarkOutline,
    error = DarkExpenseRed,
    onError = Color(0xFF3E0A06),
    errorContainer = DarkExpenseRedContainer,
    onErrorContainer = Color(0xFFFFDAD6)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BulletHisabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val ledgerColors = if (darkTheme) DarkLedgerColors else LightLedgerColors

    CompositionLocalProvider(LocalLedgerColors provides ledgerColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
