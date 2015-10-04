package trader;
import org.json.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vicident on 22/09/15.
 */
public class TraderConfiguration {

    /*
     *  substructure for training parameters
     */
    public class Training {
        public final boolean enable;
        public final String startTime;
        public final String endTime;

        private Training(boolean enable, String startTime, String endTime) {
            this.enable = enable;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    /*
    *  substructure for agent parameters
    */
    public class Agent {
        public final String host;
        public final int port;

        private Agent(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    /*
     *  structure fields
     */
    public final String reportPath;
    public final String cURL;
    public final String username;
    public final String password;
    public final List<String> subscriptions;
    public final String instrument;
    public final String period;
    public final int frame;
    public final int startAmount;
    public final int orderVolume;
    public final int brokerFee;
    public final int stoploss;
    public final int takeprofit;
    public final double leverage;
    public final double acceptableLossRate;

    public final Training training;
    public final Agent agent;
    /***/

    protected static String readFile(String path, Charset encoding) throws IOException {

        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public TraderConfiguration(String jsonFilePath) throws IOException {

        String content = readFile(jsonFilePath, Charset.defaultCharset());
        JSONObject jsonObject = new JSONObject(content);

        reportPath = jsonObject.getString("report_path");
        cURL = jsonObject.getString("cURL");
        username = jsonObject.getString("username");
        password = jsonObject.getString("password");
        instrument = jsonObject.getString("instrument");
        period = jsonObject.getString("period");
        frame = jsonObject.getInt("frame");
        startAmount = jsonObject.getInt("start_amount");
        orderVolume = jsonObject.getInt("order_volume");
        brokerFee = jsonObject.getInt("broker_fee");
        stoploss = jsonObject.getInt("stoploss");
        takeprofit = jsonObject.getInt("takeprofit");
        leverage = jsonObject.getDouble("leverage");
        acceptableLossRate = jsonObject.getDouble("acceptable_loss");
        subscriptions = new ArrayList<String>();

        JSONArray jsonArray = jsonObject.getJSONArray("subscriptions");
        for (int i = 0; i < jsonArray.length(); ++i) {
            subscriptions.add((String)jsonArray.get(i));
        }

        boolean training_enable = jsonObject.getJSONObject("training").getBoolean("enable");
        String training_startTime = jsonObject.getJSONObject("training").getString("start_time");
        String training_endTime = jsonObject.getJSONObject("training").getString("end_time");

        String agent_host = jsonObject.getJSONObject("agent").getString("host");
        int agent_port = jsonObject.getJSONObject("agent").getInt("port");

        training = new Training(training_enable, training_startTime, training_endTime);
        agent = new Agent(agent_host, agent_port);
    }
}
