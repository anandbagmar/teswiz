package com.znsio.teswiz.visual;

import static net.sourceforge.tess4j.ITessAPI.TessPageSegMode.PSM_RAW_LINE;
import static net.sourceforge.tess4j.ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK;
import static net.sourceforge.tess4j.ITessAPI.TessPageSegMode.PSM_SINGLE_LINE;
import static net.sourceforge.tess4j.ITessAPI.TessPageSegMode.PSM_SPARSE_TEXT;

/**
 * Immutable OCR read tuning for {@link OcrService#readText(byte[], com.znsio.teswiz.entities.VisualRegion,
 * com.znsio.teswiz.runner.Driver, OcrReadHint) reading} an unknown value out of a region - the knobs that make
 * the hard cases (small fonts, single values, digits-only fields) legible to Tesseract:
 *
 * <ul>
 *   <li>a {@link PageSegmentation page-segmentation mode} that tells Tesseract how the region is laid out
 *       (one line, one raw line, sparse text, or a block);</li>
 *   <li>an {@code upscaleFactor} the cropped region is enlarged by before recognition (small text reads far more
 *       reliably when magnified);</li>
 *   <li>an optional {@code characterWhitelist} restricting recognition to a known alphabet (e.g.
 *       {@code "0123456789.,"} for a currency amount), which removes a large class of look-alike mis-reads.</li>
 * </ul>
 *
 * <p>Instances are created via the static factories ({@link #defaults()}, {@link #digits()},
 * {@link #singleLine()}) or the fluent {@code with*} copies - each returns a new instance, so a shared hint is
 * never mutated. The Tesseract specifics of applying a hint live entirely in {@link OcrService}.
 */
public final class OcrReadHint {

    /** The sensible default region upscale factor; small on-screen text reads more reliably when magnified. */
    public static final double DEFAULT_UPSCALE_FACTOR = 3.0;

    /** The character whitelist used by {@link #digits()} - digits plus the common decimal/grouping separators. */
    public static final String NUMERIC_WHITELIST = "0123456789.,";

    /**
     * How Tesseract should segment the region being read. Each constant maps to a Tesseract page-segmentation
     * mode; the names describe the layout the caller expects rather than the underlying integer.
     */
    public enum PageSegmentation {
        /** A single uniform block of text (Tesseract {@code PSM_SINGLE_BLOCK}). Good general-purpose default. */
        SINGLE_BLOCK(PSM_SINGLE_BLOCK),
        /** Exactly one line of text (Tesseract {@code PSM_SINGLE_LINE}). */
        SINGLE_LINE(PSM_SINGLE_LINE),
        /** One raw line, bypassing Tesseract's line-finding (Tesseract {@code PSM_RAW_LINE}); best for tight crops. */
        RAW_LINE(PSM_RAW_LINE),
        /** Sparse text with no particular order (Tesseract {@code PSM_SPARSE_TEXT}); scattered values on a panel. */
        SPARSE_TEXT(PSM_SPARSE_TEXT);

        private final int tesseractMode;

        PageSegmentation(int tesseractMode) {
            this.tesseractMode = tesseractMode;
        }

        /**
         * @return the Tesseract {@code TessPageSegMode} integer this layout maps to
         */
        public int tesseractMode() {
            return this.tesseractMode;
        }
    }

    private final PageSegmentation pageSegmentation;
    private final double upscaleFactor;
    private final String characterWhitelist;

    private OcrReadHint(PageSegmentation pageSegmentation, double upscaleFactor, String characterWhitelist) {
        this.pageSegmentation = pageSegmentation == null ? PageSegmentation.SINGLE_BLOCK : pageSegmentation;
        this.upscaleFactor = upscaleFactor > 0 ? upscaleFactor : DEFAULT_UPSCALE_FACTOR;
        this.characterWhitelist = (characterWhitelist == null || characterWhitelist.isBlank()) ? null : characterWhitelist;
    }

    /**
     * The general-purpose default hint: a single text block, the {@link #DEFAULT_UPSCALE_FACTOR default upscale},
     * and no character whitelist.
     */
    public static OcrReadHint defaults() {
        return new OcrReadHint(PageSegmentation.SINGLE_BLOCK, DEFAULT_UPSCALE_FACTOR, null);
    }

    /**
     * A numeric-friendly hint for reading amounts/numbers: a single line, the default upscale, and the
     * {@link #NUMERIC_WHITELIST digits-and-separators} whitelist so only numeric glyphs are recognised.
     */
    public static OcrReadHint digits() {
        return new OcrReadHint(PageSegmentation.SINGLE_LINE, DEFAULT_UPSCALE_FACTOR, NUMERIC_WHITELIST);
    }

    /**
     * A hint for reading a single line of free text: single-line segmentation, the default upscale, no whitelist.
     */
    public static OcrReadHint singleLine() {
        return new OcrReadHint(PageSegmentation.SINGLE_LINE, DEFAULT_UPSCALE_FACTOR, null);
    }

    /**
     * @return a copy of this hint using the given page-segmentation layout
     */
    public OcrReadHint withPageSegmentation(PageSegmentation newPageSegmentation) {
        return new OcrReadHint(newPageSegmentation, this.upscaleFactor, this.characterWhitelist);
    }

    /**
     * @return a copy of this hint using the given upscale factor (values &le; 0 fall back to the default)
     */
    public OcrReadHint withUpscaleFactor(double newUpscaleFactor) {
        return new OcrReadHint(this.pageSegmentation, newUpscaleFactor, this.characterWhitelist);
    }

    /**
     * @return a copy of this hint restricting recognition to the given characters (blank/{@code null} clears it)
     */
    public OcrReadHint withCharacterWhitelist(String newCharacterWhitelist) {
        return new OcrReadHint(this.pageSegmentation, this.upscaleFactor, newCharacterWhitelist);
    }

    public PageSegmentation getPageSegmentation() {
        return this.pageSegmentation;
    }

    public double getUpscaleFactor() {
        return this.upscaleFactor;
    }

    /**
     * @return the character whitelist, or {@code null} when recognition is unrestricted
     */
    public String getCharacterWhitelist() {
        return this.characterWhitelist;
    }

    /**
     * @return {@code true} when a non-blank character whitelist is set
     */
    public boolean hasCharacterWhitelist() {
        return this.characterWhitelist != null;
    }

    @Override
    public String toString() {
        return String.format("OcrReadHint[pageSegmentation=%s, upscaleFactor=%.1f, characterWhitelist=%s]",
                this.pageSegmentation, this.upscaleFactor, this.characterWhitelist == null ? "<none>" : "'" + this.characterWhitelist + "'");
    }
}
