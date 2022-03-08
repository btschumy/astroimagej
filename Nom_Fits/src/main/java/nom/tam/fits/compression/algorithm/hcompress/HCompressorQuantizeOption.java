package nom.tam.fits.compression.algorithm.hcompress;

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

import nom.tam.fits.compression.algorithm.quant.QuantizeOption;
import nom.tam.fits.compression.provider.param.hcompress.HCompressQuantizeParameters;

public class HCompressorQuantizeOption extends QuantizeOption {

    private HCompressorOption hCompressorOption = new HCompressorOption();

    public HCompressorQuantizeOption() {
        super();
        this.parameters = new HCompressQuantizeParameters(this);
    }

    @Override
    public HCompressorQuantizeOption copy() {
        HCompressorQuantizeOption copy = (HCompressorQuantizeOption) super.copy();
        copy.hCompressorOption = this.hCompressorOption.copy();
        return copy;
    }

    public HCompressorOption getHCompressorOption() {
        return this.hCompressorOption;
    }

    @Override
    public HCompressorQuantizeOption setTileHeight(int value) {
        super.setTileHeight(value);
        this.hCompressorOption.setTileHeight(value);
        return this;
    }

    @Override
    public HCompressorQuantizeOption setTileWidth(int value) {
        super.setTileWidth(value);
        this.hCompressorOption.setTileWidth(value);
        return this;
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        T result = super.unwrap(clazz);
        if (result == null) {
            return this.hCompressorOption.unwrap(clazz);
        }
        return result;
    }

}
