package com.kalamet.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payments")
@RestController
@RequestMapping("/api/payments")
class PaymentController {

    private final PaymentService paymentService;

    PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Gateway return URL; redirects the browser to the frontend result page")
    @GetMapping("/callback")
    ResponseEntity<Void> callback(@RequestParam(name = "Authority", required = false) String authority,
                                  @RequestParam(name = "Status", required = false) String status) {
        String target = paymentService.handleCallback(authority, status);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }

    @Operation(summary = "Payment page of the mock gateway (development only)")
    @GetMapping(value = "/mock/{authority}", produces = MediaType.TEXT_HTML_VALUE)
    String mockPage(@PathVariable String authority) {
        return paymentService.mockPage(authority);
    }
}
