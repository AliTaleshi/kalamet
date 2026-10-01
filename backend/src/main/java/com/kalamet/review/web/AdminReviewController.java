package com.kalamet.review.web;

import com.kalamet.common.web.PageResponse;
import com.kalamet.common.web.Paging;
import com.kalamet.review.domain.ReviewStatus;
import com.kalamet.review.dto.ReviewDtos.AdminReviewResponse;
import com.kalamet.review.dto.ReviewDtos.ModerationRequest;
import com.kalamet.review.service.ReviewService;
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

@Tag(name = "Admin: reviews")
@RestController
@RequestMapping("/api/admin/reviews")
class AdminReviewController {

    private final ReviewService reviewService;

    AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    PageResponse<AdminReviewResponse> search(@RequestParam(required = false) ReviewStatus status,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size) {
        return reviewService.adminSearch(status, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PatchMapping("/{id}")
    AdminReviewResponse moderate(@PathVariable Long id, @Valid @RequestBody ModerationRequest request) {
        return reviewService.moderate(id, request.status());
    }
}
