package com.shockwave.pdfium;

/**
 * Represents a search match in PDF text.
 */
public class PdfTextSearchMatch {
    private final int startIndex;
    private final int count;

    /*package*/ PdfTextSearchMatch(int startIndex, int count) {
        this.startIndex = startIndex;
        this.count = count;
    }

    /**
     * @return The starting index (0-based) of the match in the page text
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * @return The number of characters in the match
     */
    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "PdfTextSearchMatch{" +
                "startIndex=" + startIndex +
                ", count=" + count +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PdfTextSearchMatch that = (PdfTextSearchMatch) o;
        return startIndex == that.startIndex && count == that.count;
    }

    @Override
    public int hashCode() {
        return 31 * startIndex + count;
    }
}
