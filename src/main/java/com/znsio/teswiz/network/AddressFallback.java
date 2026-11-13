package com.znsio.teswiz.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * The rule every teswiz connection follows when a host has several addresses: try each address once, in order. An
 * address that fails while connecting (an {@link Attempt} signals this with {@link NotConnectedException}, meaning
 * nothing was sent) is skipped and the next one is tried. Any other failure ends the call at once, so a request is
 * never sent twice. Whenever an address had to be skipped, one {@link NetworkNote} is recorded.
 */
public final class AddressFallback {

    private final NetworkNotes notes;

    /** @param notes where to record the connections that needed fallback */
    public AddressFallback(NetworkNotes notes) {
        this.notes = notes;
    }

    /**
     * One try of the call against one address.
     *
     * @param <T> what the call returns
     */
    @FunctionalInterface
    public interface Attempt<T> {
        /**
         * @param address the address to connect to
         * @return the call's result
         * @throws NotConnectedException when the address could not be connected to (nothing was sent)
         * @throws IOException           for any other failure (the request may have been sent)
         */
        T attempt(InetAddress address) throws IOException;
    }

    /** Signals that an address could not be connected to, so nothing was sent and the next address may be tried. */
    public static final class NotConnectedException extends IOException {
        private static final long serialVersionUID = 1L;

        /** @param cause why the connection could not be made */
        public NotConnectedException(IOException cause) {
            super(cause.getMessage(), cause);
        }

        /** @return why the connection could not be made */
        public IOException reason() {
            return (IOException) getCause();
        }
    }

    /**
     * @param purpose   what the connection is for (shown in the note)
     * @param host      the host name the addresses belong to
     * @param port      the port being connected to
     * @param addresses every address of the host, in the order to try them
     * @param attempt   the call to make against one address
     * @param <T>       what the call returns
     * @return the result of the first address that connected
     * @throws UnknownHostException when there is no address at all
     * @throws IOException          the original failure, when no address connected or the call failed after connecting
     */
    public <T> T run(String purpose, String host, int port, List<InetAddress> addresses, Attempt<T> attempt)
            throws IOException {
        if (addresses.isEmpty()) {
            throw new UnknownHostException("No address found for " + host);
        }
        Skipped skipped = new Skipped(purpose, host, port);
        for (InetAddress address : addresses) {
            try {
                return attemptAndNote(skipped, address, attempt);
            } catch (NotConnectedException e) {
                skipped.add(address, e.reason());
            }
        }
        skipped.noteIfAny(null);
        throw skipped.lastFailure();
    }

    private <T> T attemptAndNote(Skipped skipped, InetAddress address, Attempt<T> attempt) throws IOException {
        try {
            T result = attempt.attempt(address);
            skipped.noteIfAny(address.getHostAddress());
            return result;
        } catch (NotConnectedException e) {
            throw e;
        } catch (IOException e) {
            skipped.noteIfAny(address.getHostAddress());
            throw e;
        }
    }

    /** The addresses skipped during one call, and the note they become. */
    private final class Skipped {
        private final String purpose;
        private final String host;
        private final int port;
        private final List<NetworkNote.UnreachableAddress> unreachable = new ArrayList<>();
        private IOException lastFailure;

        private Skipped(String purpose, String host, int port) {
            this.purpose = purpose;
            this.host = host;
            this.port = port;
        }

        private Skipped add(InetAddress address, IOException failure) {
            unreachable.add(new NetworkNote.UnreachableAddress(address.getHostAddress(),
                    FailureDetail.describe(failure)));
            lastFailure = failure;
            return this;
        }

        private Skipped noteIfAny(String connectedAddress) {
            if (!unreachable.isEmpty()) {
                notes.record(new NetworkNote(purpose, host, port, unreachable, connectedAddress));
            }
            return this;
        }

        private IOException lastFailure() {
            return lastFailure;
        }
    }
}
