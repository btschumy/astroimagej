package nom.tam.image.compression.bintable;

/*
 * #%L
 * nom.tam FITS library
 * %%
 * Copyright (C) 1996 - 2021 nom-tam-fits
 * %%
 * This is free and unencumbered software released into the public domain.
 * 
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * 
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * #L%
 */

import nom.tam.fits.header.Compression;

public final class BinaryTableTileDescription {

    private int rowStart;

    private int rowEnd;

    private int column;

    private int tileIndex;

    private String compressionAlgorithm;

    public static BinaryTableTileDescription tile() {
        return new BinaryTableTileDescription();
    }

    private BinaryTableTileDescription() {
        // use the static method to instantiate this class.
    }

    public BinaryTableTileDescription column(int value) {
        this.column = value;
        return this;
    }

    public BinaryTableTileDescription compressionAlgorithm(String value) {
        this.compressionAlgorithm = value;
        return this;
    }

    public BinaryTableTileDescription rowEnd(int value) {
        this.rowEnd = value;
        return this;
    }

    public BinaryTableTileDescription rowStart(int value) {
        this.rowStart = value;
        return this;
    }

    public BinaryTableTileDescription tileIndex(int value) {
        this.tileIndex = value;
        return this;
    }

    protected int getColumn() {
        return this.column;
    }

    protected String getCompressionAlgorithm() {
        if (this.compressionAlgorithm == null) {
            return Compression.ZCMPTYPE_GZIP_2;
        }
        return this.compressionAlgorithm;
    }

    protected int getRowEnd() {
        return this.rowEnd;
    }

    protected int getRowStart() {
        return this.rowStart;
    }

    protected int getTileIndex() {
        return this.tileIndex;
    }
}
