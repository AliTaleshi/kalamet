-- Demo catalog (Persian) for local development and portfolio demos.
-- Repeatable migration: runs after all versioned migrations and again whenever
-- this file changes. Every insert is idempotent, so re-runs never duplicate rows.
-- Prices are in Rial (UI shows Toman = Rial / 10).

-- Top-level categories
INSERT INTO categories (name, slug, sort_order) VALUES
    ('کالای دیجیتال',   'digital',      1),
    ('مد و پوشاک',      'fashion',      2),
    ('خانه و آشپزخانه', 'home-kitchen', 3)
ON CONFLICT (slug) DO NOTHING;

-- Subcategories
INSERT INTO categories (parent_id, name, slug, sort_order)
SELECT parent.id, v.name, v.slug, v.sort_order
FROM (VALUES
    ('digital',      'هدفون',   'headphones', 1),
    ('digital',      'شارژر',   'chargers',   2),
    ('fashion',      'تی‌شرت',  't-shirts',   1),
    ('fashion',      'هودی',    'hoodies',    2),
    ('home-kitchen', 'ماگ',     'mugs',       1)
) AS v(parent_slug, name, slug, sort_order)
JOIN categories parent ON parent.slug = v.parent_slug
ON CONFLICT (slug) DO NOTHING;

-- Brands (fictional)
INSERT INTO brands (name, name_en, slug) VALUES
    ('آرین',     'Arian',    'arian'),
    ('آوا صدا',  'Ava Seda', 'ava-seda'),
    ('ولترا',    'Voltra',   'voltra'),
    ('کوزه‌گر',  'Koozegar', 'koozegar')
ON CONFLICT (slug) DO NOTHING;

-- Products
INSERT INTO products (category_id, brand_id, name, name_en, slug, description)
SELECT c.id, b.id, v.name, v.name_en, v.slug, v.description
FROM (VALUES
    ('headphones', 'ava-seda', 'هدفون بی‌سیم آوا صدا مدل AS-700', 'Ava Seda AS-700 Wireless Headphones',
        'wireless-headphones-as-700',
        'هدفون روگوشی بی‌سیم با حذف فعال نویز و شارژدهی ۳۰ ساعته، مناسب برای سفر و کار روزانه.'),
    ('chargers',   'voltra',   'شارژر دیواری ولترا ۶۵ وات USB-C', 'Voltra 65W USB-C Wall Charger',
        'voltra-usb-c-charger-65w',
        'شارژر جمع‌وجور با پشتیبانی از شارژ سریع برای گوشی، تبلت و لپ‌تاپ.'),
    ('t-shirts',   'arian',    'تی‌شرت نخی آستین کوتاه آرین',     'Arian Essential Cotton T-Shirt',
        'arian-essential-cotton-tee',
        'تی‌شرت نخی نرم با یقه گرد و برش راحت برای استفاده روزمره.'),
    ('hoodies',    'arian',    'هودی زیپ‌دار آرین',               'Arian Everyday Zip Hoodie',
        'arian-everyday-zip-hoodie',
        'هودی دورس با زیپ سرتاسری و جیب کانگورویی.'),
    ('mugs',       'koozegar', 'ماگ سرامیکی دست‌ساز کوزه‌گر',     'Koozegar Handmade Stoneware Mug',
        'koozegar-stoneware-mug',
        'ماگ ۳۵۰ میلی‌لیتری دست‌ساز، قابل شستشو در ماشین ظرفشویی.'),
    ('headphones', 'ava-seda', 'هندزفری بی‌سیم آوا صدا مدل AS-200', 'Ava Seda AS-200 True Wireless Earbuds',
        'ava-seda-as-200-earbuds',
        'هندزفری بی‌سیم سبک با کیس شارژ، مقاوم در برابر عرق و ۲۴ ساعت پخش موسیقی با کیس.'),
    ('chargers',   'voltra',   'پاوربانک ولترا ۲۰۰۰۰ میلی‌آمپر ساعت', 'Voltra 20000mAh Power Bank',
        'voltra-power-bank-20000',
        'پاوربانک با دو خروجی USB-C و USB-A و شارژ سریع ۲۲٫۵ وات؛ مناسب سفر.'),
    ('t-shirts',   'arian',    'تی‌شرت طرح کوهستان آرین',          'Arian Mountain Graphic Tee',
        'arian-mountain-graphic-tee',
        'تی‌شرت نخی با چاپ طرح کوهستان؛ چاپ مقاوم در برابر شستشو.'),
    ('mugs',       'koozegar', 'ماگ شیشه‌ای دوجداره کوزه‌گر',      'Koozegar Double-Wall Glass Mug',
        'koozegar-double-wall-glass-mug',
        'ماگ ۳۰۰ میلی‌لیتری از شیشه بوروسیلیکات دوجداره؛ نوشیدنی را گرم و دست را خنک نگه می‌دارد.')
) AS v(category_slug, brand_slug, name, name_en, slug, description)
JOIN categories c ON c.slug = v.category_slug
JOIN brands b     ON b.slug = v.brand_slug
ON CONFLICT (slug) DO NOTHING;

