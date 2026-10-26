package org.mz.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.mz.killrs.shared.*
import org.mz.killrs.shared.component.ProductCard
import org.mz.killrs.shared.util.RequestState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminProductsScreen(navigateBack: () -> Unit, editProduct: (String?) -> Unit) {
    val vm = koinViewModel<AdminPanelViewModel>()
    val result by vm.filteredProducts.collectAsState()
    val query by vm.searchQuery.collectAsState()
    var filter by rememberSaveable { mutableStateOf("All") }
    Scaffold(containerColor = BrandCream, topBar = {
        TopAppBar(title = { Text("Products & Inventory", color = BrandGreen, fontSize = 21.sp) },
            navigationIcon = { IconButton(onClick = navigateBack) { Icon(painterResource(Resources.Icon.BackArrow), "Back", tint = BrandGreen) } },
            actions = { TextButton(onClick = vm::refresh) { Text("Refresh", color = BrandGreen) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandCream))
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Surface(color = BrandGreenSoft, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("LIVE INVENTORY · CHECKOUT PAUSED", color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Products and edits are saved to the store. New and unreviewed records stay as drafts until you publish them.", color = BrandGreen, fontSize = 13.sp)
                    }
                }
            }
            item { Text("Owner · ${Constants.ADMIN_EMAIL}", color = BrandGreen, fontSize = 12.sp) }
            item { Button(onClick = { editProduct(null) }, colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = BrandCream)) { Text("Add product") } }
            item { OutlinedTextField(value = query, onValueChange = vm::updateSearchQuery, singleLine = true, label = { Text("Search name, category, or product ID") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Drafts", "Published", "Low stock").forEach { label ->
                        FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandGreen, selectedLabelColor = BrandCream, labelColor = BrandGreen))
                    }
                }
            }
            when (val state = result) {
                is RequestState.Loading, is RequestState.Idle -> item { CircularProgressIndicator(color = BrandGreen) }
                is RequestState.Error -> item {
                    Text(state.message, color = SurfaceError)
                    TextButton(onClick = vm::refresh) { Text("Try again", color = BrandGreen) }
                }
                is RequestState.Success -> {
                    val visible = state.data.filter { when (filter) {
                        "Drafts" -> !it.isPublished
                        "Published" -> it.isPublished
                        "Low stock" -> it.stockQuantity <= 5
                        else -> true
                    } }
                    item { Text("${visible.size} products · ${state.data.sumOf { it.stockQuantity }} packs in matching inventory", color = BrandGreen, fontSize = 13.sp) }
                    if (visible.isEmpty()) item { Text("No products match. Add your first product or change the filters.", color = BrandTextMuted) }
                    items(visible, key = { it.id }) { product ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProductCard(product = product, onClick = { editProduct(product.id) })
                            Text("${if (product.isPublished) "Published" else "Draft"} · ${product.stockQuantity} packs${if (product.stockQuantity == 0) " · Out of stock" else if (product.stockQuantity <= 5) " · Low stock" else ""}",
                                color = BrandGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}