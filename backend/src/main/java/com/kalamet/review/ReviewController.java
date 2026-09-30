package com.kalamet.review;

import com.kalamet.common.CurrentUserId;
import com.kalamet.common.PageResponse;
import com.kalamet.common.Paging;
import com.kalamet.review.ReviewDtos.AdminReviewResponse;
import com.kalamet.review.ReviewDtos.ModerationRequest;
import com.kalamet.review.ReviewDtos.MyReviewResponse;
import com.kalamet.review.ReviewDtos.ReviewRequest;
import com.kalamet.review.ReviewDtos.ReviewResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reviews")
@RestController
class ReviewController {

    private static final Sort NEWEST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final ReviewService reviewService;

    ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/api/products/{slug}/reviews")
    PageResponse<ReviewResponse> approved(@PathVariable String slug,
                                          @RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer size) {
        return reviewService.approved(slug, Paging.of(page, size, NEWEST));
    }

    @PostMapping("/api/products/{slug}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    MyReviewResponse create(@CurrentUserId Long userId, @PathVariable String slug,
                            @Valid @RequestBody ReviewRequest request) {
        return reviewService.create(userId, slug, request);
    }

    @GetMapping("/api/me/reviews")
    PageResponse<MyReviewResponse> mine(@CurrentUserId Long userId,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer size) {
        return reviewService.mine(userId, Paging.of(page, size, NEWEST));
    }

    @PutMapping("/api/me/reviews/{id}")
    MyReviewResponse update(@CurrentUserId Long userId, @PathVariable Long id,
                            @Valid @RequestBody ReviewRequest request) {
        return reviewService.update(userId, id, request);
    }

    @DeleteMapping("/api/me/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CurrentUserId Long userId, @PathVariable Long id) {
        reviewService.delete(userId, id);
    }

    @Tag(name = "Admin: reviews")
    @GetMapping("/api/admin/reviews")
    PageResponse<AdminReviewResponse> adminSearch(@RequestParam(required = false) ReviewStatus status,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        return reviewService.adminSearch(status, Paging.of(page, size, NEWEST));
    }

    @Tag(name = "Admin: reviews")
    @PatchMapping("/api/admin/reviews/{id}")
    AdminReviewResponse moderate(@PathVariable Long id, @Valid @RequestBody ModerationRequest request) {
        return reviewService.moderate(id, request.status());
    }
}
