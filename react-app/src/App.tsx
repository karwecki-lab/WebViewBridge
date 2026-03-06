import React from 'react';
import { useActionContext } from './hooks/useActionContext';
import { resolveRoute } from './types';
import { sdkBridge } from './services/SdkBridge';
import { ActionSimulator } from './components/ActionSimulator';
import { SubscriptionIssuePage }  from './pages/SubscriptionIssuePage';
import { PolicyWithdrawalPage }   from './pages/PolicyWithdrawalPage';
import { PolicyResignationPage }  from './pages/PolicyResignationPage';
import type { ActionContext }     from './types';

const App: React.FC = () => {
  const ctx = useActionContext();
  const route = ctx ? resolveRoute(ctx) : null;

  const handleTrigger = (newCtx: ActionContext) => {
    sdkBridge._simulateContext(newCtx);
  };

  return (
    <div style={S.app}>
      {/* ── Header ── */}
      <header style={S.header}>
        <div style={S.logo}>
          <span style={S.logoMark}>◈</span> WebViewBridge
        </div>
        <div style={S.headerRight}>
          <span style={S.ver}>SDK v2.0</span>
          {ctx && (
            <span style={S.pill}>
              {ctx.objectType} · {ctx.action}
            </span>
          )}
        </div>
      </header>

      <main style={S.main}>
        {/* Always show simulator when running in browser (no native bridge injected) */}
        <ActionSimulator onTrigger={handleTrigger} />

        {/* ── Content router ── */}
        <div style={S.content}>
          {!ctx && <EmptyState />}
          {ctx && route === 'subscription-issue'  && <SubscriptionIssuePage  ctx={ctx} />}
          {ctx && route === 'policy-withdrawal'   && <PolicyWithdrawalPage   ctx={ctx} />}
          {ctx && route === 'policy-resignation'  && <PolicyResignationPage  ctx={ctx} />}
          {ctx && !route && (
            <div style={S.unhandled}>
              No screen mapped for <strong>{ctx.objectType}</strong> + <strong>{ctx.action}</strong>
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

const EmptyState: React.FC = () => (
  <div style={{textAlign:'center', padding:'60px 20px'}}>
    <div style={{fontSize:48, marginBottom:16}}>⚡</div>
    <p style={{color:'#475569', fontSize:16}}>
      Select a preset action above to see the corresponding screen.
    </p>
    <p style={{color:'#334155', fontSize:13, marginTop:8}}>
      In production, the native app injects the context via the SDK bridge.
    </p>
  </div>
);

const S: Record<string,React.CSSProperties> = {
  app:         {minHeight:'100vh', background:'#0f172a', color:'#f1f5f9',
                 fontFamily:'system-ui,-apple-system,sans-serif'},
  header:      {display:'flex', justifyContent:'space-between', alignItems:'center',
                 padding:'14px 24px', background:'#1e293b', borderBottom:'1px solid #334155',
                 position:'sticky', top:0, zIndex:10},
  logo:        {display:'flex', alignItems:'center', gap:8, fontSize:18,
                 fontWeight:700, color:'#3b82f6'},
  logoMark:    {fontSize:22},
  headerRight: {display:'flex', alignItems:'center', gap:10},
  ver:         {fontSize:11, color:'#475569', background:'#0f172a',
                 padding:'3px 8px', borderRadius:4},
  pill:        {fontSize:12, color:'#93c5fd', background:'#1e3a5f',
                 padding:'3px 12px', borderRadius:12, fontWeight:600},
  main:        {maxWidth:920, margin:'0 auto', padding:24},
  content:     {background:'#1e293b', borderRadius:12, padding:24},
  unhandled:   {color:'#f87171', padding:20, background:'#1c0e0e', borderRadius:8},
};

export default App;
