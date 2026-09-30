package com.kalamet.review;

import com.kalamet.catalog.Product;
import com.kalamet.catalog.ProductRepository;
import com.kalamet.common.ApiException;
import com.kalamet.common.PageResponse;
import com.kalamet.common.PersianText;
import com.kalamet.order.OrderRepository;
import com.kalamet.review.ReviewDtos.AdminReviewResponse;
import com.kalamet.review.ReviewDtos.MyReviewResponse;
import com.kalamet.review.ReviewDtos.ReviewRequest;
import com.kalamet.review.ReviewDtos.ReviewResponse;
import com.kalamet.user.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviews;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final UserService userService;

    ReviewService(ReviewRepository reviews, ProductRepository products, OrderRepository orders,
                  UserService userService) {
        this.reviews = reviews;
        this.products = products;
        this.orders = orders;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> approved(String productSlug, Pageable pageable) {
        Product product = activeProduct(productSlug);
        return PageResponse.of(reviews.findByProductIdAndStatus(product.getId(), ReviewStatus.APPROVED, pageable),
                ReviewResponse::of);
    }

    /** New reviews wait for moderation. "Verified purchase" means a delivered order contains the product. */
    public MyReviewResponse create(Long userId, String productSlug, ReviewRequest request) {
        Product product = activeProduct(productSlug);
        if (reviews.existsByUserIdAndProductId(userId, product.getId())) {
            throw ApiException.conflict("REVIEW_EXISTS",
                    "شما قبلاً برای این کالا دیدگاه ثبت کرده‌اید؛ می‌توانید همان را ویرایش کنید.");
        }
        Review review = new Review(userService.require(userId), product);
        apply(review, request);
        review.setVerifiedPurchase(orders.existsDeliveredPurchase(userId, product.getId()));
        return MyReviewResponse.of(reviews.saveAndFlush(review));
    }

    /** Editing sends the review back to moderation. */
    public MyReviewResponse update(Long userId, Long reviewId, ReviewRequest request) {
        Review review = reviews.findByIdAndUserId(reviewId, userId).orElseThrow(ReviewService::notFound);
        apply(review, request);
        review.setStatus(ReviewStatus.PENDING);
        review.setVerifiedPurchase(orders.existsDeliveredPurchase(userId, review.getProduct().getId()));
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

    private Product activeProduct(String slug) {
        return products.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "کالا پیدا نشد."));
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
