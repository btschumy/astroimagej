package nom.tam.image.compression.hdu;

import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.util.BlackBoxImages;
import nom.tam.util.Cursor;
import nom.tam.util.SafeClose;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;

public class CompressedTableBlackBoxTest {

    @Test
    public void testUncompress_mddtsapcln() throws Exception {
        uncompressTableAndAssert("bintable/mddtsapcln.fits.fz", "bintable/mddtsapcln.fits");
    }

    private void compressThenUncompressTableAndAssert(String originalFileName) throws FitsException, IOException {
        String tableOrgFile = BlackBoxImages.getBlackBoxImage(originalFileName);
        Fits fitsOrg = null;
        String compressedfileName;
        try {
            fitsOrg = new Fits(tableOrgFile);
            fitsOrg.readHDU(); // skip image

            File file = new File("target/" + originalFileName + ".fz");
            if (file.exists()) {
                file.delete();
            }
            file.getParentFile().mkdirs();
            Fits fitsCompressed = new Fits();
            fitsCompressed.addHDU(//
                    CompressedTableHDU.fromBinaryTableHDU((BinaryTableHDU) fitsOrg.readHDU(), 0)//
                            .compress());
            fitsCompressed.write(file);
            compressedfileName = file.getAbsolutePath();
        } finally {
            SafeClose.close(fitsOrg);
        }

        uncompressTableAndAssert(compressedfileName, originalFileName);
    }

    private void uncompressTableAndAssert(String compressedfileName, String originalFileName) throws FitsException, IOException {
        String tableFile;
        if (new File(compressedfileName).exists()) {
            tableFile = compressedfileName;
        } else {
            tableFile = BlackBoxImages.getBlackBoxImage(compressedfileName);
        }
        Fits fitsComp = null;
        String tableOrgFile = BlackBoxImages.getBlackBoxImage(originalFileName);
        Fits fitsOrg = null;
        try {
            fitsComp = new Fits(tableFile);
            fitsComp.readHDU(); // skip image
            CompressedTableHDU compressedTable = (CompressedTableHDU) fitsComp.readHDU();
            BinaryTableHDU uncompressedTable = compressedTable.asBinaryTableHDU();

            fitsOrg = new Fits(tableOrgFile);
            fitsOrg.readHDU(); // skip image
            BinaryTableHDU orgTable = compressedTable.asBinaryTableHDU();

            assertEquals(orgTable, uncompressedTable);
        } finally {
            SafeClose.close(fitsComp);
            SafeClose.close(fitsOrg);
        }
    }

    private void assertEquals(BinaryTableHDU orgTable, BinaryTableHDU testTable) throws FitsException {
        int numberOfCards = orgTable.getHeader().getNumberOfCards();
        Assert.assertEquals(//
                numberOfCards, //
                testTable.getHeader().getNumberOfCards());
        Cursor<String, HeaderCard> orgIterator = orgTable.getHeader().iterator();
        for (int index = 0; index < numberOfCards; index++) {
            HeaderCard orgCard = orgIterator.next();
            HeaderCard testCard = testTable.getHeader().findCard(orgCard.getKey());
            Assert.assertEquals(orgCard.getValue(), testCard.getValue());
        }
        for (int column = 0; column < orgTable.getNCols(); column++) {
            for (int row = 0; row < orgTable.getNRows(); row++) {
                Object orgValue = orgTable.getElement(row, column);
                Object testValue = testTable.getElement(row, column);
                assertValues(orgValue, testValue);
            }
        }
    }

    private void assertValues(Object orgValue, Object testValue) {
        if (orgValue.getClass().isArray()) {
            int arraySize = Array.getLength(orgValue);
            for (int arrayIndex = 0; arrayIndex < arraySize; arrayIndex++) {
                Object orgValueElement = Array.get(orgValue, arrayIndex);
                Object testValueElement = Array.get(testValue, arrayIndex);
                assertValues(orgValueElement, testValueElement);
            }
        } else {
            Assert.assertEquals(orgValue, testValue);
        }
    }

    @Test
    public void testUncompress_swp06542llg() throws FitsException, IOException {
        uncompressTableAndAssert("bintable/swp06542llg.fits.fz", "bintable/swp06542llg.fits");
    }

    @Test
    public void testUncompress_testdata() throws FitsException, IOException {
        uncompressTableAndAssert("bintable/testdata.fits.fz", "bintable/testdata.fits");
    }

    @Test
    @Ignore // TODO also cfitsio can not uncompress this, mail to bill 22.7.2016
    public void testUncompress_tst0010() throws FitsException, IOException {
        uncompressTableAndAssert("bintable/tst0010.fits.fz", "bintable/tst0010.fits");
    }

    @Test
    @Ignore // TODO also cfitsio can not uncompress this, mail to bill 22.7.2016
    public void testUncompress_tst0012() throws FitsException, IOException {
        uncompressTableAndAssert("bintable/tst0012.fits.fz", "bintable/tst0012.fits");
    }

    @Test
    public void testUncompress_tst0014() throws FitsException, IOException {
        uncompressTableAndAssert("bintable/tst0014.fits.fz", "bintable/tst0014.fits");
    }

    @Test
    public void testCompressAndUncompress_dddtsuvdata() {
    }

    @Test
    public void testCompressAndUncompress_mddtsapcln() throws FitsException, IOException {
        compressThenUncompressTableAndAssert("bintable/mddtsapcln.fits");
    }

    @Test
    public void testCompressAndUncompress_swp06542llg() throws FitsException, IOException {
        compressThenUncompressTableAndAssert("bintable/swp06542llg.fits");
    }

    @Test
    public void testCompressAndUncompress_testdata() throws FitsException, IOException {
        compressThenUncompressTableAndAssert("bintable/testdata.fits");
    }

    @Test
    public void testCompressAndUncompress_tst0010() throws FitsException, IOException {
        compressThenUncompressTableAndAssert("bintable/tst0010.fits");
    }

    @Test
    public void testCompressAndUncompress_tst0012() throws FitsException, IOException {
        compressThenUncompressTableAndAssert("bintable/tst0012.fits");
    }

    @Test
    public void testCompressAndUncompress_tst0014() throws FitsException, IOException {
        compressThenUncompressTableAndAssert("bintable/tst0014.fits");
    }
}
