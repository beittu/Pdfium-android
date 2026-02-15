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

## Text Selection

The library supports interactive text selection, allowing users to select text between two points on a PDF page. This is particularly useful for implementing touch-based text selection in PDF viewers.

### Overview

Text selection works by:
1. Converting device/screen coordinates to PDF page coordinates
2. Finding character indices at the tap-down and tap-up positions
3. Extracting the text between these indices
4. Retrieving bounding rectangles for highlighting

### Coordinate Conversion

Before performing text selection, you need to convert device (screen) coordinates to PDF page coordinates:

```java
PdfiumCore pdfiumCore = new PdfiumCore(context);
PdfDocument doc = pdfiumCore.newDocument(fd);

// Open the page
pdfiumCore.openPage(doc, pageIndex);

// Convert device coordinates to page coordinates
// These parameters depend on how the PDF is rendered in your view
int startX = 0;           // Left pixel position of the display area
int startY = 0;           // Top pixel position of the display area
int sizeX = viewWidth;    // Horizontal size in pixels
int sizeY = viewHeight;   // Vertical size in pixels
int rotate = 0;           // Page rotation (0, 1, 2, or 3)

// Convert tap-down point
double[] pageCoords1 = pdfiumCore.mapDeviceToPageCoords(
    doc, pageIndex, startX, startY, sizeX, sizeY, rotate, 
    deviceX1, deviceY1
);

// Convert tap-up point
double[] pageCoords2 = pdfiumCore.mapDeviceToPageCoords(
    doc, pageIndex, startX, startY, sizeX, sizeY, rotate, 
    deviceX2, deviceY2
);
```

### Selecting Text

Once you have page coordinates, you can select text:

```java
PdfTextPage textPage = pdfiumCore.openTextPage(doc, pageIndex);
try {
    // Select text between two points using default tolerances
    PdfTextSelection selection = textPage.selectText(
        pageCoords1[0], pageCoords1[1],  // First point (x1, y1)
        pageCoords2[0], pageCoords2[1]   // Second point (x2, y2)
    );
    
    // Or specify custom tolerances for character detection
    PdfTextSelection selection = textPage.selectText(
        pageCoords1[0], pageCoords1[1],
        pageCoords2[0], pageCoords2[1],
        10.0,  // xTolerance
        10.0   // yTolerance
    );
    
    if (selection != null) {
        // Selection was successful
        String selectedText = selection.getText();
        int startIndex = selection.getStartIndex();
        int charCount = selection.getCount();
        List<RectF> highlightRects = selection.getRects();
        
        // Use the selected text (e.g., copy to clipboard)
        ClipboardManager clipboard = (ClipboardManager) 
            context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("PDF Text", selectedText);
        clipboard.setPrimaryClip(clip);
        
        // Draw highlights on the canvas
        for (RectF rect : highlightRects) {
            canvas.drawRect(rect, highlightPaint);
        }
    } else {
        // No text was selected (no characters found at the given points)
        Log.d("PDF", "No text selected");
    }
} finally {
    textPage.close();
}
```

### Complete Text Selection Example

Here's a complete example of implementing text selection in a custom view:

