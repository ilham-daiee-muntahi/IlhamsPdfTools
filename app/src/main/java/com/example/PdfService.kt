package com.example

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class PdfMetadata(val title: String, val author: String, val subject: String)

object PdfService {
    fun init(context: Context) {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun splitPdf(context: Context, sourceUri: Uri, originalName: String, rangesString: String, outputDir: File, onProgress: suspend (Float) -> Unit = {}): List<File> = withContext(Dispatchers.IO) {
        val resultFiles = mutableListOf<File>()
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            PDDocument.load(inputStream, MemoryUsageSetting.setupTempFileOnly()).use { sourceDoc ->
                val numPages = sourceDoc.numberOfPages
                val ranges = parseRanges(rangesString, numPages)
                val totalRanges = ranges.size
                
                for ((index, range) in ranges.withIndex()) {
                    PDDocument().use { newDoc ->
                        for (i in range) {
                            if (i in 1..numPages) {
                                newDoc.importPage(sourceDoc.getPage(i - 1))
                            }
                        }
                        val outputFile = File(outputDir, "${originalName}_split_part_${index + 1}.pdf")
                        newDoc.save(outputFile)
                        resultFiles.add(outputFile)
                    }
                    onProgress((index + 1).toFloat() / totalRanges.toFloat())
                }
            }
        }
        resultFiles
    }

    suspend fun mergePdfs(context: Context, sourceUris: List<Uri>, outputFile: File, onProgress: suspend (Float) -> Unit = {}): File = withContext(Dispatchers.IO) {
        val merger = PDFMergerUtility()
        merger.destinationFileName = outputFile.absolutePath
        val total = sourceUris.size
        
        for ((index, uri) in sourceUris.withIndex()) {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                merger.addSource(inputStream)
            }
            onProgress((index + 1).toFloat() / total.toFloat() * 0.5f)
        }
        merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly())
        onProgress(1.0f)
        outputFile
    }

    suspend fun compressPdf(context: Context, sourceUri: Uri, outputFile: File, compressionLevel: String = "Standard", onProgress: suspend (Float) -> Unit = {}): File = withContext(Dispatchers.IO) {
        val originalSize = context.contentResolver.openFileDescriptor(sourceUri, "r")?.statSize ?: Long.MAX_VALUE
        
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            PDDocument.load(inputStream, MemoryUsageSetting.setupTempFileOnly()).use { sourceDoc ->
                val totalPages = sourceDoc.numberOfPages
                if (compressionLevel == "Aggressive") {
                    sourceDoc.documentCatalog.acroForm = null
                    sourceDoc.documentCatalog.metadata = null
                }
                // Basic compression by copying to a new document (removes unreferenced objects)
                PDDocument().use { newDoc ->
                    for (i in 0 until totalPages) {
                        newDoc.importPage(sourceDoc.getPage(i))
                        onProgress((i + 1).toFloat() / totalPages.toFloat())
                    }
                    newDoc.save(outputFile)
                }
            }
        }
        
        if (outputFile.length() >= originalSize && originalSize > 0) {
            // Failsafe: compression made it larger or didn't help. 
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        
        outputFile
    }

    suspend fun getMetadata(context: Context, sourceUri: Uri): PdfMetadata = withContext(Dispatchers.IO) {
        var metadata = PdfMetadata("", "", "")
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            PDDocument.load(inputStream, MemoryUsageSetting.setupTempFileOnly()).use { doc ->
                val info = doc.documentInformation
                metadata = PdfMetadata(
                    title = info.title ?: "",
                    author = info.author ?: "",
                    subject = info.subject ?: ""
                )
            }
        }
        metadata
    }

    suspend fun editMetadata(context: Context, sourceUri: Uri, metadata: PdfMetadata, outputFile: File, onProgress: suspend (Float) -> Unit = {}): File = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            PDDocument.load(inputStream, MemoryUsageSetting.setupTempFileOnly()).use { doc ->
                val info = doc.documentInformation
                info.title = metadata.title.takeIf { it.isNotBlank() }
                info.author = metadata.author.takeIf { it.isNotBlank() }
                info.subject = metadata.subject.takeIf { it.isNotBlank() }
                doc.documentInformation = info
                onProgress(0.5f)
                doc.save(outputFile)
                onProgress(1.0f)
            }
        }
        outputFile
    }

    private fun parseRanges(rangesString: String, maxPage: Int): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        val parts = rangesString.split(",")
        for (part in parts) {
            val p = part.trim()
            if (p.isEmpty()) continue
            if (p.contains("-")) {
                val bounds = p.split("-")
                if (bounds.size == 2) {
                    val start = bounds[0].trim().toIntOrNull()
                    val end = bounds[1].trim().toIntOrNull()
                    if (start != null && end != null) {
                        ranges.add(start..end)
                    }
                }
            } else {
                val page = p.toIntOrNull()
                if (page != null) {
                    ranges.add(page..page)
                }
            }
        }
        return ranges.ifEmpty { listOf(1..maxPage) }
    }
}
