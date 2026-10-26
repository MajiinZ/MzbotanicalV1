package org.mz.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import org.mz.home.domain.DrawerItem
import org.mz.killrs.shared.BrandCream
import org.mz.killrs.shared.BrandGreen
import org.mz.killrs.shared.BrandGreenSoft
import org.mz.killrs.shared.Exo2FontRegular
import org.mz.killrs.shared.Resources
import org.mz.killrs.shared.domain.Customer
import org.mz.killrs.shared.util.RequestState

@Composable
fun CustomDrawer(
    onProfileClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onCartClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onAdminPanelClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSignOutClick: () -> Unit,
    customer: RequestState<Customer>
) {
    val profile = customer.getSuccessDataOrNull()
    val primaryItems = listOf(
        DrawerItem.Categories,
        DrawerItem.CartFilled,
        DrawerItem.Orders,
        DrawerItem.Settings
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.68f)
            .background(BrandCream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MZBotanicals",
                    color = BrandGreen,
                    fontFamily = Exo2FontRegular(),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "PREMIUM GENETICS",
                    color = BrandGreen,
                    fontFamily = Exo2FontRegular(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.8.sp
                )
            }
            Icon(
                painter = painterResource(Resources.Icon.Categories),
                contentDescription = null,
                modifier = Modifier.size(27.dp),
                tint = BrandGreen
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(BrandGreenSoft)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Open profile",
                    onClick = onProfileClick
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = profile?.profileImageUrl,
                fallback = painterResource(Resources.Image.KillrsLogo),
                error = painterResource(Resources.Image.KillrsLogo),
                contentDescription = "Profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = profile?.let { "${it.firstName} ${it.lastName}".trim() }
                        ?.takeIf { it.isNotEmpty() } ?: "My Profile",
                    color = BrandGreen,
                    fontFamily = Exo2FontRegular(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "View profile",
                    color = BrandGreen,
                    fontFamily = Exo2FontRegular(),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))
        SectionLabel(text = "MENU")
        Spacer(modifier = Modifier.height(8.dp))

        primaryItems.forEach { item ->
            DrawerItemCard(
                drawerItem = item,
                onClick = {
                    when (item) {
                        DrawerItem.Categories -> onCategoriesClick()
                        DrawerItem.CartFilled -> onCartClick()
                        DrawerItem.Orders -> onOrdersClick()
                        DrawerItem.Settings -> onSettingsClick()
                        else -> {}
                    }
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = BrandGreen.copy(alpha = 0.14f))
        Spacer(modifier = Modifier.height(16.dp))

        if (profile?.isAdmin == true) {
            SectionLabel(text = "ADMIN")
            Spacer(modifier = Modifier.height(8.dp))
            DrawerItemCard(
                drawerItem = DrawerItem.AdminPanel,
                onClick = onAdminPanelClick,
                highlighted = true
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        DrawerItemCard(
            drawerItem = DrawerItem.SignOut,
            onClick = onSignOutClick
        )
        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = BrandGreen,
        fontFamily = Exo2FontRegular(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.8.sp
    )
}
