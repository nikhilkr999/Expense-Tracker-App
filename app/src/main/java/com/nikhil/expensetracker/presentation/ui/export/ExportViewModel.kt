// Fixed ExportViewModel with proper PDF generation
package com.nikhil.expensetracker.presentation.ui.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nikhil.expensetracker.domain.repository.usecase.GetCategoriesUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetExpensesUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportViewModel(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase  // Not used, but kept for consistency
) : ViewModel() {

    private val _uiState = MutableLiveData<ExportUiState>()
    val uiState: LiveData<ExportUiState> = _uiState

    private val _exportProgress = MutableLiveData<Int>()
    val exportProgress: LiveData<Int> = _exportProgress

    fun exportToCSV(context: Context, dateRange: Pair<Long, Long>?) {
        viewModelScope.launch {
            _uiState.value = ExportUiState.Loading
            _exportProgress.value = 0

            try {
                val expenses = if (dateRange != null) {
                    getExpensesUseCase().first().filter { expense ->
                        expense.expense.date >= dateRange.first && expense.expense.date <= dateRange.second
                    }
                } else {
                    getExpensesUseCase().first()
                }

                _exportProgress.value = 30

                val csvContent = buildString {
                    appendLine("Date,Category,Amount,Description")

                    expenses.forEach { expense ->
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(expense.expense.date))
                        val category = expense.category?.name ?: "Unknown"
                        val amount = expense.expense.amount
                        val description = expense.expense.description.replace(",", ";") // Escape commas

                        appendLine("$date,$category,$amount,\"$description\"")
                    }
                }

                _exportProgress.value = 60

                val fileName = "expenses_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

                file.writeText(csvContent)

                _exportProgress.value = 100
                _uiState.value = ExportUiState.Success(file.absolutePath, expenses.size)

            } catch (e: Exception) {
                _uiState.value = ExportUiState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun exportToPDF(context: Context, dateRange: Pair<Long, Long>?) {
        viewModelScope.launch {
            _uiState.value = ExportUiState.Loading
            _exportProgress.value = 0

            try {
                val expenses = if (dateRange != null) {
                    getExpensesUseCase().first().filter { expense ->
                        expense.expense.date >= dateRange.first && expense.expense.date <= dateRange.second
                    }
                } else {
                    getExpensesUseCase().first()
                }

                _exportProgress.value = 30

                val fileName = "expenses_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

                val pdfDocument = PdfDocument()
                var pageNumber = 1
                var page = startNewPage(pdfDocument, pageNumber)

                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 18f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                val headerPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                val textPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 10f
                    typeface = Typeface.DEFAULT
                }

                // Draw title
                page.canvas.drawText("Expense Report", 40f, 40f, titlePaint)

                // Draw headers
                var y = 80f
                drawHeaders(page.canvas, headerPaint, y)
                y += 20f

                // Draw horizontal line
                page.canvas.drawLine(40f, y, 555f, y, textPaint)
                y += 10f

                expenses.forEachIndexed { index, exp ->
                    if (y > 780f) {  // Check if need new page (A4 height ~842, leave margin)
                        pdfDocument.finishPage(page)
                        pageNumber++
                        page = startNewPage(pdfDocument, pageNumber)
                        y = 60f
                        drawHeaders(page.canvas, headerPaint, y)
                        y += 20f
                        page.canvas.drawLine(40f, y, 555f, y, textPaint)
                        y += 10f
                    }

                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(exp.expense.date))
                    val category = exp.category?.name ?: "Uncategorized"
                    val amount = exp.expense.amount.toString()
                    val description = exp.expense.description  // Assuming description exists

                    page.canvas.drawText(date, 40f, y, textPaint)
                    page.canvas.drawText(amount, 200f, y, textPaint)
                    page.canvas.drawText(category, 300f, y, textPaint)

                    // Truncate description if too long
                    val truncatedDesc = if (description.length > 30) description.substring(0, 30) + "..." else description
                    page.canvas.drawText(truncatedDesc, 420f, y, textPaint)

                    y += 20f

                    // Update progress
                    _exportProgress.value = 30 + ((index + 1).toFloat() / expenses.size * 30).toInt()
                }

                pdfDocument.finishPage(page)
                pdfDocument.writeTo(FileOutputStream(file))
                pdfDocument.close()

                _exportProgress.value = 100
                _uiState.value = ExportUiState.Success(file.absolutePath, expenses.size)

            } catch (e: Exception) {
                _uiState.value = ExportUiState.Error(e.message ?: "Export failed")
            }
        }
    }

    private fun startNewPage(pdfDocument: PdfDocument, pageNumber: Int): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()  // A4 size in points (72 DPI)
        return pdfDocument.startPage(pageInfo)
    }

    private fun drawHeaders(canvas: Canvas, paint: Paint, y: Float) {
        canvas.drawText("Date", 40f, y, paint)
        canvas.drawText("Amount", 200f, y, paint)
        canvas.drawText("Category", 300f, y, paint)
        canvas.drawText("Description", 420f, y, paint)
    }
}

sealed class ExportUiState {
    object Loading : ExportUiState()
    data class Success(val filePath: String, val expenseCount: Int) : ExportUiState()
    data class Error(val message: String) : ExportUiState()
}

// ViewModel Factory
class ExportViewModelFactory(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExportViewModel(getExpensesUseCase, getCategoriesUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}