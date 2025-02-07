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

package nom.tam.util.array;

import java.lang.reflect.Array;

/**
 * Multi-dimensional array iterator.
 *
 * @param <BaseArray> 
 *              the generic type of array at the base of a multi-dimensional array object. For example
 *              for a <code>float[][][]</code> array the base would be <code>float[]</code>. 
 */
public class MultiArrayIterator<BaseArray> {

    private final BaseArray baseArray;

    private final boolean baseIsNoSubArray;

    private boolean baseNextCalled = false;

    private final MultiArrayPointer pointer;

    public MultiArrayIterator(BaseArray baseArray) {
        this.baseArray = baseArray;
        this.baseIsNoSubArray = !MultiArrayPointer.isSubArray(this.baseArray);
        this.pointer = new MultiArrayPointer(this.baseArray);
    }

    public Class<?> deepComponentType() {
        Class<?> clazz = this.baseArray.getClass();
        while (clazz.isArray()) {
            clazz = clazz.getComponentType();
        }
        return clazz;
    }

    public BaseArray next() {
        if (this.baseIsNoSubArray) {
            if (this.baseNextCalled) {
                return null;
            }
            this.baseNextCalled = true;
            return this.baseArray;
        }
        Object result = null;
        while (result == null || Array.getLength(result) == 0) {
            result = this.pointer.next();
            if (result == MultiArrayPointer.END) {
                return null;
            }
        }
        return (BaseArray) result;
    }

    public void reset() {
        if (this.baseIsNoSubArray) {
            this.baseNextCalled = false;
        } else {
            this.pointer.reset();
        }
    }

    public int size() {
        int size = 0;
        Object next;
        while ((next = next()) != null) {
            size += Array.getLength(next);
        }
        return size;
    }
}
