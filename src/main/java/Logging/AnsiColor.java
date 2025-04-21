package Logging;

public enum AnsiColor {
    BLACK(30),
    RED(31),
    GREEN(32),
    YELLOW(33),
    BLUE(34),
    MAGENTA(35),
    CYAN(36),
    WHITE(37),
    DEFAULT(39);
    private final int code;

    AnsiColor(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}