package com.shockwave.pdfium;

import android.graphics.RectF;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a text page of a PDF document.
 * Provides methods for extracting and searching text.
 * 
 * Always use {@link #close()} when finished to release native resources.
 */
public class PdfTextPage implements Closeable {
    
    private final PdfiumCore core;
    private final long textPagePtr;
    private boolean isClosed = false;

    /*package*/ PdfTextPage(PdfiumCore core, long textPagePtr) {
        this.core = core;
        this.textPagePtr = textPagePtr;
    }

    /**
     * Gets the total number of characters in the page.
     */
    public int getCharCount() {
        checkNotClosed();
        return core.getTextCount(textPagePtr);
    }

    /**
     * Gets all text from the page.
     */
    public String getText() {
        checkNotClosed();
        int count = getCharCount();
        if (count == 0) return "";
        return core.getText(textPagePtr, 0, count);
    }

    /**
     * Extracts a portion of text from the page.
     * 
     * @param startIndex The starting index (0-based)
     * @param count The number of characters to extract
     * @return The extracted text
     */
    public String extractText(int startIndex, int count) {
        checkNotClosed();
        if (count == 0) return "";
        return core.getText(textPagePtr, startIndex, count);
    }

    /**
     * Gets the bounding rectangle of a character at the specified index.
     * Returns [left, top, right, bottom].
     * 
     * @param index The character index (0-based)
     * @return An array of 4 doubles [left, top, right, bottom]
     */
    public double[] getCharBox(int index) {
        checkNotClosed();
        return core.getCharBox(textPagePtr, index);
    }

    /**
     * Gets the index of the character at the specified point (x, y).
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param xTolerance Horizontal tolerance
     * @param yTolerance Vertical tolerance
     * @return The character index (0-based), or -1 if not found
     */
    public int getIndexAtPos(double x, double y, double xTolerance, double yTolerance) {
        checkNotClosed();
        return core.getCharIndexAtPos(textPagePtr, x, y, xTolerance, yTolerance);
    }

    /**
     * Searches for text in the page.
     * 
     * @param query The text to search for
     * @param matchCase If true, the search is case-sensitive
     * @param matchWholeWord If true, search for whole words only
     * @return List of matches found
     */
    public List<PdfTextSearchMatch> search(String query, boolean matchCase, boolean matchWholeWord) {
        checkNotClosed();
        List<PdfTextSearchMatch> matches = new ArrayList<>();
        
        long searchHandle = core.textFindStart(textPagePtr, query, matchCase, matchWholeWord);
        if (searchHandle == 0L) {
            return matches;
        }
        
        try {
            while (core.textFindNext(searchHandle)) {
                int index = core.textGetSchResultIndex(searchHandle);
                int count = core.textGetSchCount(searchHandle);
                matches.add(new PdfTextSearchMatch(index, count));
            }
        } finally {
            core.textFindClose(searchHandle);
        }
        
        return matches;
    }

    /**
     * Searches for text in the page (simplified version, case-insensitive).
     * 
     * @param query The text to search for
     * @return List of matches found
     */
    public List<PdfTextSearchMatch> search(String query) {
        return search(query, false, false);
    }

    /**
     * Gets the bounding rectangles for a text range.
     * Handles multi-line text by returning multiple rectangles.
     * 
     * @param startIndex The starting index (0-based)
     * @param count The number of characters
     * @return List of rectangles
     */
    public List<RectF> getTextRects(int startIndex, int count) {
        checkNotClosed();
        List<RectF> rects = new ArrayList<>();
        int rectCount = core.textCountRects(textPagePtr, startIndex, count);
        
        for (int i = 0; i < rectCount; i++) {
            double[] rectArray = core.textGetRect(textPagePtr, i);
            if (rectArray != null && rectArray.length >= 4) {
                rects.add(new RectF(
                    (float) rectArray[0], // left
                    (float) rectArray[1], // top
                    (float) rectArray[2], // right
                    (float) rectArray[3]  // bottom
                ));
            }
        }
        return rects;
    }

    /**
     * Selects text between two points on the page.
     * This method is useful for implementing text selection from touch/mouse events.
     * 
     * The coordinates should be in PDF page coordinate system. Use 
     * {@link PdfiumCore#mapDeviceToPageCoords} to convert from device/screen coordinates.
     * 
     * @param x1 X coordinate of the first point (tap-down)
     * @param y1 Y coordinate of the first point (tap-down)
     * @param x2 X coordinate of the second point (tap-up)
     * @param y2 Y coordinate of the second point (tap-up)
     * @param xTolerance Horizontal tolerance for character detection
     * @param yTolerance Vertical tolerance for character detection
     * @return PdfTextSelection object containing the selection data, or null if no text is selected
     */
    public PdfTextSelection selectText(double x1, double y1, double x2, double y2, 
                                       double xTolerance, double yTolerance) {
        checkNotClosed();
        
        // Get character indices at both points
        int index1 = getIndexAtPos(x1, y1, xTolerance, yTolerance);
        int index2 = getIndexAtPos(x2, y2, xTolerance, yTolerance);
        
        // If no character found at either point, return null
        if (index1 < 0 || index2 < 0) {
            return null;
        }
        
        // Normalize the order (ensure startIndex <= endIndex)
        int startIndex = Math.min(index1, index2);
        int endIndex = Math.max(index1, index2);
        int count = endIndex - startIndex + 1;
        
        // If count is zero or negative, return null
        if (count <= 0) {
            return null;
        }
        
        // Extract the text
        String text = extractText(startIndex, count);
        
        // Get the bounding rectangles
        List<RectF> rects = getTextRects(startIndex, count);
        
        return new PdfTextSelection(startIndex, count, text, rects);
    }

    /**
     * Selects text between two points on the page using default tolerances.
     * This is a convenience method that uses default tolerance values for character detection.
     * 
     * The coordinates should be in PDF page coordinate system. Use 
     * {@link PdfiumCore#mapDeviceToPageCoords} to convert from device/screen coordinates.
     * 
     * @param x1 X coordinate of the first point (tap-down)
     * @param y1 Y coordinate of the first point (tap-down)
     * @param x2 X coordinate of the second point (tap-up)
     * @param y2 Y coordinate of the second point (tap-up)
     * @return PdfTextSelection object containing the selection data, or null if no text is selected
     */
    public PdfTextSelection selectText(double x1, double y1, double x2, double y2) {
        // Use default tolerances matching KotlinPdfium behavior (10.0, 10.0)
        return selectText(x1, y1, x2, y2, 10.0, 10.0);
    }

    @Override
    public void close() {
        if (!isClosed) {
            core.closeTextPage(textPagePtr);
            isClosed = true;
        }
    }

    private void checkNotClosed() {
        if (isClosed) {
            throw new IllegalStateException("PdfTextPage has been closed");
        }
    }
}
