package org.ikigaidigital.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ikigaidigital.application.TimeDepositService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The two REST endpoints required by the README.
 *
 * <p>The README specifies neither verbs nor paths. Assumptions:
 * <ul>
 * <li>{@code POST /time-deposits/balance-updates}: the update changes state and is not idempotent (every call
 * applies another month), so POST, not PUT. It returns nothing, so {@code 204 No Content}.</li>
 * <li>{@code GET /time-deposits}: a read-only collection resource, {@code 200 OK}.</li>
 * <li>"Exactly two endpoints" counts API endpoints: springdoc's {@code /v3/api-docs} and {@code /swagger-ui}
 * document them (the README asks for a Swagger contract), and Spring Boot's {@code /error} is framework default.</li>
 * </ul>
 */
@RestController
@RequestMapping("/time-deposits")
@Tag(name = "Time deposits")
public class TimeDepositController {

    private final TimeDepositService service;

    /**
     * Creates the controller.
     *
     * @param service the time deposit use cases
     */
    public TimeDepositController(TimeDepositService service) {
        this.service = service;
    }

    /**
     * Applies one month of interest to the balances of all time deposits.
     */
    @PostMapping("/balance-updates")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Update the balances of all time deposits",
            description = "Applies one month of interest to every time deposit according to its plan. "
                    + "Not idempotent: each call applies another month.")
    @ApiResponse(responseCode = "204", description = "Balances updated")
    public void updateAllBalances() {
        service.updateAllBalances();
    }

    /**
     * Returns all time deposits with their withdrawals.
     *
     * @return all time deposits ordered by id
     */
    @GetMapping
    @Operation(summary = "Get all time deposits", description = "Returns all time deposits with their withdrawals, ordered by id.")
    @ApiResponse(responseCode = "200", description = "All time deposits")
    public List<TimeDepositResponse> getAllTimeDeposits() {
        return service.getAllTimeDeposits().stream().map(TimeDepositResponse::from).toList();
    }
}
