package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.TransactionType
import com.example.domain.util.DateUtils
import com.example.domain.util.PdfReportExporter
import com.example.ui.components.DebtFormSheet
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.DeleteDebtConfirmDialog
import com.example.ui.components.DeleteRecurringConfirmDialog
import com.example.ui.components.OpeningBalanceDialog
import com.example.ui.components.PinLockScreen
import com.example.ui.components.PinSetupDialog
import com.example.ui.components.RecurringFormSheet
import com.example.ui.components.RestoreConfirmDialog
import com.example.ui.components.TransactionFormSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebtScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.HisabViewModel

object HisabRoutes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_HISTORY = "history"
    const val ROUTE_DEBTS = "debts"
    const val ROUTE_REPORTS = "reports"
    const val ROUTE_SETTINGS = "settings"
}

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val navItems = listOf(
    NavDestinationItem(
        route = HisabRoutes.ROUTE_DASHBOARD,
        label = "होम",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_dashboard"
    ),
    NavDestinationItem(
        route = HisabRoutes.ROUTE_HISTORY,
        label = "हिसाब",
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook,
        testTag = "nav_history"
    ),
    NavDestinationItem(
        route = HisabRoutes.ROUTE_DEBTS,
        label = "उधार",
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        testTag = "nav_debts"
    ),
    NavDestinationItem(
        route = HisabRoutes.ROUTE_REPORTS,
        label = "रिपोर्ट",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        testTag = "nav_reports"
    ),
    NavDestinationItem(
        route = HisabRoutes.ROUTE_SETTINGS,
        label = "सेटिंग्स",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
)

@Composable
fun BulletHisabApp(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: HisabRoutes.ROUTE_DASHBOARD

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState by viewModel.sheetState.collectAsStateWithLifecycle()
    val debtSheetState by viewModel.debtSheetState.collectAsStateWithLifecycle()
    val recurringSheetState by viewModel.recurringSheetState.collectAsStateWithLifecycle()
    val openingBalanceDialogOpen by viewModel.openingBalanceDialogOpen.collectAsStateWithLifecycle()
    val pinDialogOpen by viewModel.pinDialogOpen.collectAsStateWithLifecycle()
    val deleteCandidate by viewModel.deleteCandidate.collectAsStateWithLifecycle()
    val deleteDebtCandidate by viewModel.deleteDebtCandidate.collectAsStateWithLifecycle()
    val deleteRecurringCandidate by viewModel.deleteRecurringCandidate.collectAsStateWithLifecycle()
    val pendingRestore by viewModel.pendingRestore.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val jsonText = viewModel.buildJsonBackup()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonText.toByteArray(Charsets.UTF_8))
                }
                viewModel.notifyBackupExported("JSON")
            } catch (_: Exception) {
                viewModel.notifyBackupError("JSON बैकअप सेव करने में समस्या आई")
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csvText = viewModel.buildCsvBackup()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csvText.toByteArray(Charsets.UTF_8))
                }
                viewModel.notifyBackupExported("CSV")
            } catch (_: Exception) {
                viewModel.notifyBackupError("CSV बैकअप सेव करने में समस्या आई")
            }
        }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PdfReportExporter.writeAccountReportPdf(
                        outputStream = out,
                        summary = uiState.summary,
                        report = uiState.reportSummary,
                        entries = uiState.allEntries,
                        todayDateKey = uiState.todayDateKey
                    )
                }
                viewModel.notifyBackupExported("PDF रिपोर्ट")
            } catch (_: Exception) {
                viewModel.notifyBackupError("PDF रिपोर्ट बनाने में समस्या आई")
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val rawText = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                }
                if (rawText != null) {
                    viewModel.parseBackupContent(rawText, uri.lastPathSegment)
                } else {
                    viewModel.notifyBackupError("फ़ाइल पढ़ी नहीं जा सकी")
                }
            } catch (_: Exception) {
                viewModel.notifyBackupError("बैकअप फ़ाइल खोलने में समस्या आई")
            }
        }
    }

    fun launchPdfExport() {
        val dateStr = DateUtils.formatIsoDate(uiState.todayDateKey)
        exportPdfLauncher.launch("bullet_hisab_report_$dateStr.pdf")
    }

    fun launchCsvExport() {
        val dateStr = DateUtils.formatIsoDate(uiState.todayDateKey)
        exportCsvLauncher.launch("bullet_hisab_backup_$dateStr.csv")
    }

    if (uiState.isAppLocked) {
        PinLockScreen(
            onVerifyPin = viewModel::verifyAndUnlockPin,
            modifier = modifier
        )
        return
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = HisabRoutes.ROUTE_DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(HisabRoutes.ROUTE_DASHBOARD) {
                        DashboardScreen(
                            uiState = uiState,
                            onAddTransaction = { type -> viewModel.openAddTransaction(type) },
                            onEditOpeningBalance = { viewModel.openOpeningBalanceDialog() },
                            onEditTransaction = { tx -> viewModel.openEditTransaction(tx) },
                            onDeleteTransaction = { tx -> viewModel.requestDeleteTransaction(tx) },
                            onRecordRecurring = viewModel::recordDueRecurring,
                            onSkipRecurring = viewModel::skipDueRecurring,
                            onNavigateToHistory = { navigateToTab(HisabRoutes.ROUTE_HISTORY) }
                        )
                    }

                    composable(HisabRoutes.ROUTE_HISTORY) {
                        HistoryScreen(
                            uiState = uiState,
                            onPreviousMonth = viewModel::previousNotebookMonth,
                            onNextMonth = viewModel::nextNotebookMonth,
                            onToggleShowAllMonths = viewModel::toggleShowAllMonthsInDiary,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onTypeFilterChange = viewModel::setTypeFilter,
                            onAccountFilterChange = viewModel::setAccountFilter,
                            onCategoryFilterChange = viewModel::setCategoryFilter,
                            onDateFilterChange = viewModel::setDateFilter,
                            onClearFilters = viewModel::clearAllFilters,
                            onEditTransaction = { tx -> viewModel.openEditTransaction(tx) },
                            onDeleteTransaction = { tx -> viewModel.requestDeleteTransaction(tx) },
                            onQuickAddExpense = { viewModel.openAddTransaction(TransactionType.EXPENSE) }
                        )
                    }

                    composable(HisabRoutes.ROUTE_DEBTS) {
                        DebtScreen(
                            uiState = uiState,
                            onFilterType = viewModel::setDebtTypeFilter,
                            onAddDebt = viewModel::openAddDebt,
                            onEditDebt = viewModel::openEditDebt,
                            onToggleSettled = viewModel::toggleDebtStatus,
                            onDeleteDebt = viewModel::requestDeleteDebt
                        )
                    }

                    composable(HisabRoutes.ROUTE_REPORTS) {
                        ReportsScreen(
                            uiState = uiState,
                            onSelectPeriod = viewModel::selectReportPeriod,
                            onShiftYear = viewModel::shiftReportYear,
                            onExportPdf = { launchPdfExport() },
                            onExportCsv = { launchCsvExport() }
                        )
                    }

                    composable(HisabRoutes.ROUTE_SETTINGS) {
                        SettingsScreen(
                            uiState = uiState,
                            onEditOpeningBalance = { viewModel.openOpeningBalanceDialog() },
                            onAddRecurring = viewModel::openAddRecurring,
                            onEditRecurring = viewModel::openEditRecurring,
                            onToggleRecurringActive = viewModel::toggleRecurringActive,
                            onDeleteRecurring = viewModel::requestDeleteRecurring,
                            onRecordRecurring = viewModel::recordDueRecurring,
                            onOpenPinDialog = viewModel::openPinDialog,
                            onLockAppNow = viewModel::lockAppNow,
                            onExportJson = {
                                val dateStr = DateUtils.formatIsoDate(uiState.todayDateKey)
                                exportJsonLauncher.launch("bullet_hisab_backup_$dateStr.json")
                            },
                            onExportCsv = { launchCsvExport() },
                            onExportPdf = { launchPdfExport() },
                            onImportBackup = {
                                importBackupLauncher.launch(
                                    arrayOf(
                                        "application/json",
                                        "text/csv",
                                        "text/comma-separated-values",
                                        "text/plain",
                                        "*/*"
                                    )
                                )
                            },
                            onClearAllData = viewModel::clearAllData
                        )
                    }
                }
            }
        }
    }

    if (sheetState.isOpen) {
        TransactionFormSheet(
            initialType = sheetState.type,
            editingTransaction = sheetState.editingTransaction,
            todayDateKey = uiState.todayDateKey,
            defaultTimeMinutes = viewModel.currentDefaultTimeMinutes(),
            onDismiss = viewModel::closeTransactionSheet,
            onSave = viewModel::saveTransaction
        )
    }

    if (debtSheetState.isOpen) {
        DebtFormSheet(
            initialType = debtSheetState.initialType,
            editingDebt = debtSheetState.editingDebt,
            todayDateKey = uiState.todayDateKey,
            onDismiss = viewModel::closeDebtSheet,
            onSave = viewModel::saveDebt
        )
    }

    if (recurringSheetState.isOpen) {
        RecurringFormSheet(
            editingRecurring = recurringSheetState.editingRecurring,
            todayDateKey = uiState.todayDateKey,
            onDismiss = viewModel::closeRecurringSheet,
            onSave = viewModel::saveRecurring
        )
    }

    if (openingBalanceDialogOpen) {
        OpeningBalanceDialog(
            currentOpenings = uiState.summary.accountOpenings,
            onDismiss = viewModel::dismissOpeningBalanceDialog,
            onSave = viewModel::saveAccountOpenings
        )
    }

    if (pinDialogOpen) {
        PinSetupDialog(
            isPinCurrentlyEnabled = uiState.isPinEnabled,
            onDismiss = viewModel::dismissPinDialog,
            onEnablePin = viewModel::enablePin,
            onDisablePin = viewModel::disablePin
        )
    }

    deleteCandidate?.let { tx ->
        DeleteConfirmDialog(
            transaction = tx,
            onDismiss = viewModel::dismissDeleteDialog,
            onConfirmDelete = viewModel::confirmDeleteTransaction
        )
    }

    deleteDebtCandidate?.let { debt ->
        DeleteDebtConfirmDialog(
            debt = debt,
            onDismiss = viewModel::dismissDeleteDebtDialog,
            onConfirmDelete = viewModel::confirmDeleteDebt
        )
    }

    deleteRecurringCandidate?.let { rec ->
        DeleteRecurringConfirmDialog(
            entry = rec,
            onDismiss = viewModel::dismissDeleteRecurringDialog,
            onConfirmDelete = viewModel::confirmDeleteRecurring
        )
    }

    pendingRestore?.let { payload ->
        RestoreConfirmDialog(
            payload = payload,
            onDismiss = viewModel::dismissRestoreDialog,
            onReplaceExisting = { viewModel.applyRestore(replaceExisting = true) },
            onMergeWithExisting = { viewModel.applyRestore(replaceExisting = false) }
        )
    }
}
