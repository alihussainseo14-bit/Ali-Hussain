package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.InitialCatalogData
import com.example.data.formatPriceRs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("My Store", appName)
    }

    @Test
    fun `default catalog contains signature products and prices`() {
        val products = InitialCatalogData.defaultProducts
        assertTrue(products.size >= 4)
        assertEquals("Premium T-Shirt", products[0].name)
        assertEquals("Rs. 2499", formatPriceRs(products[0].price))
        assertEquals("Running Shoes", products[1].name)
        assertEquals("Rs. 5999", formatPriceRs(products[1].price))
        assertEquals("Classic Watch", products[2].name)
        assertEquals("Rs. 7999", formatPriceRs(products[2].price))
        assertEquals("Leather Bag", products[3].name)
        assertEquals("Rs. 4499", formatPriceRs(products[3].price))
    }
}
