package enums;

public enum PlayerSkin {
    CIRCLE("Circle"),
    RECTANGLE("Rectangle"),
    TRIANGLE("Triangle"),
    DEMON("Demon"),
    DONUT("Donut");

    private final String displayName;

    PlayerSkin(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
