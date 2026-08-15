# ILHAM'S PDF TOOLS

A fast, secure, and fully offline Android application for managing and processing PDF documents. Built with modern Android development practices using Kotlin and Jetpack Compose.

## Features

*   **Split PDFs**: Divide large PDF documents into smaller parts by specifying page ranges.
*   **Merge PDFs**: Combine multiple PDF files into a single document. Includes an interactive drag-and-drop interface for precise document reordering before merging.
*   **Compress PDFs**: Reduce file sizes with two available compression rates:
    *   **Standard**: Lossless structural reorganization.
    *   **Aggressive**: Strips metadata, interactive AcroForms, and unnecessary catalog references.
    *   Includes a size-protection failsafe to ensure the output file is never larger than the original.
*   **Edit Metadata**: View and edit internal PDF properties like Title, Author, and Subject.
*   **Recent Files & History**: Keeps track of your recently processed files using a local Room database, providing quick access to view or share your processed documents.
*   **Fully Offline & Secure**: All document processing happens locally on your device. No cloud uploads or internet connection required.

## Technologies Used

*   **Kotlin** - Primary language
*   **Jetpack Compose** - Declarative UI framework
*   **Room Database** - Local SQLite persistence for file history
*   **Coroutines & Flow** - Asynchronous programming
*   **PDFBox-Android** - Core PDF processing engine

## Getting Started

1. Clone this repository.
2. Open the project in Android Studio.
3. Sync project with Gradle files.
4. Build and run on an emulator or physical device running Android 7.0 (API 24) or higher.

Or, download the APK from the releases page and install.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
