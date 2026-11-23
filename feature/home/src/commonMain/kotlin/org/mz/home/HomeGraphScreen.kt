package org.mz.home

import ContentWithMessageBar
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.mz.home.component.BottomBar
import org.mz.home.component.CustomDrawer
import org.mz.home.domain.BottomBarDestination
import org.mz.home.domain.CustomDrawerState
import org.mz.home.domain.isOpened
import org.mz.home.domain.opposite
import org.mz.killrs.CartScreen
import org.mz.killrs.DetailsScreen
import org.mz.killrs.categories.CategoriesScreen
import org.mz.killrs.category_search.CategorySearchScreen
import org.mz.killrs.profile.ProfileScreen
import org.mz.killrs.shared.Exo2FontRegular
import org.mz.killrs.shared.FontSize
import org.mz.killrs.shared.IconPrimary
import org.mz.killrs.shared.Resources
import org.mz.killrs.shared.Surface
import org.mz.killrs.shared.SurfaceLighter
import org.mz.killrs.shared.TextPrimary
import org.mz.killrs.shared.domain.ProductCategory
import org.mz.killrs.shared.navigation.Screen
import org.mz.killrs.shared.navigation.ScreenSavedStateConfiguration
import org.mz.killrs.shared.navigation.navigateUp
import org.mz.killrs.shared.navigation.switchSection
import org.mz.killrs.shared.util.RequestState
import org.mz.killrs.shared.util.getScreenWidth
import org.mz.products_overview.ProductsOverviewScreen
import rememberMessageBarState


