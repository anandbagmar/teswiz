package com.znsio.teswiz.visual;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how a position phrase from a Gherkin step ("first", "last", "3rd", "2", ...) maps to a
 * zero-based list index. Shared by {@code VisualOcrBL.parsePositionIndex} and
 * {@code AbstractVisualOcrScreen.parsePositionIndex}, so this is their behaviour contract.
 */
class PositionIndexParserTest {
    private static final int LIST_SIZE = 5;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankPositionIsTheFirstElement(String positionText) {
        assertThat(PositionIndexParser.toIndex(positionText, LIST_SIZE)).isZero();
    }

    @ParameterizedTest
    @CsvSource({"first,0", "1st,0", "second,1", "2nd,1", "third,2", "3rd,2", "FIRST,0", "  Second  ,1"})
    void namedPositionsMapToTheirIndex(String positionText, int expectedIndex) {
        assertThat(PositionIndexParser.toIndex(positionText, LIST_SIZE)).isEqualTo(expectedIndex);
    }

    @ParameterizedTest
    @CsvSource({"last,5,4", "LAST,1,0", "last,0,0"})
    void lastIsTheFinalIndexAndNeverNegative(String positionText, int listSize, int expectedIndex) {
        assertThat(PositionIndexParser.toIndex(positionText, listSize)).isEqualTo(expectedIndex);
    }

    @ParameterizedTest
    @CsvSource({"4th,3", "11th,10", "21st,20", "22nd,21", "23rd,22", "0th,-1"})
    void numericOrdinalsAreOneBased(String positionText, int expectedIndex) {
        assertThat(PositionIndexParser.toIndex(positionText, LIST_SIZE)).isEqualTo(expectedIndex);
    }

    @ParameterizedTest
    @CsvSource({"2,2", "0,0", "-1,-1", "' 3 ',3"})
    void plainNumbersAreUsedAsTheIndexAsIs(String positionText, int expectedIndex) {
        assertThat(PositionIndexParser.toIndex(positionText, LIST_SIZE)).isEqualTo(expectedIndex);
    }

    @ParameterizedTest
    @ValueSource(strings = {"middle", "nth", "fourth", "2x"})
    void unrecognisedPositionsFallBackToTheFirstElement(String positionText) {
        assertThat(PositionIndexParser.toIndex(positionText, LIST_SIZE)).isZero();
    }
}
