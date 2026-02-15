package com.shockwave.pdfium;

/**
 * Rappresenta una corrispondenza di ricerca nel testo di un PDF.
 */
public class PdfTextSearchMatch {
    private final int startIndex;
    private final int count;

    /*package*/ PdfTextSearchMatch(int startIndex, int count) {
        this.startIndex = startIndex;
        this.count = count;
    }

    /**
     * @return L'indice iniziale (0-based) della corrispondenza nel testo della pagina
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * @return Il numero di caratteri nella corrispondenza
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
