package com.kalamet.user;

import com.kalamet.common.CurrentUserId;
import com.kalamet.common.PageResponse;
import com.kalamet.common.Paging;
import com.kalamet.user.UserDtos.AdminUserResponse;
import com.kalamet.user.UserDtos.AdminUserUpdateRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: users")
@RestController
@RequestMapping("/api/admin/users")
class AdminUserController {

    private final UserService userService;

    AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    PageResponse<AdminUserResponse> search(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) Role role,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size) {
        return userService.search(q, role, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PatchMapping("/{id}")
    AdminUserResponse update(@CurrentUserId Long adminId, @PathVariable Long id,
                             @RequestBody AdminUserUpdateRequest request) {
        return userService.adminUpdate(adminId, id, request);
    }
}
