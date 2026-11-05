// A calculating test blocks the page, which then can't answer Karma. Infinite sums and benchmarks
// take longer on the web than Mocha's 2 seconds, Karma's 30 without news and its 5 seconds for a ping
const FIVE_MINUTES = 300000;

config.set({
  browserNoActivityTimeout: FIVE_MINUTES,
  browserDisconnectTimeout: FIVE_MINUTES,
  pingTimeout: FIVE_MINUTES,
  client: { mocha: { timeout: 120000 } },
});
