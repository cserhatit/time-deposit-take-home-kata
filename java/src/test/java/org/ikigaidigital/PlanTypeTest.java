package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests the exact, case-sensitive lookup of {@link PlanType} codes.
 */
class PlanTypeTest {

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource({"basic, BASIC", "student, STUDENT", "premium, PREMIUM"})
    void findsKnownCodes(String code, PlanType expected) {
        assertThat(PlanType.fromCode(code)).contains(expected);
    }

    @ParameterizedTest(name = "\"{0}\" is unknown")
    @ValueSource(strings = {"gold", "Basic", "PREMIUM", " student", "", "BASIC"})
    void doesNotMatchUnknownOrDifferentlyCasedCodes(String code) {
        assertThat(PlanType.fromCode(code)).isEmpty();
    }

    @Test
    void rejectsNullCode() {
        assertThatNullPointerException().isThrownBy(() -> PlanType.fromCode(null));
    }
}
