

package nom.tam.util;

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

import nom.tam.fits.FitsFactory;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.*;

public class FitsDecoderTest {

    @After
    public void setDefaults() {
        FitsFactory.setDefaults();
    }
    
    @Test
    public void testIncomleteReadByteArray() throws Exception {
        byte[] data = new byte[100];
        FitsDecoder e = new FitsDecoder(InputReader.from(new ByteArrayInputStream(data)));
        byte[] need = new byte[101];
        assertEquals(100, e.read(need, 0, need.length));
    }

    @Test
    public void testIncompleteReadBooleanObjectArray() throws Exception {
        byte[] data = new byte[100];
        FitsDecoder e = new FitsDecoder(InputReader.from(new ByteArrayInputStream(data)));
        Boolean[] b = new Boolean[data.length + 1];
        assertEquals(data.length, e.readArray(b));
    }
    
    @Test
    public void testIncompleteReadObjectArray() throws Exception {
        byte[] data = new byte[100];
        FitsDecoder e = new FitsDecoder(InputReader.from(new ByteArrayInputStream(data)));
        Boolean[] b = new Boolean[1];
        Object[] array = new Object[] { data, b };
        assertEquals(data.length, e.readArray(array));
    }
    
    
    
    @Test(expected = EOFException.class)
    public void testIncomleteReadFully() throws Exception {
        byte[] data = new byte[100];
        FitsDecoder e = new FitsDecoder(InputReader.from(new ByteArrayInputStream(data)));
        byte[] need = new byte[101];
        e.readFully(need, 0, need.length);
    }
    
    @Test(expected = EOFException.class)
    public void testIncomleteReadArrayFully() throws Exception {
        byte[] data = new byte[100];
        FitsDecoder e = new FitsDecoder(InputReader.from(new ByteArrayInputStream(data)));
        int[] need = new int[26];
        e.readArrayFully(need);
    }
    
    
    @Test
    public void testReadNullArray() throws Exception {
        byte[] data = new byte[100];
        FitsDecoder e = new FitsDecoder(InputReader.from(new ByteArrayInputStream(data)));
        e.readArray(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testReadInvalidArray() throws Exception {
        byte[] data = new byte[100];
        FitsDecoder e = new FitsDecoder(InputReader.from(new ByteArrayInputStream(data)));
        Object[] array = new Object[] { new BigInteger("123235536566547747") };
        e.readArray(array);
    }
    
    @Test
    public void testByteOrder() throws Exception {   
        byte[] data = new byte[100];
       
        ByteBuffer b = ByteBuffer.wrap(data);
        b.putDouble(Math.PI);
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putDouble(Math.PI);

        double[] d = new double[2];
        FitsDecoder e = new FitsDecoder(InputReader.from(new ByteArrayInputStream(data)));
        e.readArray(d);
        
        assertEquals("BE", Math.PI, d[0], 1e-12);
        assertNotEquals("!BE", Math.PI, d[1], 1e-12);
        
        e = new FitsDecoder(InputReader.from(new ByteArrayInputStream(data)));
        e.getInputBuffer().setByteOrder(ByteOrder.LITTLE_ENDIAN);
        e.read(d, 0, d.length);
        
        assertEquals("byteorder", ByteOrder.LITTLE_ENDIAN, e.getInputBuffer().byteOrder());
        assertNotEquals("!LE", Math.PI, d[0], 1e-12);
        assertEquals("LE", Math.PI, d[1], 1e-12);
    }
    
    @Test
    public void testBoolean() throws Exception {
        byte[] data = new byte[] { 'T', 'F', 0, 1, 2};
        FitsDecoder e = new FitsDecoder(InputReader.from(new ByteArrayInputStream(data)));
        Boolean[] b = new Boolean[data.length];
        e.read(b, 0, b.length);
      
        assertTrue("T", b[0]);
        assertFalse("F", b[1]);
        assertNull("0", b[2]);
        assertTrue("1", b[3]);      // alternative non-standard 'true' 
        assertFalse("2", b[4]);     // everything else 'false'
    }
   
    @Test(expected = EOFException.class)
    public void testReadByteEOF() throws Exception {
        byte[] data = new byte[100];
        FitsDecoder e = new FitsDecoder(InputReader.from(new ByteArrayInputStream(data)));
        for(int i=0; i<data.length; i++) {
            assertEquals(0, e.readByte());
        }
        e.readByte(); // should throw exception.
    }
    
    
    @Test(expected = EOFException.class)
    public void testReadShortEOF() throws Exception {
        byte[] data = new byte[100];
        FitsDecoder e = new FitsDecoder(InputReader.from(new ByteArrayInputStream(data)));
        int n = data.length >>> 1;
        for(int i=0; i<n; i++) {
            assertEquals(0, e.readShort());
        }
        e.readShort(); // should throw exception.
   
    }
    
    @Test(expected = EOFException.class)
    public void testReadCharEOF() throws Exception {
        FitsFactory.setUseUnicodeChars(false);
        byte[] data = new byte[100];
        FitsDecoder e = new FitsDecoder(InputReader.from(new ByteArrayInputStream(data)));
        int n = data.length;
        for(int i=0; i<n; i++) {
            assertEquals(0, e.readChar());
        }
        e.readChar(); // should throw exception.
    }
    
    @Test(expected = EOFException.class)
    public void testBooleanArrayUnexpectedReadEOF1() throws Exception {
        FitsDecoder e = new FitsDecoder(new InputReader() {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new EOFException();
            }

            @Override
            public int read() throws IOException {
                throw new EOFException();
            }
        });
        
        e.read(new boolean[1], 0, 1);
    }
    
    @Test(expected = EOFException.class)
    public void testBooleanArrayUnexpectedReadEOF2() throws Exception {
        FitsDecoder e = new FitsDecoder(new InputReader() {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new EOFException();
            }

            @Override
            public int read() throws IOException {
                throw new EOFException();
            }
        });
        
        e.read(new Boolean[1], 0, 1);
    }
    
    @Test(expected = EOFException.class)
    public void testShortArrayUnexpectedReadEOF() throws Exception {
        FitsDecoder e = new FitsDecoder(new InputReader() {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new EOFException();
            }

            @Override
            public int read() throws IOException {
                throw new EOFException();
            }
        });
        
        e.read(new short[1], 0, 1);
    }
    
    @Test(expected = EOFException.class)
    public void testCharArrayUnexpectedReadEOF() throws Exception {
        FitsDecoder e = new FitsDecoder(new InputReader() {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new EOFException();
            }

            @Override
            public int read() throws IOException {
                throw new EOFException();
            }
        });
        
        e.read(new char[1], 0, 1);
    }
    
  
    
    
}
