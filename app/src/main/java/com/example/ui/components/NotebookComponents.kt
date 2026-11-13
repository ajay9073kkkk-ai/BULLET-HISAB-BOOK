package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.IncomeCategory
import com.example.domain.model.PaymentAccount
import com.example.ui.theme.LocalLedgerColors

fun ExpenseCategory.iconVector(): ImageVector {
    return when (this) {
        ExpenseCategory.FOOD -> Icons.Rounded.Restaurant
        ExpenseCategory.TRAVEL -> Icons.Rounded.DirectionsBus
        ExpenseCategory.EDUCATION -> Icons.Rounded.MenuBook
        ExpenseCategory.MOBILE -> Icons.Rounded.PhoneAndroid
        ExpenseCategory.HOME -> Icons.Rounded.Home
        ExpenseCategory.SHOPPING -> Icons.Rounded.ShoppingBag
        ExpenseCategory.OTHER -> Icons.Rounded.Category
    }
}

fun IncomeCategory.iconVector(): ImageVector {
    return when (this) {
        IncomeCategory.SALARY -> Icons.Rounded.Work
        IncomeCategory.BUSINESS -> Icons.Rounded.Storefront
        IncomeCategory.FREELANCE -> Icons.Rounded.AccountBalanceWallet
        IncomeCategory.INTEREST -> Icons.Rounded.TrendingUp
        IncomeCategory.GIFT -> Icons.Rounded.CardGiftcard
        IncomeCategory.OTHER -> Icons.Rounded.Category
    }
}

fun PaymentAccount.iconVector(): ImageVector {
    return when (this) {
        PaymentAccount.CASH -> Icons.Rounded.Payments
        PaymentAccount.BANK -> Icons.Rounded.AccountBalance
        PaymentAccount.UPI -> Icons.Rounded.QrCode2
    }
}

@Composable
fun Modifier.bulletGridBackground(): Modifier {
    val dotColor = LocalLedgerColors.current.ruledLine
    return this.drawBehind {
        val spacing = 22.dp.toPx()
        val radius = 1.2.dp.toPx()
        var x = spacing
        while (x < size.width) {
            var y = spacing
            while (y < size.height) {
                drawCircle(
                    color = dotColor,
                    radius = radius,
                    center = Offset(x, y)
                )
                y += spacing
            }
            x += spacing
        }
    }
}

@Composable
fun NotebookRuledCard(
    modifier: Modifier = Modifier,
    showMarginRule: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val ledgerColors = LocalLedgerColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ledgerColors.paperCard),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val lineSpacing = 28.dp.toPx()
                    var y = lineSpacing
                    while (y < size.height) {
                        drawLine(
                            color = ledgerColors.ruledLine.copy(alpha = 0.65f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        y += lineSpacing
                    }
                    if (showMarginRule) {
                        val marginX = 24.dp.toPx()
                        drawLine(
                            color = ledgerColors.marginLine.copy(alpha = 0.65f),
                            start = Offset(marginX, 0f),
                            end = Offset(marginX, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (showMarginRule) 34.dp else 16.dp,
                        end = 16.dp,
                        top = 14.dp,
                        bottom = 14.dp
                    ),
                content = content
            )
        }
    }
}

@Composable
fun BulletSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val ledgerColors = LocalLedgerColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(ledgerColors.brass, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun CategoryPill(
    label: String,
    icon: ImageVector?,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
