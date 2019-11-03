package MP3;

/**
 * Enum for message types.
 */
public enum MessageType
{
    JOIN,
    HEARTBEAT,
    LEAVE,
    ELECTION,
    VICTORY,
    COORDINATION,
    GET,
    PUT,
    DELETE,
    LIST,
    REPLICALIST,
    REREPLICATE,
    PUT_SUCCESS,
    DELETE_SUCCESS,
    FILEELAPSED
}