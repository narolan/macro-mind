'use strict';

const http = require('node:http');

const DEFAULT_TIMEOUT_MS = 800;

/** GET the advice JSON. Resolves null (never rejects) when the backend is down, slow, or answers badly. */
function fetchAdvice(url, timeoutMs = DEFAULT_TIMEOUT_MS) {
  return new Promise((resolve) => {
    const request = http.get(url, { timeout: timeoutMs }, (response) => {
      if (response.statusCode !== 200) {
        response.resume();
        resolve(null);
        return;
      }
      let body = '';
      response.setEncoding('utf8');
      response.on('data', (chunk) => { body += chunk; });
      response.on('end', () => {
        try { resolve(JSON.parse(body)); } catch { resolve(null); }
      });
      response.on('error', () => resolve(null));
    });
    request.on('timeout', () => { request.destroy(); resolve(null); });
    request.on('error', () => resolve(null));
  });
}

module.exports = { fetchAdvice };