```java
public class PdfPageView extends View {
    private PdfiumCore pdfiumCore;
    private PdfDocument pdfDocument;
    private int pageIndex;
    private PdfTextSelection currentSelection;
    
    private float touchDownX, touchDownY;
    private float touchUpX, touchUpY;
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = event.getX();
                touchDownY = event.getY();
                return true;
                
            case MotionEvent.ACTION_UP:
                touchUpX = event.getX();
                touchUpY = event.getY();
                performTextSelection();
                return true;
        }
        return super.onTouchEvent(event);
    }
    
    private void performTextSelection() {
        // Get view dimensions
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        
        // Convert device coordinates to page coordinates
        double[] pageCoords1 = pdfiumCore.mapDeviceToPageCoords(
            pdfDocument, pageIndex, 
            0, 0, viewWidth, viewHeight, 0,
            (int) touchDownX, (int) touchDownY
        );
        
        double[] pageCoords2 = pdfiumCore.mapDeviceToPageCoords(
            pdfDocument, pageIndex, 
            0, 0, viewWidth, viewHeight, 0,
            (int) touchUpX, (int) touchUpY
        );
        
        // Open text page and perform selection
        PdfTextPage textPage = pdfiumCore.openTextPage(pdfDocument, pageIndex);
        try {
            currentSelection = textPage.selectText(
                pageCoords1[0], pageCoords1[1],
                pageCoords2[0], pageCoords2[1]
            );
            
            if (currentSelection != null) {
                Log.d("PDF", "Selected text: " + currentSelection.getText());
                
                // Copy to clipboard
                ClipboardManager clipboard = (ClipboardManager) 
                    getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("PDF", currentSelection.getText());
                clipboard.setPrimaryClip(clip);
                
                // Trigger redraw to show highlights
                invalidate();
                
                // Show a toast or snackbar
                String selectedText = currentSelection.getText();
                Toast.makeText(getContext(), 
                    "Text copied: " + selectedText.substring(0, 
                        Math.min(20, selectedText.length())),
                    Toast.LENGTH_SHORT).show();
            }
        } finally {
            textPage.close();
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw PDF page...
        
        // Draw text selection highlights
        if (currentSelection != null) {
            Paint highlightPaint = new Paint();
            highlightPaint.setColor(Color.argb(100, 255, 255, 0)); // Semi-transparent yellow
            highlightPaint.setStyle(Paint.Style.FILL);
            
            for (RectF rect : currentSelection.getRects()) {
                // Convert page coordinates back to device coordinates for drawing
                Point topLeft = pdfiumCore.mapPageCoordsToDevice(
                    pdfDocument, pageIndex, 0, 0, getWidth(), getHeight(), 0,
                    rect.left, rect.top
                );
                Point bottomRight = pdfiumCore.mapPageCoordsToDevice(
                    pdfDocument, pageIndex, 0, 0, getWidth(), getHeight(), 0,
                    rect.right, rect.bottom
                );
                
                canvas.drawRect(
                    topLeft.x, topLeft.y,
                    bottomRight.x, bottomRight.y,
                    highlightPaint
                );
            }
        }
    }
}
```

### Tolerance Values

The tolerance parameters control how close a coordinate must be to a character for it to be detected:

- **xTolerance**: Horizontal tolerance in PDF points (1/72 inch)
- **yTolerance**: Vertical tolerance in PDF points (1/72 inch)

Default values are 10.0 for both, matching KotlinPdfium behavior. Larger values make character detection more lenient, smaller values make it more precise.

### Handling Edge Cases

```java
PdfTextSelection selection = textPage.selectText(x1, y1, x2, y2);

if (selection == null) {
    // No characters found at one or both points
    // This can happen if:
    // - The user tapped on empty space
    // - The tolerance values are too small
    // - The coordinates are outside the page bounds
    Log.d("PDF", "No text at the selected positions");
} else if (selection.getCount() == 0) {
    // Selection has zero characters (shouldn't happen with current implementation)
    Log.d("PDF", "Empty selection");
} else {
    // Valid selection
    Log.d("PDF", "Selected " + selection.getCount() + " characters");
}
```

### API Reference

#### PdfTextSelection

A value object containing text selection data:

- `int getStartIndex()` - Starting character index (0-based)
- `int getCount()` - Number of characters selected
- `String getText()` - The selected text
- `List<RectF> getRects()` - Bounding rectangles for highlighting (may be multiple for multi-line selections)

#### PdfTextPage Methods

- `PdfTextSelection selectText(double x1, double y1, double x2, double y2)` - Select text with default tolerances (10.0, 10.0)
- `PdfTextSelection selectText(double x1, double y1, double x2, double y2, double xTolerance, double yTolerance)` - Select text with custom tolerances

Both methods return `null` if no character is found at one or both points.

### Performance Notes

1. **Coordinate Conversion**: Coordinate conversion is relatively lightweight and can be done on the UI thread for touch events.

2. **Text Selection**: The `selectText` method performs several operations:
   - Finding character indices (2 calls)
   - Extracting text (1 call)
   - Getting bounding rectangles (1 call)
   
   Consider performing text selection on a background thread for responsiveness, especially for large selections.

3. **Resource Management**: Always close `PdfTextPage` after use to prevent native memory leaks.
