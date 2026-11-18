package org.ikigaidigital.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.ikigaidigital.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests of the two endpoints over HTTP against PostgreSQL, with Open Session in View disabled.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Sql("/time-deposits.sql")
class TimeDepositApiTest {

    @Autowired
    private TestRestTemplate http;

    @Test
    void getReturnsAllDepositsWithWithdrawals() throws Exception {
        ResponseEntity<String> response = http.getForEntity("/time-deposits", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JSONAssert.assertEquals("""
                [
                  {"id": 1, "planType": "basic", "balance": 10000.00, "days": 31, "withdrawals": [
                      {"id": 1, "amount": 100.00, "date": "2026-01-10"},
                      {"id": 2, "amount": 50.25, "date": "2026-02-10"}]},
                  {"id": 2, "planType": "student", "balance": 1234567.89, "days": 100, "withdrawals": []},
                  {"id": 3, "planType": "premium", "balance": 10000.00, "days": 46, "withdrawals": [
                      {"id": 3, "amount": 200.00, "date": "2026-03-01"}]},
                  {"id": 4, "planType": "student", "balance": 10000.00, "days": 366, "withdrawals": []},
                  {"id": 5, "planType": "premium", "balance": 10000.00, "days": 45, "withdrawals": []},
                  {"id": 6, "planType": "gold", "balance": 10000.00, "days": 100, "withdrawals": []}
                ]""", response.getBody(), JSONCompareMode.STRICT);
        assertThat(response.getBody()).contains("\"balance\":10000.00"); // two decimals, not 10000.0
    }

    @Test
    void postAppliesOneMonthOfInterestAndStoresTwoDecimals() {
        ResponseEntity<Void> response = http.postForEntity("/time-deposits/balance-updates", null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(balancesById()).containsExactlyInAnyOrderEntriesOf(Map.of(
                1, new BigDecimal("10008.33"),   // basic, day 31
                2, new BigDecimal("1237654.31"), // student; the calculator's raw double is 1237654.3099999998
                3, new BigDecimal("10041.67"),   // premium, day 46
                4, new BigDecimal("10000.00"),   // student after 365 days
                5, new BigDecimal("10000.00"),   // premium within 45 days
                6, new BigDecimal("10000.00"))); // unknown plan type
    }

    @Test
    void everyPostAppliesAnotherMonth() {
        http.postForEntity("/time-deposits/balance-updates", null, Void.class);
        http.postForEntity("/time-deposits/balance-updates", null, Void.class);

        assertThat(balancesById().get(1)).isEqualTo(new BigDecimal("10016.67"));
    }

    @Test
    void openApiDocumentsExactlyTheTwoEndpoints() {
        JsonNode paths = http.getForObject("/v3/api-docs", JsonNode.class).get("paths");

        assertThat(paths.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("/time-deposits", "/time-deposits/balance-updates");
        assertThat(paths.get("/time-deposits").fieldNames()).toIterable().containsExactly("get");
        assertThat(paths.get("/time-deposits/balance-updates").fieldNames()).toIterable().containsExactly("post");
    }

    private Map<Integer, BigDecimal> balancesById() {
        TimeDepositResponse[] deposits = http.getForObject("/time-deposits", TimeDepositResponse[].class);
        return Arrays.stream(deposits).collect(Collectors.toMap(TimeDepositResponse::id, TimeDepositResponse::balance));
    }
}
