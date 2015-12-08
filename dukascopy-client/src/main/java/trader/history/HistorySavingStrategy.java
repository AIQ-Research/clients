package trader.history;

import com.dukascopy.api.*;
import com.dukascopy.api.util.DateUtils;
import db.CurrencyHistoryDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by vicident on 15/10/15.
 */
public class HistorySavingStrategy implements IStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(HistorySavingStrategy.class);
    private SimpleDateFormat sdf = new SimpleDateFormat(" MM/dd/yyyy HH:mm:ss,");

    // JForex controls
    private IConsole console;
    private IEngine engine;
    private IHistory history;
    private IDataService dataService;

    private final Period period;
    private final CurrencyHistoryDB db;
    private final Map<String, Boolean> updatedInstruments;
    private final Map<String, Double[]> instrumentsValues;

    private long current_time;
    private long periodsCounter;
    private long dayCounter;

    public enum TableName {
        OPEN_PRICE,
        MIN_PRICE,
        MAX_PRICE,
        CLOSE_PRICE,
        VOLUME;

        public static String[] names() {
            TableName[] states = values();
            String[] names = new String[states.length];

            for (int i = 0; i < states.length; i++) {
                names[i] = states[i].name();
            }

            return names;
        }

        public static int getOrdinalByString(String name) {
            TableName[] states = values();
            for (int i = 0; i < states.length; i++) {
                if(states[i].name().equals(name)) {
                    return states[i].ordinal();
                }
            }

            return -1;
        }
    }

    public HistorySavingStrategy(Period period, List<String> subscriptions, CurrencyHistoryDB db) {
        this.period = period;
        this.db = db;

        updatedInstruments = new HashMap<String, Boolean>();
        instrumentsValues = new HashMap<String, Double[]>();

        for (String subs: subscriptions){
            updatedInstruments.put(subs, false);
            instrumentsValues.put(subs, new Double[TableName.names().length]);
        }
    }

    private boolean _checkSubscriptions() {
        boolean updated = true;
        for(String key: updatedInstruments.keySet()) {
            updated &= updatedInstruments.get(key);
        }

        if(updated) {
            for(String key: updatedInstruments.keySet()) {
                updatedInstruments.put(key, false);
            }
        }

        return updated;
    }

    public void onStart(IContext context) throws JFException {
        engine = context.getEngine();
        console = context.getConsole();
        history = context.getHistory();
        dataService = context.getDataService();

        try {
            db.connectDB();
            for (String tableName : TableName.names()) {
                db.createTable(tableName);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        periodsCounter = 0;
        dayCounter = 0;
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {

        if (period == this.period) {
            current_time = askBar.getTime();

            if (instrumentsValues.containsKey(instrument.toString()) && !dataService.isOfflineTime(current_time)) {
                Double[] array = new Double[TableName.names().length];
                array[TableName.OPEN_PRICE.ordinal()] = (askBar.getOpen() + bidBar.getOpen()) * 0.5;
                array[TableName.MIN_PRICE.ordinal()] = (askBar.getClose() + bidBar.getClose()) * 0.5;
                array[TableName.MAX_PRICE.ordinal()] = (askBar.getHigh() + bidBar.getHigh()) * 0.5;
                array[TableName.CLOSE_PRICE.ordinal()] = (askBar.getLow() + bidBar.getLow()) * 0.5;
                array[TableName.VOLUME.ordinal()] = (askBar.getVolume() + bidBar.getVolume()) * 0.5;
                instrumentsValues.put(instrument.toString(), array);
                updatedInstruments.put(instrument.toString(), true);
            }
        }

        if (_checkSubscriptions()) {

            for (String tableName : TableName.names()) {
                int id = TableName.getOrdinalByString(tableName);
                if (id >= 0) {
                    Map<String, Double> values = new HashMap<String, Double>();
                    for (String subs : updatedInstruments.keySet()) {
                        values.put(subs, instrumentsValues.get(subs)[id]);
                    }
                    db.writeRow(tableName, current_time, values);
                } else {
                    LOGGER.error("Negative enum ordinal!");
                }
            }

            periodsCounter++;

            if ( (periodsCounter * period.getInterval()/1000) >= (24 * 3600)) {
                dayCounter++;
                LOGGER.info(sdf.format(new Date(current_time)) + String.valueOf(dayCounter) + " days have been dumped");
                periodsCounter = 0;
            }
        }
    }

    public void onMessage(IMessage message) throws JFException {
        LOGGER.info(message.getContent());
    }

    public void onAccount(IAccount account) throws JFException {
    }

    public void onStop() throws JFException {
        LOGGER.info("Stop strategy");
        try {
            db.closeDB();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }
}
