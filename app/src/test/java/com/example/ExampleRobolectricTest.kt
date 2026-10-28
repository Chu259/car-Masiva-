package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.InventoryExportItem
import com.example.data.model.ProductItem
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ScanStock", appName)
    }

    @Test
    fun testInventoryExportJsonStructure() {
        val testItem = ProductItem(
            barcode = "7794000006164",
            name = "Gaseosa Cola 1.5L",
            category = "pasillo-1",
            unitsPerBulk = 6,
            scannedAt = "2026-10-28T11:30:00Z",
            alternativeNames = listOf("Cola 1500ml", "Gaseosa sabor cola"),
            thumbnail = "https://example.com/thumb.jpg"
        )

        val jsonString = InventoryExportItem.toJsonArrayString(listOf(testItem))
        val array = JSONArray(jsonString)

        assertEquals(1, array.length())
        val obj = array.getJSONObject(0)
        assertEquals("7794000006164", obj.getString("barcode"))
        assertEquals("Gaseosa Cola 1.5L", obj.getString("name"))
        assertEquals("pasillo-1", obj.getString("category"))
        assertEquals(6, obj.getInt("unitsPerBulk"))
        assertEquals(0, obj.getInt("stock"))
        assertEquals(0.0, obj.getDouble("cost"), 0.001)
        assertEquals(0.0, obj.getDouble("price"), 0.001)
        assertEquals("", obj.getString("supplier"))
        assertEquals(0.0, obj.getDouble("taxRate"), 0.001)
    }
}
