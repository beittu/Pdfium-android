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
