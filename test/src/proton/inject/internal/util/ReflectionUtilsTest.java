package proton.inject.internal.util;

import java.lang.reflect.Field;

import javax.inject.Provider;

import android.test.AndroidTestCase;

public class ReflectionUtilsTest extends AndroidTestCase {

    public void testToActualClass() throws Exception {
        Client c = new Client();
        Field field = c.getClass().getDeclaredField("provider");
        Class<?> clazz = ReflectionUtils.toActualClass(field.getGenericType());
        assertEquals(String.class, clazz);
    }

    public void testIsAbstract() {
        assertTrue(ReflectionUtils.isAbstract(Aaa.class));
        assertTrue(ReflectionUtils.isAbstract(Bbb.class));
        assertFalse(ReflectionUtils.isAbstract(Ccc.class));
    }
    
    public interface Aaa {        
    }
    public abstract class Bbb {
    }
    public static class Ccc {        
    }
    
    public static class Client {
        @SuppressWarnings("unused")
        private Provider<String> provider;
    }
}