-- Variants. A product without options gets a single variant with '{}'.
-- discount_ends_at set = shown under "amazing offers" with a countdown.
INSERT INTO product_variants (product_id, sku, attributes, price, compare_at_price, discount_ends_at, stock_quantity)
SELECT p.id, v.sku, v.attributes::jsonb, v.price, v.compare_at_price, v.discount_ends_at, v.stock
FROM (VALUES
    ('wireless-headphones-as-700', 'AS700-BLK',  '{"color": "مشکی"}',               89000000, 104000000, now() + interval '30 days', 25),
    ('wireless-headphones-as-700', 'AS700-SLV',  '{"color": "نقره‌ای"}',             89000000, 104000000, now() + interval '30 days', 10),
    ('voltra-usb-c-charger-65w',   'VLT-65W',    '{}',                              18500000, NULL,      NULL,                        80),
    ('arian-essential-cotton-tee', 'ARN-TEE-BLK-S', '{"color": "مشکی", "size": "S"}', 12900000, 15900000, NULL,                        40),
    ('arian-essential-cotton-tee', 'ARN-TEE-BLK-M', '{"color": "مشکی", "size": "M"}', 12900000, 15900000, NULL,                        55),
    ('arian-essential-cotton-tee', 'ARN-TEE-BLK-L', '{"color": "مشکی", "size": "L"}', 12900000, 15900000, NULL,                        35),
    ('arian-essential-cotton-tee', 'ARN-TEE-WHT-S', '{"color": "سفید", "size": "S"}', 12900000, 15900000, NULL,                        30),
    ('arian-essential-cotton-tee', 'ARN-TEE-WHT-M', '{"color": "سفید", "size": "M"}', 12900000, 15900000, NULL,                        45),
    ('arian-essential-cotton-tee', 'ARN-TEE-WHT-L', '{"color": "سفید", "size": "L"}', 12900000, 15900000, NULL,                        25),
    ('arian-everyday-zip-hoodie',  'ARN-HOD-GRY-M', '{"color": "طوسی", "size": "M"}', 34500000, NULL,     NULL,                        20),
    ('arian-everyday-zip-hoodie',  'ARN-HOD-GRY-L', '{"color": "طوسی", "size": "L"}', 34500000, NULL,     NULL,                        15),
    ('arian-everyday-zip-hoodie',  'ARN-HOD-NVY-M', '{"color": "سرمه‌ای", "size": "M"}', 34500000, NULL,  NULL,                        18),
    ('arian-everyday-zip-hoodie',  'ARN-HOD-NVY-L', '{"color": "سرمه‌ای", "size": "L"}', 34500000, NULL,  NULL,                        12),
    ('koozegar-stoneware-mug',     'KZG-MUG-CRM',   '{"color": "کرم"}',                4800000, 5500000,  now() + interval '7 days',  60),
    ('koozegar-stoneware-mug',     'KZG-MUG-GRY',   '{"color": "طوسی"}',               4800000, 5500000,  now() + interval '7 days',  50),
    ('ava-seda-as-200-earbuds',    'AS200-WHT',     '{"color": "سفید"}',              32900000, 39900000, now() + interval '3 days',  30),
    ('ava-seda-as-200-earbuds',    'AS200-BLK',     '{"color": "مشکی"}',              32900000, 39900000, now() + interval '3 days',  22),
    ('voltra-power-bank-20000',    'VLT-PB20',      '{}',                             24500000, 29900000, now() + interval '5 days',  40),
    ('arian-mountain-graphic-tee', 'ARN-GTE-M',     '{"size": "M"}',                  14900000, NULL,     NULL,                        35),
    ('arian-mountain-graphic-tee', 'ARN-GTE-L',     '{"size": "L"}',                  14900000, NULL,     NULL,                        28),
    ('koozegar-double-wall-glass-mug', 'KZG-GLS-300', '{}',                            6900000, 8500000,  now() + interval '2 days',  25)
) AS v(product_slug, sku, attributes, price, compare_at_price, discount_ends_at, stock)
JOIN products p ON p.slug = v.product_slug
ON CONFLICT (sku) DO NOTHING;

