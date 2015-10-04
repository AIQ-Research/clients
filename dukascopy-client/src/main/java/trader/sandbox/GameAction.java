package trader.sandbox;

/**
 * Created by vicident on 27/09/15.
 */
public enum GameAction {
    BUY(1),
    SELL(-1),
    NOP(0);

    private final int value;
    private GameAction(int value) {
        this.value = value;
    }

    public static GameAction toAction(int value) {
        if(value > 0) {
            return GameAction.BUY;
        } else if(value < 0) {
            return GameAction.SELL;
        } else {
            return GameAction.NOP;
        }
    }
}
