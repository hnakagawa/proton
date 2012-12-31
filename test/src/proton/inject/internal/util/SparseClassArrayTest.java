package proton.inject.internal.util;

import android.test.AndroidTestCase;

public class SparseClassArrayTest extends AndroidTestCase {
    public void testPutAndGet() {
        SparseClassArray<String> arr = new SparseClassArray<String>();
        arr.put(String.class, "aa");
        assertEquals("aa", arr.get(String.class));
        assertNotSame("aa", arr.get(Object.class));
    }
}
