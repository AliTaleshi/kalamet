package com.kalamet.catalog.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Which picture shows a variant: one linked to that variant, else one linked to a variant of the
 * same colour (images are linked to one size of each colour), else the product's main image.
 */
public final class VariantImages {

    private static final String COLOR = "color";

    private VariantImages() {
    }

    /** {@code images}: the product's images; returns null when it has none. */
    public static String urlFor(ProductVariant variant, List<ProductImage> images) {
        List<ProductImage> ordered = images.stream()
                .sorted(Comparator.comparingInt(ProductImage::getSortOrder).thenComparing(ProductImage::getId))
                .toList();
        String color = variant.getAttributes().get(COLOR);
        return ordered.stream()
                .filter(image -> image.getVariant() != null && image.getVariant().getId().equals(variant.getId()))
                .findFirst()
                .or(() -> color == null ? Optional.empty() : ordered.stream()
                        .filter(image -> image.getVariant() != null
                                && Objects.equals(color, image.getVariant().getAttributes().get(COLOR)))
                        .findFirst())
                .or(() -> ordered.stream().findFirst())
                .map(ProductImage::getUrl)
                .orElse(null);
    }
}
