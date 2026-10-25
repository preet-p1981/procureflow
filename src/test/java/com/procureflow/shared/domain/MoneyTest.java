package com.procureflow.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void normalizesScaleSoEqualAmountsAreEqual() {
        assertThat(Money.of("10", "INR")).isEqualTo(Money.of("10.0000", "INR"));
    }

    @Test
    void addsSameCurrency() {
        Money total = Money.of("10.50", "INR").add(Money.of("2.25", "INR"));
        assertThat(total).isEqualTo(Money.of("12.75", "INR"));
    }

    @Test
    void rejectsAddingDifferentCurrencies() {
        assertThatThrownBy(() -> Money.of("10", "INR").add(Money.of("10", "USD")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Currency mismatch")
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(CommonError.CURRENCY_MISMATCH);
    }

    @Test
    void multipliesAndRoundsHalfEven() {
        Money result = Money.of("10.00", "INR").multiply(new BigDecimal("0.33335"));
        assertThat(result.amount()).isEqualByComparingTo("3.3335");
    }

    @Test
    void subtractCanGoNegative() {
        Money result = Money.of("5", "INR").subtract(Money.of("8", "INR"));
        assertThat(result.isNegative()).isTrue();
    }

    @Test
    void zeroIsNotPositive() {
        assertThat(Money.zero(java.util.Currency.getInstance("INR")).isPositive()).isFalse();
    }
}