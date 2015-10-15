package trader.history;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.LoadingProgressListener;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.TesterFactory;
import db.CurrencyHistoryDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trader.TraderConfiguration;
import trader.sandbox.SimpleStrategy;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/**
 * Created by vicident on 15/10/15.
 */
public class HistorySaver {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistorySaver.class);

    public static void main(String[] args) throws Exception {

        final TraderConfiguration configuration = new TraderConfiguration(args[0]);
        configuration.subscriptions.add(configuration.instrument);

        CurrencyHistoryDB db_connector = new CurrencyHistoryDB(
                "/Users/vicident/Development/temp/fxpairs.db",
                configuration.subscriptions);

        // get the instance of the IClient interface
        final ITesterClient client = TesterFactory.getDefaultInstance();


        // set the listener that will receive system events
        client.setSystemListener(new ISystemListener() {

            public void onStart(long processId) {
                LOGGER.info("Strategy started: " + processId);
            }

            public void onStop(long processId) {
                LOGGER.info("Strategy stopped: " + processId);
                File reportFile = new File(configuration.reportPath);
                try {
                    client.createReport(processId, reportFile);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
                if (client.getStartedStrategies().size() == 0) {
                    System.exit(0);
                }
            }

            public void onConnect() {
                LOGGER.info("Connected");
            }

            public void onDisconnect() {
                // tester doesn't disconnect
            }
        });

        LOGGER.info("Connecting...");
        // connect to the server using jnlp, user name and password
        // connection is needed for data downloading
        client.connect(configuration.cURL, configuration.username, configuration.password);

        // wait for it to connect
        int i = 10; // wait max ten seconds
        while (i > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!client.isConnected()) {
            LOGGER.error("Failed to connect Dukascopy servers");
            System.exit(1);
        }

        final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date dateFrom = dateFormat.parse(configuration.training.startTime);
        Date dateTo = dateFormat.parse(configuration.training.endTime);

        client.setDataInterval(Period.valueOf(configuration.period), OfferSide.BID, ITesterClient.InterpolationMethod.FOUR_TICKS, dateFrom.getTime(),
                dateTo.getTime());

        // set instruments that will be used in testing
        Set<Instrument> subscriptions = new HashSet<Instrument>();
        for (String strInst : configuration.subscriptions) {
            LOGGER.info(strInst + ":" + Instrument.fromString(strInst));
            subscriptions.add(Instrument.fromString(strInst));
        }

        LOGGER.info("Subscribing instruments...");
        client.setSubscribedInstruments(subscriptions);

        // start the strategy
        LOGGER.info("Starting strategy");
        // now it's running

        client.startStrategy(

                new HistorySavingStrategy(
                        Period.valueOf(configuration.period),
                        configuration.subscriptions,
                        db_connector
                        ),

                new LoadingProgressListener() {

                    @SuppressWarnings("serial")
                    private SimpleDateFormat sdf = new SimpleDateFormat(" MM/dd/yyyy HH:mm:ss,") {{
                        setTimeZone(TimeZone.getTimeZone("GMT"));
                    }};
                    private boolean stop = false;

                    public void dataLoaded(long start, long end, long currentPosition, java.lang.String information)  {
                        LOGGER.info(information + sdf.format(start) + sdf.format(end) + sdf.format(currentPosition ));
                    }

                    public void loadingFinished(boolean allDataLoaded, long start, long end, long currentPosition) {
                        stop = allDataLoaded;
                    }

                    //stop loading data if it is past 18:00
                    public boolean stopJob() {
                        return stop;
                    }

                }
        );
    }
}
