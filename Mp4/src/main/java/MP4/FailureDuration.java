package MP4;

/**
 * Enum for fail time and cleanup time values.
 */
public enum FailureDuration
{
    // time in milliseconds
    FAIL("FAIL", 3000),
    EXIT("EXIT", 6000);

    private final String key;
    private final Integer value;

    FailureDuration(String key, Integer value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }
    public Integer getValue() {
        return value;
    }
}