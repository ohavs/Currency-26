package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.getCurrencyInfo
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val colorTheme by viewModel.colorTheme.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("הגדרות", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
                        HorizontalDivider()
                        NavigationDrawerItem(
                            label = { Text("הוסף וידג'ט לדף הבית") },
                            selected = false,
                            onClick = { 
                                viewModel.addWidgetToHomeScreen()
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("מצב תצוגה", style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { viewModel.setThemeMode(if (themeMode == "dark") "light" else "dark") }) {
                                Icon(
                                    imageVector = if (themeMode == "dark") androidx.compose.material.icons.Icons.Default.LightMode else androidx.compose.material.icons.Icons.Default.DarkMode,
                                    contentDescription = "שנה מצב תצוגה"
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text("ערכת צבעים", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleSmall)
                        val themes = listOf(
                            "standard" to Color.White, 
                            "ocean" to Color(0xFF006C52), 
                            "forest" to Color(0xFF376A20), 
                            "sunset" to Color(0xFF9E4200),
                            "rose" to Color(0xFF904A4C),
                            "lavender" to Color(0xFF4758A9)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            themes.forEach { (theme, color) ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(1.dp, Color.Gray, CircleShape)
                                        .clickable { viewModel.setColorTheme(theme) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (colorTheme == theme) {
                                        val checkColor = if (theme == "standard") Color.Black else Color.White
                                        Icon(androidx.compose.material.icons.Icons.Default.Check, contentDescription = "נבחר", tint = checkColor, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            if (state.showCurrencySelector) {
                CurrencySelector(
                    state = state,
                    onClose = { viewModel.closeCurrencySelector() },
                    onSelect = { viewModel.selectCurrency(it) },
                    onSearch = { viewModel.updateSearchQuery(it) }
                )
            } else {
                Scaffold(
                    bottomBar = { BottomRateInfo(state) }
                ) { paddingValues ->
                    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                            CalculatorContent(
                                state = state,
                                onSwap = { viewModel.swapCurrencies() },
                                onCurrencyClick = { isSource -> viewModel.openCurrencySelector(isSource) },
                                onKeypad = { viewModel.onKeypadPress(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 16.dp, end = 16.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "תפריט", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomRateInfo(state: CalculatorState) {
    val sourceRate = state.rates[state.sourceCurrency] ?: 1.0
    val targetRate = state.rates[state.targetCurrency] ?: 1.0
    val rate = targetRate / sourceRate
    val formattedRate = DecimalFormat("#,##0.0000").format(rate)
    
    val timeString = if (state.lastUpdateTimestamp > 0) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(state.lastUpdateTimestamp))
    } else {
        "לא התעדכן"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        val direction = androidx.compose.ui.text.style.TextDirection.Rtl
        Text(
            text = "שער חליפין: 1 ${state.sourceCurrency} = $formattedRate ${state.targetCurrency}\nעודכן לאחרונה: $timeString",
            style = MaterialTheme.typography.labelMedium.copy(textDirection = direction),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CalculatorContent(
    modifier: Modifier = Modifier,
    state: CalculatorState,
    onSwap: () -> Unit,
    onCurrencyClick: (Boolean) -> Unit,
    onKeypad: (Char) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(56.dp))
        
        // Source Currency Display
        CurrencyRow(
            currency = state.sourceCurrency,
            amount = state.sourceAmountRaw.ifEmpty { "0" },
            onClick = { onCurrencyClick(true) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Swap Button
        FilledIconButton(
            onClick = onSwap,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Default.SwapVert, contentDescription = "Swap Currencies")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Target Currency Display
        val formatter = DecimalFormat("#,##0.00")
        val targetAmountStr = formatter.format(state.targetAmount)
        CurrencyRow(
            currency = state.targetCurrency,
            amount = targetAmountStr,
            onClick = { onCurrencyClick(false) },
            isTarget = true
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Keypad
        Keypad(onKeyClick = onKeypad)
    }
}

@Composable
fun CurrencyRow(
    currency: String,
    amount: String,
    onClick: () -> Unit,
    isTarget: Boolean = false
) {
    val info = getCurrencyInfo(currency)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(if (isTarget) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = 16.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = amount,
            style = MaterialTheme.typography.headlineLarge,
            color = if (isTarget) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f)
        )
        
        Spacer(modifier = Modifier.weight(0.1f))

        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currency,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isTarget) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = info.flag,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = "${info.symbol} ${info.hebrewName}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isTarget) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun Keypad(onKeyClick: (Char) -> Unit) {
    val keys = listOf(
        listOf('7', '8', '9'),
        listOf('4', '5', '6'),
        listOf('1', '2', '3'),
        listOf('.', '0', '⌫')
    )
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // C Button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            KeypadButton(text = "C", weight = 0.3f, color = MaterialTheme.colorScheme.errorContainer, textColor = MaterialTheme.colorScheme.onErrorContainer) {
                onKeyClick('C')
            }
        }
        
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val isAction = key == '⌫'
                    val color = if (isAction) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer
                    val textColor = if (isAction) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimaryContainer
                    KeypadButton(text = key.toString(), weight = 1f, color = color, textColor = textColor) {
                        onKeyClick(key)
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.KeypadButton(
    text: String,
    weight: Float,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = textColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelector(
    state: CalculatorState,
    onClose: () -> Unit,
    onSelect: (String) -> Unit,
    onSearch: (String) -> Unit
) {
    val allCurrencies = state.rates.keys.toList()
    val filtered = allCurrencies.filter { 
        val info = getCurrencyInfo(it)
        it.contains(state.searchQuery, ignoreCase = true) || 
        info.hebrewName.contains(state.searchQuery, ignoreCase = true) ||
        info.keywords.any { keyword -> keyword.contains(state.searchQuery, ignoreCase = true) }
    }
    
    // Sort logic: put recent currencies first
    val sorted = filtered.sortedByDescending { state.recentCurrencies.indexOf(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("בחר מטבע") },
                navigationIcon = {
                    TextButton(onClick = onClose) {
                        Text("חזור")
                    }
                }
            )
        }
    ) { pd ->
        Column(modifier = Modifier.padding(pd).fillMaxSize()) {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearch,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("חיפוש לפי קוד או שם...") },
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    singleLine = true
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(sorted) { code ->
                        val info = getCurrencyInfo(code)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(code) }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = info.flag,
                                fontSize = 32.sp,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = info.hebrewName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (state.recentCurrencies.contains(code)) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "${info.symbol} $code",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
