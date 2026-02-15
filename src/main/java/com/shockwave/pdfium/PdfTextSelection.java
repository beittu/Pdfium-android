package com.shockwave.pdfium;

import android.graphics.RectF;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a text selection in a PDF page.
 * Contains the selected text, its position, and bounding rectangles for highlighting.
 */
public class PdfTextSelection {
    private final int startIndex;
    private final int count;
    private final String text;
    private final List<RectF> rects;

    /*package*/ PdfTextSelection(int startIndex, int count, String text, List<RectF> rects) {
        this.startIndex = startIndex;
        this.count = count;
        this.text = text;
        this.rects = rects != null ? Collections.unmodifiableList(new ArrayList<>(rects)) : Collections.emptyList();
    }

    /**
     * @return The starting index (0-based) of the selection in the page text
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * @return The number of characters in the selection
     */
    public int getCount() {
        return count;
    }

    /**
     * @return The selected text
     */
    public String getText() {
        return text;
    }

    /**
     * @return Unmodifiable list of bounding rectangles for the selected text.
     * Multiple rectangles may be returned for multi-line selections.
     */
    public List<RectF> getRects() {
        return rects;
    }

    @Override
    public String toString() {
        return "PdfTextSelection{" +
                "startIndex=" + startIndex +
                ", count=" + count +
                ", text='" + text + '\'' +
                ", rects=" + rects.size() + " rectangles" +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PdfTextSelection that = (PdfTextSelection) o;
        return startIndex == that.startIndex && 
               count == that.count &&
               text.equals(that.text);
    }

    @Override
    public int hashCode() {
        int result = startIndex;
        result = 31 * result + count;
        result = 31 * result + text.hashCode();
        return result;
    }
}
