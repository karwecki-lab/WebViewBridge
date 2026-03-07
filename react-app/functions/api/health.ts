// functions/api/health.ts
// Cloudflare Pages Function — runs as a Worker at /api/health
// Docs: https://developers.cloudflare.com/pages/functions/

interface Env {
  // Add bindings here if needed:
  // DB: D1Database;
  // KV: KVNamespace;
}

export const onRequestGet: PagesFunction<Env> = async (ctx) => {
  return Response.json({
    status: 'ok',
    app: 'webviewbridge-react-app',
    timestamp: new Date().toISOString(),
    cf: ctx.request.cf?.colo ?? 'unknown',
  });
};
