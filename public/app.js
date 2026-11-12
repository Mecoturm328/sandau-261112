(() => {
  'use strict';

  const CATEGORY_COLORS = {
    咖啡: '#cf6d3f',
    烘焙: '#c7a25e',
    非咖啡: '#8ba58e',
    轻食: '#718a93',
  };
  const FALLBACK_COLORS = ['#cf6d3f', '#c7a25e', '#8ba58e', '#718a93', '#a17a67'];
  const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  const PRESETS = [7, 30, 90];
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');

  const numberFormatter = new Intl.NumberFormat('zh-CN');
  const decimalFormatter = new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  });
  const timeFormatter = new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });

  const byId = (id) => document.getElementById(id);
  const elements = {
    loadingBar: byId('loadingBar'),
    sidebar: byId('sidebar'),
    sidebarScrim: byId('sidebarScrim'),
    sidebarClose: byId('sidebarClose'),
    menuButton: byId('menuButton'),
    refreshButton: byId('refreshButton'),
    updatedAt: byId('updatedAt'),
    heroRange: byId('heroRange'),
    heroDays: byId('heroDays'),
    startDate: byId('startDate'),
    endDate: byId('endDate'),
    applyFilter: byId('applyFilter'),
    storeSelect: byId('storeSelect'),
    storeCountLabel: byId('storeCountLabel'),
    sideStoreList: byId('sideStoreList'),
    dbMeta: byId('dbMeta'),
    kpiRevenue: byId('kpiRevenue'),
    kpiOrders: byId('kpiOrders'),
    kpiAvgTicket: byId('kpiAvgTicket'),
    kpiMemberShare: byId('kpiMemberShare'),
    changeRevenue: byId('changeRevenue'),
    changeOrders: byId('changeOrders'),
    changeAvgTicket: byId('changeAvgTicket'),
    changeMemberShare: byId('changeMemberShare'),
    sparkRevenue: byId('sparkRevenue'),
    sparkOrders: byId('sparkOrders'),
    sparkAvgTicket: byId('sparkAvgTicket'),
    sparkMemberShare: byId('sparkMemberShare'),
    dailyAvg: byId('dailyAvg'),
    trendChart: byId('trendChart'),
    trendTooltip: byId('trendTooltip'),
    categoryDonut: byId('categoryDonut'),
    categoryLegend: byId('categoryLegend'),
    hourlyChart: byId('hourlyChart'),
    peakBadge: byId('peakBadge'),
    storeRanking: byId('storeRanking'),
    storeScope: byId('storeScope'),
    productRanking: byId('productRanking'),
    ordersBody: byId('ordersBody'),
    ordersTable: document.querySelector('.orders-table'),
    ordersEmpty: byId('ordersEmpty'),
    exportButton: byId('exportButton'),
    footerRows: byId('footerRows'),
    toast: byId('toast'),
    toastMessage: byId('toastMessage'),
  };

  const state = {
    data: null,
    filters: {
      start: '',
      end: '',
      storeId: '',
      preset: null,
    },
    loading: false,
    hasLoaded: false,
    controller: null,
    toastTimer: null,
    retriedDefault: false,
  };

  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, (character) => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
    }[character]));
  }

  function safeColor(value, fallback = '#8ba58e') {
    return /^#[0-9a-f]{6}$/i.test(String(value || '')) ? value : fallback;
  }

  function pad(value) {
    return String(value).padStart(2, '0');
  }

  function formatNumber(value) {
    return numberFormatter.format(Math.round(Number(value) || 0));
  }

  function formatDecimal(value) {
    return decimalFormatter.format(Number(value) || 0);
  }

  function formatMoney(value) {
    const number = Number(value) || 0;
    const digits = Number.isInteger(number) ? 0 : 2;
    return `¥${number.toLocaleString('zh-CN', {
      minimumFractionDigits: digits,
      maximumFractionDigits: 2,
    })}`;
  }

  function formatCompactMoney(value) {
    const number = Number(value) || 0;
    if (number >= 10000) {
      const digits = number >= 100000 ? 0 : 1;
      return `¥${(number / 10000).toFixed(digits)}万`;
    }
    if (number >= 1000) return `¥${(number / 1000).toFixed(1)}k`;
    return `¥${formatNumber(number)}`;
  }

  function formatAxisMoney(value) {
    const number = Number(value) || 0;
    if (number >= 10000) {
      return `¥${(number / 10000).toFixed(number % 10000 === 0 ? 0 : 1)}万`;
    }
    if (number >= 1000) {
      return `¥${(number / 1000).toFixed(number % 1000 === 0 ? 0 : 1)}k`;
    }
    return `¥${Math.round(number)}`;
  }

  function parseIsoDate(value) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(String(value || ''))) return null;
    const [year, month, day] = value.split('-').map(Number);
    const date = new Date(Date.UTC(year, month - 1, day));
    if (
      date.getUTCFullYear() !== year
      || date.getUTCMonth() !== month - 1
      || date.getUTCDate() !== day
    ) return null;
    return date;
  }

  function formatIsoDate(date) {
    return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}`;
  }

  function addDays(value, days) {
    const date = parseIsoDate(value);
    if (!date) return value;
    date.setUTCDate(date.getUTCDate() + days);
    return formatIsoDate(date);
  }

  function daysBetween(start, end) {
    const startDate = parseIsoDate(start);
    const endDate = parseIsoDate(end);
    if (!startDate || !endDate) return 0;
    return Math.round((endDate - startDate) / 86400000);
  }

  function dateParts(value) {
    const [year, month, day] = String(value).split('-');
    return { year, month, day };
  }

  function formatRange(start, end) {
    const first = dateParts(start);
    const last = dateParts(end);
    if (first.year === last.year) {
      return `${first.year}.${first.month}.${first.day} — ${last.month}.${last.day}`;
    }
    return `${first.year}.${first.month}.${first.day} — ${last.year}.${last.month}.${last.day}`;
  }

  function formatMonthDay(value) {
    const parts = dateParts(value);
    return `${parts.month}.${parts.day}`;
  }

  function formatChineseDate(value) {
    const date = parseIsoDate(value);
    if (!date) return value;
    return `${date.getUTCMonth() + 1}月${date.getUTCDate()}日 · ${WEEKDAYS[date.getUTCDay()]}`;
  }

  function formatOrderStamp(value) {
    if (!value) return '';
    const [datePart, timePart = ''] = String(value).split(' ');
    return `${datePart.slice(5).replace('-', '.')} ${timePart.slice(0, 5)}`;
  }

  function getStore(storeId) {
    if (!state.data) return null;
    return state.data.stores.find((store) => String(store.id) === String(storeId)) || null;
  }

  function showToast(message, type = 'info') {
    window.clearTimeout(state.toastTimer);
    elements.toastMessage.textContent = message;
    elements.toast.classList.toggle('is-error', type === 'error');
    elements.toast.hidden = false;
    state.toastTimer = window.setTimeout(() => {
      elements.toast.hidden = true;
    }, type === 'error' ? 4600 : 3200);
  }

  function setLoading(isLoading) {
    state.loading = isLoading;
    elements.loadingBar.classList.toggle('is-active', isLoading);
    document.body.classList.toggle('is-loading', isLoading);
    elements.refreshButton.disabled = isLoading;
    document.querySelector('main').setAttribute('aria-busy', String(isLoading));
  }

  function animateValue(element, target, formatter) {
    const end = Number(target) || 0;
    const start = Number(element.dataset.rawValue || 0);
    element.dataset.rawValue = String(end);
    if (element._numberFrame) cancelAnimationFrame(element._numberFrame);

    if (reducedMotion.matches || start === end) {
      element.textContent = formatter(end);
      return;
    }

    const duration = 620;
    const startedAt = performance.now();
    element.textContent = formatter(start);

    const tick = (now) => {
      const progress = Math.min(1, (now - startedAt) / duration);
      const eased = 1 - (1 - progress) ** 3;
      element.textContent = formatter(start + (end - start) * eased);
      if (progress < 1) {
        element._numberFrame = requestAnimationFrame(tick);
      }
    };
    element._numberFrame = requestAnimationFrame(tick);
  }

  function renderChange(element, value, unit = '%') {
    element.classList.remove('is-up', 'is-down', 'is-flat', 'is-neutral');
    if (value === null || value === undefined) {
      element.textContent = '—';
      element.classList.add('is-neutral');
      element.setAttribute('aria-label', '上期暂无数据');
      element.title = '上期暂无数据';
      return;
    }

    const numeric = Number(value);
    const sign = numeric > 0 ? '+' : '';
    element.textContent = `${sign}${formatDecimal(numeric)}${unit}`;
    if (numeric > 0.05) {
      element.classList.add('is-up');
    } else if (numeric < -0.05) {
      element.classList.add('is-down');
    } else {
      element.classList.add('is-flat');
    }
    const direction = numeric > 0.05 ? '增长' : numeric < -0.05 ? '下降' : '持平';
    const label = numeric > 0.05 || numeric < -0.05
      ? `较上期${direction} ${formatDecimal(Math.abs(numeric))}${unit}`
      : '与上期持平';
    element.setAttribute('aria-label', label);
    element.title = label;
  }

  function renderSparkline(element, values) {
    if (!Array.isArray(values) || values.length === 0) {
      element.innerHTML = '';
      return;
    }

    const width = 96;
    const height = 30;
    const padding = 2.5;
    const min = Math.min(...values);
    const max = Math.max(...values);
    const span = max - min || 1;
    const denominator = Math.max(1, values.length - 1);
    const points = values.map((value, index) => ({
      x: padding + (index / denominator) * (width - padding * 2),
      y: padding + (1 - ((value - min) / span)) * (height - padding * 2),
    }));

    let line = `M${points[0].x.toFixed(2)},${points[0].y.toFixed(2)}`;
    for (let index = 1; index < points.length; index += 1) {
      line += ` L${points[index].x.toFixed(2)},${points[index].y.toFixed(2)}`;
    }
    const last = points[points.length - 1];
    const area = `${line} L${last.x.toFixed(2)},${height} L${points[0].x.toFixed(2)},${height} Z`;

    element.innerHTML = `
      <svg viewBox="0 0 ${width} ${height}" preserveAspectRatio="none" aria-hidden="true">
        <path d="${area}" fill="currentColor" opacity="0.1"></path>
        <path d="${line}" fill="none" stroke="currentColor" stroke-width="1.45" stroke-linecap="round" stroke-linejoin="round" vector-effect="non-scaling-stroke"></path>
        <circle cx="${last.x.toFixed(2)}" cy="${last.y.toFixed(2)}" r="1.7" fill="currentColor"></circle>
      </svg>
    `;
  }

  function renderKpis(data) {
    animateValue(elements.kpiRevenue, data.revenue, formatNumber);
    animateValue(elements.kpiOrders, data.orders, formatNumber);
    animateValue(elements.kpiAvgTicket, data.avgTicket, (value) => formatDecimal(value));
    animateValue(elements.kpiMemberShare, data.memberShare, (value) => formatDecimal(value));

    renderChange(elements.changeRevenue, data.change.revenue);
    renderChange(elements.changeOrders, data.change.orders);
    renderChange(elements.changeAvgTicket, data.change.avgTicket);
    renderChange(elements.changeMemberShare, data.change.memberShare, 'pp');

    const trend = state.data.trend;
    renderSparkline(elements.sparkRevenue, trend.map((item) => item.revenue));
    renderSparkline(elements.sparkOrders, trend.map((item) => item.orders));
    renderSparkline(elements.sparkAvgTicket, trend.map((item) => (
      item.orders ? item.revenue / item.orders : 0
    )));
    renderSparkline(elements.sparkMemberShare, trend.map((item) => (
      item.orders ? (item.memberOrders / item.orders) * 100 : 0
    )));
  }

  function niceAxisMaximum(value) {
    if (!value) return 100;
    const rough = value / 4;
    const power = 10 ** Math.floor(Math.log10(rough));
    const normalized = rough / power;
    const niceSteps = [1, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10];
    const step = niceSteps.find((candidate) => normalized <= candidate) || 10;
    return step * power * 4;
  }

  function smoothPath(points) {
    if (points.length === 0) return '';
    if (points.length === 1) return `M${points[0].x},${points[0].y}`;
    let path = `M${points[0].x.toFixed(2)},${points[0].y.toFixed(2)}`;
    for (let index = 1; index < points.length; index += 1) {
      const previous = points[index - 1];
      const current = points[index];
      const middleX = (previous.x + current.x) / 2;
      path += ` C${middleX.toFixed(2)},${previous.y.toFixed(2)} ${middleX.toFixed(2)},${current.y.toFixed(2)} ${current.x.toFixed(2)},${current.y.toFixed(2)}`;
    }
    return path;
  }

  function renderTrend(trend, range) {
    const chart = elements.trendChart;
    const tooltip = elements.trendTooltip;
    const width = Math.max(300, Math.round(chart.clientWidth || 680));
    const height = Math.max(210, Math.round(chart.clientHeight || 273));
    const totalRevenue = trend.reduce((sum, item) => sum + item.revenue, 0);
    tooltip.hidden = true;
    chart.onpointermove = null;
    chart.onpointerleave = null;

    if (!trend.length || totalRevenue === 0) {
      chart.innerHTML = `
        <div class="empty-chart">
          <span class="empty-mark" aria-hidden="true">∅</span>
          <p>这个时段还没有营业记录</p>
        </div>
      `;
      chart.setAttribute('aria-label', '所选时段没有营业额数据');
      return;
    }

    const margin = {
      top: 17,
      right: width < 540 ? 12 : 18,
      bottom: 29,
      left: width < 540 ? 44 : 52,
    };
    const plotWidth = width - margin.left - margin.right;
    const plotHeight = height - margin.top - margin.bottom;
    const baseY = margin.top + plotHeight;
    const maxRevenue = Math.max(...trend.map((item) => item.revenue));
    const yMax = niceAxisMaximum(maxRevenue);
    const denominator = Math.max(1, trend.length - 1);

    const xAt = (index) => margin.left + (index / denominator) * plotWidth;
    const yAt = (value) => margin.top + plotHeight - (value / yMax) * plotHeight;
    const points = trend.map((item, index) => ({
      x: xAt(index),
      y: yAt(item.revenue),
      item,
    }));

    const gridParts = [];
    for (let step = 0; step <= 4; step += 1) {
      const value = yMax * (1 - step / 4);
      const y = margin.top + (step / 4) * plotHeight;
      gridParts.push(`<line class="${step === 4 ? 'chart-grid-base' : 'chart-grid'}" x1="${margin.left}" y1="${y.toFixed(2)}" x2="${margin.left + plotWidth}" y2="${y.toFixed(2)}"></line>`);
      gridParts.push(`<text class="chart-axis-label" x="${margin.left - 9}" y="${(y + 3).toFixed(2)}" text-anchor="end">${formatAxisMoney(value)}</text>`);
    }

    const maxTicks = width < 520 ? 4 : (trend.length <= 14 ? 7 : 6);
    const tickStep = Math.max(1, Math.ceil((trend.length - 1) / Math.max(1, maxTicks - 1)));
    const tickIndices = [];
    for (let index = 0; index < trend.length; index += tickStep) tickIndices.push(index);
    const lastIndex = trend.length - 1;
    if (tickIndices[tickIndices.length - 1] !== lastIndex && lastIndex - tickIndices[tickIndices.length - 1] > tickStep / 2) {
      tickIndices.push(lastIndex);
    }
    const xLabels = tickIndices.map((index) => {
      const anchor = index === lastIndex ? 'end' : index === 0 ? 'start' : 'middle';
      return `<text class="chart-axis-label" x="${xAt(index).toFixed(2)}" y="${height - 8}" text-anchor="${anchor}">${formatMonthDay(trend[index].date)}</text>`;
    });
    const lastPoint = points[points.length - 1];

    chart.innerHTML = `
      <svg class="trend-svg" viewBox="0 0 ${width} ${height}" role="presentation" aria-hidden="true">
        <defs>
          <linearGradient id="trendAreaGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="#cf6d3f" stop-opacity="0.24"></stop>
            <stop offset="100%" stop-color="#cf6d3f" stop-opacity="0"></stop>
          </linearGradient>
        </defs>
        ${gridParts.join('')}
        ${xLabels.join('')}
        <path class="chart-area" d="${smoothPath(points)} L${lastPoint.x.toFixed(2)},${baseY.toFixed(2)} L${points[0].x.toFixed(2)},${baseY.toFixed(2)} Z" fill="url(#trendAreaGradient)"></path>
        <path class="trend-path" d="${smoothPath(points)}" pathLength="1"></path>
        <circle class="trend-last-halo" cx="${lastPoint.x.toFixed(2)}" cy="${lastPoint.y.toFixed(2)}" r="8"></circle>
        <circle class="trend-last-dot" cx="${lastPoint.x.toFixed(2)}" cy="${lastPoint.y.toFixed(2)}" r="3.4"></circle>
        <g class="trend-hover" style="display:none">
          <line class="chart-hover-line" x1="0" y1="${margin.top}" x2="0" y2="${baseY}"></line>
          <circle class="chart-hover-dot" cx="0" cy="0" r="4"></circle>
        </g>
        <rect class="chart-hit-area" x="${margin.left}" y="${margin.top}" width="${plotWidth}" height="${plotHeight}"></rect>
      </svg>
    `;

    const hoverGroup = chart.querySelector('.trend-hover');
    const hoverLine = chart.querySelector('.chart-hover-line');
    const hoverDot = chart.querySelector('.chart-hover-dot');
    const totalOrders = trend.reduce((sum, item) => sum + item.orders, 0);
    chart.setAttribute(
      'aria-label',
      `${formatRange(range.start, range.end)}每日营业额走势，总营业额 ${formatMoney(totalRevenue)}，总订单 ${formatNumber(totalOrders)} 单。`,
    );

    const showTooltip = (event) => {
      const rect = chart.getBoundingClientRect();
      const relativeX = event.clientX - rect.left;
      if (relativeX < margin.left - 4 || relativeX > margin.left + plotWidth + 4) {
        tooltip.hidden = true;
        hoverGroup.style.display = 'none';
        return;
      }
      const index = Math.max(0, Math.min(trend.length - 1, Math.round(((relativeX - margin.left) / plotWidth) * (trend.length - 1))));
      const point = points[index];

      hoverGroup.style.display = '';
      hoverLine.setAttribute('x1', point.x.toFixed(2));
      hoverLine.setAttribute('x2', point.x.toFixed(2));
      hoverDot.setAttribute('cx', point.x.toFixed(2));
      hoverDot.setAttribute('cy', point.y.toFixed(2));

      const tooltipWidth = 168;
      const left = Math.max(4, Math.min(width - tooltipWidth - 4, point.x - tooltipWidth / 2));
      let top = point.y - 91;
      let below = false;
      if (top < 4) {
        top = point.y + 15;
        below = true;
      }
      top = Math.max(4, Math.min(height - 80, top));
      const arrowLeft = Math.max(13, Math.min(tooltipWidth - 13, point.x - left));

      tooltip.innerHTML = `
        <span class="tooltip-date">${escapeHtml(formatChineseDate(point.item.date))}</span>
        <div class="tooltip-row"><span>营业额</span><strong>${escapeHtml(formatMoney(point.item.revenue))}</strong></div>
        <div class="tooltip-row"><span>订单</span><strong>${escapeHtml(formatNumber(point.item.orders))} 单</strong></div>
      `;
      tooltip.style.left = `${left}px`;
      tooltip.style.top = `${top}px`;
      tooltip.style.setProperty('--arrow-left', `${arrowLeft}px`);
      tooltip.classList.toggle('is-below', below);
      tooltip.hidden = false;
    };

    chart.onpointermove = showTooltip;
    chart.onpointerleave = () => {
      tooltip.hidden = true;
      hoverGroup.style.display = 'none';
    };
  }

  function renderCategories(categories) {
    const total = categories.reduce((sum, category) => sum + category.revenue, 0);
    if (!categories.length || total === 0) {
      elements.categoryDonut.innerHTML = `
        <svg class="donut-svg" viewBox="0 0 160 160" aria-hidden="true">
          <circle class="donut-track" cx="80" cy="80" r="61"></circle>
          <text class="donut-total" x="80" y="78">∅</text>
          <text class="donut-caption" x="80" y="94">暂无品类数据</text>
        </svg>
      `;
      elements.categoryLegend.innerHTML = '<li class="category-item"><span class="category-label"><strong>换个日期试试</strong></span></li>';
      elements.categoryDonut.setAttribute('aria-label', '所选时段没有品类数据');
      return;
    }

    const radius = 61;
    const circumference = 2 * Math.PI * radius;
    let offset = 0;
    const segments = categories.map((category, index) => {
      const color = CATEGORY_COLORS[category.name] || FALLBACK_COLORS[index % FALLBACK_COLORS.length];
      const length = circumference * (category.share / 100);
      const visible = Math.max(0.8, length - 2.2);
      const segment = `
        <circle
          class="donut-segment"
          cx="80"
          cy="80"
          r="${radius}"
          stroke="${color}"
          stroke-dasharray="${visible.toFixed(2)} ${(circumference - visible).toFixed(2)}"
          stroke-dashoffset="${(-offset).toFixed(2)}"
          transform="rotate(-90 80 80)"
        >
          <title>${escapeHtml(category.name)} · ${formatDecimal(category.share)}% · ${escapeHtml(formatMoney(category.revenue))}</title>
        </circle>
      `;
      offset += length;
      return segment;
    });

    elements.categoryDonut.innerHTML = `
      <svg class="donut-svg" viewBox="0 0 160 160" aria-hidden="true">
        <circle class="donut-track" cx="80" cy="80" r="${radius}"></circle>
        ${segments.join('')}
        <text class="donut-total" x="80" y="78">${escapeHtml(formatCompactMoney(total))}</text>
        <text class="donut-caption" x="80" y="93">品类营收</text>
      </svg>
    `;
    elements.categoryDonut.setAttribute('aria-label', `品类营业额占比：${categories.map((category) => `${category.name} ${formatDecimal(category.share)}%`).join('，')}`);

    elements.categoryLegend.innerHTML = categories.map((category, index) => {
      const color = CATEGORY_COLORS[category.name] || FALLBACK_COLORS[index % FALLBACK_COLORS.length];
      return `
        <li class="category-item">
          <span class="category-swatch" style="background:${color}" aria-hidden="true"></span>
          <span class="category-label">
            <strong>${escapeHtml(category.name)}</strong>
            <small>${escapeHtml(formatMoney(category.revenue))}</small>
          </span>
          <span class="category-share">${formatDecimal(category.share)}%</span>
        </li>
      `;
    }).join('');
  }

  function renderHourly(hourly) {
    const totalOrders = hourly.reduce((sum, item) => sum + item.orders, 0);
    if (!hourly.length || totalOrders === 0) {
      elements.hourlyChart.innerHTML = `
        <div class="empty-chart">
          <span class="empty-mark" aria-hidden="true">∅</span>
          <p>这个时段还没有订单</p>
        </div>
      `;
      elements.hourlyChart.setAttribute('aria-label', '所选时段没有分时订单数据');
      elements.peakBadge.textContent = '暂无高峰';
      return;
    }

    const maxOrders = Math.max(...hourly.map((item) => item.orders));
    const peakIndex = hourly.findIndex((item) => item.orders === maxOrders);
    const peak = hourly[peakIndex];
    elements.peakBadge.textContent = `高峰 ${peak.label} · ${formatNumber(peak.orders)} 单`;

    const bars = hourly.map((item, index) => {
      const height = maxOrders ? Math.max(2, (item.orders / maxOrders) * 100) : 0;
      return `
        <div class="hour-column${index === peakIndex ? ' is-peak' : ''}" title="${escapeHtml(item.label)} · ${formatNumber(item.orders)} 单">
          <span class="hour-value">${formatNumber(item.orders)}</span>
          <div class="hour-track">
            <div class="hour-bar" style="--bar-height:${height.toFixed(2)}%"></div>
          </div>
        </div>
      `;
    }).join('');

    const labels = hourly.map((item) => `<span class="hour-label">${escapeHtml(item.label.slice(0, 2))}</span>`).join('');
    elements.hourlyChart.innerHTML = `
      <div class="hourly-bars">${bars}</div>
      <div class="hour-labels" aria-hidden="true">${labels}</div>
    `;
    elements.hourlyChart.setAttribute('aria-label', `按小时统计订单量，高峰时段为 ${peak.label}，共 ${formatNumber(peak.orders)} 单。`);
  }

  function renderStores(stores, selectedStoreId) {
    const selectedStore = getStore(selectedStoreId);
    elements.storeScope.textContent = selectedStore ? selectedStore.name : '全部门店';

    const totalRevenue = stores.reduce((sum, store) => sum + store.revenue, 0);
    if (!stores.length || totalRevenue === 0) {
      elements.storeRanking.innerHTML = '<div class="store-empty">这个时段还没有门店营业数据</div>';
      return;
    }

    const maxRevenue = Math.max(...stores.map((store) => store.revenue));
    elements.storeRanking.classList.toggle('is-single', stores.length === 1);
    elements.storeRanking.innerHTML = stores.map((store, index) => {
      const accent = safeColor(store.accent);
      const width = maxRevenue ? Math.max(1.5, (store.revenue / maxRevenue) * 100) : 0;
      return `
        <div class="store-row">
          <span class="store-rank" aria-hidden="true">${pad(index + 1)}</span>
          <div class="store-main">
            <div class="store-line">
              <strong>${escapeHtml(store.name)}</strong>
              <span class="store-address">${escapeHtml(store.address)}</span>
            </div>
            <div class="store-track" aria-hidden="true"><span style="width:${width.toFixed(2)}%;background:${accent}"></span></div>
          </div>
          <div class="store-numbers">
            <strong>${escapeHtml(formatMoney(store.revenue))}</strong>
            <span>${formatNumber(store.orders)} 单 · 客单 ${formatMoney(store.avgTicket)}</span>
          </div>
        </div>
      `;
    }).join('');
  }

  function renderProducts(products) {
    if (!products.length) {
      elements.productRanking.innerHTML = '<div class="ranking-empty">这个时段还没有售出商品</div>';
      return;
    }

    const maxQuantity = Math.max(...products.map((product) => product.quantity));
    elements.productRanking.innerHTML = products.map((product, index) => {
      const width = maxQuantity ? Math.max(3, (product.quantity / maxQuantity) * 100) : 0;
      const unit = product.category === '咖啡' || product.category === '非咖啡' ? '杯' : '份';
      return `
        <div class="product-row">
          <span class="product-rank" aria-hidden="true">${pad(index + 1)}</span>
          <div class="product-main">
            <div class="product-name-line">
              <strong>${escapeHtml(product.name)}</strong>
              <span class="category-pill">${escapeHtml(product.category)}</span>
            </div>
            <div class="product-track" aria-hidden="true"><span style="width:${width.toFixed(2)}%"></span></div>
          </div>
          <div class="product-numbers">
            <strong>${formatNumber(product.quantity)} ${unit}</strong>
            <small>${escapeHtml(formatMoney(product.revenue))}</small>
          </div>
        </div>
      `;
    }).join('');
  }

  function renderOrders(orders) {
    const hasOrders = orders.length > 0;
    elements.ordersTable.hidden = !hasOrders;
    elements.ordersEmpty.hidden = hasOrders;
    elements.exportButton.disabled = !hasOrders;

    if (!hasOrders) {
      elements.ordersBody.innerHTML = '';
      return;
    }

    elements.ordersBody.innerHTML = orders.map((order) => {
      const accent = safeColor(order.accent);
      const orderParts = order.orderNo.split('-');
      const shortOrder = `${orderParts[1] || ''}·${orderParts[orderParts.length - 1] || order.orderNo}`;
      const timestamp = formatOrderStamp(order.orderedAt);
      return `
        <tr>
          <td>
            <div class="order-meta" title="${escapeHtml(`${order.orderNo} · ${order.payment}`)}">
              <strong>${escapeHtml(shortOrder)}</strong>
              <small>${escapeHtml(timestamp)}</small>
            </div>
          </td>
          <td>
            <span class="order-store">
              <span class="store-dot" style="background:${accent}" aria-hidden="true"></span>
              ${escapeHtml(order.store)}
            </span>
          </td>
          <td>
            <span class="order-items" title="${escapeHtml(order.items)}">
              <span class="order-items-text">${escapeHtml(order.items)}</span>
              ${order.isMember ? '<span class="member-chip">会员</span>' : ''}
            </span>
          </td>
          <td class="order-total">${escapeHtml(formatMoney(order.total))}</td>
        </tr>
      `;
    }).join('');
  }

  function syncUrl() {
    if (!state.data) return;
    const url = new URL(window.location.href);
    url.searchParams.set('start', state.data.range.start);
    url.searchParams.set('end', state.data.range.end);
    if (state.filters.storeId) {
      url.searchParams.set('store', state.filters.storeId);
    } else {
      url.searchParams.delete('store');
    }
    window.history.replaceState(null, '', url);
  }

  function populateStoreControls() {
    const stores = state.data.stores;
    const selected = state.filters.storeId ? String(state.filters.storeId) : '';
    elements.storeCountLabel.textContent = pad(stores.length);
    elements.storeSelect.innerHTML = `
      <option value="">全部门店</option>
      ${stores.map((store) => `<option value="${store.id}">${escapeHtml(store.name)}</option>`).join('')}
    `;
    elements.storeSelect.value = selected;

    elements.sideStoreList.innerHTML = stores.map((store) => `
      <div class="side-store">
        <span class="store-dot" style="background:${safeColor(store.accent)}" aria-hidden="true"></span>
        <span class="side-store-copy">
          <strong>${escapeHtml(store.name)}</strong>
          <small>${escapeHtml(store.address)}</small>
        </span>
        <span class="side-store-state">OPEN</span>
      </div>
    `).join('');

    elements.dbMeta.textContent = `${formatNumber(state.data.meta.totalOrdersAllTime)} 笔订单 · 累计 ${formatCompactMoney(state.data.meta.totalRevenueAllTime)}`;
    elements.footerRows.textContent = `· ${formatNumber(state.data.meta.totalOrdersAllTime)} 笔订单`;
  }

  function detectPreset(range) {
    if (range.end !== range.maxDate) return null;
    for (const days of PRESETS) {
      const expectedStart = addDays(range.maxDate, -(days - 1));
      const boundedStart = expectedStart < range.minDate ? range.minDate : expectedStart;
      if (range.start === boundedStart) return days;
    }
    return null;
  }

  function updateFilterControls() {
    const { range } = state.data;
    elements.startDate.min = range.minDate;
    elements.startDate.max = range.maxDate;
    elements.endDate.min = range.minDate;
    elements.endDate.max = range.maxDate;
    elements.startDate.value = state.filters.start;
    elements.endDate.value = state.filters.end;

    state.filters.preset = detectPreset(range);
    document.querySelectorAll('[data-preset]').forEach((button) => {
      const isActive = Number(button.dataset.preset) === state.filters.preset;
      button.classList.toggle('is-active', isActive);
      button.setAttribute('aria-pressed', String(isActive));
    });
  }

  function renderMeta() {
    const data = state.data;
    const selectedStore = getStore(state.filters.storeId);
    const storeCount = data.stores.length;
    const generatedTime = timeFormatter.format(new Date(data.meta.generatedAt));

    elements.heroRange.textContent = formatRange(data.range.start, data.range.end);
    elements.heroDays.textContent = `共 ${data.range.days} 天 · ${selectedStore ? selectedStore.name : `${storeCount} 家门店`}`;
    elements.updatedAt.textContent = `数据更新 ${generatedTime} · 截至 ${formatMonthDay(data.range.maxDate)}`;
    elements.dailyAvg.textContent = formatMoney(data.kpis.revenue / Math.max(1, data.range.days));

    populateStoreControls();
    updateFilterControls();
    syncUrl();
  }

  function renderDashboard(data) {
    state.filters.start = data.range.start;
    state.filters.end = data.range.end;
    state.filters.storeId = data.storeId ? String(data.storeId) : '';
    renderMeta();
    renderKpis(data.kpis);
    renderTrend(data.trend, data.range);
    renderCategories(data.categories);
    renderHourly(data.hourly);
    renderStores(data.storePerformance, state.filters.storeId);
    renderProducts(data.topProducts);
    renderOrders(data.recentOrders);
  }

  function buildQuery() {
    const params = new URLSearchParams();
    if (state.filters.start && state.filters.end) {
      params.set('start', state.filters.start);
      params.set('end', state.filters.end);
    }
    if (state.filters.storeId) params.set('store', state.filters.storeId);
    const query = params.toString();
    return query ? `?${query}` : '';
  }

  async function loadDashboard() {
    if (state.controller) state.controller.abort();
    const controller = new AbortController();
    state.controller = controller;
    setLoading(true);

    try {
      const response = await fetch(`/api/dashboard${buildQuery()}`, {
        signal: controller.signal,
        headers: { Accept: 'application/json' },
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        const error = new Error(payload.error || '数据加载失败，请稍后重试。');
        error.status = response.status;
        throw error;
      }
      if (controller.signal.aborted) return;

      state.data = payload;
      state.hasLoaded = true;
      state.retriedDefault = false;
      renderDashboard(payload);
    } catch (error) {
      if (error.name === 'AbortError') return;

      if (!state.data && !state.retriedDefault && error.status === 400) {
        state.retriedDefault = true;
        state.filters = { start: '', end: '', storeId: '', preset: null };
        showToast('筛选条件无效，已载入默认区间。', 'error');
        loadDashboard();
        return;
      }

      showToast(error.message || '数据加载失败，请检查服务是否已启动。', 'error');
      if (!state.data) {
        elements.trendChart.innerHTML = `
          <div class="empty-chart">
            <span class="empty-mark" aria-hidden="true">!</span>
            <p>暂时读不到数据</p>
            <p>请确认本地服务已启动，然后点击右上角刷新。</p>
          </div>
        `;
        elements.updatedAt.textContent = '数据连接失败';
      }
    } finally {
      if (state.controller === controller) {
        state.controller = null;
        setLoading(false);
      }
    }
  }

  function applyPreset(days) {
    if (!state.data) return;
    const { minDate, maxDate } = state.data.range;
    const rawStart = addDays(maxDate, -(days - 1));
    const start = rawStart < minDate ? minDate : rawStart;
    state.filters.start = start;
    state.filters.end = maxDate;
    state.filters.preset = days;
    elements.startDate.value = start;
    elements.endDate.value = maxDate;
    loadDashboard();
  }

  function applyCustomRange() {
    if (!state.data) return;
    const start = elements.startDate.value;
    const end = elements.endDate.value;

    if (!parseIsoDate(start) || !parseIsoDate(end)) {
      showToast('请选择完整的开始与结束日期。', 'error');
      return;
    }
    if (start > end) {
      showToast('开始日期不能晚于结束日期。', 'error');
      return;
    }
    if (daysBetween(start, end) + 1 > 366) {
      showToast('单次查询最多支持 366 天。', 'error');
      return;
    }
    const { minDate, maxDate } = state.data.range;
    if (start < minDate || end > maxDate) {
      showToast(`请选择 ${minDate} 至 ${maxDate} 之间的日期。`, 'error');
      return;
    }

    state.filters.start = start;
    state.filters.end = end;
    state.filters.preset = null;
    document.querySelectorAll('[data-preset]').forEach((button) => button.classList.remove('is-active'));
    loadDashboard();
  }

  function changeStore() {
    if (!state.data) return;
    state.filters.storeId = elements.storeSelect.value;
    loadDashboard();
  }

  function downloadCsv() {
    if (!state.data || state.data.kpis.orders === 0) return;
    const params = new URLSearchParams({
      start: state.filters.start,
      end: state.filters.end,
    });
    if (state.filters.storeId) params.set('store', state.filters.storeId);

    const link = document.createElement('a');
    link.href = `/api/orders/export?${params.toString()}`;
    link.download = '';
    document.body.appendChild(link);
    link.click();
    link.remove();

    showToast('正在准备 CSV 文件，请留意浏览器下载。');
    const label = elements.exportButton.querySelector('span');
    const original = label.textContent;
    label.textContent = '生成中…';
    elements.exportButton.disabled = true;
    window.setTimeout(() => {
      label.textContent = original;
      elements.exportButton.disabled = !state.data || state.data.kpis.orders === 0;
    }, 900);
  }

  function openSidebar() {
    document.body.classList.add('nav-open');
    elements.menuButton.setAttribute('aria-expanded', 'true');
    elements.sidebarScrim.setAttribute('aria-hidden', 'false');
    elements.sidebar.removeAttribute('inert');
  }

  function closeSidebar({ restoreFocus = false } = {}) {
    document.body.classList.remove('nav-open');
    elements.menuButton.setAttribute('aria-expanded', 'false');
    elements.sidebarScrim.setAttribute('aria-hidden', 'true');
    if (window.matchMedia('(max-width: 960px)').matches) {
      elements.sidebar.setAttribute('inert', '');
    }
    if (restoreFocus) elements.menuButton.focus();
  }

  function setupSidebar() {
    elements.menuButton.addEventListener('click', openSidebar);
    elements.sidebarClose.addEventListener('click', () => closeSidebar({ restoreFocus: true }));
    elements.sidebarScrim.addEventListener('click', () => closeSidebar({ restoreFocus: true }));
    document.querySelectorAll('[data-nav]').forEach((link) => {
      link.addEventListener('click', () => {
        document.querySelectorAll('[data-nav]').forEach((item) => {
          item.classList.remove('is-active');
          item.removeAttribute('aria-current');
        });
        link.classList.add('is-active');
        link.setAttribute('aria-current', 'location');
        closeSidebar();
      });
    });
    window.addEventListener('keydown', (event) => {
      if (event.key === 'Escape' && document.body.classList.contains('nav-open')) {
        closeSidebar({ restoreFocus: true });
      }
    });

    const mediaQuery = window.matchMedia('(max-width: 960px)');
    const syncSidebarMode = () => {
      if (mediaQuery.matches) {
        closeSidebar();
      } else {
        document.body.classList.remove('nav-open');
        elements.menuButton.setAttribute('aria-expanded', 'false');
        elements.sidebar.removeAttribute('inert');
      }
    };
    if (mediaQuery.addEventListener) mediaQuery.addEventListener('change', syncSidebarMode);
    else mediaQuery.addListener(syncSidebarMode);
    syncSidebarMode();
  }

  function setupScrollSpy() {
    const links = Array.from(document.querySelectorAll('[data-nav]'));
    const sections = links
      .map((link) => document.querySelector(link.getAttribute('href')))
      .filter(Boolean);
    let scheduled = false;

    const update = () => {
      scheduled = false;
      let current = sections[0]?.id;
      for (const section of sections) {
        const top = section.getBoundingClientRect().top + window.scrollY;
        if (top <= window.scrollY + 170) current = section.id;
      }
      links.forEach((link) => {
        const isActive = link.getAttribute('href') === `#${current}`;
        link.classList.toggle('is-active', isActive);
        if (isActive) link.setAttribute('aria-current', 'location');
        else link.removeAttribute('aria-current');
      });
    };

    const schedule = () => {
      if (scheduled) return;
      scheduled = true;
      requestAnimationFrame(update);
    };

    window.addEventListener('scroll', schedule, { passive: true });
    update();
  }

  function setupEvents() {
    document.querySelectorAll('[data-preset]').forEach((button) => {
      button.addEventListener('click', () => applyPreset(Number(button.dataset.preset)));
    });
    elements.applyFilter.addEventListener('click', applyCustomRange);
    elements.startDate.addEventListener('keydown', (event) => {
      if (event.key === 'Enter') applyCustomRange();
    });
    elements.endDate.addEventListener('keydown', (event) => {
      if (event.key === 'Enter') applyCustomRange();
    });
    elements.storeSelect.addEventListener('change', changeStore);
    elements.refreshButton.addEventListener('click', () => {
      if (!state.loading) loadDashboard();
    });
    elements.exportButton.addEventListener('click', downloadCsv);

    let resizeTimer;
    window.addEventListener('resize', () => {
      if (!state.data) return;
      window.clearTimeout(resizeTimer);
      resizeTimer = window.setTimeout(() => {
        renderTrend(state.data.trend, state.data.range);
      }, 160);
    });
  }

  function hydrateFiltersFromUrl() {
    const params = new URLSearchParams(window.location.search);
    const start = params.get('start');
    const end = params.get('end');
    const store = params.get('store');
    if (start && end && parseIsoDate(start) && parseIsoDate(end) && start <= end) {
      state.filters.start = start;
      state.filters.end = end;
    }
    if (store && /^\d+$/.test(store)) {
      state.filters.storeId = store;
    }
  }

  function init() {
    hydrateFiltersFromUrl();
    setupSidebar();
    setupScrollSpy();
    setupEvents();
    loadDashboard();
  }

  init();
})();