-- Specifications (مشخصات)
INSERT INTO product_specs (product_id, group_name, name, value, sort_order)
SELECT p.id, v.group_name, v.name, v.value, v.sort_order
FROM (VALUES
    ('wireless-headphones-as-700', 'مشخصات کلی', 'نوع اتصال',        'بی‌سیم (بلوتوث ۵٫۳)', 1),
    ('wireless-headphones-as-700', 'مشخصات کلی', 'عمر باتری',        '۳۰ ساعت',             2),
    ('wireless-headphones-as-700', 'مشخصات کلی', 'حذف فعال نویز',    'دارد',                3),
    ('wireless-headphones-as-700', 'مشخصات کلی', 'وزن',              '۲۵۰ گرم',             4),
    ('voltra-usb-c-charger-65w',   'مشخصات کلی', 'توان خروجی',       '۶۵ وات',              1),
    ('voltra-usb-c-charger-65w',   'مشخصات کلی', 'نوع درگاه',        'USB-C',               2),
    ('voltra-usb-c-charger-65w',   'مشخصات کلی', 'شارژ سریع',        'PD 3.0',              3),
    ('arian-essential-cotton-tee', 'مشخصات کلی', 'جنس',              'نخ پنبه ۱۰۰٪',        1),
    ('arian-essential-cotton-tee', 'مشخصات کلی', 'نوع یقه',          'گرد',                 2),
    ('arian-everyday-zip-hoodie',  'مشخصات کلی', 'جنس',              'دورس',                1),
    ('koozegar-stoneware-mug',     'مشخصات کلی', 'حجم',              '۳۵۰ میلی‌لیتر',       1),
    ('koozegar-stoneware-mug',     'مشخصات کلی', 'جنس',              'سرامیک',              2),
    ('koozegar-stoneware-mug',     'مشخصات کلی', 'مناسب ماشین ظرفشویی', 'بله',             3),
    ('ava-seda-as-200-earbuds',    'مشخصات کلی', 'نوع اتصال',        'بلوتوث ۵٫۳',          1),
    ('ava-seda-as-200-earbuds',    'مشخصات کلی', 'عمر باتری',        '۶ ساعت (۲۴ ساعت با کیس)', 2),
    ('ava-seda-as-200-earbuds',    'مشخصات کلی', 'مقاومت در برابر آب', 'IPX4',               3),
    ('voltra-power-bank-20000',    'مشخصات کلی', 'ظرفیت',            '۲۰۰۰۰ میلی‌آمپر ساعت', 1),
    ('voltra-power-bank-20000',    'مشخصات کلی', 'توان خروجی',       '۲۲٫۵ وات',            2),
    ('voltra-power-bank-20000',    'مشخصات کلی', 'وزن',              '۳۹۰ گرم',             3),
    ('arian-mountain-graphic-tee', 'مشخصات کلی', 'جنس',              'نخ پنبه ۱۰۰٪',        1),
    ('arian-mountain-graphic-tee', 'مشخصات کلی', 'نوع چاپ',          'سیلک',                2),
    ('koozegar-double-wall-glass-mug', 'مشخصات کلی', 'حجم',          '۳۰۰ میلی‌لیتر',       1),
    ('koozegar-double-wall-glass-mug', 'مشخصات کلی', 'جنس',          'شیشه بوروسیلیکات',    2)
) AS v(product_slug, group_name, name, value, sort_order)
JOIN products p ON p.slug = v.product_slug
WHERE NOT EXISTS (
    SELECT 1 FROM product_specs s WHERE s.product_id = p.id AND s.name = v.name
);

