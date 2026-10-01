package com.kalamet.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VariantImagesTest {

    private final ProductVariant blackS = variant(1L, Map.of("color", "مشکی", "size", "S"));
    private final ProductVariant blackM = variant(2L, Map.of("color", "مشکی", "size", "M"));
    private final ProductVariant whiteS = variant(3L, Map.of("color", "سفید", "size", "S"));
    private final ProductVariant redS = variant(4L, Map.of("color", "قرمز", "size", "S"));

    private final List<ProductImage> images = List.of(
            image(10L, "/black.svg", 0, blackS),
            image(11L, "/white.svg", 1, whiteS),
            image(12L, "/detail.svg", 2, null));

    @Test
    void anImageOfTheVariantWins() {
        assertThat(VariantImages.urlFor(whiteS, images)).isEqualTo("/white.svg");
    }

    @Test
    void otherSizesOfAColourShareItsImage() {
        assertThat(VariantImages.urlFor(blackM, images)).isEqualTo("/black.svg");
    }

    @Test
    void withoutAMatchTheMainImageIsUsed() {
        assertThat(VariantImages.urlFor(redS, images)).isEqualTo("/black.svg");
        assertThat(VariantImages.urlFor(variant(5L, Map.of()), List.of(image(13L, "/only.svg", 0, null))))
                .isEqualTo("/only.svg");
        assertThat(VariantImages.urlFor(redS, List.of())).isNull();
    }

    private static ProductVariant variant(Long id, Map<String, String> attributes) {
        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setAttributes(attributes);
        return variant;
    }

    private static ProductImage image(Long id, String url, int sortOrder, ProductVariant variant) {
        ProductImage image = new ProductImage();
        image.setId(id);
        image.setUrl(url);
        image.setSortOrder(sortOrder);
        image.setVariant(variant);
        return image;
    }
}
