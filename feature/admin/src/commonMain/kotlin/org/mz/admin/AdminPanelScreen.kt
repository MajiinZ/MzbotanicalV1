package org.mz.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.mz.killrs.shared.*
import org.koin.compose.koinInject
import org.mz.data.domain.AdminRepository

private enum class AdminSection(val label: String, val title: String, val description: String, val icon: DrawableResource) {
    Products("Products", "Products & Inventory", "Catalog, stock, and product records", Resources.Icon.Categories),
    Orders("Orders", "Orders & Fulfillment", "Payments, packing, and delivery", Resources.Icon.Orders),
    Customers("Customers", "Customers & Verification", "Accounts, eligibility, and privacy", Resources.Icon.Profile),
    Settings("Settings", "Compliance & Settings", "Policies, permissions, and activity", Resources.Icon.Settings)
}

private data class DemoRecord(
    val id: String, val section: AdminSection, val title: String,
    val subtitle: String, val status: String, val details: String
)

private fun sampleRecords() = listOf(
    DemoRecord("SKU-101", AdminSection.Products, "Botanical Collection A", "24 packs in stock · Sample price $45", "Active", "Sample batch: DEMO-A01\nSupplier: Example Genetics\nTesting document: Not provided\nProduct classification: Pending review"),
    DemoRecord("SKU-102", AdminSection.Products, "Botanical Collection B", "4 packs in stock · Sample price $60", "Low stock", "Sample batch: DEMO-B02\nSupplier: Example Genetics\nTesting document: Not provided\nReorder level: 5 packs"),
    DemoRecord("SKU-103", AdminSection.Products, "Seasonal Collection", "12 packs in stock · Sample price $50", "Paused", "Sample batch: DEMO-C03\nListing paused for document review.\nNo products are actually offered through this demo."),
    DemoRecord("DEMO-2041", AdminSection.Orders, "Order #2041", "Alex Sample · 2 items · $90", "Processing", "Payment: Simulated\nCustomer: Alex Sample\nDelivery: Example address, California\nEligibility: Not verified\nThis record cannot create a shipment or charge."),
    DemoRecord("DEMO-2042", AdminSection.Orders, "Order #2042", "Jordan Example · 1 item · $60", "Ready to pack", "Payment: Simulated\nCustomer: Jordan Example\nDelivery: Example address, California\nEligibility: Not verified\nThis record cannot create a shipment or charge."),
    DemoRecord("DEMO-2043", AdminSection.Orders, "Order #2043", "Taylor Demo · 1 item · $45", "Fulfilled", "Payment: Simulated\nTracking: DEMO-ONLY\nNo carrier was contacted."),
    DemoRecord("CUS-001", AdminSection.Customers, "Alex Sample", "alex@example.com · 2 sample orders", "Review needed", "Age / identity: Not verified\nPrivacy requests: None\nThis is a fictional customer. No ID document is stored."),
    DemoRecord("CUS-002", AdminSection.Customers, "Jordan Example", "jordan@example.com · 1 sample order", "Restricted", "Age / identity: Not verified\nAccount: Restricted for demo\nPrivacy requests: Example access request\nThis is a fictional customer."),
    DemoRecord("CUS-003", AdminSection.Customers, "Taylor Demo", "taylor@example.com · 1 sample order", "Review needed", "Age / identity: Not verified\nPrivacy requests: Example deletion request\nThis is a fictional customer.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun AdminPanelScreen(navigateBack: () -> Unit, navigateToManageProduct: (String?) -> Unit) {
    val repository = koinInject<AdminRepository>()
    if (!repository.isCurrentUserAdmin()) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Administrator access is required.", color = BrandGreen)
            TextButton(onClick = navigateBack) { Text("Back", color = BrandGreen) }
        }
        return
    }
    var selected by rememberSaveable { mutableStateOf<AdminSection?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val records = remember { mutableStateListOf<DemoRecord>().apply { addAll(sampleRecords()) } }
    val activity = remember { mutableStateListOf("Demo workspace opened") }
    var detail by remember { mutableStateOf<DemoRecord?>(null) }
    var adding by remember { mutableStateOf(false) }
    var productName by remember { mutableStateOf("") }
    var requireReview by remember { mutableStateOf(true) }
    var restrictRegions by remember { mutableStateOf(true) }
    var staffAccess by remember { mutableStateOf(true) }
    var reset by remember { mutableStateOf(false) }

    if (selected == AdminSection.Products) {
        AdminProductsScreen(navigateBack = { selected = null }, editProduct = navigateToManageProduct)
        return
    }

    fun updateRecord(record: DemoRecord, status: String) {
        val index = records.indexOfFirst { it.id == record.id }
        if (index >= 0) records[index] = record.copy(status = status)
        activity.add(0, "${record.title}: $status (demo)")
        detail = null
    }

    Scaffold(
        containerColor = BrandCream,
        topBar = {
            TopAppBar(
                title = { Text("MZ Admin", fontFamily = Exo2FontRegular(), color = BrandGreen) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selected == null) navigateBack() else { selected = null; query = "" }
                    }) { Icon(painterResource(Resources.Icon.BackArrow), "Back", tint = BrandGreen) }
                },
                actions = { TextButton(onClick = { reset = true }) { Text("Reset demo", color = BrandGreen) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandCream)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(color = BrandGreenSoft, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("ADMIN WORKSPACE", color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.height(5.dp))
                        Text("Products & Inventory uses real store records. Orders, Customers, and Settings remain demos. Checkout is paused.", color = BrandGreen, fontSize = 13.sp)
                    }
                }
            }
            item {
                Text(selected?.title ?: "Your business, at a glance", color = BrandGreen, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(selected?.description ?: "Explore the four areas of your admin workspace.", color = BrandTextMuted)
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selected == null, onClick = { selected = null; query = "" }, label = { Text("Overview") }, colors = adminChipColors())
                    AdminSection.entries.forEach { section ->
                        FilterChip(selected = selected == section, onClick = { selected = section; query = "" }, label = { Text(section.label) }, colors = adminChipColors())
                    }
                }
            }
            if (selected == null) {
                items(AdminSection.entries, key = { it.name }) { section ->
                    Card(onClick = { selected = section; query = "" }, colors = CardDefaults.cardColors(containerColor = White), shape = RoundedCornerShape(20.dp)) {
                        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(section.icon), null, modifier = Modifier.size(30.dp), tint = BrandGreen)
                            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                Text(section.title, color = BrandGreen, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(section.description, color = BrandTextMuted, fontSize = 13.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(when (section) {
                                    AdminSection.Products -> "Manage real products and stock"
                                    AdminSection.Orders -> "${records.count { it.section == section && it.status != "Fulfilled" && it.status != "Cancelled" }} open sample orders"
                                    AdminSection.Customers -> "${records.count { it.section == section && it.status == "Review needed" }} awaiting review"
                                    AdminSection.Settings -> "Legal review pending"
                                }, color = BrandGreen, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            }
                            Text("›", color = BrandGreen, fontSize = 28.sp)
                        }
                    }
                }
            } else if (selected == AdminSection.Settings) {
                item { AdminNote("Launch readiness", "Not assessed. Product classification, licenses, permitted territories, age thresholds, and verification requirements need professional review.") }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = White), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SettingRow("Hold orders for eligibility review", "Prototype control only", requireReview) { requireReview = it; activity.add(0, "Eligibility review setting changed (demo)") }
                            SettingRow("Limit permitted delivery regions", "No approved regions configured", restrictRegions) { restrictRegions = it; activity.add(0, "Delivery region setting changed (demo)") }
                            SettingRow("Restrict staff permissions", "Example role: Owner", staffAccess) { staffAccess = it; activity.add(0, "Staff permission setting changed (demo)") }
                        }
                    }
                }
                item { AdminNote("Documents & policies", "Business license: Not provided\nPrivacy policy: Draft needed\nTerms / refunds: Draft needed\nRetention policy: Review needed") }
            } else {
                item {
                    OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search ${selected!!.label.lowercase()}") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen, focusedLabelColor = BrandGreen, cursorColor = BrandGreen, focusedTextColor = BrandGreen, unfocusedTextColor = BrandGreen))
                }
                if (selected == AdminSection.Products) {
                    item { Button(onClick = { productName = ""; adding = true }, colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = BrandCream)) { Text("Add sample product") } }
                }
                val visibleRecords = records.filter { it.section == selected && "${it.title} ${it.subtitle} ${it.id} ${it.status}".contains(query.trim(), ignoreCase = true) }
                if (visibleRecords.isEmpty()) item { AdminNote("No matches", "Try another name, order number, or status.") }
                items(visibleRecords, key = { it.id }) { record ->
                    Card(onClick = { detail = record }, colors = CardDefaults.cardColors(containerColor = White), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(record.title, color = BrandGreen, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(record.subtitle, color = BrandTextMuted, fontSize = 13.sp)
                            Surface(color = BrandGreenSoft, shape = RoundedCornerShape(30.dp)) { Text(record.status, Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = BrandGreen, fontSize = 12.sp) }
                            Text("View details ›", color = BrandGreen, fontSize = 13.sp)
                        }
                    }
                }
            }
            if (selected == null || selected == AdminSection.Settings) {
                item { Text("Demo activity", color = BrandGreen, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }
                items(activity.take(8)) { event -> Text("• $event", color = BrandTextMuted, fontSize = 13.sp) }
            }
        }
    }
    detail?.let { record ->
        AlertDialog(
            onDismissRequest = { detail = null }, containerColor = BrandCream,
            title = { Text(record.title, color = BrandGreen) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${record.id} · ${record.status}", color = BrandGreen, fontWeight = FontWeight.SemiBold)
                Text(record.details, color = BrandGreen)
                Text("All changes are simulated. Customer eligibility is not verified by these actions.", color = BrandTextMuted, fontSize = 12.sp)
            } },
            confirmButton = {
                TextButton(onClick = {
                    updateRecord(record, when (record.section) {
                        AdminSection.Products -> if (record.status == "Paused") "Active" else "Paused"
                        AdminSection.Orders -> when (record.status) { "Processing" -> "Ready to pack"; "Ready to pack" -> "Fulfilled"; else -> "Processing" }
                        AdminSection.Customers -> if (record.status == "Restricted") "Review needed" else "Restricted"
                        else -> record.status
                    })
                }) { Text(when (record.section) {
                    AdminSection.Products -> if (record.status == "Paused") "Resume sample listing" else "Pause sample listing"
                    AdminSection.Orders -> when (record.status) { "Processing" -> "Mark ready to pack"; "Ready to pack" -> "Mark fulfilled"; else -> "Reopen sample order" }
                    AdminSection.Customers -> if (record.status == "Restricted") "Return to review" else "Restrict sample account"
                    else -> "Done"
                }, color = BrandGreen) }
            },
            dismissButton = { TextButton(onClick = { detail = null }) { Text("Close", color = BrandGreen) } }
        )
    }
    if (adding) AlertDialog(
        onDismissRequest = { adding = false }, containerColor = BrandCream,
        title = { Text("Add sample product", color = BrandGreen) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Creates a local draft with no stock or price. Nothing is published.", color = BrandGreen)
            OutlinedTextField(value = productName, onValueChange = { productName = it.take(80) }, label = { Text("Product name") }, singleLine = true)
        } },
        confirmButton = { TextButton(enabled = productName.isNotBlank(), onClick = {
            val name = productName.trim()
            records.add(DemoRecord("DEMO-SKU-${records.size + 1}", AdminSection.Products, name, "0 packs · Price not set", "Draft", "Sample draft only.\nSupplier, classification, batch, and documents: Not provided."))
            activity.add(0, "$name: draft created (demo)")
            adding = false
        }) { Text("Create draft", color = BrandGreen) } },
        dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel", color = BrandGreen) } }
    )
    if (reset) AlertDialog(
        onDismissRequest = { reset = false }, containerColor = BrandCream,
        title = { Text("Reset this demo?", color = BrandGreen) },
        text = { Text("Sample records and settings will return to their starting values. Your real data is unaffected.", color = BrandGreen) },
        confirmButton = { TextButton(onClick = {
            records.clear(); records.addAll(sampleRecords()); activity.clear(); activity.add("Demo workspace reset")
            requireReview = true; restrictRegions = true; staffAccess = true; query = ""; reset = false
        }) { Text("Reset demo", color = BrandGreen) } },
        dismissButton = { TextButton(onClick = { reset = false }) { Text("Cancel", color = BrandGreen) } }
    )
}

@Composable
private fun adminChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = BrandGreen, selectedLabelColor = BrandCream, labelColor = BrandGreen
)

@Composable
private fun AdminNote(title: String, body: String) {
    Surface(color = BrandGreenSoft, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = BrandGreen, fontWeight = FontWeight.SemiBold)
            Text(body, color = BrandGreen, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = BrandGreen, fontWeight = FontWeight.Medium)
            Text(subtitle, color = BrandTextMuted, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedThumbColor = BrandCream, checkedTrackColor = BrandGreen))
    }
}