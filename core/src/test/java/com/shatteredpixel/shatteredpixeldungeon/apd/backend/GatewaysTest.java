package com.shatteredpixel.shatteredpixeldungeon.apd.backend;

import org.junit.Test;

import static org.junit.Assert.*;

public class GatewaysTest {
    @Test public void plainHttpRequiresAnExactLoopbackHost() {
        assertEquals("http://localhost:8080", Gateways.cleanBaseUrl("http://localhost:8080/"));
        assertEquals("http://127.0.0.1:8080/api", Gateways.cleanBaseUrl("http://127.0.0.1:8080/api"));
        for (String address : new String[]{"http://localhost.example.org", "http://127.0.0.1.example.org",
                "http://localhost@evil.example", "http://127.0.0.10", "http://example.org"}) {
            assertNull(address, Gateways.cleanBaseUrl(address));
            assertTrue(address, Gateways.choose(true, address, null) instanceof OfflineGateway);
        }
    }

    @Test public void malformedOrCredentialBearingAddressesAreRejected() {
        for (String address : new String[]{"https://", "https:///api", "https://user:secret@example.org",
                "https://example.org:bad", "https://example.org:65536", "https://example.org:-1",
                "https://example.org/api?secret=x", "https://example.org/#fragment",
                "https://example.org/\tpath", "https://example.org/\npath"}) {
            assertNull(address, Gateways.cleanBaseUrl(address));
        }
    }

    @Test public void securePathsAndIpv6RemainUsableWithoutSendingAnything() {
        assertEquals("https://example.org/api", Gateways.cleanBaseUrl(" https://example.org/api/// "));
        assertEquals("https://[::1]:8443/api", Gateways.cleanBaseUrl("https://[::1]:8443/api"));
        assertTrue(Gateways.choose(false, "https://example.org", null) instanceof OfflineGateway);
        assertNull(Gateways.cleanBaseUrl(null));
    }
}
