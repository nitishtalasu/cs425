package MP3;

/**
 * Enum for setting ports
 */
public enum Ports
{

    TCPPort("TCPPort", "5000"),
    UDPPort("UDPPort", "5500"),
    HEARTBEAT("HEARTBEAT", "5500");

    private final String key;
    private final String value;

    Ports(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }
    public int getValue() {
        return Integer.parseInt(value);
    }
}
