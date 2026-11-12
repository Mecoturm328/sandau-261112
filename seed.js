const { openDatabase, isEmpty, DB_PATH } = require('./database');

const STORES = [
  { id: 1, name: '梧桐里店', short_name: '梧桐里', code: 'W', address: '徐汇区 · 武康路 118 号', accent: '#d97745', base: 42 },
  { id: 2, name: '苏河店', short_name: '苏河', code: 'S', address: '静安区 · 苏河湾万象天地', accent: '#8ba58e', base: 35 },
  { id: 3, name: '江湾店', short_name: '江湾', code: 'J', address: '杨浦区 · 大学路 88 号', accent: '#d3ad63', base: 29 },
];

const PRODUCTS = [
  { id: 1, name: '云顶拿铁', category: '咖啡', price: 32, weight: 17 },
  { id: 2, name: '琥珀美式', category: '咖啡', price: 28, weight: 13 },
  { id: 3, name: '澳白', category: '咖啡', price: 35, weight: 10 },
  { id: 4, name: '青柠冷萃', category: '咖啡', price: 34, weight: 7 },
  { id: 5, name: '耶加雪菲手冲', category: '咖啡', price: 45, weight: 5 },
  { id: 6, name: '宇治抹茶拿铁', category: '非咖啡', price: 36, weight: 8 },
  { id: 7, name: '暖可可', category: '非咖啡', price: 32, weight: 4 },
  { id: 8, name: '青提气泡水', category: '非咖啡', price: 28, weight: 4 },
  { id: 9, name: '黄油可颂', category: '烘焙', price: 22, weight: 9 },
  { id: 10, name: '肉桂卷', category: '烘焙', price: 24, weight: 5 },
  { id: 11, name: '巴斯克芝士', category: '烘焙', price: 32, weight: 6 },
  { id: 12, name: '原味贝果', category: '烘焙', price: 20, weight: 5 },
  { id: 13, name: '牛油果酸种吐司', category: '轻食', price: 42, weight: 5 },
  { id: 14, name: '香草鸡肉帕尼尼', category: '轻食', price: 38, weight: 5 },
];

const HOUR_WEIGHTS = [
  [8, 4], [9, 11], [10, 8], [11, 8], [12, 9], [13, 10], [14, 12],
  [15, 12], [16, 9], [17, 6], [18, 5], [19, 4], [20, 2],
];

const PAYMENTS = [
  ['微信支付', 50],
  ['支付宝', 34],
  ['会员储值', 10],
  ['银行卡', 6],
];

const CATEGORY_STORE_FACTOR = {
  1: { 咖啡: 1.12, 非咖啡: 0.95, 烘焙: 0.95, 轻食: 1 },
  2: { 咖啡: 1, 非咖啡: 1, 烘焙: 1.22, 轻食: 1.12 },
  3: { 咖啡: 0.92, 非咖啡: 1.22, 烘焙: 1, 轻食: 0.88 },
};

