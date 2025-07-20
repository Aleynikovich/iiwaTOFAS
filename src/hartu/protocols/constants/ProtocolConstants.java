package hartu.protocols.constants;

public class ProtocolConstants
{

    public static final String INITIAL_TASK_CLIENT_RESPONSE = "FREE|0#";

    public static final String MESSAGE_TERMINATOR = "#";

    public static final String PRIMARY_DELIMITER = "\\|";

    public static final String SECONDARY_DELIMITER = ";";

    public static final String MULTI_POINT_DELIMITER = ",";

    public enum ListenerType
    { // Renamed from ClientType
        TASK_LISTENER("Task Listener", 0), LOG_LISTENER("Log Listener", 1);

        private final String name;
        private final int value;

        ListenerType(String name, int value)
        {
            this.name = name;
            this.value = value;
        }

        public static ListenerType fromString(String listenerTypeName)
        { // Method name updated
            for (ListenerType type : ListenerType.values())
            {
                if (type.name.equals(listenerTypeName))
                {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown listener type name: " + listenerTypeName);
        }

        public static ListenerType fromValue(int listenerTypeValue)
        { // Method name updated
            for (ListenerType type : ListenerType.values())
            {
                if (type.value == listenerTypeValue)
                {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown listener type value: " + listenerTypeValue);
        }

        public String getName()
        {
            return name;
        }

        public int getValue()
        {
            return value;
        }
    }
}
