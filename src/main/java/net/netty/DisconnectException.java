package net.netty;

import client.Client;

public class DisconnectException extends RuntimeException {
    private final Client client;
    private final boolean shutdown;

    public DisconnectException(Client client, boolean shutdown) {
        this.client = client;
        this.shutdown = shutdown;
    }

    public Client getClient() {
        return client;
    }

    public boolean isShutdown() {
        return shutdown;
    }
}
