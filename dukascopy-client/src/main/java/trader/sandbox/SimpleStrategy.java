package trader.sandbox;

import com.dukascopy.api.*;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.krb5.internal.crypto.Des;

import java.io.IOException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by vicident on 20/09/15.
 */
public class SimpleStrategy implements IStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleStrategy.class);
    private SimpleDateFormat sdf = new SimpleDateFormat(" MM/dd/yyyy HH:mm:ss,");

    // JForex controls
    private IConsole console;
    private IEngine engine;
    private IHistory history;
    private IDataService dataService;

    // Instruments
    private final Map<String, Boolean> updatedInstruments;
    private final Instrument instrument;
    private final Period period;
    private final double stopLossPips;
    private final double takeProfitPips;
    private final double acceptableLossRate;

    public final class Descriptors {
        public static final String OPEN = "_open";
        public static final String CLOSE = "_close";
        public static final String MIN = "_min";
        public static final String MAX = "_max";
        public static final String VOLUME = "_volume";
        public static final String LEVERAGE = "leverage";
        public static final String ORDERS_NUM = "orders_num";
        public static final String TIME = "time";
        public static final String SCORE = "score";
    }

    // Order manager (broker emulator)
    private final SimpleOrderManager orderManager;
    private final double orderVolume;

    // Frame grubber
    private final SimpleFrameGrabber frameGrabber;

    // AI agent
    private final SimpleAIWrapper agent;
    private GameState gameState;


    public SimpleStrategy(Instrument instrument,
                          Period period,
                          int frame,
                          double amount,
                          double orderVolume,
                          int feeRatePips,
                          int stopLossPips,
                          int takeProfitPips,
                          double leverage,
                          double acceptableLossRate,
                          List<String> subscription,
                          String agent_host,
                          int agent_port) {

        this.instrument = instrument;
        this.frameGrabber = new SimpleFrameGrabber(frame);
        this.agent = new SimpleAIWrapper(agent_host, agent_port);
        this.period = period;
        this.stopLossPips = stopLossPips;
        this.takeProfitPips = takeProfitPips;
        this.orderVolume = orderVolume;
        this.acceptableLossRate = acceptableLossRate;
        updatedInstruments = new HashMap<String, Boolean>();

        frameGrabber.putKey(instrument.toString() + Descriptors.OPEN);
        frameGrabber.putKey(instrument.toString() + Descriptors.CLOSE);
        frameGrabber.putKey(instrument.toString() + Descriptors.MIN);
        frameGrabber.putKey(instrument.toString() + Descriptors.MAX);
        frameGrabber.putKey(instrument.toString() + Descriptors.VOLUME);
        updatedInstruments.put(instrument.toString(), false);

        for (String subs: subscription){
            updatedInstruments.put(subs, false);
            frameGrabber.putKey(subs + Descriptors.OPEN);    // open price key
            frameGrabber.putKey(subs + Descriptors.CLOSE);   // close price key
            frameGrabber.putKey(subs + Descriptors.MIN);     // min price key
            frameGrabber.putKey(subs + Descriptors.MAX);     // max price key
            frameGrabber.putKey(subs + Descriptors.VOLUME);  // volume key
        }

        frameGrabber.putKey(Descriptors.ORDERS_NUM);    // orders number key
        frameGrabber.putKey(Descriptors.LEVERAGE);      // leverage key
        frameGrabber.putKey(Descriptors.TIME);          // time key
        frameGrabber.putKey(Descriptors.SCORE);         // score key

        this.orderManager = new SimpleOrderManager(amount, feeRatePips, leverage);
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
        gameState = GameState.NEXT_GAME;
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {

        // local broker
        if(instrument == this.instrument) {
            if(!isOfflineHourNear(tick.getTime())) {
                double price = (tick.getAsk() + tick.getBid()) * 0.5;
                orderManager.onTick(price);
                LOGGER.debug("Balance = " + orderManager.getBalance());
                LOGGER.debug("Equity = " + orderManager.getEquity(price));
            } else {
                orderManager.closeAllOrders();
                gameState = GameState.NEXT_GAME;
            }
        }
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {

        if(period == this.period && !isOfflineHourNear(askBar.getTime())) {

            long time = askBar.getTime();

            frameGrabber.putValue(instrument.toString() + Descriptors.OPEN, ( askBar.getOpen() + bidBar.getOpen() )*0.5);
            frameGrabber.putValue(instrument.toString() + Descriptors.CLOSE, ( askBar.getClose() + bidBar.getClose() )*0.5);
            frameGrabber.putValue(instrument.toString() + Descriptors.MIN, ( askBar.getLow() + bidBar.getLow() )*0.5);
            frameGrabber.putValue(instrument.toString() + Descriptors.MAX, ( askBar.getHigh() + bidBar.getHigh() )*0.5);
            frameGrabber.putValue(instrument.toString() + Descriptors.VOLUME, (askBar.getVolume() + bidBar.getVolume()) * 0.5);

            updatedInstruments.put(instrument.toString(), true);

            LOGGER.debug(sdf.format(time)
                            + "\t " + instrument.toString()
                            + "\t ASK CLOSE: " + String.valueOf(askBar.getClose())
                            + "\t BID CLOSE: " + String.valueOf(bidBar.getClose())
            );

            if (_checkSubscriptions()) {

                frameGrabber.putValue(Descriptors.LEVERAGE, orderManager.getUseOfLeverage());
                frameGrabber.putValue(Descriptors.ORDERS_NUM, orderManager.getOrdersNumber());
                frameGrabber.putValue(Descriptors.TIME, askBar.getTime());
                frameGrabber.putValue(Descriptors.SCORE, orderManager.getBalance());

                if (frameGrabber.isFilled()) {

                    try {
                        double closePrice = frameGrabber.getValue(this.instrument.toString() + Descriptors.CLOSE);
                        GameAction act = agent.play(frameGrabber, GameState.CONTINUE_PLAY);

                        if (act == GameAction.BUY) {
                            SimpleOrder order = new SimpleOrder(
                                    SimpleOrder.OrderType.BID,
                                    closePrice,
                                    orderVolume,
                                    closePrice - stopLossPips*instrument.getPipValue(),
                                    closePrice + takeProfitPips*instrument.getPipValue(),
                                    Long.toString(time, 10));

                            orderManager.submitOrder(order);

                        } else if(act == GameAction.SELL) {
                            SimpleOrder order = new SimpleOrder(
                                    SimpleOrder.OrderType.ASK,
                                    closePrice,
                                    orderVolume,
                                    closePrice + stopLossPips*instrument.getPipValue(),
                                    closePrice - takeProfitPips*instrument.getPipValue(),
                                    Long.toString(time, 10));

                            orderManager.submitOrder(order);
                        }

                        gameState = GameState.CONTINUE_PLAY;

                        if (orderManager.getBalance() - orderManager.getStartBalance() < -acceptableLossRate*orderManager.getStartBalance()) {
                            gameState = GameState.LOSE_GAME;
                            orderManager.reset();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected boolean isOfflineHourNear(long time) throws JFException {

        // Offline hours
        Set<ITimeDomain> nextOffline = dataService.getOfflineTimeDomains(time - Period.FIFTEEN_MINS.getInterval(), time + Period.FIFTEEN_MINS.getInterval());

        return (nextOffline.size() > 0);
    }

    public void onMessage(IMessage message) throws JFException {

    }

    public void onAccount(IAccount account) throws JFException {

    }

    public void onStop() throws JFException {
        LOGGER.info("Stop strategy");
    }
}
