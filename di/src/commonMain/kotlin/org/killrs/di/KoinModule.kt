package org.killrs.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.mz.killrs.shared.domain.ProductCategory
import org.mz.admin.AdminPanelViewModel
import org.mz.data.AdminRepositoryImpl
import org.mz.data.CustomerRepositoryImpl
import org.mz.data.MockOrderRepository
import org.mz.data.OrderRepositoryImpl
import org.mz.data.ProductRepositoryImpl
import org.mz.data.domain.AdminRepository
import org.mz.data.domain.CustomerRepository
import org.mz.data.domain.OrderRepository
import org.mz.data.domain.ProductRepository
import org.mz.home.HomeGraphViewModel
import org.mz.home.OrdersViewModel
import org.mz.killrs.CartViewModel
import org.mz.killrs.DetailsViewModel
import org.mz.killrs.auth.AuthViewModel
import org.mz.killrs.manage_product.ManageProductViewModel
import org.mz.killrs.profile.ProfileViewModel
import org.mz.products_overview.ProductsOverviewViewmodel
import org.mz.killrs.categories.CategoriesViewModel
import org.mz.killrs.category_search.CategorySearchViewmodel
import org.mz.killrs.CheckoutViewModel


val sharedModule = module {
    single<CustomerRepository>{CustomerRepositoryImpl()}
    single<AdminRepository>{ AdminRepositoryImpl() }
    single<ProductRepository>{ ProductRepositoryImpl() }
    single<OrderRepository>{ OrderRepositoryImpl(get()) }
    single {
        val customerRepository = get<CustomerRepository>()
        MockOrderRepository(get(), customerRepository::getCurrentUserId)
    }

    viewModelOf(::AuthViewModel)
    viewModelOf(::HomeGraphViewModel)
    viewModelOf(::OrdersViewModel)
    viewModelOf(::ProfileViewModel)
    // Route arguments are passed as Koin parameters from the Navigation 3 keys
    // (Navigation 3 does not copy route arguments into SavedStateHandle).
    viewModel { params -> ManageProductViewModel(get(), params.getOrNull<String>()) }
    viewModelOf(::AdminPanelViewModel)
    viewModelOf(::ProductsOverviewViewmodel)
    viewModel { params -> DetailsViewModel(get(), get(), params.get<String>()) }
    viewModelOf(::CartViewModel)
    viewModelOf(::CategoriesViewModel)
    viewModel { params -> CategorySearchViewmodel(get(), params.get<ProductCategory>()) }
    viewModelOf(::CheckoutViewModel)
}



expect val targetModule: Module

fun initializeKoin(
    config: (KoinApplication.() -> Unit)? = null,
) {
    startKoin {
        config?.invoke(this)
        modules(sharedModule, targetModule)
    }
}
