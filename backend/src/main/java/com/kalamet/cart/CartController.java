package com.kalamet.cart;

import com.kalamet.cart.CartDtos.AddItemRequest;
import com.kalamet.cart.CartDtos.CartView;
import com.kalamet.cart.CartDtos.MergeRequest;
import com.kalamet.cart.CartDtos.QuantityRequest;
import com.kalamet.common.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cart")
@RestController
@RequestMapping("/api/cart")
class CartController {

    private final CartService cartService;

    CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    CartView cart(@CurrentUserId Long userId) {
        return cartService.view(userId);
    }

    @Operation(summary = "Add a variant; adding it again increases the quantity")
    @PostMapping("/items")
    CartView add(@CurrentUserId Long userId, @Valid @RequestBody AddItemRequest request) {
        return cartService.add(userId, request);
    }

    @Operation(summary = "Set the quantity of a variant (0 removes it)")
    @PutMapping("/items/{variantId}")
    CartView update(@CurrentUserId Long userId, @PathVariable Long variantId,
                    @Valid @RequestBody QuantityRequest request) {
        return cartService.updateQuantity(userId, variantId, request.quantity());
    }

    @DeleteMapping("/items/{variantId}")
    CartView remove(@CurrentUserId Long userId, @PathVariable Long variantId) {
        return cartService.remove(userId, variantId);
    }

    @DeleteMapping
    CartView clear(@CurrentUserId Long userId) {
        return cartService.clear(userId);
    }

    @Operation(summary = "Merge a guest cart after sign-in")
    @PostMapping("/merge")
    CartView merge(@CurrentUserId Long userId, @Valid @RequestBody MergeRequest request) {
        return cartService.merge(userId, request.items());
    }
}
