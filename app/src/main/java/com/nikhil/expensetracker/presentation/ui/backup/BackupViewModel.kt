// Updated BackupViewModel
package com.nikhil.expensetracker.presentation.ui.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nikhil.expensetracker.data.database.AppDatabase
import com.nikhil.expensetracker.domain.repository.usecase.GetExpensesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupViewModel(
    private val getExpensesUseCase: GetExpensesUseCase
) : ViewModel() {

    private val _backupStatus = MutableLiveData<BackupStatus>()
    val backupStatus: LiveData<BackupStatus> = _backupStatus

    private val _restoreStatus = MutableLiveData<RestoreStatus>()
    val restoreStatus: LiveData<RestoreStatus> = _restoreStatus

    fun createBackup(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _backupStatus.postValue(BackupStatus.Loading)

                // Get database path
                val dbPath = context.getDatabasePath("expense_database").absolutePath
                val dbFile = File(dbPath)

                // Create backup directory
                val backupDir = File(context.getExternalFilesDir("backups"), "")
                backupDir.mkdirs()

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupFile = File(backupDir, "expense_backup_$timestamp.zip")

                // Zip the database file
                ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                    val entry = ZipEntry(dbFile.name)
                    zos.putNextEntry(entry)
                    FileInputStream(dbFile).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }

                withContext(Dispatchers.Main) {
                    _backupStatus.value = BackupStatus.Success(backupFile.absolutePath)
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    _backupStatus.value = BackupStatus.Error(e.message ?: "Backup failed")
                }
            }
        }
    }

    fun restoreBackup(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _restoreStatus.postValue(RestoreStatus.Loading)

                val inputStream = context.contentResolver.openInputStream(uri)
                val dbPath = context.getDatabasePath("expense_database").absolutePath
                val dbFile = File(dbPath)

                // Close the database before overwriting
                AppDatabase.getDatabase(context).close()

                // Unzip if it's a zip file
                inputStream?.use { input ->
                    ZipInputStream(input).use { zis ->
                        var entry: ZipEntry? = zis.nextEntry
                        while (entry != null) {
                            if (entry.name == "expense_database") {
                                FileOutputStream(dbFile).use { output ->
                                    zis.copyTo(output)
                                }
                            }
                            entry = zis.nextEntry
                        }
                    }
                }

                // Reopen the database
                AppDatabase.getDatabase(context)

                withContext(Dispatchers.Main) {
                    _restoreStatus.value = RestoreStatus.Success
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _restoreStatus.value = RestoreStatus.Error(e.message ?: "Restore failed")
                }
            }
        }
    }
}

sealed class BackupStatus {
    object Loading : BackupStatus()
    data class Success(val filePath: String) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}

sealed class RestoreStatus {
    object Loading : RestoreStatus()
    object Success : RestoreStatus()
    data class Error(val message: String) : RestoreStatus()
}

class BackupViewModelFactory(
    private val getExpensesUseCase: GetExpensesUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BackupViewModel(getExpensesUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}