package trader.sandbox;

/**
 * Created by vicident on 27/09/15.
 */
public class SimpleOrder {

    public enum OrderType {
        ASK(0), BID(1);
        private final int type;

        private OrderType(int type) {
            this.type = type;
        }

        public String toString() {
            switch (type) {
                case 0: return "ASK";
                case 1: return "BID";
                default: return "NONE";
            }
        }
    }

    private final OrderType type;
    private final double volume;
    private final String label;
    private final double stopLossPrice;
    private final double takeProfitPrice;
    private final double enterPrice;

    public SimpleOrder(OrderType type, double enterPrice, double volume, double stopLossPrice, double takeProfitPrice, String label) {
        this.type = type;
        this.volume = volume;
        this.label = label;
        this.stopLossPrice = stopLossPrice;
        this.takeProfitPrice = takeProfitPrice;
        this.enterPrice = enterPrice;
    }

    public OrderType getType() {
        return type;
    }

    public double getVolume() {
        return volume;
    }

    public String getLabel() {
        return label;
    }

    public double getStopLossPrice() {
        return stopLossPrice;
    }

    public double getTakeProfitPrice() {
        return takeProfitPrice;
    }

    public double getEnterPrice() {
        return enterPrice;
    }
}
