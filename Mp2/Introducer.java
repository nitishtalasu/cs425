public enum Introducer
{
    IPADDRESS("ipaddress", "value"),
    PORT("port", "port");

    private final String key;
    private final String value;

    Introducer(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }
    public String getValue() {
        return value;
    }
}