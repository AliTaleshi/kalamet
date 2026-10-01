package com.kalamet.review.service;

import com.kalamet.catalog.domain.Product;
import com.kalamet.catalog.service.CatalogService;
import com.kalamet.common.error.ApiException;
import com.kalamet.common.text.PersianText;
import com.kalamet.common.web.PageResponse;
import com.kalamet.order.service.OrderService;
import com.kalamet.review.domain.Review;
import com.kalamet.review.domain.ReviewStatus;
import com.kalamet.review.dto.ReviewDtos.AdminReviewResponse;
import com.kalamet.review.dto.ReviewDtos.MyReviewResponse;
import com.kalamet.review.dto.ReviewDtos.ReviewRequest;
import com.kalamet.review.dto.ReviewDtos.ReviewResponse;
import com.kalamet.review.repository.ReviewRepository;
import com.kalamet.user.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviews;
    private final CatalogService catalog;
    private final OrderService orderService;
    private final UserService userService;

    ReviewService(ReviewRepository reviews, CatalogService catalog, OrderService orderService,
                  UserService userService) {
        this.reviews = reviews;
        this.catalog = catalog;
        this.orderService = orderService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> approved(String productSlug, Pageable pageable) {
        Product product = catalog.activeProduct(productSlug);
        return PageResponse.of(reviews.findByProductIdAndStatus(product.getId(), ReviewStatus.APPROVED, pageable),
                ReviewResponse::of);
    }

    /** New reviews wait for moderation. "Verified purchase" means a delivered order contains the product. */
    public MyReviewResponse create(Long userId, String productSlug, ReviewRequest request) {
        Product product = catalog.activeProduct(productSlug);
        if (reviews.existsByUserIdAndProductId(userId, product.getId())) {
            throw ApiException.conflict("REVIEW_EXISTS",
                    "شما قبلاً برای این کالا دیدگاه ثبت کرده‌اید؛ می‌توانید همان را ویرایش کنید.");
        }
        Review review = new Review(userService.require(userId), product);
        apply(review, request);
        review.setVerifiedPurchase(orderService.hasReceivedProduct(userId, product.getId()));
        return MyReviewResponse.of(reviews.saveAndFlush(review));
    }

    /** Editing sends the review back to moderation. */
    public MyReviewResponse update(Long userId, Long reviewId, ReviewRequest request) {
        Review review = reviews.findByIdAndUserId(reviewId, userId).orElseThrow(ReviewService::notFound);
        apply(review, request);
        review.setStatus(ReviewStatus.PENDING);
        review.setVerifiedPurchase(orderService.hasReceivedProduct(userId, review.getProduct().getId()));
        return MyReviewResponse.of(reviews.saveAndFlush(review));
    }

    public void delete(Long userId, Long reviewId) {
        reviews.delete(reviews.findByIdAndUserId(reviewId, userId).orElseThrow(ReviewService::notFound));
    }

    @Transactional(readOnly = true)
    public PageResponse<MyReviewResponse> mine(Long userId, Pageable pageable) {
        return PageResponse.of(reviews.findByUserId(userId, pageable), MyReviewResponse::of);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminReviewResponse> adminSearch(ReviewStatus status, Pageable pageable) {
        return PageResponse.of(reviews.adminSearch(status, pageable), AdminReviewResponse::of);
    }

    public AdminReviewResponse moderate(Long reviewId, ReviewStatus status) {
        if (status == ReviewStatus.PENDING) {
            throw ApiException.badRequest("INVALID_MODERATION", "وضعیت باید APPROVED یا REJECTED باشد.");
        }
        Review review = reviews.findById(reviewId).orElseThrow(ReviewService::notFound);
        review.setStatus(status);
        return AdminReviewResponse.of(reviews.saveAndFlush(review));
    }

    /** A bare star rating is allowed; title, comment and recommendation are optional. */
    private static void apply(Review review, ReviewRequest request) {
        review.setRating(request.rating());
        review.setTitle(PersianText.normalize(request.title()));
        review.setComment(PersianText.normalize(request.comment()));
        review.setRecommended(request.recommended());
    }

    private static ApiException notFound() {
        return ApiException.notFound("REVIEW_NOT_FOUND", "دیدگاه پیدا نشد.");
    }
}
