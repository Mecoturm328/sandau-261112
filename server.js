const http = require('node:http');
const fs = require('node:fs/promises');
const path = require('node:path');
const { openDatabase, isEmpty, DB_PATH } = require('./database');
const { seedDatabase } = require('./seed');

const PORT = Number(process.env.PORT) || 4173;
const HOST = process.env.HOST || '127.0.0.1';
const PUBLIC_DIR = path.join(__dirname, 'public');
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;
const MAX_RANGE_DAYS = 366;

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.ico': 'image/x-icon',
  '.woff2': 'font/woff2',
};

const db = openDatabase();
if (isEmpty(db)) {
  const result = seedDatabase(db);
  console.log(`首次启动，已生成 ${result.totalOrders} 笔演示订单。`);
}

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function round(value, digits = 2) {
  const factor = 10 ** digits;
  return Math.round((value + Number.EPSILON) * factor) / factor;
}

function pad(value) {
  return String(value).padStart(2, '0');
}

function parseIsoDate(value) {
  if (!DATE_PATTERN.test(value)) return null;
  const [year, month, day] = value.split('-').map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  if (
    date.getUTCFullYear() !== year
    || date.getUTCMonth() !== month - 1
    || date.getUTCDate() !== day
  ) {
    return null;
  }
  return date;
}

