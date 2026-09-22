package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ChatMessage
import com.example.ai.GeminiRole
import com.example.data.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHubScreen(
    // Chat state
    chatMessages: List<ChatMessage>,
    currentRole: GeminiRole,
    isChatLoading: Boolean,
    onRoleSelected: (GeminiRole) -> Unit,
    onSendMessage: (String) -> Unit,
    onClearHistory: () -> Unit,
    // Image Studio state
    products: List<Product>,
    isGeneratingImage: Boolean,
    generatedBitmap: Bitmap?,
    imageErrorMessage: String?,
    onGenerateImage: (prompt: String, aspectRatio: String) -> Unit,
    onEditImage: (prompt: String, baseBitmap: Bitmap) -> Unit,
    onAssignImageToProduct: (productId: Long, bitmap: Bitmap) -> Unit,
    onResetImage: () -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Chatbot, 1: Image Studio

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Hub Tab Navigation
        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedSubTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_hub_tab_row")
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gemini Chat", fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier.testTag("ai_tab_chat")
                )

                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Image Studio", fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier.testTag("ai_tab_image_studio")
                )
            }
        }

        if (selectedSubTab == 0) {
            GeminiChatScreen(
                messages = chatMessages,
                currentRole = currentRole,
                isLoading = isChatLoading,
                onRoleSelected = onRoleSelected,
                onSendMessage = onSendMessage,
                onClearHistory = onClearHistory
            )
        } else {
            ImageStudioScreen(
                products = products,
                isGenerating = isGeneratingImage,
                generatedBitmap = generatedBitmap,
                errorMessage = imageErrorMessage,
                onGenerateImage = onGenerateImage,
                onEditImage = onEditImage,
                onAssignImageToProduct = onAssignImageToProduct,
                onResetImage = onResetImage
            )
        }
    }
}
