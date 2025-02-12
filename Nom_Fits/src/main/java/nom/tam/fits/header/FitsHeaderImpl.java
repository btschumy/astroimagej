package nom.tam.fits.header;

import java.io.Serializable;
import java.util.HashSet;

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

public class FitsHeaderImpl implements IFitsHeader, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 2393951402526656978L;

    private final String comment;

    private final HDU hdu;

    private final String key;

    private final SOURCE status;

    private final VALUE valueType;
    
    private static HashSet<String> commentStyleKeys = new HashSet<>();

    static {
        commentStyleKeys.add("ANNOTATE");
    }

    public FitsHeaderImpl(String headerName, SOURCE status, HDU hdu, VALUE valueType, String comment) {
        this.key = headerName;
        this.status = status;
        this.hdu = hdu;
        this.valueType = valueType;
        this.comment = comment;
        if (valueType == VALUE.NONE) {
            commentStyleKeys.add(headerName);
        }
    }

    @Override
    public String comment() {
        return this.comment;
    }

    @Override
    public HDU hdu() {
        return this.hdu;
    }

    @Override
    public String key() {
        return this.key;
    }

    @Override
    public IFitsHeader n(int... numbers) {
        StringBuffer headerName = new StringBuffer(this.key);
        for (int number : numbers) {
            int indexOfN = headerName.indexOf("n");
            headerName.replace(indexOfN, indexOfN + 1, Integer.toString(number));
        }
        return new FitsHeaderImpl(headerName.toString(), this.status, this.hdu, this.valueType, this.comment);
    }

    @Override
    public SOURCE status() {
        return this.status;
    }

    @Override
    public VALUE valueType() {
        return this.valueType;
    }
    
    /**
     * Checks if a keywords is known to be a comment-style keyword. That is, it checks if the <code>key</code> argument matches 
     * any {@link IFitsHeader} constructed via this implementation with a <code>valueType</code> argument that was
     * <code>null</code>, or if the key is empty.
     * 
     * @param key       the keyword to check
     * @return          <code>true</code> if the key is empty or if it matches any known {@link IFitsHeader} keywords
     *                  implemented through this class that have valueType of <code>null</code>. Otherwise <code>false</code>.
     *                  
     * @since 1.17
     */
    public static boolean isCommentStyleKey(String key) {
        return commentStyleKeys.contains(key) || key.trim().isEmpty();
    }
}
