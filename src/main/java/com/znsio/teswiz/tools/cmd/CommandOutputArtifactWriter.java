package com.znsio.teswiz.tools.cmd;

import com.znsio.teswiz.tools.JsonPrettyPrinter;
import com.znsio.teswiz.tools.SensitiveDataMasker;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

final class CommandOutputArtifactWriter {
    private static final AtomicLong COMMAND_NUMBER = new AtomicLong();

    private CommandOutputArtifactWriter() {
    }

    static Path write(String command, String stdout, String stderr) {
        String directory = ThreadContext.get("scenarioLogDirectory");
        if (directory == null || directory.isBlank()) {
            directory = System.getProperty("LOG_DIR", "target") + "/commandOutput";
        } else {
            directory = directory + "/commandOutput";
        }

        long number = COMMAND_NUMBER.incrementAndGet();
        Path artifactDirectory = Path.of(directory);
        try {
            Files.createDirectories(artifactDirectory);
            Path artifact = artifactDirectory.resolve(String.format("command-%04d.log", number));
            String content = "COMMAND\n" + SensitiveDataMasker.mask(command)
                    + "\n\nSTDOUT\n" + maskAndPrettyPrint(stdout) + "\n\nSTDERR\n" + maskAndPrettyPrint(stderr);
            Files.writeString(artifact, content, StandardCharsets.UTF_8);
            return artifact;
        } catch (IOException e) {
            return null;
        }
    }

    private static String maskAndPrettyPrint(String output) {
        if (output == null || output.isBlank()) {
            return "(empty)";
        }
        return SensitiveDataMasker.mask(prettyPrintIfJson(output));
    }

    private static String prettyPrintIfJson(String rawOutput) {
        String trimmed = rawOutput.trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            return rawOutput;
        }
        String pretty = JsonPrettyPrinter.prettyPrint(trimmed);
        return pretty.startsWith("\u26a0\ufe0f Failed to pretty print JSON") ? rawOutput : pretty;
    }
}