function formatIsoDate(date) {
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}`;
}

function addDays(dateString, days) {
  const date = parseIsoDate(dateString);
  date.setUTCDate(date.getUTCDate() + days);
  return formatIsoDate(date);
}

function daysBetween(start, end) {
  return Math.round((parseIsoDate(end) - parseIsoDate(start)) / 86400000);
}

function previousPeriod(start, end) {
  const days = daysBetween(start, end) + 1;
  const previousEnd = addDays(start, -1);
  const previousStart = addDays(previousEnd, -(days - 1));
  return { start: previousStart, end: previousEnd };
}

function getBounds() {
  const row = db.prepare(`
    SELECT
      MIN(substr(ordered_at, 1, 10)) AS min_date,
      MAX(substr(ordered_at, 1, 10)) AS max_date,
      COUNT(*) AS total_orders,
      COALESCE(SUM(total_amount), 0) AS total_revenue
    FROM orders
  `).get();

  const today = formatIsoDate(new Date());
  return {
    minDate: row.min_date || today,
    maxDate: row.max_date || today,
    totalOrders: row.total_orders || 0,
    totalRevenue: row.total_revenue || 0,
  };
}

function getStores() {
  return db.prepare(`
    SELECT id, name, short_name AS shortName, address, accent
    FROM stores
    ORDER BY id
  `).all();
}

function resolveQuery(searchParams, bounds) {
  const rawStart = searchParams.get('start');
  const rawEnd = searchParams.get('end');
  const rawStore = searchParams.get('store');

  let start;
  let end;

  if ((rawStart && !rawEnd) || (!rawStart && rawEnd)) {
    throw new HttpError(400, '开始日期与结束日期需要同时提供。');
  }

  if (rawStart && rawEnd) {
    if (!parseIsoDate(rawStart) || !parseIsoDate(rawEnd)) {
      throw new HttpError(400, '日期格式应为 YYYY-MM-DD。');
    }
    start = rawStart;
    end = rawEnd;
  } else {
    end = bounds.maxDate;
    const thirtyDaysAgo = addDays(end, -29);
    start = thirtyDaysAgo < bounds.minDate ? bounds.minDate : thirtyDaysAgo;
  }

  if (start > end) {
    throw new HttpError(400, '开始日期不能晚于结束日期。');
  }
  if (daysBetween(start, end) + 1 > MAX_RANGE_DAYS) {
    throw new HttpError(400, `单次查询最多支持 ${MAX_RANGE_DAYS} 天。`);
  }

  let storeId = null;
  if (rawStore) {
    if (!/^\d+$/.test(rawStore)) {
      throw new HttpError(400, '门店参数无效。');
    }
    storeId = Number(rawStore);
    const exists = db.prepare('SELECT 1 AS ok FROM stores WHERE id = ?').get(storeId);
    if (!exists) {
      throw new HttpError(400, '所选门店不存在。');
    }
  }

  return { start, end, storeId };
}

function dateFilter(start, end, storeId, alias = 'o') {
  let clause = `${alias}.ordered_at >= ? AND ${alias}.ordered_at < ?`;
  const params = [`${start} 00:00:00`, `${addDays(end, 1)} 00:00:00`];
  if (storeId) {
    clause += ` AND ${alias}.store_id = ?`;
    params.push(storeId);
  }
  return { clause, params };
}

function getKpis(start, end, storeId) {
  const filter = dateFilter(start, end, storeId);
  const row = db.prepare(`
    SELECT
      COALESCE(SUM(total_amount), 0) AS revenue,
      COUNT(*) AS orders,
      COALESCE(SUM(item_count), 0) AS items,
      COALESCE(SUM(is_member), 0) AS member_orders
    FROM orders o
    WHERE ${filter.clause}
  `).get(...filter.params);

  const orders = Number(row.orders) || 0;
  return {
    revenue: round(Number(row.revenue), 2),
    orders,
    items: Number(row.items) || 0,
    avgTicket: orders ? round(Number(row.revenue) / orders, 2) : 0,
    memberShare: orders ? round((Number(row.member_orders) / orders) * 100, 1) : 0,
    memberOrders: Number(row.member_orders) || 0,
  };
}

function percentChange(current, previous) {
  if (!previous) return null;
  return round(((current - previous) / previous) * 100, 1);
}

function getTrend(start, end, storeId) {
  const filter = dateFilter(start, end, storeId);
  const rows = db.prepare(`
    SELECT
      substr(ordered_at, 1, 10) AS date,
      ROUND(SUM(total_amount), 2) AS revenue,
      COUNT(*) AS orders,
      SUM(is_member) AS member_orders
    FROM orders o
    WHERE ${filter.clause}
    GROUP BY substr(ordered_at, 1, 10)
    ORDER BY date ASC
  `).all(...filter.params);

  const byDate = new Map(rows.map((row) => [row.date, row]));
  const totalDays = daysBetween(start, end) + 1;
  const series = [];
  for (let index = 0; index < totalDays; index += 1) {
    const date = addDays(start, index);
    const row = byDate.get(date);
    series.push({
      date,
      revenue: row ? Number(row.revenue) : 0,
      orders: row ? Number(row.orders) : 0,
      memberOrders: row ? Number(row.member_orders) : 0,
    });
  }
  return series;
}

function getCategories(start, end, storeId) {
  const filter = dateFilter(start, end, storeId);
  const rows = db.prepare(`
    SELECT
      p.category AS name,
      ROUND(SUM(oi.subtotal), 2) AS revenue,
      SUM(oi.quantity) AS quantity
    FROM order_items oi
    JOIN orders o ON o.id = oi.order_id
    JOIN products p ON p.id = oi.product_id
    WHERE ${filter.clause}
    GROUP BY p.category
    ORDER BY revenue DESC
  `).all(...filter.params);

  const total = rows.reduce((sum, row) => sum + Number(row.revenue), 0);
  return rows.map((row) => ({
    name: row.name,
    revenue: Number(row.revenue),
    quantity: Number(row.quantity),
    share: total ? round((Number(row.revenue) / total) * 100, 1) : 0,
  }));
}

function getHourly(start, end, storeId) {
  const filter = dateFilter(start, end, storeId);
  const rows = db.prepare(`
    SELECT
      CAST(substr(ordered_at, 12, 2) AS INTEGER) AS hour,
      COUNT(*) AS orders,
      ROUND(SUM(total_amount), 2) AS revenue
    FROM orders o
    WHERE ${filter.clause}
    GROUP BY hour
    ORDER BY hour
  `).all(...filter.params);

  const byHour = new Map(rows.map((row) => [row.hour, row]));
  return Array.from({ length: 13 }, (_, index) => {
    const hour = index + 8;
    const row = byHour.get(hour);
    return {
      hour,
      label: `${pad(hour)}:00`,
      orders: row ? Number(row.orders) : 0,
      revenue: row ? Number(row.revenue) : 0,
    };
  });
}

function getTopProducts(start, end, storeId) {
  const filter = dateFilter(start, end, storeId);
  return db.prepare(`
    SELECT
      p.name,
      p.category,
      SUM(oi.quantity) AS quantity,
      ROUND(SUM(oi.subtotal), 2) AS revenue
    FROM order_items oi
    JOIN orders o ON o.id = oi.order_id
    JOIN products p ON p.id = oi.product_id
    WHERE ${filter.clause}
    GROUP BY p.id, p.name, p.category
    ORDER BY quantity DESC, revenue DESC
    LIMIT 6
  `).all(...filter.params).map((row) => ({
    name: row.name,
    category: row.category,
    quantity: Number(row.quantity),
    revenue: Number(row.revenue),
  }));
}

function getStorePerformance(start, end, storeId) {
  const params = [`${start} 00:00:00`, `${addDays(end, 1)} 00:00:00`];
  let where = '';
  if (storeId) {
    where = 'WHERE s.id = ?';
    params.push(storeId);
  }

  return db.prepare(`
    SELECT
      s.id,
      s.name,
      s.short_name AS shortName,
      s.address,
      s.accent,
      COUNT(o.id) AS orders,
      COALESCE(ROUND(SUM(o.total_amount), 2), 0) AS revenue,
      COALESCE(ROUND(SUM(o.total_amount) / NULLIF(COUNT(o.id), 0), 2), 0) AS avgTicket
    FROM stores s
    LEFT JOIN orders o
      ON o.store_id = s.id
      AND o.ordered_at >= ?
      AND o.ordered_at < ?
    ${where}
    GROUP BY s.id
    ORDER BY revenue DESC, s.id ASC
  `).all(...params).map((row) => ({
    id: row.id,
    name: row.name,
    shortName: row.shortName,
    address: row.address,
    accent: row.accent,
    orders: Number(row.orders),
    revenue: Number(row.revenue),
    avgTicket: Number(row.avgTicket),
  }));
}

function getRecentOrders(start, end, storeId) {
  const filter = dateFilter(start, end, storeId);
  return db.prepare(`
    SELECT
      o.order_no AS orderNo,
      o.ordered_at AS orderedAt,
      s.short_name AS store,
      s.accent,
      o.total_amount AS total,
      o.item_count AS itemCount,
      o.payment_method AS payment,
      o.is_member AS isMember,
      (
        SELECT group_concat(p.name || ' ×' || oi.quantity, ' · ')
        FROM order_items oi
        JOIN products p ON p.id = oi.product_id
        WHERE oi.order_id = o.id
      ) AS items
    FROM orders o
    JOIN stores s ON s.id = o.store_id
    WHERE ${filter.clause}
    ORDER BY o.ordered_at DESC, o.id DESC
    LIMIT 7
  `).all(...filter.params).map((row) => ({
    orderNo: row.orderNo,
    orderedAt: row.orderedAt,
    store: row.store,
    accent: row.accent,
    total: Number(row.total),
    itemCount: Number(row.itemCount),
    payment: row.payment,
    isMember: Boolean(row.isMember),
    items: row.items || '',
  }));
}

function getDashboard(query, bounds) {
  const stores = getStores();
  const previous = previousPeriod(query.start, query.end);
  const current = getKpis(query.start, query.end, query.storeId);
  const prior = getKpis(previous.start, previous.end, query.storeId);
  const trend = getTrend(query.start, query.end, query.storeId);

  return {
    meta: {
      generatedAt: new Date().toISOString(),
      source: 'SQLite',
      totalOrdersAllTime: bounds.totalOrders,
      totalRevenueAllTime: round(bounds.totalRevenue, 2),
    },
    range: {
      start: query.start,
      end: query.end,
      days: daysBetween(query.start, query.end) + 1,
      minDate: bounds.minDate,
      maxDate: bounds.maxDate,
      previous,
    },
    storeId: query.storeId,
    stores,
    kpis: {
      ...current,
      previous: {
        revenue: prior.revenue,
        orders: prior.orders,
        avgTicket: prior.avgTicket,
        memberShare: prior.memberShare,
      },
      change: {
        revenue: percentChange(current.revenue, prior.revenue),
        orders: percentChange(current.orders, prior.orders),
        avgTicket: percentChange(current.avgTicket, prior.avgTicket),
        memberShare: prior.orders
          ? round(current.memberShare - prior.memberShare, 1)
          : null,
      },
    },
    trend,
    categories: getCategories(query.start, query.end, query.storeId),
    hourly: getHourly(query.start, query.end, query.storeId),
    topProducts: getTopProducts(query.start, query.end, query.storeId),
    storePerformance: getStorePerformance(query.start, query.end, query.storeId),
    recentOrders: getRecentOrders(query.start, query.end, query.storeId),
  };
}

function csvCell(value) {
  const text = value === null || value === undefined ? '' : String(value);
  return `"${text.replaceAll('"', '""')}"`;
}

