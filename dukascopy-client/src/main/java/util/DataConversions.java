package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;

/**
 * Created by vicident on 22/09/15.
 */
public class DataConversions {

    public static final int SIZEOF_DOUBLE = 8;

    public static byte[] doubleToCByteArray(double value) {

        ByteBuffer buffer = ByteBuffer.allocate(SIZEOF_DOUBLE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(value);
        return buffer.array();
    }

    public static byte[] doubleArrayToCByteArray(Double[] array) throws IOException {

        ByteBuffer buffer = ByteBuffer.allocate(SIZEOF_DOUBLE*array.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int i=0; i<array.length; ++i) {
            buffer.putDouble(array[i]);
        }

        return buffer.array();
    }

    public static byte[] doubleCollectionToCByteArray(Collection<Double> collection) throws IOException {

        ByteBuffer buffer = ByteBuffer.allocate(SIZEOF_DOUBLE*collection.size());
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (Double element: collection) {
            buffer.putDouble(element);
        }

        return buffer.array();
    }

    public static byte[] concatCByteArrays(byte[]... arrays) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        for(byte[] array: arrays) {
            outputStream.write(array);
        }

        return outputStream.toByteArray();
    }
}
