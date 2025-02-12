package nom.tam.fits;

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

import nom.tam.fits.header.Standard;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.NoSuchElementException;

import static nom.tam.fits.header.Standard.NAXISn;
import static nom.tam.fits.header.Standard.XTENSION_IMAGE;

public class ProtectedFitsTest {

    @Test(expected = FitsException.class)
    public void testFitsInconsistent() throws Exception {
        try {
            UndefinedData undefinedData = new UndefinedData(new byte[1]);
            Fits fits = new Fits();
            fits.insertHDU(new UndefinedHDU(UndefinedHDU.manufactureHeader(undefinedData), undefinedData), 0);
            fits.insertHDU(new UndefinedHDU(UndefinedHDU.manufactureHeader(undefinedData), undefinedData) {

                @Override
                void setPrimaryHDU(boolean newPrimary) throws FitsException {
                    throw new NoSuchElementException();
                }
            }, 1);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("inconsistency"));
            throw e;
        }
    }

    @Test
    public void testRandomGroupsHDUmanufactureHeader() throws Exception {
        FitsException actual = null;
        try {
            RandomGroupsHDU.manufactureHeader(null);
        } catch (FitsException e) {
            actual = e;
        }
        Assert.assertNotNull(actual);
        Assert.assertTrue(actual.getMessage().contains("null"));

    }

    @Test
    public void testFitsRandomGroupHDUisHeader() throws Exception {
        Header header = new Header()//
                .card(Standard.GROUPS).value(true)//
                .card(Standard.GCOUNT).value(2)//
                .card(Standard.PCOUNT).value(2)//
                .card(Standard.NAXIS).value(2)//
                .card(NAXISn.n(1)).value(0)//
                .card(NAXISn.n(2)).value(2)//
                .card(Standard.NAXIS.BITPIX).value(32)//
                .header();

        Assert.assertFalse(RandomGroupsHDU.isHeader(header));
        header.card(Standard.SIMPLE).value(true);
        Assert.assertTrue(RandomGroupsHDU.isHeader(header));
        header.deleteKey(Standard.SIMPLE);
        header.card(Standard.XTENSION).value(XTENSION_IMAGE);
        Assert.assertTrue(RandomGroupsHDU.isHeader(header));
        RandomGroupsHDU randomGroupsHDU = new RandomGroupsHDU(header, RandomGroupsHDU.manufactureData(header));
        Assert.assertTrue(randomGroupsHDU.isHeader());
        randomGroupsHDU.setPrimaryHDU(true);
        Assert.assertTrue(randomGroupsHDU.getHeader().getBooleanValue(Standard.SIMPLE));
        randomGroupsHDU.setPrimaryHDU(false);
        Assert.assertFalse(randomGroupsHDU.getHeader().getBooleanValue(Standard.SIMPLE));
        Assert.assertEquals(XTENSION_IMAGE, randomGroupsHDU.getHeader().getStringValue(Standard.XTENSION));

        randomGroupsHDU = new RandomGroupsHDU(null, RandomGroupsHDU.manufactureData(header));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        randomGroupsHDU.info(new PrintStream(out));
        String groupInfo = out.toString("UTF-8");
        Assert.assertTrue(groupInfo.contains("No Header"));

    }

    @Test
    public void testFitsRandomGroupData() throws Exception {
        RandomGroupsData data = new RandomGroupsData(new Object[0][]);
        FitsException actual = null;
        try {
            data.fillHeader(new Header());
        } catch (FitsException e) {
            actual = e;
        }
        Assert.assertNotNull(actual);
        Assert.assertTrue(actual.getMessage().toLowerCase().contains("not conform"));
        Object[][] dataArray = {
            new Object[]{
                new double[10],
                new int[10],
            }
        };
        data = new RandomGroupsData(dataArray);
        actual = null;
        try {
            data.fillHeader(new Header());
        } catch (FitsException e) {
            actual = e;
        }
        Assert.assertNotNull(actual);
        Assert.assertTrue(actual.getMessage().toLowerCase().contains("not agree"));
        dataArray = new Object[][]{
            new Object[]{
                new int[10][10],
                new int[10],
            }
        };
        data = new RandomGroupsData(dataArray);
        actual = null;
        try {
            data.fillHeader(new Header());
        } catch (FitsException e) {
            actual = e;
        }
        Assert.assertNotNull(actual);
        Assert.assertTrue(actual.getMessage().toLowerCase().contains("not 1 d array"));
        dataArray = new Object[][]{
            new Object[]{
                new String[10],
                new String[10],
            }
        };
        data = new RandomGroupsData(dataArray);
        actual = null;
        try {
            data.fillHeader(new Header());
        } catch (FitsException e) {
            actual = e;
        }
        Assert.assertNotNull(actual);
        Assert.assertTrue(actual.getMessage().toLowerCase().contains("string"));
    }
}
