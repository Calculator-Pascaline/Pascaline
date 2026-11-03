// Keeps Pascaline working offline. The build puts the list of its files and a new version in here,
// so each release replaces the old cache. Paths are relative: the site lives in a folder on GitHub
// Pages.
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
      for (const asset of PRECACHED_ASSETS) await cache.add(asset);
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

self.addEventListener("fetch", (event) => {
  if (!OFFLINE || event.request.method !== "GET") return;
  if (!event.request.url.startsWith(self.location.origin)) return;

  event.respondWith(
    (async () => {
      const cached = await caches.match(event.request);
      if (cached) return cached;

      try {
        const response = await fetch(event.request);
        if (response.ok && response.type === "basic") {
          const copy = response.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copy));
        }
        return response;
      } catch (error) {
        if (event.request.mode === "navigate") {
          const page = await caches.match("./");
          if (page) return page;
        }
        return new Response("Offline", { status: 503, statusText: "Unavailable" });
      }
    })(),
  );
});
