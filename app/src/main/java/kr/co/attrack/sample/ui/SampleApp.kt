package kr.co.attrack.sample.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import kr.co.attrack.sample.BuildConfig
import kr.co.attrack.sample.Destination
import kr.co.attrack.sample.LinkArrival
import kr.co.attrack.sample.LogEntry
import kr.co.attrack.sample.SampleApplication
import kr.co.attrack.sample.SampleEvents
import kr.co.attrack.sample.SampleLog
import kr.co.attrack.sample.SampleNavigator
import kr.co.attrack.tracker.Tracker
import kr.co.attrack.tracker.TrackerResult
import kr.co.attrack.tracker.TrackerStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Tab(val label: String, val glyph: String) {
    OVERVIEW("Overview", "◎"),
    EVENTS("Events", "⚡"),
    LINKS("Links", "↗"),
    DATA("Data", "✓"),
    LOG("Log", "≡"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleApp(
    app: SampleApplication,
    pendingLinkRetry: StateFlow<Intent?>,
    onRetryLink: () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xFF8FB0FF)) else lightColorScheme(primary = Color(0xFF275DE7))) {
        // Every SDK result lands in the log; recomposing on it refreshes the
        // status/schema/attribution reads below, which are plain getters.
        val entries by SampleLog.entries.collectAsState()
        val destination by SampleNavigator.destination.collectAsState()
        val arrival by SampleNavigator.arrival.collectAsState()
        val retry by pendingLinkRetry.collectAsState()
        var tab by rememberSaveable { mutableStateOf(Tab.OVERVIEW) }

        if (destination != Destination.Home) {
            BackHandler { SampleNavigator.open(Destination.Home) }
            LinkDestinationScreen(destination, arrival) { SampleNavigator.open(Destination.Home) }
            return@MaterialTheme
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ATTRACK Sample") },
                    actions = { StatusChip(Tracker.status(), entries.size) },
                )
            },
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Text(item.glyph, style = MaterialTheme.typography.titleMedium) },
                            label = { Text(item.label) },
                        )
                    }
                }
            },
        ) { padding ->
            val modifier = Modifier.padding(padding).fillMaxSize()
            when (tab) {
                Tab.OVERVIEW -> OverviewTab(app, entries.size, modifier)
                Tab.EVENTS -> EventsTab(modifier)
                Tab.LINKS -> LinksTab(entries, retry != null, onRetryLink, modifier)
                Tab.DATA -> DataTab(modifier)
                Tab.LOG -> LogList(entries, modifier)
            }
        }
    }
}

@Composable
private fun StatusChip(status: TrackerStatus, @Suppress("UNUSED_PARAMETER") refresh: Int) {
    val color = when (status) {
        TrackerStatus.ACTIVE -> Color(0xFF1B8A4B)
        TrackerStatus.INITIALIZING, TrackerStatus.NOT_STARTED -> Color(0xFF8A6D1B)
        TrackerStatus.DISABLED, TrackerStatus.BLOCKED -> Color(0xFFB3261E)
    }
    Text(
        status.name,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(end = 16.dp),
    )
}

