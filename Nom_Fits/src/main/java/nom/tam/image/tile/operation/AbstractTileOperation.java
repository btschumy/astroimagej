package nom.tam.image.tile.operation;

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

import nom.tam.image.tile.operation.buffer.TileBuffer;
import nom.tam.image.tile.operation.buffer.TileBufferFactory;
import nom.tam.util.type.ElementType;

import java.nio.Buffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public abstract class AbstractTileOperation implements Runnable, ITileOperation {

    private final ITiledImageOperation tiledImageOperation;

    private Future<?> future;

    private TileBuffer tileBuffer;

    private final int tileIndex;

    private final TileArea area;

    public AbstractTileOperation(ITiledImageOperation operation, int tileIndex, TileArea area) {
        this.tiledImageOperation = operation;
        this.tileIndex = tileIndex;
        this.area = area;
    }

    public void execute(ExecutorService threadPool) {
        this.future = threadPool.submit(this);
    }

    public TileArea getArea() {
        return area;
    }

    /**
     * @return the number of pixels in this tile.
     */
    public int getPixelSize() {
        return tileBuffer.getPixelSize();
    }

    public int getTileIndex() {
        return tileIndex;
    }

    /**
     * set the buffer that describes the whole image and let the tile create a
     * slice of it from the position where the tile starts in the whole image.
     * Attention this method is not thread-safe because it changes the position
     * of the buffer parameter.
     *
     * @param buffer
     *            the buffer that describes the whole image.
     */
    public void setWholeImageBuffer(Buffer buffer) {
        this.tileBuffer.setData(buffer);
    }

    /**
     * Wait for the result of the tile processing.
     */
    @Override
    public void waitForResult() {
        try {
            this.future.get();
        } catch (Exception e) {
            throw new IllegalStateException("could not process tile", e);
        }
    }

    protected ElementType<Buffer> getBaseType() {
        return tiledImageOperation.getBaseType();
    }

    protected ITileOperation getPreviousTileOperation() {
        return tiledImageOperation.getTileOperation(getTileIndex() - 1);
    }

    protected TileBuffer getTileBuffer() {
        return tileBuffer;
    }

    protected ITiledImageOperation getTiledImageOperation() {
        return tiledImageOperation;
    }

    @Override
    public ITileOperation setDimensions(int dataOffset, int width, int height) {
        setTileBuffer(TileBufferFactory.createTileBuffer(getBaseType(), //
                dataOffset, //
                tiledImageOperation.getImageWidth(), //
                width, height));
        area.size(width, height);
        return this;
    }

    protected void setTileBuffer(TileBuffer tileBuffer) {
        this.tileBuffer = tileBuffer;
    }
}
