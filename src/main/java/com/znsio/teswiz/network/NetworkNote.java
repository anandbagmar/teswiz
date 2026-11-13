package com.znsio.teswiz.network;

import java.util.List;
import java.util.stream.Collectors;

/**
 * One connection that needed address fallback: the host had several addresses, at least one could not be reached, and
 * either another address answered ({@link #connectedAddress()} names it) or none did ({@code null}). A note never
 * changes a result; it makes a firewall or routing problem visible.
 *
 * @param purpose          what the connection was for, e.g. {@code slack}, {@code api}
 * @param host             the host name that was looked up
 * @param port             the port connected to
 * @param unreachable      every address that could not be reached, in the order tried
 * @param connectedAddress the address that answered, or {@code null} when none did
 */
public record NetworkNote(String purpose, String host, int port, List<UnreachableAddress> unreachable,
        String connectedAddress) {

    public NetworkNote {
        unreachable = List.copyOf(unreachable);
    }

    /**
     * An address that could not be reached.
     *
     * @param address the IP address
     * @param reason  why, e.g. {@code SocketTimeoutException: connect timed out}
     */
    public record UnreachableAddress(String address, String reason) {
    }

    /** @return true when another address answered after the skipped ones */
    public boolean connected() {
        return connectedAddress != null;
    }

    /** @return the IP addresses that could not be reached, in the order tried */
    public List<String> unreachableAddresses() {
        return unreachable.stream().map(UnreachableAddress::address).toList();
    }

    /** @return one log-friendly line, e.g. {@code host:443 (slack): 1.2.3.4 unreachable (...) -> connected via ...} */
    public String describe() {
        String target = host + ":" + port + " (" + purpose + "): ";
        if (connected()) {
            return target + reasons(" unreachable (") + " -> connected via " + connectedAddress;
        }
        return target + "all " + unreachable.size() + " address(es) unreachable: " + reasons(" (");
    }

    private String reasons(String beforeReason) {
        return unreachable.stream().map(address -> address.address() + beforeReason + address.reason() + ")")
                .collect(Collectors.joining(", "));
    }
}
