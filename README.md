Original https://github.com/realmystic/PdfiumAndroid

The purpose is to remove old support libraries so we no longer need to use jetifier.

com.github.beittu:Pdfium-android:1.0

It will be used with the forked [AndroidPdFViewer](https://github.com/realmystic/AndroidPdfViewer)

## What's new in 1.9.9
* Add `getLinkAtPoint()` API for link detection at specific page coordinates
* Add `mapDeviceToPageCoords()` API to convert screen coordinates to page coordinates
* Improve `nativeGetDestPageIndex` with FPDFLink_GetAction fallback when FPDFLink_GetDest returns null
* Enhanced link handling for better parity with KotlinPdfium

## What's new in 1.9.8
* Migrate to .KTS format for gradle
* Update Proguard rules to use optimized rules for android
* Bump minSdk to 23 to align with Android's current supported minSdk
* Bump AGP to 8.13.2 & Appcompat to 1.7.1

## What's new in 1.9.7
* Add 16KB Page size support for Android 15+
* Bump compile & target SDK to 36
* Bump AGP to 8.9.1

## What's new in 1.9.4
* Fix issue for the newest Android devices with API 34+

## What's new in 1.9.3
* Update Gradle plugins and configurations
* Change minimum SDK to 21
* Update pdfium libs

## What's new in 1.9.1
* Update Gradle plugins and configurations
* Update compile sdk to 34
* Change minimum SDK to 23
* Remove support-v4 library
* Drop support for mips
* Update pdfium libs and refactor to cmake

## What's new in 1.9.0?
* Updated Pdfium library to 7.1.2_r36
* Changed `gnustl_static` to `c++_shared`
* Update Gradle plugins
* Update compile SDK and support library to 26
* Change minimum SDK to 14
* Add support for mips64

## Installation
Add to the root _build.gradle_:

```groovy
dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
```

Add to the app _build.gradle_:

`implementation 'com.github.realmystic:PdfiumAndroid:1.9.7'`

Library is available in jcenter and Maven Central repositories.

## Methods inconsistency
Version 1.8.0 added method for getting page size - `PdfiumCore#getPageSize(...)`.
It is important to note, that this method does not require page to be opened. However, there are also
old `PdfiumCore#getPageWidth(...)`, `PdfiumCore#getPageWidthPoint(...)`, `PdfiumCore#getPageHeight()`
and `PdfiumCore#getPageHeightPoint()` which require page to be opened.

This inconsistency will be resolved in next major version, which aims to redesign API.

## Reading links
Version 1.8.0 introduces `PdfiumCore#getPageLinks(PdfDocument, int)` method, which allows to get list
of links from given page. Links are returned as `List` of type `PdfDocument.Link`.
`PdfDocument.Link` holds destination page (may be null), action URI (may be null or empty)
and link bounds in document page coordinates. To map page coordinates to screen coordinates you may use
`PdfiumCore#mapRectToDevice(...)`. See `PdfiumCore#mapPageCoordsToDevice(...)` for parameters description.

### New Link Handling APIs
**Version 1.9.9** adds enhanced link handling capabilities for better parity with KotlinPdfium:

* `PdfiumCore#getLinkAtPoint(PdfDocument doc, int pageIndex, double x, double y)` - Get link at specific page coordinates (useful for tap/click detection)
* `PdfiumCore#mapDeviceToPageCoords(...)` - Convert device (screen) coordinates to page coordinates
* Improved `nativeGetDestPageIndex` with action fallback - now tries FPDFLink_GetAction if FPDFLink_GetDest returns null

These new APIs enable better hit-testing for link detection in PDF viewers.

Sample usage for getting all links:
``` java
PdfiumCore core = ...;
PdfDocument document = ...;
int pageIndex = 0;
core.openPage(document, pageIndex);
List<PdfDocument.Link> links = core.getPageLinks(document, pageIndex);
for (PdfDocument.Link link : links) {
    RectF mappedRect = core.mapRectToDevice(document, pageIndex, ..., link.getBounds())

    if (clickedArea(mappedRect)) {
        String uri = link.getUri();
        if (link.getDestPageIdx() != null) {
            // jump to page
        } else if (uri != null && !uri.isEmpty()) {
            // open URI using Intent
        }
    }
}

```

Sample usage for link detection at tap point:
``` java
PdfiumCore core = ...;
PdfDocument document = ...;
int pageIndex = 0;
core.openPage(document, pageIndex);

// User taps at screen coordinates (deviceX, deviceY)
// Convert to page coordinates first
double[] pageCoords = core.mapDeviceToPageCoords(document, pageIndex, 
    startX, startY, sizeX, sizeY, rotation, deviceX, deviceY);

// Check if there's a link at this point
PdfDocument.Link link = core.getLinkAtPoint(document, pageIndex, 
    pageCoords[0], pageCoords[1]);

if (link != null) {
    if (link.getDestPageIdx() != null) {
        // Navigate to destination page
        int destPage = link.getDestPageIdx();
    } else if (link.getUri() != null && !link.getUri().isEmpty()) {
        // Open external URI
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.getUri()));
        startActivity(intent);
    }
}

```

## Simple example
``` java
void openPdf() {
    ImageView iv = (ImageView) findViewById(R.id.imageView);
    ParcelFileDescriptor fd = ...;
    int pageNum = 0;
    PdfiumCore pdfiumCore = new PdfiumCore(context);
    try {
        PdfDocument pdfDocument = pdfiumCore.newDocument(fd);

        pdfiumCore.openPage(pdfDocument, pageNum);

        int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNum);
        int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNum);

        // ARGB_8888 - best quality, high memory usage, higher possibility of OutOfMemoryError
        // RGB_565 - little worse quality, twice less memory usage
        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.RGB_565);
        pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNum, 0, 0,
                width, height);
        //if you need to render annotations and form fields, you can use
        //the same method above adding 'true' as last param

        iv.setImageBitmap(bitmap);

        printInfo(pdfiumCore, pdfDocument);

        pdfiumCore.closeDocument(pdfDocument); // important!
    } catch(IOException ex) {
        ex.printStackTrace();
    }
}

public void printInfo(PdfiumCore core, PdfDocument doc) {
    PdfDocument.Meta meta = core.getDocumentMeta(doc);
    Log.e(TAG, "title = " + meta.getTitle());
    Log.e(TAG, "author = " + meta.getAuthor());
    Log.e(TAG, "subject = " + meta.getSubject());
    Log.e(TAG, "keywords = " + meta.getKeywords());
    Log.e(TAG, "creator = " + meta.getCreator());
    Log.e(TAG, "producer = " + meta.getProducer());
    Log.e(TAG, "creationDate = " + meta.getCreationDate());
    Log.e(TAG, "modDate = " + meta.getModDate());

    printBookmarksTree(core.getTableOfContents(doc), "-");

}

public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
    for (PdfDocument.Bookmark b : tree) {

        Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

        if (b.hasChildren()) {
            printBookmarksTree(b.getChildren(), sep + "-");
        }
    }
}

```
## Build native part
Go to `PROJECT_PATH/src/main/jni` and run command `$ ndk-build`.
This step may be executed only once, every future `.aar` build will use generated libs.
