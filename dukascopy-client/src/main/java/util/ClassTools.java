package util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * Created by vicident on 09/09/15.
 */
public class ClassTools {

    public static String getClassName(Object object) {

        String className;
        Class<?> enclosingClass = object.getClass().getEnclosingClass();
        if (enclosingClass != null) {
            className = enclosingClass.getName();
        } else {
            className = object.getClass().getName();
        }
        return className;
    }

    public static Object createInstanceByClassName(String className) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<?> clazz = Class.forName(className);
        Constructor<?> ctor = clazz.getConstructor(String.class);
        return ctor.newInstance();
    }
}
