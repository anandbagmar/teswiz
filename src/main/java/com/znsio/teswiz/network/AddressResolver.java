package com.znsio.teswiz.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/** Looks up every address of a host name, in the order the system's resolver returns them. */
@FunctionalInterface
public interface AddressResolver {

    /**
     * @param host the host name
     * @return all of its addresses (never empty)
     * @throws UnknownHostException when the name cannot be resolved
     */
    List<InetAddress> resolve(String host) throws UnknownHostException;

    /** @return the system resolver, the same lookup every other Java connection on this machine uses */
    static AddressResolver system() {
        return host -> List.of(InetAddress.getAllByName(host));
    }
}
