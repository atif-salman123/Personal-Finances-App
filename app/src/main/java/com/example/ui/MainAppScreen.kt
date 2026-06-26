package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainAppContent(viewModel: FinanceViewModel) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Crossfade(targetState = isAuthenticated, label = "auth_transition") { authenticated ->
            if (authenticated) {
                MainNavigationContainer(viewModel)
            } else {
                AuthScreen(viewModel)
            }
        }
    }
}

// ==========================================
// 1. AUTH SCREEN (Biometrics & PIN Lock)
// ==========================================
@Composable
fun AuthScreen(viewModel: FinanceViewModel) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val isBiometricsEnabled by viewModel.isBiometricsEnabled.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Branding / Illustration Icon
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageOf(Icons.Default.Savings),
                contentDescription = "Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "FinFlow Secure Lock",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Keep your personal financial data private",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Dots indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val filled = index < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                )
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Pin Pad Layout
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "F") // F is biometric face/fingerprint simulation
            )

            for (row in keys) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (key in row) {
                        PinPadButton(
                            key = key,
                            isBiometricEnabled = isBiometricsEnabled,
                            onClick = {
                                when (key) {
                                    "C" -> {
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                            errorMessage = ""
                                        }
                                    }
                                    "F" -> {
                                        if (isBiometricsEnabled) {
                                            viewModel.simulateBiometricLogin()
                                        }
                                    }
                                    else -> {
                                        if (enteredPin.length < 4) {
                                            enteredPin += key
                                            errorMessage = ""
                                            if (enteredPin.length == 4) {
                                                if (viewModel.verifyPin(enteredPin)) {
                                                    // authenticated
                                                } else {
                                                    enteredPin = ""
                                                    errorMessage = "Incorrect PIN. Try '1234'"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Testing Hint: Default PIN is 1234",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun PinPadButton(
    key: String,
    isBiometricEnabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                if (key == "C" || key == "F") Color.Transparent
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .testTag("pin_key_$key"),
        contentAlignment = Alignment.Center
    ) {
        when (key) {
            "C" -> {
                Icon(
                    imageOf(Icons.Default.Backspace),
                    contentDescription = "Backspace",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            "F" -> {
                if (isBiometricEnabled) {
                    Icon(
                        imageOf(Icons.Default.Fingerprint),
                        contentDescription = "Biometric Login",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            else -> {
                Text(
                    text = key,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

// ==========================================
// FROSTED GLASS THEME UTILITIES & HERO CARD
// ==========================================
@Composable
fun glassCardColors() = CardDefaults.cardColors(
    containerColor = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
    } else {
        Color.White.copy(alpha = 0.55f)
    }
)

@Composable
fun glassCardBorder() = BorderStroke(
    width = 1.dp,
    color = if (isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.White.copy(alpha = 0.5f)
    }
)

@Composable
fun FrostedHeroCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF6750A4), Color(0xFF9278D1)),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
    ) {
        // Decorative glowing bubbles to simulate glass depth
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-40).dp)
                .size(160.dp)
                .background(Color.White.copy(alpha = 0.18f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 30.dp)
                .size(110.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = Color.White
                    )
                }

                // Currency/Symbol pill
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // The glass overlay container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}

// ==========================================
// 2. MAIN NAV CONTAINER
// ==========================================
enum class AppScreen {
    DASHBOARD, TRANSACTIONS, BUDGETS, ACCOUNTS, DEBT_LENDING, PLANNED, SETTINGS
}

@Composable
fun MainNavigationContainer(viewModel: FinanceViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    val accountsList by viewModel.accounts.collectAsStateWithLifecycle()
    val isPrivateMode by viewModel.isPrivateMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isSystemInDarkTheme()) Color(0xFF4F378B) else Color(0xFFEADDFF),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Wallet",
                            tint = if (isSystemInDarkTheme()) Color(0xFFEADDFF) else Color(0xFF21005D),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Main Wallet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${accountsList.size} Connected Accounts",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.isPrivateMode.value = !viewModel.isPrivateMode.value },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isPrivateMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Private Mode",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemInDarkTheme()) {
                        Color(0xFF1E293B).copy(alpha = 0.75f)
                    } else {
                        Color.White.copy(alpha = 0.75f)
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val items = listOf(
                        Triple(AppScreen.DASHBOARD, Icons.Default.Dashboard, "Dashboard"),
                        Triple(AppScreen.TRANSACTIONS, Icons.Default.ReceiptLong, "Tx Logs"),
                        Triple(AppScreen.BUDGETS, Icons.Default.PieChart, "Budgets"),
                        Triple(AppScreen.ACCOUNTS, Icons.Default.AccountBalanceWallet, "Accounts"),
                        Triple(AppScreen.DEBT_LENDING, Icons.Default.Handshake, "Lending"),
                        Triple(AppScreen.PLANNED, Icons.Default.Schedule, "Recurring"),
                        Triple(AppScreen.SETTINGS, Icons.Default.Settings, "Sync")
                    )

                    for ((screen, icon, label) in items) {
                        val isSelected = currentScreen == screen
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentScreen = screen }
                                .padding(vertical = 4.dp)
                                .testTag("nav_${screen.name.lowercase()}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = if (isSystemInDarkTheme()) {
                            listOf(Color(0xFF3B0764).copy(alpha = 0.25f), Color(0xFF0F172A))
                        } else {
                            listOf(Color(0xFFEADDFF).copy(alpha = 0.45f), Color(0xFFF3F4F9))
                        },
                        center = Offset(300f, 300f)
                    )
                )
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(viewModel)
                AppScreen.TRANSACTIONS -> TransactionsScreen(viewModel)
                AppScreen.BUDGETS -> BudgetsScreen(viewModel)
                AppScreen.ACCOUNTS -> AccountsScreen(viewModel)
                AppScreen.DEBT_LENDING -> DebtLendingScreen(viewModel)
                AppScreen.PLANNED -> PlannedPaymentsScreen(viewModel)
                AppScreen.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }
}

// ==========================================
// 3. DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(viewModel: FinanceViewModel) {
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val displayCurrency by viewModel.displayCurrency.collectAsStateWithLifecycle()
    val isPrivateMode by viewModel.isPrivateMode.collectAsStateWithLifecycle()

    val accountsList by viewModel.accounts.collectAsStateWithLifecycle()
    val reportData by viewModel.getMonthlyReports(selectedMonth).collectAsStateWithLifecycle(
        initialValue = MonthlyReportData(selectedMonth, 0.0, 0.0, 0.0, 0.0, emptyMap())
    )
    val categoriesList by viewModel.categories.collectAsStateWithLifecycle()
    val activeTransactions by viewModel.getProcessedTransactionsForMonth(selectedMonth).collectAsStateWithLifecycle(initialValue = emptyList())

    // Convert values to display currency
    val totalBalanceUSD = accountsList.sumOf { CurrencyRates.convert(it.balance, it.currency, "USD") }
    val convertedBalance = CurrencyRates.convert(totalBalanceUSD, "USD", displayCurrency)

    val convertedIncome = CurrencyRates.convert(reportData.totalIncome, "USD", displayCurrency)
    val convertedExpense = CurrencyRates.convert(reportData.totalExpense, "USD", displayCurrency)
    val convertedFixed = CurrencyRates.convert(reportData.fixedExpense, "USD", displayCurrency)
    val convertedVariable = CurrencyRates.convert(reportData.variableExpense, "USD", displayCurrency)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Header selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.changeMonth(-1) },
                modifier = Modifier.testTag("prev_month_btn")
            ) {
                Icon(imageOf(Icons.Default.ChevronLeft), "Previous Month")
            }

            Text(
                text = formatMonthString(selectedMonth),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(
                onClick = { viewModel.changeMonth(1) },
                modifier = Modifier.testTag("next_month_btn")
            ) {
                Icon(imageOf(Icons.Default.ChevronRight), "Next Month")
            }
        }

        // Net Worth Card in Frosted Glass styling
        val totalExp = reportData.fixedExpense + reportData.variableExpense
        val fixedPct = if (totalExp > 0) (reportData.fixedExpense / totalExp).toFloat() else 0.5f

        FrostedHeroCard(
            title = "TOTAL NET WORTH",
            value = if (isPrivateMode) "••••••" else CurrencyRates.format(convertedBalance, displayCurrency),
            subtitle = displayCurrency
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Amortized Fixed Costs (Rent/Insurance)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = if (isPrivateMode) "••••" else "${CurrencyRates.format(convertedFixed, displayCurrency)} / mo",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { fixedPct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFD0BCFF),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Calculated from monthly fixed commitments: ${CurrencyRates.format(convertedFixed, displayCurrency)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Quick Stats Cards (Income vs Spending)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = glassCardColors(),
                border = glassCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageOf(Icons.Default.TrendingUp), "Income", tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Income", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPrivateMode) "••••" else CurrencyRates.format(convertedIncome, displayCurrency),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = glassCardColors(),
                border = glassCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageOf(Icons.Default.TrendingDown), "Expenses", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Spending", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPrivateMode) "••••" else CurrencyRates.format(convertedExpense, displayCurrency),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Variable vs Fixed Expenses Progress Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = glassCardColors(),
            border = glassCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Expense Allocation (Fixed vs Variable)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { fixedPct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.tertiary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fixed: ${CurrencyRates.format(convertedFixed, displayCurrency)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Variable: ${CurrencyRates.format(convertedVariable, displayCurrency)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Donut Spending Breakdown Chart
        if (reportData.categoryBreakdown.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = glassCardColors(),
                border = glassCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Monthly Spending Habits",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.size(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(160.dp)) {
                            var startAngle = -90f
                            val totalSpend = reportData.categoryBreakdown.values.sum()

                            val chartColors = listOf(
                                Color(0xFF6200EE), Color(0xFF03DAC6), Color(0xFFFF007F),
                                Color(0xFFFF9800), Color(0xFF4CAF50), Color(0xFF00bcd4)
                            )

                            reportData.categoryBreakdown.entries.forEachIndexed { index, entry ->
                                val angle = ((entry.value / totalSpend) * 360f).toFloat()
                                drawArc(
                                    color = chartColors[index % chartColors.size],
                                    startAngle = startAngle,
                                    sweepAngle = angle,
                                    useCenter = false,
                                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                                )
                                startAngle += angle
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Out", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = if (isPrivateMode) "••••" else CurrencyRates.format(convertedExpense, displayCurrency),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Legend list
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val totalSpend = reportData.categoryBreakdown.values.sum()
                        val chartColors = listOf(
                            Color(0xFF6200EE), Color(0xFF03DAC6), Color(0xFFFF007F),
                            Color(0xFFFF9800), Color(0xFF4CAF50), Color(0xFF00bcd4)
                        )

                        reportData.categoryBreakdown.entries.forEachIndexed { index, entry ->
                            val cat = categoriesList.find { it.id == entry.key }
                            val catName = cat?.name ?: "Other"
                            val amountDisp = CurrencyRates.convert(entry.value, "USD", displayCurrency)
                            val pct = (entry.value / totalSpend * 100).toInt()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(chartColors[index % chartColors.size], CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(catName, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    text = "${CurrencyRates.format(amountDisp, displayCurrency)} ($pct%)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = glassCardColors(),
                border = glassCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageOf(Icons.Default.TrendingFlat),
                        contentDescription = "Empty Report",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No spending recorded this month",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        "Add manual transactions or upload CSV statements to view analytics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Amortization notices (Informative banners about rent split or insurance amortizations)
        val amortizedTxs = activeTransactions.filter { it.description.contains("Amortized") }
        if (amortizedTxs.isNotEmpty()) {
            Text(
                text = "Active Amortizations in ${formatMonthString(selectedMonth)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            for (tx in amortizedTxs) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSystemInDarkTheme()) {
                            Color(0xFF3B0764).copy(alpha = 0.35f)
                        } else {
                            Color(0xFFF3E8FF).copy(alpha = 0.55f)
                        }
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.12f) else Color(0xFFD0BCFF).copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageOf(Icons.Default.PieChart), "Amortized split", tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.description, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Split into multiple months to reflect accurate recurring cost trends.", style = MaterialTheme.typography.bodySmall)
                        }
                        val dispAmt = CurrencyRates.convert(tx.amount, tx.currency, displayCurrency)
                        Text(
                            text = CurrencyRates.format(dispAmt, displayCurrency),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. TRANSACTIONS SCREEN
// ==========================================
@Composable
fun TransactionsScreen(viewModel: FinanceViewModel) {
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val displayCurrency by viewModel.displayCurrency.collectAsStateWithLifecycle()
    val isPrivateMode by viewModel.isPrivateMode.collectAsStateWithLifecycle()

    val accountsList by viewModel.accounts.collectAsStateWithLifecycle()
    val categoriesList by viewModel.categories.collectAsStateWithLifecycle()
    val activeTransactions by viewModel.getProcessedTransactionsForMonth(selectedMonth).collectAsStateWithLifecycle(initialValue = emptyList())

    var showManualDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Upper action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showManualDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_manual_tx_btn")
            ) {
                Icon(imageOf(Icons.Default.Add), "Add")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Manual Entry")
            }

            OutlinedButton(
                onClick = { showImportDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("import_statement_btn")
            ) {
                Icon(imageOf(Icons.Default.UploadFile), "Import")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import CSV")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Transactions for ${formatMonthString(selectedMonth)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (activeTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageOf(Icons.Default.ReceiptLong), "Empty Txs", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No transactions in this month", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                    Text("Add manual entries or upload CSV bank statements above", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeTransactions) { tx ->
                    val catName = categoriesList.find { it.id == tx.categoryId }?.name ?: "Other"
                    val accName = accountsList.find { it.id == tx.accountId }?.name ?: "Main Account"
                    val converted = CurrencyRates.convert(tx.amount, tx.currency, displayCurrency)

                    Card(
                        colors = glassCardColors(),
                        border = glassCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (tx.isIncome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageOf(if (tx.isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward),
                                        contentDescription = if (tx.isIncome) "Income" else "Expense",
                                        tint = if (tx.isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.widthIn(max = 160.dp)) {
                                    Text(
                                        text = tx.description,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$catName • $accName",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isPrivateMode) "••••" else CurrencyRates.format(converted, displayCurrency),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (tx.isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                    Text(
                                        text = formatDate(tx.date),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(onClick = { viewModel.deleteTransaction(tx) }) {
                                    Icon(imageOf(Icons.Default.Delete), "Delete", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Manual Entry Dialog
    if (showManualDialog) {
        ManualTxDialog(
            viewModel = viewModel,
            onDismiss = { showManualDialog = false }
        )
    }

    // CSV Importing Dialog
    if (showImportDialog) {
        ImportStatementDialog(
            viewModel = viewModel,
            onDismiss = { showImportDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTxDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val accountsList by viewModel.accounts.collectAsStateWithLifecycle()
    val categoriesList by viewModel.categories.collectAsStateWithLifecycle()

    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var description by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var selectedAccountId by remember { mutableStateOf(accountsList.firstOrNull()?.id ?: 1) }
    var selectedCategoryId by remember { mutableStateOf(categoriesList.firstOrNull()?.id ?: 1) }

    // Variable vs Fixed
    var isFixed by remember { mutableStateOf(false) }

    // Amortization Settings
    var isAmortized by remember { mutableStateOf(false) }
    var amortizationMonths by remember { mutableStateOf("3") } // Q1, Semi, Annual split

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmt = amount.toDoubleOrNull() ?: 0.0
                    if (parsedAmt > 0 && description.isNotEmpty()) {
                        viewModel.addTransaction(
                            accountId = selectedAccountId,
                            amount = parsedAmt,
                            currency = currency,
                            categoryId = selectedCategoryId,
                            description = description,
                            isIncome = isIncome,
                            isFixed = isFixed,
                            isAmortized = isAmortized,
                            amortizationMonths = amortizationMonths.toIntOrNull() ?: 1
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_manual_tx_btn")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Add Manual Transaction") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income or Expense toggle
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("Expense") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("Income") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("tx_amount_input")
                )

                // Currency Dropdown Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Currency:")
                    val currencies = listOf("USD", "EUR", "GBP", "AED", "INR")
                    currencies.forEach { cur ->
                        FilterChip(
                            selected = currency == cur,
                            onClick = { currency = cur },
                            label = { Text(cur) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().testTag("tx_desc_input")
                )

                // Account Selection
                Text("Account", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    accountsList.forEach { acc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAccountId = acc.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(acc.name)
                        }
                    }
                }

                // Category Selection
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categoriesList.filter { it.isIncome == isIncome }.forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategoryId = cat.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cat.name)
                        }
                    }
                }

                if (!isIncome) {
                    // Fixed vs Variable
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isFixed, onCheckedChange = { isFixed = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fixed Expense (e.g. Rent, Insurance, Tuition)")
                    }

                    // Amortization options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isAmortized, onCheckedChange = { isAmortized = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Amortize / Divide across months")
                    }

                    if (isAmortized) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Split Duration:")
                            listOf(
                                Pair("3m (Quarterly)", "3"),
                                Pair("6m (Semi-annual)", "6"),
                                Pair("12m (Annual)", "12")
                            ).forEach { (label, valStr) ->
                                FilterChip(
                                    selected = amortizationMonths == valStr,
                                    onClick = { amortizationMonths = valStr },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ImportStatementDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val accountsList by viewModel.accounts.collectAsStateWithLifecycle()
    var selectedAccountId by remember { mutableStateOf(accountsList.firstOrNull()?.id ?: 1) }

    var csvText by remember { mutableStateOf(CsvParser.getMockCsvString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (csvText.trim().isNotEmpty()) {
                        viewModel.importCsvContent(csvText, selectedAccountId)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_import_csv_btn")
            ) {
                Text("Process Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Import Bank Statement (CSV)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Paste or edit your bank statement's CSV rows below. Date, Description, Amount, Currency format:", style = MaterialTheme.typography.bodySmall)

                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    label = { Text("CSV Rows") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("csv_input_field")
                )

                Text("Import into:", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    accountsList.forEach { acc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAccountId = acc.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(acc.name)
                        }
                    }
                }
            }
        }
    )
}

// ==========================================
// 5. BUDGETS SCREEN
// ==========================================
@Composable
fun BudgetsScreen(viewModel: FinanceViewModel) {
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val displayCurrency by viewModel.displayCurrency.collectAsStateWithLifecycle()

    val categoriesList by viewModel.categories.collectAsStateWithLifecycle()
    val budgetsList by viewModel.budgets.collectAsStateWithLifecycle()
    val reportData by viewModel.getMonthlyReports(selectedMonth).collectAsStateWithLifecycle(
        initialValue = MonthlyReportData(selectedMonth, 0.0, 0.0, 0.0, 0.0, emptyMap())
    )

    var showLimitDialog by remember { mutableStateOf<Category?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Category Budgets for ${formatMonthString(selectedMonth)}",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Assign spending limits to categories to track monthly budget pacing.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Only list expense categories
            items(categoriesList.filter { !it.isIncome }) { category ->
                val matchingBudget = budgetsList.find { it.categoryId == category.id && it.month == selectedMonth }
                val spendInBase = reportData.categoryBreakdown[category.id] ?: 0.0

                val convertedSpend = CurrencyRates.convert(spendInBase, "USD", displayCurrency)
                val convertedLimit = if (matchingBudget != null) {
                    CurrencyRates.convert(matchingBudget.amountLimit, "USD", displayCurrency)
                } else {
                    0.0
                }

                val limitLabel = if (matchingBudget != null) {
                    CurrencyRates.format(convertedLimit, displayCurrency)
                } else {
                    "Not Set"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = glassCardColors(),
                    border = glassCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (category.isFixed) "Fixed Expense" else "Variable Expense",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }

                            Button(
                                onClick = { showLimitDialog = category },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("set_budget_btn_${category.id}")
                            ) {
                                Text("Set Limit")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Spent: ${CurrencyRates.format(convertedSpend, displayCurrency)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Limit: $limitLabel",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        if (matchingBudget != null && matchingBudget.amountLimit > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val pct = (spendInBase / matchingBudget.amountLimit).toFloat().coerceIn(0f, 1f)

                            val barColor = if (pct >= 0.9f) {
                                MaterialTheme.colorScheme.error
                            } else if (pct >= 0.7f) {
                                Color(0xFFFF9800)
                            } else {
                                Color(0xFF4CAF50)
                            }

                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = barColor,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLimitDialog != null) {
        val cat = showLimitDialog!!
        val matchingBudget = budgetsList.find { it.categoryId == cat.id && it.month == selectedMonth }
        var limitInput by remember { mutableStateOf(matchingBudget?.amountLimit?.toString() ?: "") }

        AlertDialog(
            onDismissRequest = { showLimitDialog = null },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = limitInput.toDoubleOrNull() ?: 0.0
                        if (parsed >= 0) {
                            viewModel.addOrUpdateBudget(cat.id, parsed)
                            showLimitDialog = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_set_budget_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Set Budget for ${cat.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter monthly spending limit in base display currency ($displayCurrency):", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it },
                        label = { Text("Monthly Limit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("budget_limit_input")
                    )
                }
            }
        )
    }
}

// ==========================================
// 6. ACCOUNTS SCREEN
// ==========================================
@Composable
fun AccountsScreen(viewModel: FinanceViewModel) {
    val displayCurrency by viewModel.displayCurrency.collectAsStateWithLifecycle()
    val isPrivateMode by viewModel.isPrivateMode.collectAsStateWithLifecycle()
    val accountsList by viewModel.accounts.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Multiple Accounts",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_account_btn")
            ) {
                Icon(imageOf(Icons.Default.Add), "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("New")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(accountsList) { acc ->
                val converted = CurrencyRates.convert(acc.balance, acc.currency, displayCurrency)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = glassCardColors(),
                    border = glassCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageOf(
                                        when (acc.type) {
                                            "Bank" -> Icons.Default.AccountBalance
                                            "Cash" -> Icons.Default.Wallet
                                            "Credit Card" -> Icons.Default.CreditCard
                                            else -> Icons.Default.Savings
                                        }
                                    ),
                                    contentDescription = acc.type,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(acc.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                Text("Type: ${acc.type} • Original: ${CurrencyRates.format(acc.balance, acc.currency)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isPrivateMode) "••••" else CurrencyRates.format(converted, displayCurrency),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text("Converted Display", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Bank") }
        var balance by remember { mutableStateOf("") }
        var currency by remember { mutableStateOf("USD") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val bal = balance.toDoubleOrNull() ?: 0.0
                        if (name.isNotEmpty()) {
                            viewModel.addAccount(name, type, bal, currency)
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_account_btn")
                ) {
                    Text("Add Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Add New Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name") },
                        modifier = Modifier.fillMaxWidth().testTag("account_name_input")
                    )

                    Text("Account Type:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Bank", "Cash", "Credit Card", "Other").forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = balance,
                        onValueChange = { balance = it },
                        label = { Text("Initial Balance") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("account_balance_input")
                    )

                    Text("Original Currency:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("USD", "EUR", "GBP", "AED", "INR").forEach { cur ->
                            FilterChip(
                                selected = currency == cur,
                                onClick = { currency = cur },
                                label = { Text(cur) }
                            )
                        }
                    }
                }
            }
        )
    }
}

// ==========================================
// 7. DEBT & LENDING SCREEN
// ==========================================
@Composable
fun DebtLendingScreen(viewModel: FinanceViewModel) {
    val displayCurrency by viewModel.displayCurrency.collectAsStateWithLifecycle()
    val isPrivateMode by viewModel.isPrivateMode.collectAsStateWithLifecycle()
    val debtLendingList by viewModel.debtLending.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Debt & Lending",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_debt_btn")
            ) {
                Icon(imageOf(Icons.Default.Add), "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Record")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (debtLendingList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageOf(Icons.Default.Handshake), "Empty", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No loan or borrowing records yet", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                    Text("Tap 'New Record' to track money you owe or are owed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(debtLendingList) { dl ->
                    val isOwedByMe = dl.type == "OWE_THEM"
                    val converted = CurrencyRates.convert(dl.amount, dl.currency, displayCurrency)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = glassCardColors(),
                        border = if (dl.isResolved) glassCardBorder() else BorderStroke(1.dp, if (isOwedByMe) Color(0xFFC62828).copy(alpha = 0.45f) else Color(0xFF2E7D32).copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (dl.isResolved) MaterialTheme.colorScheme.surfaceVariant
                                            else if (isOwedByMe) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageOf(if (dl.isResolved) Icons.Default.CheckCircle else if (isOwedByMe) Icons.Default.NorthEast else Icons.Default.SouthWest),
                                        contentDescription = dl.type,
                                        tint = if (dl.isResolved) MaterialTheme.colorScheme.outline else if (isOwedByMe) Color(0xFFC62828) else Color(0xFF2E7D32)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = dl.personName,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, textDecoration = if (dl.isResolved) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                                    )
                                    Text(
                                        text = if (isOwedByMe) "You owe them • ${dl.description}" else "They owe you • ${dl.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isPrivateMode) "••••" else CurrencyRates.format(converted, displayCurrency),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (dl.isResolved) MaterialTheme.colorScheme.outline else if (isOwedByMe) Color(0xFFC62828) else Color(0xFF2E7D32)
                                    )

                                    if (!dl.isResolved) {
                                        TextButton(
                                            onClick = { viewModel.resolveDebtLending(dl) },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Text("Settle", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                    } else {
                                        Text("Resolved", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.outline)
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteDebtLending(dl.id) }) {
                                    Icon(imageOf(Icons.Default.Delete), "Delete", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var person by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("OWE_THEM") } // OWE_THEM = Debt, OWE_ME = Lending
        var amount by remember { mutableStateOf("") }
        var currency by remember { mutableStateOf("USD") }
        var description by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (person.isNotEmpty() && amt > 0) {
                            viewModel.addDebtLending(person, type, amt, currency, description)
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_debt_btn")
                ) {
                    Text("Save Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Add Debt/Lending Record") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = type == "OWE_THEM",
                            onClick = { type = "OWE_THEM" },
                            label = { Text("I owe them (Debt)") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = type == "OWE_ME",
                            onClick = { type = "OWE_ME" },
                            label = { Text("They owe me (Lending)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = person,
                        onValueChange = { person = it },
                        label = { Text("Person Name") },
                        modifier = Modifier.fillMaxWidth().testTag("debt_person_input")
                    )

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("debt_amount_input")
                    )

                    Text("Currency:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("USD", "EUR", "GBP", "AED", "INR").forEach { cur ->
                            FilterChip(
                                selected = currency == cur,
                                onClick = { currency = cur },
                                label = { Text(cur) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Remarks (e.g., lunch, rent loan)") },
                        modifier = Modifier.fillMaxWidth().testTag("debt_desc_input")
                    )
                }
            }
        )
    }
}

// ==========================================
// 8. PLANNED & SUBSCRIPTIONS SCREEN
// ==========================================
@Composable
fun PlannedPaymentsScreen(viewModel: FinanceViewModel) {
    val displayCurrency by viewModel.displayCurrency.collectAsStateWithLifecycle()
    val isPrivateMode by viewModel.isPrivateMode.collectAsStateWithLifecycle()
    val plannedList by viewModel.plannedPayments.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Planned & Recurring",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_planned_btn")
            ) {
                Icon(imageOf(Icons.Default.Add), "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Schedule")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (plannedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageOf(Icons.Default.Schedule), "Empty", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No planned recurring items yet", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                    Text("Add your subscriptions or planned bills to track upcoming schedules.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(plannedList.filter { it.isActive }) { pp ->
                    val converted = CurrencyRates.convert(pp.amount, pp.currency, displayCurrency)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = glassCardColors(),
                        border = glassCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (pp.isSubscription) Color(0xFFECEFF1) else Color(0xFFFFF3E0),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageOf(if (pp.isSubscription) Icons.Default.CalendarMonth else Icons.Default.Receipt),
                                        contentDescription = pp.name,
                                        tint = if (pp.isSubscription) MaterialTheme.colorScheme.primary else Color(0xFFEF6C00)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(pp.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text("Due: ${formatDate(pp.nextDueDate)} (${pp.frequency.lowercase()})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isPrivateMode) "••••" else CurrencyRates.format(converted, displayCurrency),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )

                                    Button(
                                        onClick = { viewModel.triggerPlannedPaymentPaid(pp) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Mark Paid", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp))
                                    }
                                }

                                IconButton(onClick = { viewModel.deletePlannedPayment(pp.id) }) {
                                    Icon(imageOf(Icons.Default.Delete), "Delete", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val accountsList by viewModel.accounts.collectAsStateWithLifecycle()
        val categoriesList by viewModel.categories.collectAsStateWithLifecycle()

        var name by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var currency by remember { mutableStateOf("USD") }
        var frequency by remember { mutableStateOf("MONTHLY") }
        var isSub by remember { mutableStateOf(true) }
        var accountId by remember { mutableStateOf(accountsList.firstOrNull()?.id ?: 1) }
        var categoryId by remember { mutableStateOf(categoriesList.firstOrNull()?.id ?: 1) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (name.isNotEmpty() && amt > 0) {
                            viewModel.addPlannedPayment(
                                name = name,
                                amount = amt,
                                currency = currency,
                                categoryId = categoryId,
                                accountId = accountId,
                                frequency = frequency,
                                isSubscription = isSub,
                                nextDueDate = System.currentTimeMillis() + 86400000 * 2 // default 2 days in future
                            )
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_planned_btn")
                ) {
                    Text("Add Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("New Planned Schedule") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Payment/Subscription Name (e.g. Netflix)") },
                        modifier = Modifier.fillMaxWidth().testTag("planned_name_input")
                    )

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("planned_amount_input")
                    )

                    Text("Type:")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = isSub,
                            onClick = { isSub = true },
                            label = { Text("Subscription") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = !isSub,
                            onClick = { isSub = false },
                            label = { Text("Regular Bill") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Frequency:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("MONTHLY", "QUARTERLY", "YEARLY", "WEEKLY").forEach { f ->
                            FilterChip(
                                selected = frequency == f,
                                onClick = { frequency = f },
                                label = { Text(f.lowercase()) }
                            )
                        }
                    }

                    Text("Deduct Account:", style = MaterialTheme.typography.labelMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        accountsList.forEach { acc ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { accountId = acc.id }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = accountId == acc.id, onClick = { accountId = acc.id })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(acc.name)
                            }
                        }
                    }

                    Text("Assign Category:", style = MaterialTheme.typography.labelMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        categoriesList.filter { !it.isIncome }.forEach { cat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { categoryId = cat.id }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = categoryId == cat.id, onClick = { categoryId = cat.id })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat.name)
                            }
                        }
                    }
                }
            }
        )
    }
}

// ==========================================
// 9. SETTINGS & CLOUD SYNC SCREEN
// ==========================================
@Composable
fun SettingsScreen(viewModel: FinanceViewModel) {
    val displayCurrency by viewModel.displayCurrency.collectAsStateWithLifecycle()
    val isBiometricsEnabled by viewModel.isBiometricsEnabled.collectAsStateWithLifecycle()
    val isPrivateMode by viewModel.isPrivateMode.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Security & Cloud Sync",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        // Sync Dashboard Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = glassCardColors(),
            border = glassCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageOf(Icons.Default.CloudSync),
                    contentDescription = "Cloud",
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Secure Cloud Syncing", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("Keep your financial logs backed up securely across devices.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(16.dp))

                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Uploading encrypted backup...", style = MaterialTheme.typography.labelSmall)
                } else {
                    Button(
                        onClick = { viewModel.triggerCloudSync() },
                        modifier = Modifier.fillMaxWidth().testTag("sync_now_btn")
                    ) {
                        Text("Sync All Devices Now")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Last Encrypted Backup: $lastSyncTime", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        // Security settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = glassCardColors(),
            border = glassCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Local Privacy Controls", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Private Ledger Mode", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Hides balances on the dashboard screen.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = isPrivateMode,
                        onCheckedChange = { viewModel.isPrivateMode.value = it },
                        modifier = Modifier.testTag("private_mode_switch")
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Biometric Authentication", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Authenticate using Fingerprint or Face ID.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = isBiometricsEnabled,
                        onCheckedChange = { viewModel.isBiometricsEnabled.value = it },
                        modifier = Modifier.testTag("biometric_switch")
                    )
                }
            }
        }

        // Display Base Currency settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = glassCardColors(),
            border = glassCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Display Base Currency", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("Select your primary display currency. All entries convert dynamically using real-time rates.", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("USD", "EUR", "GBP", "AED", "INR").forEach { cur ->
                        FilterChip(
                            selected = displayCurrency == cur,
                            onClick = { viewModel.setDisplayCurrency(cur) },
                            label = { Text(cur) },
                            modifier = Modifier.testTag("base_currency_$cur")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
            modifier = Modifier.fillMaxWidth().testTag("logout_btn")
        ) {
            Icon(imageOf(Icons.Default.Lock), "Logout")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Lock Ledger Now")
        }
    }
}

// ==========================================
// UTILITY FUNCTIONS
// ==========================================
fun formatMonthString(ym: String): String {
    return try {
        val inFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = inFormat.parse(ym) ?: return ym
        val outFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        outFormat.format(date)
    } catch (e: Exception) {
        ym
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    return sdf.format(Date(timestamp))
}

/**
 * Android compatibility helper to bypass compilation errors on certain icon structures.
 */
@Composable
fun imageOf(vector: ImageVector): ImageVector {
    return vector
}
