package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiClient
import com.example.data.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageStudioScreen(
    products: List<Product>,
    isGenerating: Boolean,
    generatedBitmap: Bitmap?,
    errorMessage: String?,
    onGenerateImage: (prompt: String, aspectRatio: String) -> Unit,
    onEditImage: (prompt: String, baseBitmap: Bitmap) -> Unit,
    onAssignImageToProduct: (productId: Long, bitmap: Bitmap) -> Unit,
    onResetImage: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Create, 1: Edit
    var promptText by remember { mutableStateOf("") }
    var selectedAspectRatio by remember { mutableStateOf("1:1") }
    var baseBitmapForEdit by remember { mutableStateOf<Bitmap?>(null) }
    var selectedProductForEdit by remember { mutableStateOf<Product?>(null) }
    var selectedProductToAssign by remember { mutableStateOf<Product?>(null) }
    var isAssignDropdownExpanded by remember { mutableStateOf(false) }
    var isProductPickerExpanded by remember { mutableStateOf(false) }

    val aspectRatios = listOf("1:1", "4:3", "16:9", "3:4")

    val sampleCreatePrompts = listOf(
        "Luxury rose gold bathroom basin faucet on black Italian marble",
        "Modern thermostatic rain shower system in matte black finish",
        "Minimalist wall-hung rimless toilet with concealed cistern",
        "Designer vanity cabinet with backlit LED mirror and chrome mixer",
        "Antique brass cabinet door pull handles on teak wood"
    )

    val sampleEditPrompts = listOf(
        "Change the metallic finish to brushed brass gold",
        "Convert the finish to sleek matte black",
        "Place this fitting in a high-end luxury marble showroom display",
        "Add subtle warm ambient lighting and water droplet reflections"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI Product Image Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Powered by gemini-3.1-flash-image-preview",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create New Image")
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Image with Prompt")
                            }
                        }
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Mode 1: Create Image
            if (selectedTab == 0) {
                Text(
                    text = "Text Prompt",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = { Text("Describe the product or bathroom concept...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("image_prompt_input"),
                    minLines = 3,
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Aspect Ratio Selector
                Text(
                    text = "Aspect Ratio",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    aspectRatios.forEach { ratio ->
                        FilterChip(
                            selected = selectedAspectRatio == ratio,
                            onClick = { selectedAspectRatio = ratio },
                            label = { Text(ratio) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Prompt Suggestions
                Text(
                    text = "Quick Inspiration:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(sampleCreatePrompts) { p ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { promptText = p }
                        ) {
                            Text(
                                text = p,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (promptText.isNotBlank() && !isGenerating) {
                            onGenerateImage(promptText.trim(), selectedAspectRatio)
                        }
                    },
                    enabled = promptText.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_image_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating with gemini-3.1-flash-image-preview...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Image", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Mode 2: Edit Image
                Text(
                    text = "1. Base Image to Edit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (baseBitmapForEdit != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = baseBitmapForEdit!!.asImageBitmap(),
                                contentDescription = "Base image for edit",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            IconButton(
                                onClick = { baseBitmapForEdit = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        }
                    }
                } else if (generatedBitmap != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = generatedBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Use last generated image", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Continue editing the current result", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = { baseBitmapForEdit = generatedBitmap },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Select")
                            }
                        }
                    }
                } else {
                    // Prompt user to select from products or generate first
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Choose an image to edit with prompts",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "You can generate an image in the Create tab, or edit a sample catalog fitting.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Pick from catalog products
                            ExposedDropdownMenuBox(
                                expanded = isProductPickerExpanded,
                                onExpandedChange = { isProductPickerExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedProductForEdit?.name ?: "Select from Catalog Products",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProductPickerExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = isProductPickerExpanded,
                                    onDismissRequest = { isProductPickerExpanded = false }
                                ) {
                                    products.take(10).forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text("${p.brand} - ${p.name}") },
                                            onClick = {
                                                selectedProductForEdit = p
                                                isProductPickerExpanded = false
                                                promptText = "Transform ${p.name} into a luxury brushed bronze finish with soft showroom lighting"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "2. Edit Instruction Prompt",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = { Text("e.g. Change the color to matte black, add LED backlighting...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("image_edit_prompt_input"),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Edit Suggestions
                Text(
                    text = "Sample Edit Instructions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(sampleEditPrompts) { p ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { promptText = p }
                        ) {
                            Text(
                                text = p,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val base = baseBitmapForEdit ?: generatedBitmap
                        if (base != null && promptText.isNotBlank() && !isGenerating) {
                            onEditImage(promptText.trim(), base)
                        }
                    },
                    enabled = (baseBitmapForEdit != null || generatedBitmap != null) && promptText.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("apply_edit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Editing with gemini-3.1-flash-image-preview...")
                    } else {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply Image Edit", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Error display if any
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Generated Result Display
            if (generatedBitmap != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Generated / Edited Result",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generated_image_card")
                ) {
                    Column {
                        Image(
                            bitmap = generatedBitmap.asImageBitmap(),
                            contentDescription = "Generated Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Produced by gemini-3.1-flash-image-preview",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Assign to Catalog Product
                            Text(
                                text = "Save to Catalog Product:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            ExposedDropdownMenuBox(
                                expanded = isAssignDropdownExpanded,
                                onExpandedChange = { isAssignDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedProductToAssign?.name ?: "Choose product in catalog...",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAssignDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = isAssignDropdownExpanded,
                                    onDismissRequest = { isAssignDropdownExpanded = false }
                                ) {
                                    products.forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text("${p.code} - ${p.name}") },
                                            onClick = {
                                                selectedProductToAssign = p
                                                isAssignDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        selectedProductToAssign?.let { prod ->
                                            onAssignImageToProduct(prod.id, generatedBitmap)
                                        }
                                    },
                                    enabled = selectedProductToAssign != null,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Assign Photo")
                                }

                                OutlinedButton(
                                    onClick = {
                                        baseBitmapForEdit = generatedBitmap
                                        selectedTab = 1
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Refine / Edit")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
