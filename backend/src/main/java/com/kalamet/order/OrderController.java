package com.kalamet.order;

import com.kalamet.common.CurrentUserId;
import com.kalamet.common.PageResponse;
import com.kalamet.common.Paging;
import com.kalamet.order.OrderDtos.CheckoutRequest;
import com.kalamet.order.OrderDtos.OrderResponse;
import com.kalamet.order.OrderDtos.OrderSummary;
import com.kalamet.order.OrderDtos.PayRequest;
import com.kalamet.order.OrderDtos.PayResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Orders")
@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @Operation(summary = "Place an order from the cart; stock is reserved until the payment timeout")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse checkout(@CurrentUserId Long userId, @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(userId, request.addressId(), request.expectedPayable());
    }

    @GetMapping
    PageResponse<OrderSummary> list(@CurrentUserId Long userId,
                                    @RequestParam(required = false) Integer page,
                                    @RequestParam(required = false) Integer size) {
        return orderService.list(userId, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/{orderNumber}")
    OrderResponse get(@CurrentUserId Long userId, @PathVariable String orderNumber) {
        return orderService.get(userId, orderNumber);
    }

    @PostMapping("/{orderNumber}/cancel")
    OrderResponse cancel(@CurrentUserId Long userId, @PathVariable String orderNumber) {
        return orderService.cancel(userId, orderNumber);
    }

    @Operation(summary = "Start a payment attempt; send the customer to paymentUrl")
    @PostMapping("/{orderNumber}/pay")
    PayResponse pay(@CurrentUserId Long userId, @PathVariable String orderNumber,
                    @RequestBody(required = false) PayRequest request) {
        return paymentService.start(userId, orderNumber, request == null ? null : request.gateway());
    }
}
