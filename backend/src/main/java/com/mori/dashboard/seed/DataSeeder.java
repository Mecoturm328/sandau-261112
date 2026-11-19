package com.mori.dashboard.seed;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final List<StoreSeed> STORES = List.of(
            new StoreSeed(1, "梧桐里店", "梧桐里", "徐汇区 · 武康路 118 号", "#d97745", 42),
            new StoreSeed(2, "苏河店", "苏河", "静安区 · 苏河湾万象天地", "#8ba58e", 35),
            new StoreSeed(3, "江湾店", "江湾", "杨浦区 · 大学路 88 号", "#d3ad63", 29)
    );

    private static final List<ProductSeed> PRODUCTS = List.of(
            new ProductSeed(1, "云顶拿铁", "咖啡", 32, 17),
            new ProductSeed(2, "琥珀美式", "咖啡", 28, 13),
            new ProductSeed(3, "澳白", "咖啡", 35, 10),
            new ProductSeed(4, "青柠冷萃", "咖啡", 34, 7),
            new ProductSeed(5, "耶加雪菲手冲", "咖啡", 45, 5),
            new ProductSeed(6, "宇治抹茶拿铁", "非咖啡", 36, 8),
            new ProductSeed(7, "暖可可", "非咖啡", 32, 4),
            new ProductSeed(8, "青提气泡水", "非咖啡", 28, 4),
            new ProductSeed(9, "黄油可颂", "烘焙", 22, 9),
            new ProductSeed(10, "肉桂卷", "烘焙", 24, 5),
            new ProductSeed(11, "巴斯克芝士", "烘焙", 32, 6),
            new ProductSeed(12, "原味贝果", "烘焙", 20, 5),
            new ProductSeed(13, "牛油果酸种吐司", "轻食", 42, 5),
            new ProductSeed(14, "香草鸡肉帕尼尼", "轻食", 38, 5)
    );

    private static final int[][] HOUR_WEIGHTS = {
            {8, 4}, {9, 11}, {10, 8}, {11, 8}, {12, 9}, {13, 10}, {14, 12},
            {15, 12}, {16, 9}, {17, 6}, {18, 5}, {19, 4}, {20, 2}
    };

    private static final List<WeightedValue<String>> PAYMENTS = List.of(
            new WeightedValue<>("微信支付", 50),
            new WeightedValue<>("支付宝", 34),
            new WeightedValue<>("会员储值", 10),
            new WeightedValue<>("银行卡", 6)
    );

    private static final Map<Integer, Map<String, Double>> CATEGORY_STORE_FACTOR = Map.of(
            1, Map.of("咖啡", 1.12, "非咖啡", 0.95, "烘焙", 0.95, "轻食", 1.0),
            2, Map.of("咖啡", 1.0, "非咖啡", 1.0, "烘焙", 1.22, "轻食", 1.12),
            3, Map.of("咖啡", 0.92, "非咖啡", 1.22, "烘焙", 1.0, "轻食", 0.88)
    );

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactionTemplate;
    private final boolean seedEnabled;

    public DataSeeder(
            JdbcTemplate jdbc,
            TransactionTemplate transactionTemplate,
            @Value("${app.seed.enabled:true}") boolean seedEnabled
    ) {
        this.jdbc = jdbc;
        this.transactionTemplate = transactionTemplate;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }
        Long orders = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        if (orders != null && orders > 0) {
            return;
        }
        log.info("SQLite 中没有订单数据，开始生成 120 天演示订单…");
        long startedAt = System.currentTimeMillis();
        SeedResult result = generate();
        transactionTemplate.executeWithoutResult(status -> insertAll(result));
        log.info(
                "演示数据生成完成：{} 笔订单，日期 {} 至 {}，耗时 {} ms",
                result.orders().size(),
                result.startDate(),
                result.endDate(),
                System.currentTimeMillis() - startedAt
        );
    }

    private SeedResult generate() {
        Random random = new Random(20261112L);
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(119);
        int dayCount = 120;

        List<OrderSeed> orders = new ArrayList<>();
        List<ItemSeed> items = new ArrayList<>();
        Map<String, Integer> dailySequence = new HashMap<>();
        long orderId = 1;

        for (int offset = 0; offset < dayCount; offset += 1) {
            LocalDate date = startDate.plusDays(offset);
            double progress = offset / (double) (dayCount - 1);

            for (StoreSeed store : STORES) {
                int orderCount = buildDailyCount(random, store, date, progress);
                for (int index = 0; index < orderCount; index += 1) {
                    int hour = pickWeighted(random, HOUR_WEIGHTS);
                    int minute = random.nextInt(60);
                    int second = random.nextInt(60);
                    String orderedAt = String.format(
                            Locale.ROOT,
                            "%s %02d:%02d:%02d",
                            date.format(DATE_FORMAT),
                            hour,
                            minute,
                            second
                    );
                    String counterKey = store.id() + "-" + date;
                    int sequence = dailySequence.merge(counterKey, 1, Integer::sum);
                    String orderNo = "MR-" + store.code() + "-"
                            + date.format(DATE_FORMAT).replace("-", "")
                            + "-" + String.format(Locale.ROOT, "%03d", sequence);

                    boolean member = random.nextDouble() < 0.37;
                    String payment = member && random.nextDouble() < 0.32
                            ? "会员储值"
                            : pickWeighted(random, PAYMENTS);

                    int targetItems = weightedOrderSize(random);
                    Set<Integer> excluded = new HashSet<>();
                    List<SelectedItem> selected = new ArrayList<>();
                    for (int itemIndex = 0; itemIndex < targetItems; itemIndex += 1) {
                        ProductSeed product = pickProduct(random, store.id(), excluded);
                        excluded.add(product.id());
                        int quantity = product.category().equals("咖啡") && random.nextDouble() < 0.12 ? 2 : 1;
                        selected.add(new SelectedItem(product, quantity));
                    }

                    int total = 0;
                    int units = 0;
                    for (SelectedItem selectedItem : selected) {
                        total += selectedItem.product().price() * selectedItem.quantity();
                        units += selectedItem.quantity();
                    }

                    orders.add(new OrderSeed(
                            orderId,
                            orderNo,
                            store.id(),
                            orderedAt,
                            total,
                            units,
                            payment,
                            member
                    ));
                    for (SelectedItem selectedItem : selected) {
                        items.add(new ItemSeed(
                                orderId,
                                selectedItem.product().id(),
                                selectedItem.quantity(),
                                selectedItem.product().price(),
                                selectedItem.product().price() * selectedItem.quantity()
                        ));
                    }
                    orderId += 1;
                }
            }
        }

        return new SeedResult(startDate, endDate, orders, items);
    }

    private void insertAll(SeedResult result) {
        jdbc.update("DELETE FROM order_items");
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM products");
        jdbc.update("DELETE FROM stores");

        jdbc.batchUpdate(
                "INSERT INTO stores (id, name, short_name, address, accent) VALUES (?, ?, ?, ?, ?)",
                STORES,
                100,
                (statement, store) -> {
                    statement.setInt(1, store.id());
                    statement.setString(2, store.name());
                    statement.setString(3, store.shortName());
                    statement.setString(4, store.address());
                    statement.setString(5, store.accent());
                }
        );

        jdbc.batchUpdate(
                "INSERT INTO products (id, name, category, price, sort_order) VALUES (?, ?, ?, ?, ?)",
                PRODUCTS,
                100,
                (statement, product) -> {
                    statement.setInt(1, product.id());
                    statement.setString(2, product.name());
                    statement.setString(3, product.category());
                    statement.setDouble(4, product.price());
                    statement.setInt(5, product.id());
                }
        );

        jdbc.batchUpdate(
                """
                INSERT INTO orders
                  (id, order_no, store_id, ordered_at, total_amount, item_count, payment_method, is_member)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                result.orders(),
                500,
                (statement, order) -> {
                    statement.setLong(1, order.id());
                    statement.setString(2, order.orderNo());
                    statement.setInt(3, order.storeId());
                    statement.setString(4, order.orderedAt());
                    statement.setDouble(5, order.total());
                    statement.setInt(6, order.itemCount());
                    statement.setString(7, order.payment());
                    statement.setInt(8, order.isMember() ? 1 : 0);
                }
        );

        jdbc.batchUpdate(
                """
                INSERT INTO order_items
                  (order_id, product_id, quantity, unit_price, subtotal)
                VALUES (?, ?, ?, ?, ?)
                """,
                result.items(),
                500,
                (statement, item) -> {
                    statement.setLong(1, item.orderId());
                    statement.setInt(2, item.productId());
                    statement.setInt(3, item.quantity());
                    statement.setDouble(4, item.unitPrice());
                    statement.setDouble(5, item.subtotal());
                }
        );
    }

    private static int buildDailyCount(Random random, StoreSeed store, LocalDate date, double progress) {
        double factor = 1 + progress * 0.24;
        DayOfWeek weekday = date.getDayOfWeek();
        if (weekday == DayOfWeek.SATURDAY || weekday == DayOfWeek.SUNDAY) {
            factor *= 1.24;
        }
        if (weekday == DayOfWeek.FRIDAY) {
            factor *= 1.12;
        }
        if (weekday == DayOfWeek.MONDAY) {
            factor *= 0.88;
        }
        if (date.getDayOfMonth() >= 8 && date.getDayOfMonth() <= 12) {
            factor *= 1.08;
        }
        factor *= 0.84 + random.nextDouble() * 0.3;
        return Math.max(8, (int) Math.round(store.base() * factor));
    }

    private static int weightedOrderSize(Random random) {
        double roll = random.nextDouble();
        if (roll < 0.46) {
            return 1;
        }
        if (roll < 0.86) {
            return 2;
        }
        return 3;
    }

    private static ProductSeed pickProduct(Random random, int storeId, Set<Integer> excluded) {
        Map<String, Double> factors = CATEGORY_STORE_FACTOR.get(storeId);
        double totalWeight = 0;
        List<WeightedProduct> candidates = new ArrayList<>();
        for (ProductSeed product : PRODUCTS) {
            if (excluded.contains(product.id())) {
                continue;
            }
            double weight = product.weight() * factors.getOrDefault(product.category(), 1.0);
            candidates.add(new WeightedProduct(product, weight));
            totalWeight += weight;
        }

        double cursor = random.nextDouble() * totalWeight;
        for (WeightedProduct candidate : candidates) {
            cursor -= candidate.weight();
            if (cursor <= 0) {
                return candidate.product();
            }
        }
        return candidates.get(candidates.size() - 1).product();
    }

    private static <T> T pickWeighted(Random random, List<WeightedValue<T>> values) {
        double total = values.stream().mapToDouble(WeightedValue::weight).sum();
        double cursor = random.nextDouble() * total;
        for (WeightedValue<T> value : values) {
            cursor -= value.weight();
            if (cursor <= 0) {
                return value.value();
            }
        }
        return values.get(values.size() - 1).value();
    }

    private static int pickWeighted(Random random, int[][] values) {
        int total = 0;
        for (int[] value : values) {
            total += value[1];
        }
        int cursor = random.nextInt(total);
        for (int[] value : values) {
            cursor -= value[1];
            if (cursor < 0) {
                return value[0];
            }
        }
        return values[values.length - 1][0];
    }

    private record StoreSeed(
            int id,
            String name,
            String shortName,
            String address,
            String accent,
            int base
    ) {
        String code() {
            return switch (id) {
                case 1 -> "W";
                case 2 -> "S";
                default -> "J";
            };
        }
    }

    private record ProductSeed(int id, String name, String category, int price, int weight) {
    }

    private record WeightedValue<T>(T value, double weight) {
    }

    private record WeightedProduct(ProductSeed product, double weight) {
    }

    private record SelectedItem(ProductSeed product, int quantity) {
    }

    private record OrderSeed(
            long id,
            String orderNo,
            int storeId,
            String orderedAt,
            int total,
            int itemCount,
            String payment,
            boolean isMember
    ) {
    }

    private record ItemSeed(
            long orderId,
            int productId,
            int quantity,
            int unitPrice,
            int subtotal
    ) {
    }

    private record SeedResult(
            LocalDate startDate,
            LocalDate endDate,
            List<OrderSeed> orders,
            List<ItemSeed> items
    ) {
    }
}
