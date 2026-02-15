package com.shockwave.pdfium;

import android.graphics.RectF;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * Rappresenta una pagina di testo di un documento PDF.
 * Fornisce metodi per estrarre e cercare testo.
 * 
 * Usa sempre {@link #close()} quando hai finito per rilasciare le risorse native.
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
     * Ottiene il numero totale di caratteri nella pagina.
     */
    public int getCharCount() {
        checkNotClosed();
        return core.getTextCount(textPagePtr);
    }

    /**
     * Ottiene tutto il testo della pagina.
     */
    public String getText() {
        checkNotClosed();
        int count = getCharCount();
        if (count == 0) return "";
        return core.getText(textPagePtr, 0, count);
    }

    /**
     * Estrae una porzione di testo dalla pagina.
     * 
     * @param startIndex L'indice iniziale (0-based)
     * @param count Il numero di caratteri da estrarre
     * @return Il testo estratto
     */
    public String extractText(int startIndex, int count) {
        checkNotClosed();
        if (count == 0) return "";
        return core.getText(textPagePtr, startIndex, count);
    }

    /**
     * Ottiene il rettangolo di delimitazione di un carattere all'indice specificato.
     * Ritorna [left, top, right, bottom].
     * 
     * @param index L'indice del carattere (0-based)
     * @return Un array di 4 double [left, top, right, bottom]
     */
    public double[] getCharBox(int index) {
        checkNotClosed();
        return core.getCharBox(textPagePtr, index);
    }

    /**
     * Ottiene l'indice del carattere al punto specificato (x, y).
     * 
     * @param x Coordinata X
     * @param y Coordinata Y
     * @param xTolerance Tolleranza orizzontale
     * @param yTolerance Tolleranza verticale
     * @return L'indice del carattere (0-based), o -1 se non trovato
     */
    public int getIndexAtPos(double x, double y, double xTolerance, double yTolerance) {
        checkNotClosed();
        return core.getCharIndexAtPos(textPagePtr, x, y, xTolerance, yTolerance);
    }

    /**
     * Cerca del testo nella pagina.
     * 
     * @param query Il testo da cercare
     * @param matchCase Se true, la ricerca è case-sensitive
     * @param matchWholeWord Se true, cerca solo parole intere
     * @return Lista di corrispondenze trovate
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
     * Cerca del testo nella pagina (versione semplificata, non case-sensitive).
     * 
     * @param query Il testo da cercare
     * @return Lista di corrispondenze trovate
     */
    public List<PdfTextSearchMatch> search(String query) {
        return search(query, false, false);
    }

    /**
     * Ottiene i rettangoli di delimitazione per un intervallo di testo.
     * Gestisce testo multi-linea ritornando più rettangoli.
     * 
     * @param startIndex L'indice iniziale (0-based)
     * @param count Il numero di caratteri
     * @return Lista di rettangoli
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
