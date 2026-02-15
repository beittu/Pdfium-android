# Text Search Feature Usage Guide

## Overview

This document describes the new text search functionality added to the Pdfium-android library. The implementation provides comprehensive text extraction and search capabilities for PDF documents using PDFium's native text APIs.

## New Classes

### PdfTextPage

Main class for text operations on a PDF page. Implements `Closeable` to properly manage native resources.

**Important:** Always call `close()` when finished to release native resources, preferably using try-with-resources.

### PdfTextSearchMatch

Represents a single search result containing:
- `startIndex`: Starting position (0-based) in the page text
- `count`: Number of characters in the match

## API Methods

### Opening a Text Page

```java
PdfiumCore pdfiumCore = new PdfiumCore(context);
PdfDocument doc = pdfiumCore.newDocument(fd);
pdfiumCore.openPage(doc, 0);

PdfTextPage textPage = pdfiumCore.openTextPage(doc, 0);
```

### Extracting Text

```java
// Get all text from the page
String allText = textPage.getText();

// Get character count
int charCount = textPage.getCharCount();

// Extract specific portion
String portion = textPage.extractText(startIndex, count);
```

### Searching Text

```java
// Simple search (case-insensitive, not whole-word)
List<PdfTextSearchMatch> results = textPage.search("keyword");

// Advanced search with options
List<PdfTextSearchMatch> results = textPage.search(
    "keyword",      // query
    true,           // matchCase - case-sensitive search
    false           // matchWholeWord - whole word matching
);

// Process results
for (PdfTextSearchMatch match : results) {
    int start = match.getStartIndex();
    int count = match.getCount();
    String matchedText = textPage.extractText(start, count);
    System.out.println("Found: " + matchedText);
}
```

### Getting Text Coordinates

```java
// Get bounding box for a single character
double[] charBox = textPage.getCharBox(index);
// Returns [left, top, right, bottom]

// Get rectangles for a text range (handles multi-line text)
List<RectF> rects = textPage.getTextRects(startIndex, count);
for (RectF rect : rects) {
    // Use rect to highlight text in UI
    canvas.drawRect(rect, highlightPaint);
}
```

### Finding Character at Position

```java
int charIndex = textPage.getIndexAtPos(x, y, xTolerance, yTolerance);
if (charIndex >= 0) {
    String character = textPage.extractText(charIndex, 1);
}
```

## Complete Usage Example

```java
PdfiumCore pdfiumCore = new PdfiumCore(context);
PdfDocument doc = pdfiumCore.newDocument(fd);

try {
    // Open the page
    pdfiumCore.openPage(doc, 0);
    
    // Open text page for searching
    PdfTextPage textPage = pdfiumCore.openTextPage(doc, 0);
    try {
        // Extract all text
        String pageText = textPage.getText();
        Log.d("PDF", "Page text length: " + pageText.length());
        
        // Search for a keyword
        List<PdfTextSearchMatch> results = textPage.search("important", false, false);
        Log.d("PDF", "Found " + results.size() + " matches");
        
        // Get coordinates for highlighting
        for (PdfTextSearchMatch match : results) {
            List<RectF> rects = textPage.getTextRects(
                match.getStartIndex(), 
                match.getCount()
            );
            
            // Use rects to highlight the matched text
            for (RectF rect : rects) {
                // Draw highlight on canvas
                canvas.drawRect(rect, highlightPaint);
            }
        }
    } finally {
        textPage.close(); // Always close to free native resources
    }
} finally {
    pdfiumCore.closeDocument(doc);
}
```

## Resource Management

The text search functionality uses native PDFium resources that must be properly cleaned up:

```java
// Recommended: Use try-with-resources (Java 7+)
try (PdfTextPage textPage = pdfiumCore.openTextPage(doc, 0)) {
    List<PdfTextSearchMatch> results = textPage.search("query");
    // Process results...
} // textPage.close() called automatically

// Alternative: Manual cleanup
PdfTextPage textPage = pdfiumCore.openTextPage(doc, 0);
try {
    List<PdfTextSearchMatch> results = textPage.search("query");
    // Process results...
} finally {
    textPage.close(); // Always close in finally block
}
```

## Performance Considerations

1. **Text Page Creation**: Creating a `PdfTextPage` involves native processing. Cache it if you need to perform multiple operations on the same page.

2. **Search Operations**: Each search creates a native search handle that is automatically closed after results are collected.

3. **Rectangle Queries**: Getting text rectangles may return multiple rectangles for multi-line matches.

4. **Memory**: Always close `PdfTextPage` objects to prevent native memory leaks.

## Error Handling

The API throws `IllegalStateException` if you attempt to use a closed `PdfTextPage`:

```java
PdfTextPage textPage = pdfiumCore.openTextPage(doc, 0);
textPage.close();

try {
    textPage.getText(); // Throws IllegalStateException
} catch (IllegalStateException e) {
    Log.e("PDF", "Attempted to use closed text page", e);
}
```

## Implementation Details

### Native API Mapping

The implementation uses the following PDFium APIs:

- `FPDFText_LoadPage` - Create text page
- `FPDFText_ClosePage` - Release text page
- `FPDFText_CountChars` - Get character count
- `FPDFText_GetText` - Extract text
- `FPDFText_GetCharBox` - Get character bounds
- `FPDFText_GetCharIndexAtPos` - Find character at position
- `FPDFText_FindStart` - Start text search
- `FPDFText_FindNext` - Find next match
- `FPDFText_FindClose` - Close search handle
- `FPDFText_GetSchResultIndex` - Get match position
- `FPDFText_GetSchCount` - Get match length
- `FPDFText_CountRects` - Count rectangles for text range
- `FPDFText_GetRect` - Get rectangle coordinates

### Thread Safety

All native calls are synchronized through the `PdfiumCore` lock, making the API thread-safe for concurrent access to different operations.

## Credits

This implementation was adapted from [KotlinPdfium](https://github.com/HyntixHQ/KotlinPdfium) which provides similar functionality for Kotlin-based projects.
