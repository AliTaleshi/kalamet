package com.kalamet.catalog;

import com.kalamet.catalog.CatalogDtos.ProductSummary;
import com.kalamet.catalog.CatalogDtos.RatingSummary;
import com.kalamet.common.PageResponse;
import com.kalamet.common.PersianText;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Read-side queries for product listings, written in SQL: a listing needs the cheapest
 * variant, the main image and review stats of each product, which is one query here and
 * several round trips through JPA. The pricing rule matches {@link PriceQuote#of}.
 */
@Repository
public class ProductSearch {

    private static final int MAX_QUERY_TOKENS = 5;
    private static final String PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹";
    private static final String ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩";

    /** Effective price of every active variant, then the one shown per product (in stock first, then cheapest). */
    private static final String CTES = """
            WITH priced AS (
                SELECT v.product_id, v.stock_quantity,
                       CASE WHEN v.discount_ends_at <= :now THEN v.compare_at_price ELSE v.price END AS price,
                       CASE WHEN v.discount_ends_at <= :now THEN NULL ELSE v.compare_at_price END AS original_price,
                       CASE WHEN v.discount_ends_at > :now THEN v.discount_ends_at END AS offer_ends_at
                FROM product_variants v
                WHERE v.active
            ),
            best AS (
                SELECT DISTINCT ON (product_id)
                       product_id, price, original_price, offer_ends_at, stock_quantity > 0 AS in_stock
                FROM priced
                ORDER BY product_id, (stock_quantity > 0) DESC, price, offer_ends_at NULLS LAST
            ),
            ratings AS (
                SELECT product_id, round(avg(rating), 1) AS rating, count(*) AS review_count
                FROM reviews
                WHERE status = 'APPROVED'
                GROUP BY product_id
            )
            """;

    private static final String SALES_CTE = """
            , sales AS (
                SELECT v.product_id, sum(oi.quantity) AS sold
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                JOIN product_variants v ON v.id = oi.variant_id
                WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED')
                GROUP BY v.product_id
            )
            """;

    private final JdbcClient jdbc;

    ProductSearch(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @param query       free text; every word must appear in the product or brand name
     * @param category    category slug; products of its subcategories are included
     * @param brands      brand slugs (any of them)
     */
    public record Criteria(String query, String category, List<String> brands, Long minPrice, Long maxPrice,
                           boolean inStockOnly, boolean offersOnly, ProductSort sort) {
    }

    public PageResponse<ProductSummary> search(Criteria criteria, Instant now, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        params.put("now", OffsetDateTime.ofInstant(now, ZoneOffset.UTC));
        boolean bestselling = criteria.sort() == ProductSort.BESTSELLING;

        StringBuilder from = new StringBuilder("""
                FROM products p
                JOIN best ON best.product_id = p.id
                LEFT JOIN brands b ON b.id = p.brand_id
                LEFT JOIN ratings r ON r.product_id = p.id
                """);
        if (bestselling) {
            from.append("LEFT JOIN sales s ON s.product_id = p.id\n");
        }
        from.append("WHERE p.active\n");

        List<String> tokens = tokens(criteria.query());
        for (int i = 0; i < tokens.size(); i++) {
            String param = "q" + i;
            // Half-spaces and digit scripts are ignored: "تی شرت" finds "تی‌شرت", "65" finds "۶۵".
            from.append("""
                    AND (%2$s ILIKE :%1$s ESCAPE '\\'
                         OR p.name_en ILIKE :%1$s ESCAPE '\\'
                         OR %3$s ILIKE :%1$s ESCAPE '\\'
                         OR b.name_en ILIKE :%1$s ESCAPE '\\')
                    """.formatted(param, searchable("p.name"), searchable("b.name")));
            params.put(param, "%" + escapeLike(tokens.get(i)) + "%");
        }
        if (criteria.category() != null) {
            from.append("""
                    AND p.category_id IN (
                        WITH RECURSIVE tree AS (
                            SELECT id FROM categories WHERE slug = :category
                            UNION ALL
                            SELECT c.id FROM categories c JOIN tree t ON c.parent_id = t.id
                        )
                        SELECT id FROM tree)
                    """);
            params.put("category", criteria.category());
        }
        if (criteria.brands() != null && !criteria.brands().isEmpty()) {
            from.append("AND b.slug IN (:brands)\n");
            params.put("brands", criteria.brands());
        }
        if (criteria.minPrice() != null) {
            from.append("AND best.price >= :minPrice\n");
            params.put("minPrice", criteria.minPrice());
        }
        if (criteria.maxPrice() != null) {
            from.append("AND best.price <= :maxPrice\n");
            params.put("maxPrice", criteria.maxPrice());
        }
        if (criteria.inStockOnly()) {
            from.append("AND best.in_stock\n");
        }
        if (criteria.offersOnly()) {
            from.append("AND EXISTS (SELECT 1 FROM priced o WHERE o.product_id = p.id "
                    + "AND o.offer_ends_at IS NOT NULL AND o.stock_quantity > 0)\n");
        }

        String ctes = bestselling ? CTES + SALES_CTE : CTES;
        long total = jdbc.sql(ctes + "SELECT count(*) " + from)
                .params(params)
                .query(Long.class)
                .single();

        params.put("limit", size);
        params.put("offset", (long) page * size);
        String select = ctes + """
                SELECT p.id, p.slug, p.name, p.name_en, b.name AS brand_name,
                       (SELECT i.url FROM product_images i WHERE i.product_id = p.id
                        ORDER BY i.sort_order, i.id LIMIT 1) AS image_url,
                       best.price, best.original_price, best.offer_ends_at, best.in_stock,
                       r.rating, coalesce(r.review_count, 0) AS review_count
                """ + from + "ORDER BY best.in_stock DESC, " + criteria.sort().orderBy + ", p.id DESC\n"
                + "LIMIT :limit OFFSET :offset";
        List<ProductSummary> items = jdbc.sql(select)
                .params(params)
                .query((rs, row) -> summary(rs))
                .list();
        return PageResponse.of(items, page, size, total);
    }

    /** Brands that have active products, optionally within a category tree (for the filter sidebar). */
    public List<Long> brandIdsWithProducts(String categorySlug) {
        String sql = """
                SELECT DISTINCT p.brand_id FROM products p
                WHERE p.active AND p.brand_id IS NOT NULL
                """;
        if (categorySlug == null) {
            return jdbc.sql(sql).query(Long.class).list();
        }
        return jdbc.sql(sql + """
                AND p.category_id IN (
                    WITH RECURSIVE tree AS (
                        SELECT id FROM categories WHERE slug = :category
                        UNION ALL
                        SELECT c.id FROM categories c JOIN tree t ON c.parent_id = t.id
                    )
                    SELECT id FROM tree)
                """).param("category", categorySlug).query(Long.class).list();
    }

    public RatingSummary ratingSummary(Long productId) {
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int stars = 5; stars >= 1; stars--) {
            distribution.put(stars, 0L);
        }
        long[] totals = new long[3];   // count, recommended, with an opinion
        long[] sum = new long[1];
        jdbc.sql("""
                        SELECT rating, count(*) AS n,
                               count(*) FILTER (WHERE recommended) AS yes,
                               count(recommended) AS opinions
                        FROM reviews
                        WHERE product_id = :productId AND status = 'APPROVED'
                        GROUP BY rating
                        """)
                .param("productId", productId)
                .query(rs -> {
                    int rating = rs.getInt("rating");
                    long n = rs.getLong("n");
                    distribution.put(rating, n);
                    totals[0] += n;
                    totals[1] += rs.getLong("yes");
                    totals[2] += rs.getLong("opinions");
                    sum[0] += rating * n;
                });
        BigDecimal average = totals[0] == 0 ? null
                : BigDecimal.valueOf(sum[0]).divide(BigDecimal.valueOf(totals[0]), 1, RoundingMode.HALF_UP);
        Integer recommendedPercent = totals[2] == 0 ? null : (int) Math.round(totals[1] * 100.0 / totals[2]);
        return new RatingSummary(average, totals[0], recommendedPercent, distribution);
    }

    private static ProductSummary summary(ResultSet rs) throws SQLException {
        long price = rs.getLong("price");
        Long originalPrice = rs.getObject("original_price", Long.class);
        OffsetDateTime offerEndsAt = rs.getObject("offer_ends_at", OffsetDateTime.class);
        return new ProductSummary(
                rs.getLong("id"), rs.getString("slug"), rs.getString("name"), rs.getString("name_en"),
                rs.getString("brand_name"), rs.getString("image_url"),
                price, originalPrice, PriceQuote.discountPercent(price, originalPrice),
                offerEndsAt == null ? null : offerEndsAt.toInstant(),
                rs.getBoolean("in_stock"), rs.getBigDecimal("rating"), rs.getLong("review_count"));
    }

    /** The column without half-spaces and with ASCII digits, matching {@link #tokens}. */
    private static String searchable(String column) {
        return "translate(replace(" + column + ", chr(8204), ''), '" + PERSIAN_DIGITS + ARABIC_DIGITS
                + "', '01234567890123456789')";
    }

    private static List<String> tokens(String query) {
        String normalized = PersianText.digitsToAscii(PersianText.normalize(query));
        if (normalized == null) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        Arrays.stream(normalized.replace("\u200C", "").split("\\s+"))
                .filter(token -> !token.isBlank())
                .limit(MAX_QUERY_TOKENS)
                .forEach(tokens::add);
        return tokens;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