-- Images: illustrations served by the frontend from public/demo/ (relative URLs, same origin as
-- the store). Earlier versions of this file used random picsum.photos pictures; remove those.
DELETE FROM product_images WHERE url IN (
    'https://picsum.photos/seed/as700-1/800/800',
    'https://picsum.photos/seed/as700-2/800/800',
    'https://picsum.photos/seed/voltra-65w-1/800/800',
    'https://picsum.photos/seed/arian-tee-1/800/800',
    'https://picsum.photos/seed/arian-hoodie-1/800/800',
    'https://picsum.photos/seed/koozegar-mug-1/800/800'
);

INSERT INTO product_images (product_id, url, alt_text, sort_order)
SELECT p.id, v.url, v.alt_text, v.sort_order
FROM (VALUES
    ('wireless-headphones-as-700', '/demo/as700-black.svg',  'هدفون بی‌سیم آوا صدا، مشکی', 0),
    ('wireless-headphones-as-700', '/demo/as700-silver.svg', 'هدفون بی‌سیم آوا صدا، نقره‌ای', 1),
    ('voltra-usb-c-charger-65w',   '/demo/voltra-65w.svg',   'شارژر ولترا ۶۵ وات', 0),
    ('arian-essential-cotton-tee', '/demo/tee-black.svg',    'تی‌شرت نخی آرین، مشکی', 0),
    ('arian-essential-cotton-tee', '/demo/tee-white.svg',    'تی‌شرت نخی آرین، سفید', 1),
    ('arian-everyday-zip-hoodie',  '/demo/hoodie-grey.svg',  'هودی زیپ‌دار آرین، طوسی', 0),
    ('arian-everyday-zip-hoodie',  '/demo/hoodie-navy.svg',  'هودی زیپ‌دار آرین، سرمه‌ای', 1),
    ('koozegar-stoneware-mug',     '/demo/mug-cream.svg',    'ماگ سرامیکی کوزه‌گر، کرم', 0),
    ('koozegar-stoneware-mug',     '/demo/mug-grey.svg',     'ماگ سرامیکی کوزه‌گر، طوسی', 1),
    ('ava-seda-as-200-earbuds',    '/demo/earbuds-white.svg', 'هندزفری آوا صدا AS-200، سفید', 0),
    ('ava-seda-as-200-earbuds',    '/demo/earbuds-black.svg', 'هندزفری آوا صدا AS-200، مشکی', 1),
    ('voltra-power-bank-20000',    '/demo/power-bank.svg',   'پاوربانک ولترا ۲۰۰۰۰', 0),
    ('arian-mountain-graphic-tee', '/demo/graphic-tee.svg',  'تی‌شرت طرح کوهستان آرین', 0),
    ('koozegar-double-wall-glass-mug', '/demo/glass-mug.svg', 'ماگ شیشه‌ای دوجداره کوزه‌گر', 0)
) AS v(product_slug, url, alt_text, sort_order)
JOIN products p ON p.slug = v.product_slug
WHERE NOT EXISTS (
    SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.url = v.url
);

-- Pictures of one colour: linked to a variant of that colour, so the product page and the cart
-- show the colour the customer picked (the other sizes of that colour use the same picture).
UPDATE product_images i
SET variant_id = v.id
FROM (VALUES
    ('/demo/as700-black.svg',   'AS700-BLK'),
    ('/demo/as700-silver.svg',  'AS700-SLV'),
    ('/demo/tee-black.svg',     'ARN-TEE-BLK-S'),
    ('/demo/tee-white.svg',     'ARN-TEE-WHT-S'),
    ('/demo/hoodie-grey.svg',   'ARN-HOD-GRY-M'),
    ('/demo/hoodie-navy.svg',   'ARN-HOD-NVY-M'),
    ('/demo/mug-cream.svg',     'KZG-MUG-CRM'),
    ('/demo/mug-grey.svg',      'KZG-MUG-GRY'),
    ('/demo/earbuds-white.svg', 'AS200-WHT'),
    ('/demo/earbuds-black.svg', 'AS200-BLK')
) AS link(url, sku)
JOIN product_variants v ON v.sku = link.sku
WHERE i.url = link.url AND i.product_id = v.product_id AND i.variant_id IS DISTINCT FROM v.id;
