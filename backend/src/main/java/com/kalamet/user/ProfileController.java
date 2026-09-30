package com.kalamet.user;

import com.kalamet.common.CurrentUserId;
import com.kalamet.user.UserDtos.AddressRequest;
import com.kalamet.user.UserDtos.AddressResponse;
import com.kalamet.user.UserDtos.ProfileResponse;
import com.kalamet.user.UserDtos.ProfileUpdateRequest;
import com.kalamet.user.UserDtos.ProvinceResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Profile and addresses")
@RestController
class ProfileController {

    private final UserService userService;
    private final AddressService addressService;

    ProfileController(UserService userService, AddressService addressService) {
        this.userService = userService;
        this.addressService = addressService;
    }

    @GetMapping("/api/me")
    ProfileResponse me(@CurrentUserId Long userId) {
        return userService.profile(userId);
    }

    @PutMapping("/api/me")
    ProfileResponse update(@CurrentUserId Long userId, @Valid @RequestBody ProfileUpdateRequest request) {
        return userService.updateProfile(userId, request);
    }

    @GetMapping("/api/provinces")
    List<ProvinceResponse> provinces() {
        return addressService.provinces();
    }

    @GetMapping("/api/me/addresses")
    List<AddressResponse> addresses(@CurrentUserId Long userId) {
        return addressService.list(userId);
    }

    @PostMapping("/api/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    AddressResponse create(@CurrentUserId Long userId, @Valid @RequestBody AddressRequest request) {
        return addressService.create(userId, request);
    }

    @PutMapping("/api/me/addresses/{id}")
    AddressResponse update(@CurrentUserId Long userId, @PathVariable Long id,
                           @Valid @RequestBody AddressRequest request) {
        return addressService.update(userId, id, request);
    }

    @PostMapping("/api/me/addresses/{id}/default")
    AddressResponse makeDefault(@CurrentUserId Long userId, @PathVariable Long id) {
        return addressService.makeDefault(userId, id);
    }

    @DeleteMapping("/api/me/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CurrentUserId Long userId, @PathVariable Long id) {
        addressService.delete(userId, id);
    }
}
