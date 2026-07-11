package server.responses;

public class SelectionListStringResult
{
    private final boolean result;
    private final String message;

    public SelectionListStringResult(boolean result, String message) {
        this.result = result;
        this.message = message;
    }

    public boolean getResult() { return result; }
    public String getMessage() { return message; }
}
