package com.kalamet.order.web;

import com.kalamet.common.web.PageResponse;
import com.kalamet.common.web.Paging;
import com.kalamet.order.domain.OrderStatus;
import com.kalamet.order.dto.OrderDtos.OrderResponse;
import com.kalamet.order.dto.OrderDtos.OrderSummary;
import com.kalamet.order.dto.OrderDtos.StatusChangeRequest;
import com.kalamet.order.service.OrderService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: orders")
@RestController
@RequestMapping("/api/admin/orders")
class AdminOrderController {

    private final OrderService orderService;

    AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    PageResponse<OrderSummary> search(@RequestParam(required = false) OrderStatus status,
                                      @Parameter(description = "Order number or customer mobile")
                                      @RequestParam(required = false) String q,
                                      @RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size) {
        return orderService.adminSearch(status, q, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/{orderNumber}")
    OrderResponse get(@PathVariable String orderNumber) {
        return orderService.adminGet(orderNumber);
    }

    @PatchMapping("/{orderNumber}/status")
    OrderResponse changeStatus(@PathVariable String orderNumber, @Valid @RequestBody StatusChangeRequest request) {
        return orderService.adminChangeStatus(orderNumber, request.status());
    }
}