function getExportRows(start, end, storeId) {
  const filter = dateFilter(start, end, storeId);
  return db.prepare(`
    SELECT
      o.order_no AS orderNo,
      o.ordered_at AS orderedAt,
      s.name AS store,
      (
        SELECT group_concat(p.name || ' ×' || oi.quantity, ' / ')
        FROM order_items oi
        JOIN products p ON p.id = oi.product_id
        WHERE oi.order_id = o.id
      ) AS items,
      o.item_count AS itemCount,
      o.total_amount AS total,
      o.payment_method AS payment,
      o.is_member AS isMember
    FROM orders o
    JOIN stores s ON s.id = o.store_id
    WHERE ${filter.clause}
    ORDER BY o.ordered_at DESC, o.id DESC
  `).all(...filter.params);
}

function applySecurityHeaders(response) {
  response.setHeader('X-Content-Type-Options', 'nosniff');
  response.setHeader('X-Frame-Options', 'DENY');
  response.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  response.setHeader(
    'Content-Security-Policy',
    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; font-src 'self' data:; base-uri 'none'; form-action 'self'; frame-ancestors 'none'",
  );
  response.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');
}

function sendJson(response, status, payload) {
  const body = JSON.stringify(payload);
  response.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store',
    'Content-Length': Buffer.byteLength(body),
  });
  response.end(body);
}

