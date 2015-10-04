package trader.sandbox;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.DataConversions;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vicident on 20/09/15.
 */
public class SimpleAIWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleStrategy.class);

    private final String host;
    private final int port;

    private final int NEXT_FRAME = 0;
    private final int NEW_GAME = 1;
    private final int GAME_OVER = 2;

    public SimpleAIWrapper(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public GameAction play(SimpleFrameGrabber frameGrabber, GameState gs) throws IOException {
        Socket clientSocket = new Socket(host, port);
        LittleEndianDataOutputStream outToServer = new LittleEndianDataOutputStream(clientSocket.getOutputStream());
        outToServer.writeInt(Integer.valueOf(gs.getValue()));
        outToServer.writeInt(frameGrabber.getRows());
        outToServer.writeInt(frameGrabber.getCols());
        outToServer.write(frameGrabber.getAsCByteMatrix());

        LittleEndianDataInputStream in = new LittleEndianDataInputStream(clientSocket.getInputStream());
        int action = in.readInt();
        clientSocket.close();

        return GameAction.toAction(action);
    }
}
