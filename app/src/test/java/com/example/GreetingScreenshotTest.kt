package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.Product
import com.example.ui.components.ProductCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun product_card_screenshot() {
        val testProduct = Product(
            id = 1,
            companyId = 1,
            companyName = "Hindware",
            code = "HW-1234",
            name = "Wall Mixer 3-in-1 with Bend Pipe",
            brand = "Hindware",
            category = "CP",
            subcategory = "Mixer",
            mrp = 10000.0,
            discountPercent = 35.0,
            gstPercent = 18.0,
            purchasePrice = 5000.0,
            stockQuantity = 12
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                ProductCard(
                    product = testProduct,
                    isCustomerMode = false,
                    onViewClick = {},
                    onEditClick = {},
                    onDeleteClick = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
