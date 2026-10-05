// Service worker do SGFL.
//
// Estratégia:
//  - Shell (HTML, JS, CSS, ícones): cache-first com atualização em segundo plano.
//  - Navegações: network-first e, sem rede, a página salva ou /offline.html.
//  - /api/**: NUNCA é cacheado. As respostas carregam dados do usuário e o
//    cache deles quebraria o CSRF e a sessão (o app pede /auth/session na abertura).
const CACHE = 'sgfl-shell-v2';

const SHELL = [
  '/',
  '/index.html',
  '/offline.html',
  '/manifest.webmanifest',
  '/favicon.svg',
  '/icon-192.png',
  '/icon-512.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE).then((cache) =>
      Promise.all(
        SHELL.map((url) =>
          cache.add(new Request(url, { cache: 'reload' })).catch(() => {})
        )
      )
    )
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((chaves) =>
        Promise.all(
          chaves
            .filter((chave) => chave !== CACHE)
            .map((chave) => caches.delete(chave))
        )
      )
      .then(() => self.clients.claim())
  );
});

function ehApi(url) {
  return url.pathname.startsWith('/api/');
}

function ehAssetEstatico(url) {
  return /\.(?:js|css|svg|png|jpg|jpeg|webp|woff2?|webmanifest)$/.test(url.pathname);
}

self.addEventListener('fetch', (event) => {
  const { request } = event;

  if (request.method !== 'GET') return;

  const url = new URL(request.url);

  // Dados da API: sempre rede. Falhou, propaga o erro para o interceptor.
  if (url.origin !== self.location.origin || ehApi(url)) return;

  // Navegação (SPA): rede primeiro, cache em seguida, offline no fim.
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          const copia = response.clone();
          caches.open(CACHE).then((cache) => cache.put('/index.html', copia));
          return response;
        })
        .catch(() =>
          caches
            .match(request)
            .then((salvo) => salvo || caches.match('/index.html'))
            .then((salvo) => salvo || caches.match('/offline.html'))
        )
    );
    return;
  }

  // Assets: cache primeiro e revalida em silêncio.
  if (ehAssetEstatico(url)) {
    event.respondWith(
      caches.match(request).then((salvo) => {
        const rede = fetch(request)
          .then((response) => {
            if (response.ok) {
              const copia = response.clone();
              caches.open(CACHE).then((cache) => cache.put(request, copia));
            }
            return response;
          })
          .catch(() => salvo);

        return salvo || rede;
      })
    );
  }
});