function createRandom(seed) {
  let state = seed >>> 0;
  return function random() {
    state += 0x6d2b79f5;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function pickWeighted(random, entries) {
  const total = entries.reduce((sum, entry) => sum + entry[1], 0);
  let cursor = random() * total;
  for (const [value, weight] of entries) {
    cursor -= weight;
    if (cursor <= 0) return value;
  }
  return entries[entries.length - 1][0];
}

function pad(value, length = 2) {
  return String(value).padStart(length, '0');
}

function formatDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function addDays(date, days) {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}

function weightedOrderSize(random) {
  const roll = random();
  if (roll < 0.46) return 1;
  if (roll < 0.86) return 2;
  return 3;
}

function buildDailyCount(random, store, date, progress) {
  const weekday = date.getDay();
  let factor = 1 + progress * 0.24;
  if (weekday === 0 || weekday === 6) factor *= 1.24;
  if (weekday === 5) factor *= 1.12;
  if (weekday === 1) factor *= 0.88;
  // 月初和发薪日前后会有一小波消费高峰。
  if (date.getDate() >= 8 && date.getDate() <= 12) factor *= 1.08;
  factor *= 0.84 + random() * 0.3;
  return Math.max(8, Math.round(store.base * factor));
}

function seedDatabase(db, { force = false } = {}) {
  if (!force && !isEmpty(db)) {
    return { skipped: true };
  }

  const random = createRandom(20261112);
  const today = new Date();
  today.setHours(12, 0, 0, 0);
  const endDate = addDays(today, -1);
  const startDate = addDays(endDate, -119);

  const insertStore = db.prepare(
    'INSERT INTO stores (id, name, short_name, address, accent) VALUES (?, ?, ?, ?, ?)',
  );
  const insertProduct = db.prepare(
    'INSERT INTO products (id, name, category, price, sort_order) VALUES (?, ?, ?, ?, ?)',
  );
  const insertOrder = db.prepare(`
    INSERT INTO orders (order_no, store_id, ordered_at, total_amount, item_count, payment_method, is_member)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `);
  const insertItem = db.prepare(`
    INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal)
    VALUES (?, ?, ?, ?, ?)
  `);

  db.exec('BEGIN IMMEDIATE');
  try {
    db.exec('DELETE FROM order_items; DELETE FROM orders; DELETE FROM products; DELETE FROM stores;');

    for (const store of STORES) {
      insertStore.run(store.id, store.name, store.short_name, store.address, store.accent);
    }
    PRODUCTS.forEach((product, index) => {
      insertProduct.run(product.id, product.name, product.category, product.price, index);
    });

    const dayCount = 120;
    let totalOrders = 0;
    const storeDayCounters = new Map();

    for (let dayOffset = 0; dayOffset < dayCount; dayOffset += 1) {
      const date = addDays(startDate, dayOffset);
      const dateKey = formatDate(date);
      const progress = dayOffset / Math.max(1, dayCount - 1);

      for (const store of STORES) {
        const count = buildDailyCount(random, store, date, progress);
        for (let orderIndex = 0; orderIndex < count; orderIndex += 1) {
          const hour = pickWeighted(random, HOUR_WEIGHTS);
          const minute = Math.floor(random() * 60);
          const second = Math.floor(random() * 60);
          const timestamp = `${dateKey} ${pad(hour)}:${pad(minute)}:${pad(second)}`;
          const counterKey = `${store.id}-${dateKey}`;
          const dailySequence = (storeDayCounters.get(counterKey) || 0) + 1;
          storeDayCounters.set(counterKey, dailySequence);
          const orderNo = `MR-${store.code}-${dateKey.replaceAll('-', '')}-${pad(dailySequence, 3)}`;

          const isMember = random() < 0.37;
          const paymentMethod = isMember && random() < 0.32
            ? '会员储值'
            : pickWeighted(random, PAYMENTS);

          const quantities = [];
          const itemCount = Math.min(weightedOrderSize(random), PRODUCTS.length);
          const excluded = new Set();
          for (let itemIndex = 0; itemIndex < itemCount; itemIndex += 1) {
            const factors = CATEGORY_STORE_FACTOR[store.id];
            const candidates = PRODUCTS
              .filter((product) => !excluded.has(product.id))
              .map((product) => [
                product,
                product.weight * (factors[product.category] || 1),
              ]);
            const product = pickWeighted(random, candidates);
            excluded.add(product.id);
            const quantity = product.category === '咖啡' && random() < 0.12 ? 2 : 1;
            quantities.push({ product, quantity });
          }

          let total = 0;
          let units = 0;
          for (const { product, quantity } of quantities) {
            total += product.price * quantity;
            units += quantity;
          }

          const orderResult = insertOrder.run(
            orderNo,
            store.id,
            timestamp,
            total,
            units,
            paymentMethod,
            isMember ? 1 : 0,
          );
          const orderId = Number(orderResult.lastInsertRowid);
          for (const { product, quantity } of quantities) {
            insertItem.run(orderId, product.id, quantity, product.price, product.price * quantity);
          }
          totalOrders += 1;
        }
      }
    }

    db.exec('COMMIT');
    return {
      skipped: false,
      totalOrders,
      startDate: formatDate(startDate),
      endDate: formatDate(endDate),
    };
  } catch (error) {
    db.exec('ROLLBACK');
    throw error;
  }
}

if (require.main === module) {
  const db = openDatabase();
  const result = seedDatabase(db, { force: true });
  db.close();
  console.log(`数据库已重建：${DB_PATH}`);
  console.log(`已生成 ${result.totalOrders} 笔订单，日期范围 ${result.startDate} 至 ${result.endDate}`);
}

module.exports = { seedDatabase };
