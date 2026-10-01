package com.kalamet.review.dto;

import com.kalamet.review.domain.Review;
import com.kalamet.review.domain.ReviewStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ReviewDtos {

    private ReviewDtos() {
    }

    public record ReviewRequest(
            @NotNull(message = "امتیاز را انتخاب کنید.")
            @Min(value = 1, message = "امتیاز باید بین ۱ تا ۵ باشد.")
            @Max(value = 5, message = "امتیاز باید بین ۱ تا ۵ باشد.") Integer rating,
            @Size(max = 150, message = "عنوان حداکثر ۱۵۰ حرف است.") String title,
            @Size(max = 3000, message = "متن دیدگاه حداکثر ۳۰۰۰ حرف است.") String comment,
            Boolean recommended) {
    }

    public record ModerationRequest(@NotNull ReviewStatus status) {
    }

    /** As shown on the product page. */
    public record ReviewResponse(Long id, String authorName, int rating, String title, String comment,
                                 Boolean recommended, boolean verifiedPurchase, Instant createdAt) {

        public static ReviewResponse of(Review r) {
            return new ReviewResponse(r.getId(), r.getUser().displayName(), r.getRating(), r.getTitle(),
                    r.getComment(), r.getRecommended(), r.isVerifiedPurchase(), r.getCreatedAt());
        }
    }

    public record ProductRef(Long id, String name, String slug) {
    }

    /** The author's own review, with its moderation status. */
    public record MyReviewResponse(Long id, ProductRef product, int rating, String title, String comment,
                                   Boolean recommended, boolean verifiedPurchase, ReviewStatus status,
                                   Instant createdAt) {

        public static MyReviewResponse of(Review r) {
            return new MyReviewResponse(r.getId(), productRef(r), r.getRating(), r.getTitle(), r.getComment(),
                    r.getRecommended(), r.isVerifiedPurchase(), r.getStatus(), r.getCreatedAt());
        }
    }

    public record AdminReviewResponse(Long id, ProductRef product, Long userId, String userMobile, String authorName,
                                      int rating, String title, String comment, Boolean recommended,
                                      boolean verifiedPurchase, ReviewStatus status, Instant createdAt) {

        public static AdminReviewResponse of(Review r) {
            return new AdminReviewResponse(r.getId(), productRef(r), r.getUser().getId(), r.getUser().getMobile(),
                    r.getUser().displayName(), r.getRating(), r.getTitle(), r.getComment(), r.getRecommended(),
                    r.isVerifiedPurchase(), r.getStatus(), r.getCreatedAt());
        }
    }

    private static ProductRef productRef(Review r) {
        return new ProductRef(r.getProduct().getId(), r.getProduct().getName(), r.getProduct().getSlug());
    }
}
