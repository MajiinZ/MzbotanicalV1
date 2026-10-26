package org.mz.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.mz.home.domain.DrawerItem
import org.mz.killrs.shared.BrandCream
import org.mz.killrs.shared.BrandGreen
import org.mz.killrs.shared.BrandGreenSoft
import org.mz.killrs.shared.Exo2FontRegular

@Composable
fun DrawerItemCard(
    drawerItem: DrawerItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (highlighted) BrandGreenSoft else Color.Transparent)
            .clickable(
                role = Role.Button,
                onClickLabel = drawerItem.title,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (highlighted) BrandCream else BrandGreenSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(drawerItem.icon),
                contentDescription = drawerItem.title,
                modifier = Modifier.size(20.dp),
                tint = BrandGreen
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = drawerItem.title,
            color = BrandGreen,
            fontFamily = Exo2FontRegular(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
