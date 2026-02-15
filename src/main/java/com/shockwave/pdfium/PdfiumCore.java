package com.shockwave.pdfium;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Surface;

import com.shockwave.pdfium.util.Size;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class PdfiumCore {
    private static final String TAG = PdfiumCore.class.getName();
    private static final Class FD_CLASS = FileDescriptor.class;
    private static final String FD_FIELD_NAME = "descriptor";

    static {
        try {
            System.loadLibrary("pdfiumandroid");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Native libraries failed to load - " + e);
        }
    }

    private native long nativeOpenDocument(int fd, String password);

    private native long nativeOpenMemDocument(byte[] data, String password);

    private native void nativeCloseDocument(long docPtr);

    private native int nativeGetPageCount(long docPtr);

    private native long nativeLoadPage(long docPtr, int pageIndex);

    private native long[] nativeLoadPages(long docPtr, int fromIndex, int toIndex);

    private native void nativeClosePage(long pagePtr);

    private native void nativeClosePages(long[] pagesPtr);

    private native int nativeGetPageWidthPixel(long pagePtr, int dpi);

    private native int nativeGetPageHeightPixel(long pagePtr, int dpi);

    private native int nativeGetPageWidthPoint(long pagePtr);

    private native int nativeGetPageHeightPoint(long pagePtr);

    //private native long nativeGetNativeWindow(Surface surface);
    //private native void nativeRenderPage(long pagePtr, long nativeWindowPtr);
    private native void nativeRenderPage(long pagePtr, Surface surface, int dpi,
                                         int startX, int startY,
                                         int drawSizeHor, int drawSizeVer,
                                         boolean renderAnnot);

    private native void nativeRenderPageBitmap(long pagePtr, Bitmap bitmap, int dpi,
                                               int startX, int startY,
                                               int drawSizeHor, int drawSizeVer,
                                               boolean renderAnnot);

    private native String nativeGetDocumentMetaText(long docPtr, String tag);

    private native Long nativeGetFirstChildBookmark(long docPtr, Long bookmarkPtr);

    private native Long nativeGetSiblingBookmark(long docPtr, long bookmarkPtr);

    private native String nativeGetBookmarkTitle(long bookmarkPtr);

    private native long nativeGetBookmarkDestIndex(long docPtr, long bookmarkPtr);

    private native Size nativeGetPageSizeByIndex(long docPtr, int pageIndex, int dpi);

    private native long[] nativeGetPageLinks(long pagePtr);

    private native Integer nativeGetDestPageIndex(long docPtr, long linkPtr);

    private native String nativeGetLinkURI(long docPtr, long linkPtr);

    private native RectF nativeGetLinkRect(long linkPtr);
    
    private native long nativeGetLinkAtPoint(long pagePtr, double x, double y);
    
    private native double[] nativeDeviceToPageCoords(long pagePtr, int startX, int startY, int sizeX,
                                                     int sizeY, int rotate, int deviceX, int deviceY);

    private native Point nativePageCoordsToDevice(long pagePtr, int startX, int startY, int sizeX,
                                                  int sizeY, int rotate, double pageX, double pageY);



    /* synchronize native methods */
    private static final Object lock = new Object();
    private static Field mFdField = null;
    private int mCurrentDpi;

    public static int getNumFd(ParcelFileDescriptor fdObj) {
        try {
            if (mFdField == null) {
                mFdField = FD_CLASS.getDeclaredField(FD_FIELD_NAME);
                mFdField.setAccessible(true);
            }

            return mFdField.getInt(fdObj.getFileDescriptor());
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return -1;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return -1;
        }
    }


    /** Context needed to get screen density */
    public PdfiumCore(Context ctx) {
        mCurrentDpi = ctx.getResources().getDisplayMetrics().densityDpi;
        Log.d(TAG, "Starting PdfiumAndroid " + BuildConfig.VERSION_NAME);
    }

    /** Create new document from file */
    public PdfDocument newDocument(ParcelFileDescriptor fd) throws IOException {
        return newDocument(fd, null);
    }

    /** Create new document from file with password */
    public PdfDocument newDocument(ParcelFileDescriptor fd, String password) throws IOException {
        PdfDocument document = new PdfDocument();
        document.parcelFileDescriptor = fd;
        synchronized (lock) {
            document.mNativeDocPtr = nativeOpenDocument(getNumFd(fd), password);
        }

        return document;
    }

    /** Create new document from bytearray */
    public PdfDocument newDocument(byte[] data) throws IOException {
        return newDocument(data, null);
    }

    /** Create new document from bytearray with password */
    public PdfDocument newDocument(byte[] data, String password) throws IOException {
        PdfDocument document = new PdfDocument();
        synchronized (lock) {
            document.mNativeDocPtr = nativeOpenMemDocument(data, password);
        }
        return document;
    }

    /** Get total numer of pages in document */
    public int getPageCount(PdfDocument doc) {
        synchronized (lock) {
            return nativeGetPageCount(doc.mNativeDocPtr);
        }
    }

    /** Open page and store native pointer in {@link PdfDocument} */
    public long openPage(PdfDocument doc, int pageIndex) {
        long pagePtr;
        synchronized (lock) {
            pagePtr = nativeLoadPage(doc.mNativeDocPtr, pageIndex);
            doc.mNativePagesPtr.put(pageIndex, pagePtr);
            return pagePtr;
        }

    }

    /** Open range of pages and store native pointers in {@link PdfDocument} */
    public long[] openPage(PdfDocument doc, int fromIndex, int toIndex) {
        long[] pagesPtr;
        synchronized (lock) {
            pagesPtr = nativeLoadPages(doc.mNativeDocPtr, fromIndex, toIndex);
            int pageIndex = fromIndex;
            for (long page : pagesPtr) {
                if (pageIndex > toIndex) break;
                doc.mNativePagesPtr.put(pageIndex, page);
                pageIndex++;
            }

            return pagesPtr;
        }
    }

    /**
     * Get page width in pixels. <br>
     * This method requires page to be opened.
     */
    public int getPageWidth(PdfDocument doc, int index) {
        synchronized (lock) {
            Long pagePtr;
            if ((pagePtr = doc.mNativePagesPtr.get(index)) != null) {
                return nativeGetPageWidthPixel(pagePtr, mCurrentDpi);
            }
            return 0;
        }
    }

    /**
     * Get page height in pixels. <br>
     * This method requires page to be opened.
     */
    public int getPageHeight(PdfDocument doc, int index) {
        synchronized (lock) {
            Long pagePtr;
            if ((pagePtr = doc.mNativePagesPtr.get(index)) != null) {
                return nativeGetPageHeightPixel(pagePtr, mCurrentDpi);
            }
            return 0;
        }
    }

    /**
     * Get page width in PostScript points (1/72th of an inch).<br>
     * This method requires page to be opened.
     */
    public int getPageWidthPoint(PdfDocument doc, int index) {
        synchronized (lock) {
            Long pagePtr;
            if ((pagePtr = doc.mNativePagesPtr.get(index)) != null) {
                return nativeGetPageWidthPoint(pagePtr);
            }
            return 0;
        }
    }

    /**
     * Get page height in PostScript points (1/72th of an inch).<br>
     * This method requires page to be opened.
     */
    public int getPageHeightPoint(PdfDocument doc, int index) {
        synchronized (lock) {
            Long pagePtr;
            if ((pagePtr = doc.mNativePagesPtr.get(index)) != null) {
                return nativeGetPageHeightPoint(pagePtr);
            }
            return 0;
        }
    }

    /**
     * Get size of page in pixels.<br>
     * This method does not require given page to be opened.
     */
    public Size getPageSize(PdfDocument doc, int index) {
        synchronized (lock) {
            return nativeGetPageSizeByIndex(doc.mNativeDocPtr, index, mCurrentDpi);
        }
    }

    /**
     * Render page fragment on {@link Surface}.<br>
     * Page must be opened before rendering.
     */
    public void renderPage(PdfDocument doc, Surface surface, int pageIndex,
                           int startX, int startY, int drawSizeX, int drawSizeY) {
        renderPage(doc, surface, pageIndex, startX, startY, drawSizeX, drawSizeY, false);
    }

    /**
     * Render page fragment on {@link Surface}. This method allows to render annotations.<br>
     * Page must be opened before rendering.
     */
    public void renderPage(PdfDocument doc, Surface surface, int pageIndex,
                           int startX, int startY, int drawSizeX, int drawSizeY,
                           boolean renderAnnot) {
        synchronized (lock) {
            try {
                //nativeRenderPage(doc.mNativePagesPtr.get(pageIndex), surface, mCurrentDpi);
                nativeRenderPage(doc.mNativePagesPtr.get(pageIndex), surface, mCurrentDpi,
                        startX, startY, drawSizeX, drawSizeY, renderAnnot);
            } catch (NullPointerException e) {
                Log.e(TAG, "mContext may be null");
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "Exception throw from native");
                e.printStackTrace();
            }
        }
    }

    /**
     * Render page fragment on {@link Bitmap}.<br>
     * Page must be opened before rendering.
     * <p>
     * Supported bitmap configurations:
     * <ul>
     * <li>ARGB_8888 - best quality, high memory usage, higher possibility of OutOfMemoryError
     * <li>RGB_565 - little worse quality, twice less memory usage
     * </ul>
     */
    public void renderPageBitmap(PdfDocument doc, Bitmap bitmap, int pageIndex,
                                 int startX, int startY, int drawSizeX, int drawSizeY) {
        renderPageBitmap(doc, bitmap, pageIndex, startX, startY, drawSizeX, drawSizeY, false);
    }

    /**
     * Render page fragment on {@link Bitmap}. This method allows to render annotations.<br>
     * Page must be opened before rendering.
     * <p>
     * For more info see {@link PdfiumCore#renderPageBitmap(PdfDocument, Bitmap, int, int, int, int, int)}
     */
    public void renderPageBitmap(PdfDocument doc, Bitmap bitmap, int pageIndex,
                                 int startX, int startY, int drawSizeX, int drawSizeY,
                                 boolean renderAnnot) {
        synchronized (lock) {
            try {
                nativeRenderPageBitmap(doc.mNativePagesPtr.get(pageIndex), bitmap, mCurrentDpi,
                        startX, startY, drawSizeX, drawSizeY, renderAnnot);
            } catch (NullPointerException e) {
                Log.e(TAG, "mContext may be null");
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "Exception throw from native");
                e.printStackTrace();
            }
        }
    }

    /** Release native resources and opened file */
    public void closeDocument(PdfDocument doc) {
        synchronized (lock) {
            for (Integer index : doc.mNativePagesPtr.keySet()) {
                nativeClosePage(doc.mNativePagesPtr.get(index));
            }
            doc.mNativePagesPtr.clear();

            nativeCloseDocument(doc.mNativeDocPtr);

            if (doc.parcelFileDescriptor != null) { //if document was loaded from file
                try {
                    doc.parcelFileDescriptor.close();
                } catch (IOException e) {
                    /* ignore */
                }
                doc.parcelFileDescriptor = null;
            }
        }
    }

    /** Get metadata for given document */
    public PdfDocument.Meta getDocumentMeta(PdfDocument doc) {
        synchronized (lock) {
            PdfDocument.Meta meta = new PdfDocument.Meta();
            meta.title = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Title");
            meta.author = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Author");
            meta.subject = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Subject");
            meta.keywords = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Keywords");
            meta.creator = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Creator");
            meta.producer = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Producer");
            meta.creationDate = nativeGetDocumentMetaText(doc.mNativeDocPtr, "CreationDate");
            meta.modDate = nativeGetDocumentMetaText(doc.mNativeDocPtr, "ModDate");

            return meta;
        }
    }

    /** Get table of contents (bookmarks) for given document */
    public List<PdfDocument.Bookmark> getTableOfContents(PdfDocument doc) {
        synchronized (lock) {
            List<PdfDocument.Bookmark> topLevel = new ArrayList<>();
            Long first = nativeGetFirstChildBookmark(doc.mNativeDocPtr, null);
            if (first != null) {
                recursiveGetBookmark(topLevel, doc, first);
            }
            return topLevel;
        }
    }

    private void recursiveGetBookmark(List<PdfDocument.Bookmark> tree, PdfDocument doc, long bookmarkPtr) {
        PdfDocument.Bookmark bookmark = new PdfDocument.Bookmark();
        bookmark.mNativePtr = bookmarkPtr;
        bookmark.title = nativeGetBookmarkTitle(bookmarkPtr);
        bookmark.pageIdx = nativeGetBookmarkDestIndex(doc.mNativeDocPtr, bookmarkPtr);
        tree.add(bookmark);

        Long child = nativeGetFirstChildBookmark(doc.mNativeDocPtr, bookmarkPtr);
        if (child != null) {
            recursiveGetBookmark(bookmark.getChildren(), doc, child);
        }

        Long sibling = nativeGetSiblingBookmark(doc.mNativeDocPtr, bookmarkPtr);
        if (sibling != null) {
            recursiveGetBookmark(tree, doc, sibling);
        }
    }

    /** Get all links from given page */
    public List<PdfDocument.Link> getPageLinks(PdfDocument doc, int pageIndex) {
        synchronized (lock) {
            List<PdfDocument.Link> links = new ArrayList<>();
            Long nativePagePtr = doc.mNativePagesPtr.get(pageIndex);
            if (nativePagePtr == null) {
                return links;
            }
            long[] linkPtrs = nativeGetPageLinks(nativePagePtr);
            for (long linkPtr : linkPtrs) {
                Integer index = nativeGetDestPageIndex(doc.mNativeDocPtr, linkPtr);
                String uri = nativeGetLinkURI(doc.mNativeDocPtr, linkPtr);

                RectF rect = nativeGetLinkRect(linkPtr);
                if (rect != null && (index != null || uri != null)) {
                    links.add(new PdfDocument.Link(rect, index, uri));
                }

            }
            return links;
        }
    }

    /**
     * Get link at specific point on page in page coordinates.
     * This method allows to detect which link (if any) is at a given point on the page.
     * 
     * @param doc       pdf document
     * @param pageIndex index of page (must be opened)
     * @param x         X coordinate in page coordinate system
     * @param y         Y coordinate in page coordinate system
     * @return Link object if found at the point, null otherwise
     */
    public PdfDocument.Link getLinkAtPoint(PdfDocument doc, int pageIndex, double x, double y) {
        synchronized (lock) {
            Long nativePagePtr = doc.mNativePagesPtr.get(pageIndex);
            if (nativePagePtr == null) {
                return null;
            }
            
            long linkPtr = nativeGetLinkAtPoint(nativePagePtr, x, y);
            if (linkPtr == 0) {
                return null;
            }
            
            Integer index = nativeGetDestPageIndex(doc.mNativeDocPtr, linkPtr);
            String uri = nativeGetLinkURI(doc.mNativeDocPtr, linkPtr);
            RectF rect = nativeGetLinkRect(linkPtr);
            
            if (rect != null && (index != null || uri != null)) {
                return new PdfDocument.Link(rect, index, uri);
            }
            
            return null;
        }
    }

    /**
     * Convert device (screen) coordinates to page coordinates.
     * This is useful for converting tap/click coordinates to page coordinates
     * for use with getLinkAtPoint.
     * 
     * @param doc       pdf document
     * @param pageIndex index of page
     * @param startX    left pixel position of the display area in device coordinates
     * @param startY    top pixel position of the display area in device coordinates
     * @param sizeX     horizontal size (in pixels) for displaying the page
     * @param sizeY     vertical size (in pixels) for displaying the page
     * @param rotate    page orientation: 0 (normal), 1 (rotated 90 degrees clockwise),
     *                  2 (rotated 180 degrees), 3 (rotated 90 degrees counter-clockwise)
     * @param deviceX   X value in device coordinates
     * @param deviceY   Y value in device coordinates
     * @return array with two elements: [pageX, pageY]
     */
    public double[] mapDeviceToPageCoords(PdfDocument doc, int pageIndex, int startX, int startY, 
                                          int sizeX, int sizeY, int rotate, int deviceX, int deviceY) {
        long pagePtr = doc.mNativePagesPtr.get(pageIndex);
        return nativeDeviceToPageCoords(pagePtr, startX, startY, sizeX, sizeY, rotate, deviceX, deviceY);
    }

    /**
     * Map page coordinates to device screen coordinates
     *
     * @param doc       pdf document
     * @param pageIndex index of page
     * @param startX    left pixel position of the display area in device coordinates
     * @param startY    top pixel position of the display area in device coordinates
     * @param sizeX     horizontal size (in pixels) for displaying the page
     * @param sizeY     vertical size (in pixels) for displaying the page
     * @param rotate    page orientation: 0 (normal), 1 (rotated 90 degrees clockwise),
     *                  2 (rotated 180 degrees), 3 (rotated 90 degrees counter-clockwise)
     * @param pageX     X value in page coordinates
     * @param pageY     Y value in page coordinate
     * @return mapped coordinates
     */
    public Point mapPageCoordsToDevice(PdfDocument doc, int pageIndex, int startX, int startY, int sizeX,
                                       int sizeY, int rotate, double pageX, double pageY) {
        long pagePtr = doc.mNativePagesPtr.get(pageIndex);
        return nativePageCoordsToDevice(pagePtr, startX, startY, sizeX, sizeY, rotate, pageX, pageY);
    }

    /**
     * @return mapped coordinates
     * @see PdfiumCore#mapPageCoordsToDevice(PdfDocument, int, int, int, int, int, int, double, double)
     */
    public RectF mapRectToDevice(PdfDocument doc, int pageIndex, int startX, int startY, int sizeX,
                                 int sizeY, int rotate, RectF coords) {

        Point leftTop = mapPageCoordsToDevice(doc, pageIndex, startX, startY, sizeX, sizeY, rotate,
                coords.left, coords.top);
        Point rightBottom = mapPageCoordsToDevice(doc, pageIndex, startX, startY, sizeX, sizeY, rotate,
                coords.right, coords.bottom);
        return new RectF(leftTop.x, leftTop.y, rightBottom.x, rightBottom.y);
    }

    // ===============================================
    // NUOVI METODI PER LA GESTIONE DEL TESTO
    // ===============================================
    
    // Metodi nativi per operazioni su testo
    private native long nativeLoadTextPage(long docPtr, long pagePtr);
    private native void nativeCloseTextPage(long textPagePtr);
    private native int nativeTextCountChars(long textPagePtr);
    private native String nativeGetText(long textPagePtr, int startIndex, int count);
    private native void nativeGetCharBox(long textPagePtr, int index, double[] result);
    private native int nativeGetCharIndexAtPos(long textPagePtr, double x, double y, 
                                               double xTolerance, double yTolerance);
    
    // Metodi nativi per la ricerca
    private native long nativeTextFindStart(long textPagePtr, String query, 
                                           boolean matchCase, boolean matchWholeWord);
    private native boolean nativeTextFindNext(long searchHandle);
    private native boolean nativeTextFindPrev(long searchHandle);
    private native int nativeTextGetSchResultIndex(long searchHandle);
    private native int nativeTextGetSchCount(long searchHandle);
    private native void nativeTextFindClose(long searchHandle);
    
    // Metodi nativi per i rettangoli di testo
    private native int nativeTextCountRects(long textPagePtr, int startIndex, int count);
    private native double[] nativeTextGetRect(long textPagePtr, int rectIndex);
    
    // Metodi pubblici per operazioni su testo
    
    /**
     * Opens a text page for a PDF document.
     * Remember to close the page when finished using {@link PdfTextPage#close()}.
     * 
     * @param doc The PDF document
     * @param pageIndex The page index (0-based)
     * @return A PdfTextPage object
     */
    public PdfTextPage openTextPage(PdfDocument doc, int pageIndex) {
        synchronized (lock) {
            Long pagePtr = doc.mNativePagesPtr.get(pageIndex);
            if (pagePtr == null) {
                pagePtr = openPage(doc, pageIndex);
            }
            long textPagePtr = nativeLoadTextPage(doc.mNativeDocPtr, pagePtr);
            return new PdfTextPage(this, textPagePtr);
        }
    }
    
    /*package*/ void closeTextPage(long textPagePtr) {
        synchronized (lock) {
            nativeCloseTextPage(textPagePtr);
        }
    }
    
    /*package*/ int getTextCount(long textPagePtr) {
        synchronized (lock) {
            return nativeTextCountChars(textPagePtr);
        }
    }
    
    /*package*/ String getText(long textPagePtr, int startIndex, int count) {
        synchronized (lock) {
            String text = nativeGetText(textPagePtr, startIndex, count);
            return text != null ? text : "";
        }
    }
    
    /*package*/ double[] getCharBox(long textPagePtr, int index) {
        synchronized (lock) {
            double[] result = new double[4];
            nativeGetCharBox(textPagePtr, index, result);
            return result;
        }
    }
    
    /*package*/ int getCharIndexAtPos(long textPagePtr, double x, double y, 
                                      double xTolerance, double yTolerance) {
        synchronized (lock) {
            return nativeGetCharIndexAtPos(textPagePtr, x, y, xTolerance, yTolerance);
        }
    }
    
    // Metodi per la ricerca
    
    /*package*/ long textFindStart(long textPagePtr, String query, 
                                   boolean matchCase, boolean matchWholeWord) {
        synchronized (lock) {
            return nativeTextFindStart(textPagePtr, query, matchCase, matchWholeWord);
        }
    }
    
    /*package*/ boolean textFindNext(long searchHandle) {
        synchronized (lock) {
            return nativeTextFindNext(searchHandle);
        }
    }
    
    /*package*/ boolean textFindPrev(long searchHandle) {
        synchronized (lock) {
            return nativeTextFindPrev(searchHandle);
        }
    }
    
    /*package*/ int textGetSchResultIndex(long searchHandle) {
        synchronized (lock) {
            return nativeTextGetSchResultIndex(searchHandle);
        }
    }
    
    /*package*/ int textGetSchCount(long searchHandle) {
        synchronized (lock) {
            return nativeTextGetSchCount(searchHandle);
        }
    }
    
    /*package*/ void textFindClose(long searchHandle) {
        synchronized (lock) {
            nativeTextFindClose(searchHandle);
        }
    }
    
    /*package*/ int textCountRects(long textPagePtr, int startIndex, int count) {
        synchronized (lock) {
            return nativeTextCountRects(textPagePtr, startIndex, count);
        }
    }
    
    /*package*/ double[] textGetRect(long textPagePtr, int rectIndex) {
        synchronized (lock) {
            return nativeTextGetRect(textPagePtr, rectIndex);
        }
    }
}
