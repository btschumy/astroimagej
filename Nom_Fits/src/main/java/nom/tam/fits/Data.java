package nom.tam.fits;

import nom.tam.fits.utilities.FitsCheckSum;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.RandomAccess;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static nom.tam.util.LoggerHelper.getLogger;

/**
 * This class provides methods to access the data segment of an HDU.
 * <p>
 * This is the object which contains the actual data for the HDU.
 * </p>
 * <ul>
 * <li>For images and primary data this is a simple (but possibly
 * multi-dimensional) primitive array. When group data is supported it will be a
 * possibly multidimensional array of group objects.
 * <li>For ASCII data it is a two dimensional Object array where each of the
 * constituent objects is a primitive array of length 1.
 * <li>For Binary data it is a two dimensional Object array where each of the
 * constituent objects is a primitive array of arbitrary (more or less)
 * dimensionality.
 * </ul>
 */
public abstract class Data implements FitsElement {

    private static final Logger LOG = getLogger(Data.class);

    private static final int FITS_BLOCK_SIZE_MINUS_ONE = FitsFactory.FITS_BLOCK_SIZE - 1;

    /**
     * The starting location of the data when last read
     */
    protected long fileOffset = -1;

    /**
     * The size of the data when last read
     */
    protected long dataSize;

    /**
     * The input stream used.
     */
    protected RandomAccess input;

    /**
     * Modify a header to point to this data, this differs per subclass, they
     * all need oder provided different informations to the header. Basically
     * they describe the structure of this data object.
     * 
     * @param head
     *            header to fill with the data from the current data object
     * @throws FitsException
     *             if the operation fails
     */
    abstract void fillHeader(Header head) throws FitsException;

    /**
     * Checks if the data should be assumed to be in deferred read mode. The default
     * implementation is to return <code>false</code>, but concrete subclasses should override
     * this as appropriate.
     * 
     * @return      <code>true</code> if it is set for deferred reading at a later time, or else
     *              <code>false</code> if this data is currently loaded into RAM. 
     * 
     * @since 1.17  
     */
    public boolean isDeferred() {
        return false;
    }

    /**
     * Computes and returns the FITS checksum for this data, e.g. to compare agains the 
     * stored <code>DATASUM</code> in the FITS header. This method always computes the
     * checksum from data in into memory. As such it will fully load deferred read mode data 
     * into RAM to perform the calculation. If you prefer to leave the data in deferred read 
     * mode, you can use {@link FitsCheckSum#checksum(RandomAccess, long, long)} instead 
     * directly on the input with this data's {@link #getFileOffset()} and {@link #getSize()} 
     * arguments; or equivalently use {@link Fits#calcDatasum(int)}.
     * 
     * @return      the computed FITS checksum from the data (fully loaded in memory).
     * @throws FitsException    if there was an error while calculating the checksum
     * 
     * @see Fits#calcDatasum(int)
     * @see FitsCheckSum#checksum(RandomAccess, long, long)
     * @see FitsCheckSum#checksum(Data)
     * 
     * @since 1.17
     */
    public long calcChecksum() throws FitsException {
        return FitsCheckSum.checksum(this);
    }
    
    /**
     * @return the data array object.
     * @throws FitsException
     *             if the data could not be gathered .
     */
    public abstract Object getData() throws FitsException;

    /**
     * @return the file offset
     */
    @Override
    public long getFileOffset() {
        return this.fileOffset;
    }

    /**
     * @return the non-FITS data object.
     * @throws FitsException
     *             if the data could not be gathered .
     */
    public Object getKernel() throws FitsException {
        return getData();
    }

    /**
     * @return the size of the data element in bytes.
     */
    @Override
    public long getSize() {
        return FitsUtil.addPadding(getTrueSize());
    }

    abstract long getTrueSize();

    @Override
    public abstract void read(ArrayDataInput in) throws FitsException;
    
    @Override
    public boolean reset() {
        try {
            FitsUtil.reposition(this.input, this.fileOffset);
            return true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unable to reset", e);
            return false;
        }
    }

    @Override
    public void rewrite() throws FitsException {
        if (isDeferred()) {
            return;             // Nothing to do...
        }
        
        if (!rewriteable()) {
            throw new FitsException("Illegal attempt to rewrite data");
        }
        
        FitsUtil.reposition(this.input, this.fileOffset);
        write((ArrayDataOutput) this.input);
        try {
            ((ArrayDataOutput) this.input).flush();
        } catch (IOException e) {
            throw new FitsException("Error in rewrite flush: ", e);
        }
    }

    @Override
    public boolean rewriteable() {
        return this.input != null && this.fileOffset >= 0
                && (getTrueSize() + FITS_BLOCK_SIZE_MINUS_ONE) / FitsFactory.FITS_BLOCK_SIZE == (this.dataSize + FITS_BLOCK_SIZE_MINUS_ONE) / FitsFactory.FITS_BLOCK_SIZE;
    }

    /**
     * Set the fields needed for a re-read.
     * 
     * @param o
     *            reread information.
     */
    protected void setFileOffset(ArrayDataInput o) {
        if (o instanceof RandomAccess) {
            this.fileOffset = FitsUtil.findOffset(o);
            this.dataSize = getTrueSize();
            this.input = (RandomAccess) o;
        }
    }

    /**
     * Write the data -- including any buffering needed
     * 
     * @param o
     *            The output stream on which to write the data.
     */
    @Override
    public abstract void write(ArrayDataOutput o) throws FitsException;
}
