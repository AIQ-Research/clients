package trader.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by vicident on 27/09/15.
 */
public class SimpleOrderManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleOrderManager.class);

    private final List<SimpleOrder> ordersList = new CopyOnWriteArrayList<SimpleOrder>();
    private final Map<String, SimpleOrder> closeOrders = new HashMap<String, SimpleOrder>();
    private double balance;
    private final double fee_rate;
    private final double leverage;
    private final double start_balance;
    private final double LEVERAGE_WARNING_THRESHOLD = 0.9;
    private double price;

    public SimpleOrderManager(double start_balance, double fee_rate, double leverage){
        this.balance = start_balance;
        this.fee_rate = fee_rate;
        this.leverage = leverage;
        this.start_balance = start_balance;
    }

    public void reset() {
        this.closeOrders.clear();
        this.ordersList.clear();
        this.balance = this.start_balance;
    }

    synchronized protected void setPrice(double price) {
        this.price = price;
    }

    synchronized protected double getPrice() {
        return price;
    }

    public void submitOrder(SimpleOrder order) {

        double volume = order.getVolume();

        LOGGER.debug("Use of leverage: " + getUseOfLeverage());
        LOGGER.debug("Used margin: " + getOrdersVolume());

        if(canUseLeverage(volume)) {
            ordersList.add(order);
            LOGGER.debug("Order " + order.getLabel() + ": " +
                    "price=" + order.getEnterPrice() +
                    ", volume=" + order.getVolume() +
                    ", type=" + order.getType().toString() + " - SUBMITTED");
        } else {
            LOGGER.debug("Order " + order.getLabel() + " REJECTED");
        }
    }

    public double getUseOfLeverage() {
        return (getOrdersVolume() / leverage) / getEquity(price);
    }

    private boolean canUseLeverage(double volume) {
        return (( (getOrdersVolume() + volume) / leverage) / getEquity(getPrice()) ) < LEVERAGE_WARNING_THRESHOLD;
    }

    public double onTick(double price) {

        setPrice(price);

        // Check margin call
        if (getUseOfLeverage() >= 1.) {
            LOGGER.info("MARGIN CALL ;-P");
            closeAllOrders();
        }

        // Check stoploss & takeprofit and close orders
        for(SimpleOrder order: ordersList) {
            boolean closeFlag = false;
            if(order.getType() == SimpleOrder.OrderType.BID) {
                if (order.getStopLossPrice() >= price
                        || order.getTakeProfitPrice() <= price
                        || closeOrders.containsKey(order.getLabel())) {

                    balance += (price - order.getEnterPrice()) * order.getVolume() * (1. - fee_rate);
                    closeFlag = true;
                }
            } else if (order.getType() == SimpleOrder.OrderType.ASK) {
                if (order.getStopLossPrice() <= price
                        || order.getTakeProfitPrice() >= price
                        || closeOrders.containsKey(order.getLabel())) {

                    balance += (order.getEnterPrice() - price) * order.getVolume() * (1. - fee_rate);
                    closeFlag = true;
                }
            }

            if(closeFlag) {
                closeOrders.remove(order.getLabel());
                ordersList.remove(order);
                LOGGER.debug("Order " + order.getLabel() + " CLOSED");
            }
        }

        return balance;
    }

    public double getEquity(double price) {
        double equity = balance;
        for(SimpleOrder order: ordersList) {
            if(order.getType() == SimpleOrder.OrderType.BID) {
                equity += (price - order.getEnterPrice()) * order.getVolume() * (1. - fee_rate);
            } else if (order.getType() == SimpleOrder.OrderType.ASK) {
                equity += (order.getEnterPrice() - price) * order.getVolume() * (1. - fee_rate);
            }
        }

        return equity;
    }

    public double getBalance() {
        return balance;
    }

    public double getStartBalance() {
        return start_balance;
    }

    public double getOrdersVolume() {
        double volume = 0.;

        for(SimpleOrder order: ordersList) {
           volume += order.getVolume();
        }

        return volume;
    }

    public int getOrdersNumber() {
        return ordersList.size();
    }

    public void closeOrder(SimpleOrder order) {
        closeOrders.put(order.getLabel(), order);
    }

    public void closeAllOrders() {
        for(SimpleOrder order: ordersList) {
            closeOrders.put(order.getLabel(), order);
        }
    }
}
