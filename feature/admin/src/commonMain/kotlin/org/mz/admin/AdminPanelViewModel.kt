package org.mz.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.mz.data.domain.AdminRepository
import org.mz.killrs.shared.util.RequestState

@OptIn(ExperimentalCoroutinesApi::class)
class AdminPanelViewModel(private val adminRepository: AdminRepository) : ViewModel() {
    private val refreshVersion = MutableStateFlow(0)
    private val query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = query
    fun updateSearchQuery(value: String) { query.value = value }
    fun refresh() { refreshVersion.value += 1 }
    private val products = refreshVersion.flatMapLatest { adminRepository.searchProductsByTitle("") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RequestState.Loading)
    val filteredProducts = combine(products, query) { state, text ->
        when (state) {
            is RequestState.Success -> RequestState.Success(state.data.filter {
                "${it.title} ${it.category} ${it.id}".contains(text.trim(), ignoreCase = true)
            }.sortedByDescending { it.createdAt })
            else -> state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RequestState.Loading)
}