package com.znsio.teswiz.network;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The run-wide record of every connection that needed address fallback. Each note is logged as a warning when it is
 * recorded, handed to every listener (for example to attach it to the running scenario), summarised per host at the
 * end of the run, and written to a file the pipeline summary reads back. Notes never change a result.
 * <p>
 * Thread-safe: connections are made from many threads at once.
 */
public final class NetworkNotes {

    // LogManager/Logger are fully-qualified ON PURPOSE (same as JUnitResults): an auto-import-organizer in this
    // environment rewrites a plain log4j import to java.util.logging.LogManager and drops Logger, breaking the build.
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager
            .getLogger(NetworkNotes.class.getName());
    private static final NetworkNotes GLOBAL = new NetworkNotes();
    private static final String NONE = "none";
    private static final String PURPOSE = "purpose";
    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String UNREACHABLE = "unreachable";
    private static final String ADDRESS = "address";
    private static final String REASON = "reason";
    private static final String CONNECTED_ADDRESS = "connectedAddress";

    private final List<NetworkNote> recorded = new CopyOnWriteArrayList<>();
    private final List<Consumer<NetworkNote>> listeners = new CopyOnWriteArrayList<>();

    /** @return the notes of this run, shared by every teswiz connection */
    public static NetworkNotes global() {
        return GLOBAL;
    }

    /**
     * @param file a notes file written by {@link #writeTo(Path)}
     * @return its notes; none when the file is missing or unreadable (a note is never worth failing a run for)
     */
    public static List<NetworkNote> readFrom(Path file) {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        try {
            return fromJson(new JSONArray(Files.readString(file, StandardCharsets.UTF_8)));
        } catch (IOException | JSONException e) {
            LOGGER.warn("Could not read the network notes file {}: {}", file, FailureDetail.describe(e));
            return List.of();
        }
    }

    /** @return this, after logging the note as a warning and handing it to every listener */
    public NetworkNotes record(NetworkNote note) {
        recorded.add(note);
        LOGGER.warn("Network note: {}", note.describe());
        listeners.forEach(listener -> tellListener(listener, note));
        return this;
    }

    /** @return every note recorded so far, in order */
    public List<NetworkNote> notes() {
        return List.copyOf(recorded);
    }

    /** @return this, with {@code listener} told about every note recorded from now on */
    public NetworkNotes onRecord(Consumer<NetworkNote> listener) {
        listeners.add(listener);
        return this;
    }

    /** @return this, with every note forgotten (listeners are kept) */
    public NetworkNotes clear() {
        recorded.clear();
        return this;
    }

    /** @return this, after writing every note to {@code file} as JSON (parent folders are created) */
    public NetworkNotes writeTo(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, toJson(notes()).toString(2), StandardCharsets.UTF_8);
        return this;
    }

    /** @return one block, one line per host (fallbacks, unreachable and used addresses); empty when no notes */
    public String summary() {
        List<NetworkNote> notes = notes();
        if (notes.isEmpty()) {
            return "";
        }
        Map<String, List<NetworkNote>> byHost = groupByHost(notes);
        StringBuilder summary = new StringBuilder("Network notes for this run: ").append(byHost.size())
                .append(" host(s) needed address fallback");
        byHost.forEach((target, hostNotes) -> summary.append(System.lineSeparator()).append("  ")
                .append(summaryLine(target, hostNotes)));
        return summary.toString();
    }

    private static void tellListener(Consumer<NetworkNote> listener, NetworkNote note) {
        try {
            listener.accept(note);
        } catch (RuntimeException e) {
            LOGGER.warn("A network note listener failed: {}", FailureDetail.describe(e));
        }
    }

    private static Map<String, List<NetworkNote>> groupByHost(List<NetworkNote> notes) {
        Map<String, List<NetworkNote>> byHost = new LinkedHashMap<>();
        notes.forEach(note -> byHost.computeIfAbsent(note.host() + ":" + note.port(), target -> new ArrayList<>())
                .add(note));
        return byHost;
    }

    private static String summaryLine(String target, List<NetworkNote> hostNotes) {
        Set<String> unreachable = new LinkedHashSet<>();
        Set<String> used = new LinkedHashSet<>();
        hostNotes.forEach(note -> {
            unreachable.addAll(note.unreachableAddresses());
            used.add(note.connected() ? note.connectedAddress() : NONE);
        });
        return target + "  fallbacks=" + hostNotes.size() + "  unreachable=" + unreachable + "  used=" + used;
    }

    private static JSONArray toJson(List<NetworkNote> notes) {
        JSONArray array = new JSONArray();
        notes.forEach(note -> array.put(toJson(note)));
        return array;
    }

    private static JSONObject toJson(NetworkNote note) {
        JSONArray unreachable = new JSONArray();
        note.unreachable().forEach(address -> unreachable
                .put(new JSONObject().put(ADDRESS, address.address()).put(REASON, address.reason())));
        JSONObject json = new JSONObject().put(PURPOSE, note.purpose()).put(HOST, note.host())
                .put(PORT, note.port()).put(UNREACHABLE, unreachable);
        if (note.connected()) {
            json.put(CONNECTED_ADDRESS, note.connectedAddress());
        }
        return json;
    }

    private static List<NetworkNote> fromJson(JSONArray array) {
        List<NetworkNote> notes = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            notes.add(noteFromJson(array.getJSONObject(index)));
        }
        return notes;
    }

    private static NetworkNote noteFromJson(JSONObject json) {
        List<NetworkNote.UnreachableAddress> unreachable = new ArrayList<>();
        JSONArray addresses = json.getJSONArray(UNREACHABLE);
        for (int index = 0; index < addresses.length(); index++) {
            JSONObject address = addresses.getJSONObject(index);
            unreachable.add(new NetworkNote.UnreachableAddress(address.getString(ADDRESS), address.getString(REASON)));
        }
        return new NetworkNote(json.getString(PURPOSE), json.getString(HOST), json.getInt(PORT), unreachable,
                json.optString(CONNECTED_ADDRESS, null));
    }
}
