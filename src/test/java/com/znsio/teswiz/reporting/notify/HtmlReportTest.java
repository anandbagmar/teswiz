package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HtmlReport#reproduceCommand(Map)} — the generic, domain-agnostic "how to re-run this" command
 * builder. teswiz owns only the shell-safety and the {@code ./gradlew run} suffix; the caller (e.g. a casino composer)
 * chooses which keys go in and in what order, so these tests pin the quoting rules, the ordering, the blank-skipping
 * and the empty case — not any particular run-input vocabulary.
 */
class HtmlReportTest {

    @Test
    void leavesMetacharacterFreeValuesBareAndPreservesInsertionOrder() {
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("INTEGRATION", "sup_cleo");
        inputs.put("TAG", "@suite_sanity");
        inputs.put("TARGET_ENVIRONMENT", "STAGE");

        assertThat(HtmlReport.reproduceCommand(inputs))
                .isEqualTo("INTEGRATION=sup_cleo TAG=@suite_sanity TARGET_ENVIRONMENT=STAGE ./gradlew run");
    }

    @Test
    void singleQuotesAValueWithSpacesAndParentheses() {
        // A TAG with spaces and parentheses must be single-quoted, or a shell mis-parses it
        // (e.g. zsh: "unknown sort specifier" on the parentheses).
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("REGION", "row");
        inputs.put("MARKET", "uk");
        inputs.put("BRAND", "bwincom");
        inputs.put("INTEGRATION", "ent_rewards");
        inputs.put("TAG", "(not @workflow)");
        inputs.put("CONFIG", "./configs/config.properties");

        assertThat(HtmlReport.reproduceCommand(inputs)).isEqualTo("REGION=row MARKET=uk BRAND=bwincom "
                + "INTEGRATION=ent_rewards TAG='(not @workflow)' CONFIG=./configs/config.properties ./gradlew run");
    }

    @Test
    void escapesAnEmbeddedSingleQuoteTheStandardPosixWay() {
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("TAG", "@a and @b's");

        // Close the quote, emit an escaped literal quote, reopen: '@a and @b'\''s'
        assertThat(HtmlReport.reproduceCommand(inputs)).isEqualTo("TAG='@a and @b'\\''s' ./gradlew run");
    }

    @Test
    void skipsBlankOrNullValues() {
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("INTEGRATION", "sup_cleo");
        inputs.put("GAME", "   ");
        inputs.put("TAG", null);

        assertThat(HtmlReport.reproduceCommand(inputs)).isEqualTo("INTEGRATION=sup_cleo ./gradlew run");
    }

    @Test
    void anEmptyOrNullMapYieldsJustTheGradleCommand() {
        assertThat(HtmlReport.reproduceCommand(new LinkedHashMap<>())).isEqualTo("./gradlew run");
        assertThat(HtmlReport.reproduceCommand(null)).isEqualTo("./gradlew run");
    }
}
