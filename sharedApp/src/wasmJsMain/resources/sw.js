// Keeps Pascaline working offline. The build puts the list of its files and a new version in here,
// so each release replaces the old cache. Paths are relative: the site lives in a folder on GitHub
// Pages.
//
// Online, a new release shows on the next load: files that keep their name (index.html,
// sharedApp.js, resources) are checked with the server first, the cache is only used offline. The
// .wasm files are named after their content, a new release has new ones: those come from the cache.
const CACHE_NAME = "pascaline-cache-{VERSION}";

const PRECACHED_ASSETS = [/*__PRECACHED_ASSETS__*/];

// Development builds have no list: always load the latest files
const OFFLINE = PRECACHED_ASSETS.length > 0;

self.addEventListener("install", (event) => {
  if (!OFFLINE) return self.skipWaiting();
  event.waitUntil(
    (async () => {
      const cache = await caches.open(CACHE_NAME);
      // One file at a time: all at once uses too much memory on phones while the app starts
      // Past the browser's own cache: GitHub Pages lets it keep files for 10 minutes, the new
      // version would be saved with old files
      for (const asset of PRECACHED_ASSETS) await cache.add(new Request(asset, { cache: "reload" }));
      await self.skipWaiting();
    })(),
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    (async () => {
      const keys = await caches.keys();
      await Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key)));
      await self.clients.claim();
    })(),
  );
});

/** Named after their content, like 24fc1b3e2bcdf710ce10.wasm: a file with this name never changes. */
const IMMUTABLE = /\/[0-9a-f]{16,}\.wasm$/;

self.addEventListener("fetch", (event) => {
  if (!OFFLINE || event.request.method !== "GET") return;
  if (!event.request.url.startsWith(self.location.origin)) return;

  event.respondWith(
    IMMUTABLE.test(new URL(event.request.url).pathname)
      ? cacheFirst(event.request)
      : networkFirst(event.request),
  );
});

async function cacheFirst(request) {
  const cached = await caches.match(request);
  return cached || fetchAndKeep(request);
}

// Asks the server whether the file changed (a short answer when it didn't), the saved one offline
async function networkFirst(request) {
  try {
    return await fetchAndKeep(new Request(request, { cache: "no-cache" }), request);
  } catch (error) {
    const cached = await caches.match(request);
    if (cached) return cached;
    if (request.mode === "navigate") {
      const page = await caches.match("./");
      if (page) return page;
    }
    return new Response("Offline", { status: 503, statusText: "Unavailable" });
  }
}

/** Fetches [request] and keeps the answer for offline use under [key]. */
async function fetchAndKeep(request, key = request) {
  const response = await fetch(request);
  if (response.ok && response.type === "basic") {
    const copy = response.clone();
    caches.open(CACHE_NAME).then((cache) => cache.put(key, copy));
  }
  return response;
}
