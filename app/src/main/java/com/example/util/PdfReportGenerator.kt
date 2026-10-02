package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.Expense
import com.example.model.Shop
import com.example.model.TransactionWithItems
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    data class ReportData(
        val shop: Shop,
        val periodTitle: String,
        val totalSales: Double,
        val totalCost: Double,
        val grossProfit: Double,
        val totalExpenses: Double,
        val estimatedNetProfit: Double,
        val transactionCount: Int,
        val topProducts: List<Pair<String, Int>>,
        val expenses: List<Expense>,
        val cashierBreakdown: List<Pair<String, Double>>
    )

    fun generateReportPdf(context: Context, data: ReportData): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val primaryColor = Color.rgb(13, 126, 107) // Emerald
        val secondaryColor = Color.rgb(217, 119, 6) // Amber
        val darkTextColor = Color.rgb(15, 23, 42)
        val mutedTextColor = Color.rgb(100, 116, 139)
        val lightBgColor = Color.rgb(241, 245, 249)
        val successColor = Color.rgb(16, 185, 129)

        var y = 40f

        // 1. Top Header Banner
        paint.color = primaryColor
        canvas.drawRoundRect(RectF(30f, y, 565f, y + 70f), 12f, 12f, paint)

        // Brand Title
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 22f
        canvas.drawText("DUKALEO", 46f, y + 32f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        canvas.drawText("Shop Performance & Financial Audit Report", 46f, y + 52f, paint)

        // Period & Date on right side
        paint.textSize = 10f
        val periodText = "Period: ${data.periodTitle}"
        val genDateText = "Date: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())}"
        canvas.drawText(periodText, 380f, y + 32f, paint)
        canvas.drawText(genDateText, 380f, y + 50f, paint)

        y += 90f

        // 2. Shop Details Card
        paint.color = lightBgColor
        canvas.drawRoundRect(RectF(30f, y, 565f, y + 50f), 8f, 8f, paint)

        paint.color = darkTextColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText(data.shop.name, 45f, y + 22f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = mutedTextColor
        paint.textSize = 10f
        canvas.drawText("Owner: ${data.shop.ownerName}  •  Phone: ${data.shop.phone}  •  Location: ${data.shop.location}", 45f, y + 38f, paint)

        y += 65f

        // 3. Financial Summary Grid
        paint.color = darkTextColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("Financial Summary", 30f, y, paint)

        y += 12f

        val boxWidth = 125f
        val boxHeight = 52f
        val gap = 12f

        val stats = listOf(
            Triple("Total Sales", FormatUtils.formatCurrency(data.totalSales), primaryColor),
            Triple("Cost of Goods", FormatUtils.formatCurrency(data.totalCost), mutedTextColor),
            Triple("Gross Profit", FormatUtils.formatCurrency(data.grossProfit), successColor),
            Triple("Expenses", FormatUtils.formatCurrency(data.totalExpenses), Color.rgb(239, 68, 68))
        )

        var x = 30f
        for (stat in stats) {
            paint.color = lightBgColor
            canvas.drawRoundRect(RectF(x, y, x + boxWidth, y + boxHeight), 6f, 6f, paint)

            paint.color = mutedTextColor
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9f
            canvas.drawText(stat.first, x + 10f, y + 18f, paint)

            paint.color = stat.third
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 11f
            canvas.drawText(stat.second, x + 10f, y + 38f, paint)

            x += boxWidth + gap
        }

        y += boxHeight + 14f

        // Net Profit Highlight Box
        paint.color = Color.rgb(230, 245, 242)
        canvas.drawRoundRect(RectF(30f, y, 565f, y + 42f), 8f, 8f, paint)

        paint.color = primaryColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("ESTIMATED NET PROFIT (Gross Profit - Expenses):", 45f, y + 26f, paint)

        paint.textSize = 15f
        canvas.drawText(FormatUtils.formatCurrency(data.estimatedNetProfit), 380f, y + 27f, paint)

        y += 60f

        // 4. Top Selling Products
        paint.color = darkTextColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("Top Selling Products", 30f, y, paint)

        y += 12f

        // Table Header
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRect(RectF(30f, y, 565f, y + 20f), paint)

        paint.color = darkTextColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PRODUCT NAME", 40f, y + 14f, paint)
        canvas.drawText("QUANTITY SOLD", 430f, y + 14f, paint)

        y += 24f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        if (data.topProducts.isEmpty()) {
            canvas.drawText("No product sales recorded in this period.", 40f, y + 12f, paint)
            y += 20f
        } else {
            for (item in data.topProducts.take(5)) {
                paint.color = darkTextColor
                canvas.drawText(item.first, 40f, y + 12f, paint)
                paint.color = primaryColor
                canvas.drawText("${item.second} units", 430f, y + 12f, paint)

                paint.color = Color.rgb(241, 245, 249)
                canvas.drawLine(30f, y + 18f, 565f, y + 18f, paint)
                y += 22f
            }
        }

        y += 15f

        // 5. Cashier Performance & Expense Summary Side-by-side
        paint.color = darkTextColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("Cashier Sales Breakdown", 30f, y, paint)
        canvas.drawText("Recent Expenses", 310f, y, paint)

        y += 14f

        // Cashier Column
        var colY = y
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        if (data.cashierBreakdown.isEmpty()) {
            paint.color = mutedTextColor
            canvas.drawText("No cashier sales recorded.", 30f, colY + 12f, paint)
        } else {
            for (c in data.cashierBreakdown) {
                paint.color = darkTextColor
                canvas.drawText(c.first, 30f, colY + 12f, paint)
                paint.color = primaryColor
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(FormatUtils.formatCurrency(c.second), 180f, colY + 12f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                colY += 18f
            }
        }

        // Expense Column
        var expY = y
        if (data.expenses.isEmpty()) {
            paint.color = mutedTextColor
            canvas.drawText("No expenses recorded in this period.", 310f, expY + 12f, paint)
        } else {
            for (e in data.expenses.take(4)) {
                paint.color = darkTextColor
                canvas.drawText("${e.category}: ${e.description}", 310f, expY + 12f, paint)
                paint.color = Color.rgb(239, 68, 68)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(FormatUtils.formatCurrency(e.amount), 480f, expY + 12f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                expY += 18f
            }
        }

        y = maxOf(colY, expY) + 30f

        // 6. Footer
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(30f, 790f, 565f, 790f, paint)

        paint.color = mutedTextColor
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("DukaLeo POS & Shop Management System • Designed for Tanzanian Retail Businesses", 30f, 805f, paint)
        canvas.drawText("Page 1 of 1 • Official Business Record", 430f, 805f, paint)

        pdfDocument.finishPage(page)

        // Save File
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val fileName = "DukaLeo_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        val pdfFile = File(reportsDir, fileName)

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "com.aistudio.dukaleo.tzpos.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "DukaLeo Shop Performance Report")
            putExtra(Intent.EXTRA_TEXT, "Attached is the latest shop performance report generated from DukaLeo.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share DukaLeo Report PDF"))
    }

    fun viewPdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "com.aistudio.dukaleo.tzpos.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            sharePdf(context, file)
        }
    }
}
