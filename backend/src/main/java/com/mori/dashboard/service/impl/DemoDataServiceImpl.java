package com.mori.dashboard.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mori.dashboard.dao.OrderDao;
import com.mori.dashboard.dao.OrderItemDao;
import com.mori.dashboard.dao.ProductDao;
import com.mori.dashboard.dao.StoreDao;
import com.mori.dashboard.entity.Order;
import com.mori.dashboard.entity.OrderItem;
import com.mori.dashboard.entity.Product;
import com.mori.dashboard.entity.Store;
import com.mori.dashboard.service.DemoDataService;

/**
 * 演示数据生成：用固定随机种子生成最近 120 天、3 家门店的订单。
 */
@Service
public class DemoDataServiceImpl implements DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataServiceImpl.class);

    private static final long RANDOM_SEED = 20261112L;
    private static final int DAY_COUNT = 120;
    private static final DateTimeFormatter COMPACT_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private static final List<StoreSeed> STORES = List.of(
            new StoreSeed(new Store(1L, "梧桐里店", "梧桐里", "徐汇区 · 武康路 118 号", "#d97745"), "W", 42),
            new StoreSeed(new Store(2L, "苏河店", "苏河", "静安区 · 苏河湾万象天地", "#8ba58e"), "S", 35),
            new StoreSeed(new Store(3L, "江湾店", "江湾", "杨浦区 · 大学路 88 号", "#d3ad63"), "J", 29)
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

    /** {小时, 权重} */
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

    private static final Map<Long, Map<String, Double>> CATEGORY_STORE_FACTOR = Map.of(
            1L, Map.of("咖啡", 1.12, "非咖啡", 0.95, "烘焙", 0.95, "轻食", 1.0),
            2L, Map.of("咖啡", 1.0, "非咖啡", 1.0, "烘焙", 1.22, "轻食", 1.12),
            3L, Map.of("咖啡", 0.92, "非咖啡", 1.22, "烘焙", 1.0, "轻食", 0.88)
    );

    private final StoreDao storeDao;
    private final ProductDao productDao;
    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;

    public DemoDataServiceImpl(
            StoreDao storeDao,
            ProductDao productDao,
            OrderDao orderDao,
            OrderItemDao orderItemDao
    ) {
        this.storeDao = storeDao;
        this.productDao = productDao;
        this.orderDao = orderDao;
        this.orderItemDao = orderItemDao;
    }

    @Override
    @Transactional
    public int initIfEmpty() {
        if (orderDao.count() > 0) {
            return 0;
        }
        log.info("SQLite 中没有订单数据，开始生成 {} 天演示订单…", DAY_COUNT);
        long startedAt = System.currentTimeMillis();

        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(DAY_COUNT - 1L);
        GeneratedData data = generate(startDate);

        orderItemDao.deleteAll();
        orderDao.deleteAll();
        productDao.deleteAll();
        storeDao.deleteAll();

        storeDao.batchInsert(STORES.stream().map(StoreSeed::store).toList());
        productDao.batchInsert(PRODUCTS.stream().map(ProductSeed::toEntity).toList());
        orderDao.batchInsert(data.orders());
        orderItemDao.batchInsert(data.items());

        log.info(
                "演示数据生成完成：{} 笔订单，日期 {} 至 {}，耗时 {} ms",
                data.orders().size(),
                startDate,
                endDate,
                System.currentTimeMillis() - startedAt
        );
        return data.orders().size();
    }

    private GeneratedData generate(LocalDate startDate) {
        Random random = new Random(RANDOM_SEED);
        List<Order> orders = new ArrayList<>();
        List<OrderItem> items = new ArrayList<>();
        Map<String, Integer> dailySequence = new HashMap<>();
        long orderId = 1;

        for (int offset = 0; offset < DAY_COUNT; offset += 1) {
            LocalDate date = startDate.plusDays(offset);
            double progress = offset / (double) (DAY_COUNT - 1);

            for (StoreSeed storeSeed : STORES) {
                Store store = storeSeed.store();
                int orderCount = buildDailyCount(random, storeSeed, date, progress);
                for (int index = 0; index < orderCount; index += 1) {
                    int hour = pickHour(random);
                    int minute = random.nextInt(60);
                    int second = random.nextInt(60);

                    int sequence = dailySequence.merge(store.getId() + "-" + date, 1, Integer::sum);
                    String orderNo = "MR-" + storeSeed.code() + "-" + date.format(COMPACT_DATE)
                            + "-" + String.format(Locale.ROOT, "%03d", sequence);

                    boolean member = random.nextDouble() < 0.37;
                    String payment = member && random.nextDouble() < 0.32
                            ? "会员储值"
                            : pickWeighted(random, PAYMENTS);

                    int targetItems = weightedOrderSize(random);
                    Set<Integer> excluded = new HashSet<>();
                    int total = 0;
                    int units = 0;
                    List<OrderItem> orderItems = new ArrayList<>();
                    for (int itemIndex = 0; itemIndex < targetItems; itemIndex += 1) {
                        ProductSeed product = pickProduct(random, store.getId(), excluded);
                        excluded.add(product.id());
                        int quantity = product.category().equals("咖啡") && random.nextDouble() < 0.12 ? 2 : 1;
                        int subtotal = product.price() * quantity;
                        total += subtotal;
                        units += quantity;
                        orderItems.add(new OrderItem(
                                null,
                                orderId,
                                (long) product.id(),
                                quantity,
                                (double) product.price(),
                                (double) subtotal
                        ));
                    }

                    orders.add(new Order(
                            orderId,
                            orderNo,
                            store.getId(),
                            date.atTime(hour, minute, second),
                            (double) total,
                            units,
                            payment,
                            member
                    ));
                    items.addAll(orderItems);
                    orderId += 1;
                }
            }
        }
        return new GeneratedData(orders, items);
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
        return Math.max(8, (int) Math.round(store.baseOrders() * factor));
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

    private static ProductSeed pickProduct(Random random, long storeId, Set<Integer> excluded) {
        Map<String, Double> factors = CATEGORY_STORE_FACTOR.get(storeId);
        List<WeightedValue<ProductSeed>> candidates = new ArrayList<>();
        for (ProductSeed product : PRODUCTS) {
            if (!excluded.contains(product.id())) {
                double weight = product.weight() * factors.getOrDefault(product.category(), 1.0);
                candidates.add(new WeightedValue<>(product, weight));
            }
        }
        return pickWeighted(random, candidates);
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

    private static int pickHour(Random random) {
        int total = 0;
        for (int[] value : HOUR_WEIGHTS) {
            total += value[1];
        }
        int cursor = random.nextInt(total);
        for (int[] value : HOUR_WEIGHTS) {
            cursor -= value[1];
            if (cursor < 0) {
                return value[0];
            }
        }
        return HOUR_WEIGHTS[HOUR_WEIGHTS.length - 1][0];
    }

    private record StoreSeed(Store store, String code, int baseOrders) {
    }

    private record ProductSeed(int id, String name, String category, int price, int weight) {
        Product toEntity() {
            return new Product((long) id, name, category, (double) price, id);
        }
    }

    private record WeightedValue<T>(T value, double weight) {
    }

    private record GeneratedData(List<Order> orders, List<OrderItem> items) {
    }
}
