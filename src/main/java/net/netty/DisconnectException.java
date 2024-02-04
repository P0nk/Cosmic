package net.netty;

import client.Client;

public class DisconnectException extends RuntimeException {
    private final Client client;

    public DisconnectException(Client client, String message) {
        super(message);
        this.client = client;
    }

    public Client getClient() {
        return client;
    }
}
