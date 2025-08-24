package net.server.coordinator.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IpAddressesTest {

    @Test
    public void testLocalAddress() {
        assertTrue(IpAddresses.isLocalAddress("127.0.0.1"));
        assertTrue(IpAddresses.isLocalAddress("127.255.255.255"));
        assertFalse(IpAddresses.isLocalAddress("192.168.0.1"));
        assertFalse(IpAddresses.isLocalAddress("8.127.127.127"));
        assertFalse(IpAddresses.isLocalAddress("8.8.8.8"));
    }

    @Test
    public void testLanAddress() {
        assertTrue(IpAddresses.isLanAddress("10.0.0.1"));
        assertTrue(IpAddresses.isLanAddress("10.255.255.255"));
        assertFalse(IpAddresses.isLanAddress("11.10.10.10"));
        assertFalse(IpAddresses.isLanAddress("123.123.123.10"));

        assertTrue(IpAddresses.isLanAddress("192.168.1.1"));
        assertFalse(IpAddresses.isLanAddress("1.1.192.168"));

        assertTrue(IpAddresses.isLanAddress("172.16.0.1"));
        assertTrue(IpAddresses.isLanAddress("172.31.255.255"));
        assertFalse(IpAddresses.isLanAddress("172.15.0.1"));
        assertFalse(IpAddresses.isLanAddress("172.32.0.1"));
        assertFalse(IpAddresses.isLanAddress("1.1.172.16"));
        assertFalse(IpAddresses.isLanAddress("8.8.8.8"));
    }
}
