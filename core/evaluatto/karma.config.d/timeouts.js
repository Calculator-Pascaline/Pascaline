// Infinite sums and benchmarks take longer than Mocha's 2 seconds on the web, and Karma took the
// page for lost after 30 seconds while a test was calculating
config.set({
  browserNoActivityTimeout: 600000,
  client: { mocha: { timeout: 120000 } },
});