async function serveStatic(requestPath, response) {
  let decoded;
  try {
    decoded = decodeURIComponent(requestPath);
  } catch {
    throw new HttpError(400, '无效的请求路径。');
  }

  const relativePath = decoded === '/' ? 'index.html' : decoded.replace(/^\/+/, '');
  const filePath = path.join(PUBLIC_DIR, relativePath);
  if (!filePath.startsWith(`${PUBLIC_DIR}${path.sep}`)) {
    throw new HttpError(403, '禁止访问该路径。');
  }

  let stats;
  try {
    stats = await fs.stat(filePath);
  } catch {
    throw new HttpError(404, '页面不存在。');
  }
  if (!stats.isFile()) {
    throw new HttpError(404, '页面不存在。');
  }

  const extension = path.extname(filePath).toLowerCase();
  const body = await fs.readFile(filePath);
  response.writeHead(200, {
    'Content-Type': MIME_TYPES[extension] || 'application/octet-stream',
    'Content-Length': body.length,
    'Cache-Control': extension === '.html' ? 'no-cache' : 'public, max-age=300',
  });
  response.end(body);
}

async function handleRequest(request, response) {
  const startedAt = Date.now();
  const url = new URL(request.url, `http://${request.headers.host || 'localhost'}`);

  if (request.method !== 'GET' && request.method !== 'HEAD') {
    response.setHeader('Allow', 'GET, HEAD');
    throw new HttpError(405, '仅支持 GET 请求。');
  }

  const bounds = getBounds();

  if (url.pathname === '/api/health') {
    sendJson(response, 200, {
      status: 'ok',
      database: 'sqlite',
      orders: bounds.totalOrders,
      dataRange: { start: bounds.minDate, end: bounds.maxDate },
    });
    return;
  }

  if (url.pathname === '/api/dashboard') {
    const query = resolveQuery(url.searchParams, bounds);
    sendJson(response, 200, getDashboard(query, bounds));
    return;
  }

  if (url.pathname === '/api/orders/export') {
    const query = resolveQuery(url.searchParams, bounds);
    const rows = getExportRows(query.start, query.end, query.storeId);
    const header = ['订单号', '下单时间', '门店', '商品明细', '件数', '金额（元）', '支付方式', '会员订单'];
    const lines = ['\uFEFF' + header.map(csvCell).join(',')];
    for (const row of rows) {
      lines.push([
        row.orderNo,
        row.orderedAt,
        row.store,
        row.items,
        row.itemCount,
        Number(row.total).toFixed(2),
        row.payment,
        row.isMember ? '是' : '否',
      ].map(csvCell).join(','));
    }

    const body = `${lines.join('\r\n')}\r\n`;
    response.writeHead(200, {
      'Content-Type': 'text/csv; charset=utf-8',
      'Content-Disposition': `attachment; filename="mori-orders-${query.start}-${query.end}.csv"`,
      'Cache-Control': 'no-store',
      'Content-Length': Buffer.byteLength(body),
    });
    response.end(body);
    return;
  }

  if (url.pathname.startsWith('/api/')) {
    throw new HttpError(404, '接口不存在。');
  }

  await serveStatic(url.pathname, response);
  const elapsed = Date.now() - startedAt;
  if (url.pathname.startsWith('/api/')) {
    console.log(`[api] ${url.pathname} ${elapsed}ms`);
  }
}

const server = http.createServer(async (request, response) => {
  applySecurityHeaders(response);
  try {
    await handleRequest(request, response);
  } catch (error) {
    const status = error instanceof HttpError ? error.status : 500;
    if (status === 500) {
      console.error('[server]', error);
    }
    if (!response.headersSent) {
      sendJson(response, status, {
        error: status === 500 ? '服务器暂时开小差了，请稍后再试。' : error.message,
      });
    } else {
      response.end();
    }
  }
});

server.listen(PORT, HOST, () => {
  console.log(`MORI 咖啡数据台已启动：http://${HOST}:${PORT}`);
  console.log(`SQLite：${DB_PATH}`);
});

function shutdown(signal) {
  console.log(`\n收到 ${signal}，正在关闭服务…`);
  server.close(() => {
    db.close();
    process.exit(0);
  });
  setTimeout(() => process.exit(1), 5000).unref();
}

process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));
