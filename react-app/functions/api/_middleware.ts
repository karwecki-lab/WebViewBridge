// functions/api/_middleware.ts
// Applied to every request under /api/

export const onRequest: PagesFunction = async (ctx) => {
  const response = await ctx.next();

  // Add CORS headers for WebView cross-origin requests
  const headers = new Headers(response.headers);
  headers.set('Access-Control-Allow-Origin', '*');
  headers.set('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  headers.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (ctx.request.method === 'OPTIONS') {
    return new Response(null, { status: 204, headers });
  }

  return new Response(response.body, {
    status: response.status,
    headers,
  });
};
