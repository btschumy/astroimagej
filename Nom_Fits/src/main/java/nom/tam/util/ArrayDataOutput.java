package nom.tam.util;

import java.io.Closeable;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Interface for writing array data to outputs.
 */
public interface ArrayDataOutput extends DataOutput, Closeable {
    
    /**
     * Flush the output buffer
     * 
     * @throws IOException
     *             if the flush of the underlying stream failed
     */
    void flush() throws IOException;

    /**
     * Write an array of boolean's.
     * 
     * @param buf
     *            array of boolean's.
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    default void write(boolean[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    /**
     * Write a segment of an array of boolean's.
     * 
     * @param buf
     *            array of boolean's.
     * @param offset
     *            start index in the array
     * @param size
     *            number of array elements to write
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    void write(boolean[] buf, int offset, int size) throws IOException;

    /**
     * Write an array of booleans, including legal <code>null</code> values.
     * 
     * @param buf
     *            array of booleans.
     * @throws IOException
     *             if one of the underlying write operations failed
     *             
     * @since 1.16
     */
    default void write(Boolean[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    /**
     * Write a segment of an array of booleans, possibly including legal
     * <code>null</code> values.
     * The method has a default implementation, which calls {@link #writeBoolean(boolean)}
     * element by element. Classes that implement this interface might want to
     * replace that with a more efficient block read implementation/
     * 
     * @param buf
     *            array of booleans.
     * @param offset
     *            start index in the array
     * @param size
     *            number of array elements to write
     * @throws IOException
     *             if one of the underlying write operations failed
     *             
     * @since 1.16
     */
    default void write(Boolean[] buf, int offset, int size) throws IOException {
       int to = offset + size;

       for (int i = offset; i < to; i++) {
           writeBoolean(buf[i].booleanValue());
       }
    }

    /**
     * Write an array of char's.
     * 
     * @param buf
     *            array of char's.
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    default void write(char[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    /**
     * Write a segment of an array of char's.
     * 
     * @param buf
     *            array of char's.
     * @param offset
     *            start index in the array
     * @param size
     *            number of array elements to write
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    void write(char[] buf, int offset, int size) throws IOException;

    /**
     * Write an array of double's.
     * 
     * @param buf
     *            array of double's.
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    default void write(double[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    /**
     * Write a segment of an array of double's.
     * 
     * @param buf
     *            array of double's.
     * @param offset
     *            start index in the array
     * @param size
     *            number of array elements to write
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    void write(double[] buf, int offset, int size) throws IOException;

    /**
     * Write an array of float's.
     * 
     * @param buf
     *            array of float's.
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    default void write(float[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    /**
     * Write a segment of an array of float's.
     * 
     * @param buf
     *            array of float's.
     * @param offset
     *            start index in the array
     * @param size
     *            number of array elements to write
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    void write(float[] buf, int offset, int size) throws IOException;

    /**
     * Write an array of int's.
     * 
     * @param buf
     *            array of int's
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    default void write(int[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    /**
     * Write a segment of an array of int's.
     * 
     * @param buf
     *            array of int's
     * @param offset
     *            start index in the array
     * @param size
     *            number of array elements to write
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    void write(int[] buf, int offset, int size) throws IOException;

    /**
     * Write an array of longs.
     * 
     * @param buf
     *            array of longs
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    default void write(long[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    /**
     * Write a segment of an array of longs.
     * 
     * @param buf
     *            array of longs
     * @param offset
     *            start index in the array
     * @param size
     *            number of array elements to write
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    void write(long[] buf, int offset, int size) throws IOException;

    /**
     * Write an array of shorts.
     * 
     * @param buf
     *            the value to write
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    default void write(short[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    /**
     * Write a segment of an array of shorts.
     * 
     * @param buf
     *            the value to write
     * @param offset
     *            start index in the array
     * @param size
     *            number of array elements to write
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    void write(short[] buf, int offset, int size) throws IOException;

    /**
     * Write an array of Strings. Equivalent to calling writeBytes for the
     * selected elements.
     * 
     * @param buf
     *            the array to write
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    default void write(String[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    /**
     * Write a segment of an array of Strings. Equivalent to calling writeBytes
     * for the selected elements.
     * 
     * @param buf
     *            the array to write
     * @param offset
     *            start index in the array
     * @param size
     *            number of array elements to write
     * @throws IOException
     *             if one of the underlying write operations failed
     */
    void write(String[] buf, int offset, int size) throws IOException;

    /**
     * Writes the contents of a Java array to the output translating the data to
     * the required binary representation. The argument may be a generic Java
     * array, including multi-dimensional arrays and heterogeneous arrays of
     * arrays.
     * 
     * @param o
     *            the Java array, including heterogeneous arrays of arrays. If
     *            <code>null</code> nothing will be written to the output.
     * @throws IOException
     *             if there was an IO error writing to the output
     * @throws IllegalArgumentException
     *             if the supplied object is not a Java array or if it contains
     *             Java types that are not supported by the encoder.
     */
    void writeArray(Object o) throws IOException, IllegalArgumentException;

}
