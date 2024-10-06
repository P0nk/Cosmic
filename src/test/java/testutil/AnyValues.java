package testutil;

import database.DatabaseException;

public class AnyValues {

    public static String string() {
        return "string";
    }

    public static int integer() {
        return 17;
    }

    public static short anyShort() {
        return 4;
    }

    public static DatabaseException dbException() {
        return new DatabaseException(string(), new RuntimeException());
    }
}