@Composable
private fun OverviewTab(app: SampleApplication, @Suppress("UNUSED_PARAMETER") refresh: Int, modifier: Modifier) {
    val attribution = Tracker.installAttribution()
    val schema = Tracker.eventSchema()
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Section("SDK") {
                Field("Status", Tracker.status().name)
                Field("App ID", BuildConfig.ATTRACK_APP_ID.ifEmpty { "not configured" })
                Field("Start", app.initStart?.let { "${it.code}" + if (it.success) "" else " — ${it.message}" } ?: "—")
                Field("Schema version", Tracker.eventSchemaVersion().ifEmpty { "not published yet" })
            }
        }
        item {
            Section("Install attribution (Play Install Referrer)") {
                if (attribution == null) {
                    Text(
                        "Not known yet, or no referrer: an organic Play install, a sideload, or a lookup still running.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Field("Status", attribution.status)
                    Field("Click ID", attribution.clickId.ifEmpty { "—" })
                    Field("Source / medium", "${attribution.utmSource.ifEmpty { "—" }} / ${attribution.utmMedium.ifEmpty { "—" }}")
                    Field("Campaign", attribution.utmCampaign.ifEmpty { "—" })
                    Field("Sender", attribution.sender.ifEmpty { "—" })
                }
            }
        }
        item {
            Section("Events this build may send (${schema.size})") {
                if (schema.isEmpty()) {
                    Text("Published by the server on initialization.", style = MaterialTheme.typography.bodyMedium)
                }
                schema.forEach { definition ->
                    Text(definition.describe(), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun EventsTab(modifier: Modifier) {
    var last by remember { mutableStateOf<TrackerResult?>(null) }
    var level by remember { mutableIntStateOf(1) }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Section("Send an event") {
                Text(
                    "EVENT_QUEUED means it is safely on disk. EVENT_DELIVERED arrives in the Log once the server accepts it.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.padding(4.dp))
                Button({ last = SampleEvents.signUp() }, Modifier.fillMaxWidth()) { Text("sign_up") }
                Button({ last = SampleEvents.tutorialComplete() }, Modifier.fillMaxWidth()) { Text("tutorial_complete") }
                Button({ last = SampleEvents.levelComplete(level, level * 300, level % 3 == 0).also { level++ } }, Modifier.fillMaxWidth()) {
                    Text("level_complete (level $level)")
                }
                Button({ last = SampleEvents.purchase() }, Modifier.fillMaxWidth()) { Text("purchase (9.99 USD)") }
            }
        }
        item {
            Section("Common mistakes (rejected on the device)") {
                SampleEvents.mistakes.forEach { (label, call) ->
                    OutlinedButton({ last = call() }, Modifier.fillMaxWidth()) { Text(label) }
                }
            }
        }
        last?.let { result ->
            item {
                Section("Last result") {
                    Field("Code", result.code.name)
                    result.transactionId?.let { Field("Transaction", it) }
                    if (result.fieldErrors.isNotEmpty()) Field("Fields", result.fieldErrors.toString())
                    Text(result.message, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun LinksTab(entries: List<LogEntry>, hasRetry: Boolean, onRetry: () -> Unit, modifier: Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Section("How links reach this app") {
                Text(
                    "Open an ATTRACK link for this app with parameters such as screen=offer and " +
                        "offer_id=summer-42. It routes here; with the app not installed, Google Play passes it " +
                        "through and the first open replays it.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("Supported: screen=home | offer (offer_id) | product (product_id)", style = MaterialTheme.typography.bodySmall)
                if (hasRetry) {
                    Spacer(Modifier.padding(4.dp))
                    Button(onRetry) { Text("Retry unavailable link") }
                }
            }
        }
        item { Text("Recent link results", style = MaterialTheme.typography.titleSmall) }
        val links = entries.filter { it.title.startsWith("Link ") }
        if (links.isEmpty()) item { Text("None yet.", style = MaterialTheme.typography.bodyMedium) }
        items(links) { LogRow(it) }
    }
}

@Composable
private fun DataTab(modifier: Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Section("Collected on every event") {
                Field("Advertising ID", "Google Advertising ID (empty if the user deleted or limited it)")
                Field("Secure ID", "Settings.Secure.ANDROID_ID, for paid-reward deduplication")
                Field("Install", "Google Play Install Referrer, app/OS version, device model")
            }
        }
        item {
            Text(
                "The SDK collects these automatically; there is no consent switch. The server keeps the " +
                    "secure ID only as a package-scoped hash. Declare \"Device or other IDs\" in Google Play's " +
                    "Data safety form.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LinkDestinationScreen(destination: Destination, arrival: LinkArrival?, onBack: () -> Unit) {
    Scaffold { padding ->
        Column(
            Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onBack) { Text("← Back") }
            when (destination) {
                is Destination.Offer -> {
                    Text("Offer", style = MaterialTheme.typography.headlineMedium)
                    Text("Offer ID: ${destination.offerId}")
                }
                is Destination.Product -> {
                    Text("Product", style = MaterialTheme.typography.headlineMedium)
                    Text("Product ID: ${destination.productId}")
                }
                Destination.Home -> Unit
            }
            arrival?.let { link ->
                Section(if (link.deferred) "Deferred link (first open after install)" else "Direct link") {
                    Field("Link ID", link.linkId)
                    Field("Parameters", link.parameters.entries.joinToString { "${it.key}=${it.value}" }.ifEmpty { "none" })
                    if (link.deferred) Field("Acknowledged", if (link.acknowledged) "yes" else "no — will be delivered again")
                }
            }
        }
    }
}

@Composable
private fun LogList(entries: List<LogEntry>, modifier: Modifier) {
    if (entries.isEmpty()) {
        Column(modifier.padding(16.dp)) { Text("Nothing yet.") }
        return
    }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(entries) { LogRow(it) }
    }
}

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

@Composable
private fun LogRow(entry: LogEntry) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.title,
                    fontWeight = FontWeight.SemiBold,
                    color = if (entry.ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                Text(timeFormat.format(Date(entry.atMillis)), style = MaterialTheme.typography.labelSmall)
            }
            if (entry.detail.isNotEmpty()) {
                SelectionContainer { Text(entry.detail, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(128.dp))
        SelectionContainer { Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace) }
    }
}

@Composable
private fun ToggleRow(title: String, body: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
