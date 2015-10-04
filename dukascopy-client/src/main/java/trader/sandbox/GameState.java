package trader.sandbox;

/**
 * Created by vicident on 27/09/15.
 */
public enum GameState {
    CONTINUE_PLAY(0),
    NEXT_GAME(1),
    LOSE_GAME(-1);

    private final int value;
    private GameState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
