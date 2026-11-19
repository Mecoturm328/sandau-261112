export default defineNuxtConfig({
  compatibilityDate: '2026-11-19',
  devtools: { enabled: false },
  css: ['~/assets/css/main.css'],
  app: {
    head: {
      htmlAttrs: { lang: 'zh-CN' },
      title: 'MORI COFFEE · 经营数据台',
      meta: [
        { name: 'theme-color', content: '#173a2c' },
        {
          name: 'description',
          content: 'MORI COFFEE 森屿咖啡经营数据台：营业额、订单趋势、品类构成与门店表现，一目了然。',
        },
      ],
      link: [{ rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' }],
    },
  },
  runtimeConfig: {
    public: {
      apiBase: 'http://127.0.0.1:8080/api',
    },
  },
  nitro: {
    compressPublicAssets: true,
  },
})
