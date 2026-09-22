package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Countertops
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Shower
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.ShowroomNavyLight

@Composable
fun ProductImagePlaceholder(
    imageUri: String?,
    category: String,
    name: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUri.isNullOrBlank() && !imageUri.startsWith("icon:")) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val icon = getCategoryIcon(category, name, imageUri)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ShowroomNavyLight.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun getCategoryIcon(category: String, name: String, imageUri: String?): ImageVector {
    val lowName = name.lowercase()
    val lowCat = category.lowercase()
    val lowUri = imageUri?.lowercase() ?: ""

    return when {
        lowUri.contains("shower") || lowName.contains("shower") -> Icons.Default.Shower
        lowUri.contains("mixer") || lowUri.contains("tap") || lowName.contains("mixer") || lowName.contains("tap") || lowCat.contains("cp") -> Icons.Default.Plumbing
        lowUri.contains("basin") || lowUri.contains("wc") || lowName.contains("basin") || lowName.contains("toilet") || lowName.contains("closet") || lowCat.contains("sanitary") -> Icons.Default.Bathtub
        lowUri.contains("kitchen") || lowName.contains("kitchen") || lowName.contains("sink") -> Icons.Default.Kitchen
        lowCat.contains("hardware") || lowName.contains("lock") || lowName.contains("handle") || lowName.contains("hinge") -> Icons.Default.Build
        lowCat.contains("accessories") || lowName.contains("towel") || lowName.contains("shelf") -> Icons.Default.Checkroom
        lowCat.contains("bath") -> Icons.Default.Countertops
        else -> Icons.Default.Inventory2
    }
}
