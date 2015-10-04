package trader.sandbox;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import util.DataConversions;

import java.io.IOException;
import java.util.*;

/**
 * Created by vicident on 22/09/15.
 */
public class SimpleFrameGrabber {

    private final Map<String, CircularFifoQueue<Double>> dataWindow = new HashMap<String, CircularFifoQueue<Double>>();
    private final List<String> keysList = new ArrayList<String>();
    private final int size;

    public SimpleFrameGrabber(int size) {
        this.size = size;
    }

    public void putKey(String key) {

        if(!keysList.contains(key)) {
            keysList.add(key);
        }

        dataWindow.put(key, new CircularFifoQueue<Double>(size));
    }

    public void putKeys(Set<String> keys) {
        for (String key: keys) {
            putKey(key);
        }
    }

    public void putValue(String key, double value) {
        if(keysList.contains(key)) {
            dataWindow.get(key).add(value);
        }
    }

    public double getValue(String key) throws Exception {
        if(keysList.contains(key)) {
            return dataWindow.get(key).get(dataWindow.get(key).size() - 1);
        } else {
            throw new Exception("has no key");
        }
    }

    public boolean isFilled() {
        boolean filled = true;
        for(String key : dataWindow.keySet()) {
                filled &= (dataWindow.get(key).size() == size);
        }

        return filled;
    }

    public byte[] getAsCByteMatrix() throws IOException {
        byte[] pool = new byte[0];

        for (String key: keysList) {
            byte[] current = DataConversions.doubleCollectionToCByteArray(dataWindow.get(key));
            pool = DataConversions.concatCByteArrays(pool, current);
        }

        return pool;
    }

    public int getRows() {
        return keysList.size();
    }

    public int getCols() {
        return size;
    }
}