private val CollectionSurface = org.mz.killrs.shared.BrandCream
private val CollectionGreen = org.mz.killrs.shared.BrandGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeGraphScreen(
    navigateToAuth: () -> Unit,
    navigateToProfile: () -> Unit,
    navigateToAdmin: () -> Unit,
    navigateToDetails: (String) -> Unit,
    navigateToCart: () -> Unit,
    navigateToCategorySearch: (String) -> Unit,
    navigateToCheckout: (String) -> Unit
) {
    // Storefront back stack (Navigation 3). Shape: [ProductsOverview, <section>, ...section sub-screens]
    // where <section> is a bottom-bar tab or drawer destination. ProductsOverview stays at the root.
    val backStack = rememberNavBackStack(ScreenSavedStateConfiguration, Screen.ProductsOverview)

    // Sub-stacks of sections the user switched away from, restored when they come back
    // (what Navigation 2's saveState/restoreState did). Kept in memory for this screen.
    val savedSections = remember { mutableMapOf<NavKey, List<NavKey>>() }

    val selectedDestination by remember {
        derivedStateOf {
            when (backStack.lastOrNull()) {
                Screen.Cart -> BottomBarDestination.Cart
                Screen.Profile -> BottomBarDestination.Profile
                Screen.Categories -> BottomBarDestination.Categories
                else -> BottomBarDestination.ProductsOverview
            }
        }
    }

    /** Switch storefront section (bottom bar / drawer), preserving each section's history. */
    fun navigateToSection(destination: Screen) =
        backStack.switchSection(destination, root = Screen.ProductsOverview, savedSections = savedSections)

    val isCollectionScreen by remember {
        derivedStateOf {
            selectedDestination == BottomBarDestination.Categories
        }
    }

    val screenWidth = remember { getScreenWidth() }
    val drawerState = remember { mutableStateOf(CustomDrawerState.Closed) }

    val offSetValue by remember { derivedStateOf { (screenWidth / 1.5).dp } }
    val animatedOffset by animateDpAsState(
        targetValue = if (drawerState.value.isOpened()) offSetValue else 0.dp
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (drawerState.value.isOpened()) 0.9f else 1f
    )
    val animatedRadius by animateDpAsState(
        targetValue = if (drawerState.value.isOpened()) 20.dp else 0.dp
    )

    val viewModel = koinViewModel<HomeGraphViewModel>()
    val customer by viewModel.customer.collectAsState()
    val totalAmount by viewModel.totalAmountFlow.collectAsState(RequestState.Loading)
    val messageBarState = rememberMessageBarState()

    fun navigateFromDrawer(destination: Screen) {
        drawerState.value = CustomDrawerState.Closed
        navigateToSection(destination)
    }

    val screenTitle by remember {
        derivedStateOf {
            when (backStack.lastOrNull()) {
                Screen.Orders -> "Orders"
                Screen.Settings -> "Settings"
                else -> if (isCollectionScreen) "MZBotanicals" else selectedDestination.title
            }
        }
    }

    LaunchedEffect(Unit) {
        println("TOTAL AMOUNT: $totalAmount")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLighter)
            .systemBarsPadding()
    ) {
        CustomDrawer(
            customer = customer,
            onProfileClick = { navigateFromDrawer(Screen.Profile) },
            onCategoriesClick = { navigateFromDrawer(Screen.Categories) },
            onCartClick = { navigateFromDrawer(Screen.Cart) },
            onOrdersClick = { navigateFromDrawer(Screen.Orders) },
            onAdminPanelClick = {
                drawerState.value = CustomDrawerState.Closed
                navigateToAdmin()
            },
            onSettingsClick = { navigateFromDrawer(Screen.Settings) },
            onSignOutClick = {
                drawerState.value = CustomDrawerState.Closed
                viewModel.signOut(
                    onSuccess = navigateToAuth,
                    onError = { message -> messageBarState.addError(message) }
                )
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(animatedRadius))
                .offset(x = animatedOffset)
                .scale(animatedScale)
                .shadow(20.dp, RoundedCornerShape(20.dp))
        ) {
            Scaffold(
                containerColor = if (isCollectionScreen) CollectionSurface else Surface,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            AnimatedContent(targetState = screenTitle) { title ->
                                Text(
                                    text = title,
                                    fontFamily = Exo2FontRegular(),
                                    fontSize = if (isCollectionScreen) 24.sp else FontSize.LARGE,
                                    color = if (isCollectionScreen) CollectionGreen else TextPrimary
                                )
                            }
                        },
                        actions = {
                            AnimatedVisibility(visible = isCollectionScreen) {
                                Icon(
                                    modifier = Modifier.padding(end = 16.dp),
                                    painter = painterResource(Resources.Icon.Categories),
                                    contentDescription = "MZBotanicals leaf",
                                    tint = CollectionGreen
                                )
                            }
                            AnimatedVisibility(
                                visible = selectedDestination == BottomBarDestination.Cart,
                            ) {


                                if (totalAmount.isSuccess()) {


                                } else if (totalAmount.isError()) {
                                    messageBarState.addError("Error fetching total amount.")
                                }

                                IconButton(
                                    onClick = {
                                        if (totalAmount.isSuccess()) {
                                            navigateToCheckout(
                                                totalAmount.getSuccessData().toString()
                                            )

                                        } else if (totalAmount.isError()) {
                                            messageBarState.addError("Something went wrong")
                                        }
                                    }

                                ) {
                                    Icon(
                                        painter = painterResource(Resources.Icon.ShoppingCartFilled),
                                        contentDescription = "Right Arrow",
                                        tint = IconPrimary
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                drawerState.value = drawerState.value.opposite()
                            }) {
                                Icon(
                                    painter = painterResource(Resources.Icon.Menu),
                                    contentDescription = "Menu Icon",
                                    tint = IconPrimary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (isCollectionScreen) CollectionSurface else Surface,
                            titleContentColor = if (isCollectionScreen) CollectionGreen else TextPrimary,
                            navigationIconContentColor = if (isCollectionScreen) CollectionGreen else TextPrimary,
                            actionIconContentColor = if (isCollectionScreen) CollectionGreen else IconPrimary,
                            scrolledContainerColor = if (isCollectionScreen) CollectionSurface else Surface
                        )
                    )
                }
            ) { padding ->
                ContentWithMessageBar(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = padding.calculateTopPadding(),
                            bottom = padding.calculateBottomPadding()
                        ),
                    messageBarState = messageBarState,
                    errorMaxLines = 3,
                    contentBackgroundColor = if (isCollectionScreen) CollectionSurface else Surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = if (isCollectionScreen) 0.dp else padding.calculateTopPadding(),
                                bottom = if (isCollectionScreen) 0.dp else padding.calculateBottomPadding()
                            )
                    ) {
                        NavDisplay(
                            modifier = Modifier.weight(1f),
                            backStack = backStack,
                            onBack = { backStack.navigateUp() },
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator(),
                            ),
                            entryProvider = entryProvider {
                            entry<Screen.ProductsOverview> {
                                ProductsOverviewScreen(
                                    navigateToDetails = navigateToDetails
                                )
                            }
                            entry<Screen.Cart> {
                                CartScreen(
                                    navigateToDetails = navigateToDetails,
                                    navigateToCheckout = navigateToCheckout,
                                    navigateToProfile = { navigateFromDrawer(Screen.Profile) },
                                    navigateToCategories = navigateToCategorySearch,
                                    navigateToCart = { navigateFromDrawer(Screen.Cart) },
                                    navigateToPayment = {
                                        navigateToCheckout(
                                            totalAmount.getSuccessData().toString()
                                        )
                                    }

                                )
                            }
                            entry<Screen.Profile> {
                                ProfileScreen(
                                    navigateBack = { backStack.navigateUp() }
                                )
                            }
                            entry<Screen.Categories> {
                                CategoriesScreen(
                                    navigateToCategories = { categoryName ->
                                        backStack.add(Screen.CategorySearch(categoryName))
                                    }
                                )
                            }
                            entry<Screen.Orders> {
                               OrdersScreen(navigateToShop = { navigateFromDrawer(Screen.ProductsOverview) }
                                )
                            }
                            entry<Screen.Settings> {
                                SettingsScreen()
                            }
                            entry<Screen.Details> { key ->
                                DetailsScreen(
                                    productId = key.id,
                                    navigateBack = { backStack.navigateUp() },
                                    navigateToCart = { navigateFromDrawer(Screen.Cart) }
                                )
                            }
                            entry<Screen.CategorySearch> { key ->
                                val category = ProductCategory.valueOf(key.category.toString())
                                CategorySearchScreen(
                                    category = category,
                                    navigateBack = { backStack.navigateUp() },
                                    navigateToDetails = { id ->
                                        backStack.add(Screen.Details(id))
                                    }
                                )
                            }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.padding(12.dp)) {
                            BottomBar(
                                onSelect = { destination ->
                                    navigateToSection(destination.screen)
                                },
                                selected = selectedDestination,
                                customer = customer
                            )
                        }
                    }
                }
            }
        }
    }
}
