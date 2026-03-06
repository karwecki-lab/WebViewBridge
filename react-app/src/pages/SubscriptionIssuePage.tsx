import React from 'react';
import type { ActionContext } from '../types';
import { ContextCard } from '../components/ContextCard';
import { sdkBridge } from '../services/SdkBridge';

export const SubscriptionIssuePage: React.FC<{ ctx: ActionContext }> = ({ ctx }) => (
  <div style={S.page}>
    <div style={S.header}>
      <h1 style={S.h1}>New Subscription</h1>
      <Badge color="#10b981">ISSUE</Badge>
    </div>

    <ContextCard ctx={ctx} title="Session context" />

    {/* Product catalogue — the web-app handles all further navigation */}
    <div style={S.frameWrap}>
      <div style={S.frameBar}>
        <span style={S.frameLabel}>Product Catalogue</span>
        <span style={S.frameUrl}>nnw-frontend.int.npr.dbyc.cardif.io/en/products</span>
      </div>
      <iframe
        src="https://nnw-frontend.int.npr.dbyc.cardif.io/en/products"
        style={S.frame}
        title="Product catalogue"
        sandbox="allow-scripts allow-same-origin allow-forms"
      />
    </div>

    <div style={S.actions}>
      <button style={S.secondary} onClick={() => sdkBridge.navigateBack()}>← Back</button>
      <button style={S.primary}
        onClick={() => sdkBridge.returnResult('ISSUED', { objectId: ctx.objectId })}>
        Confirm & Issue
      </button>
    </div>
  </div>
);

const Badge: React.FC<{ color: string; children: React.ReactNode }> = ({ color, children }) => (
  <span style={{ background: color, color:'#fff', borderRadius:6,
    padding:'4px 12px', fontSize:12, fontWeight:700 }}>{children}</span>
);

const S: Record<string,React.CSSProperties> = {
  page:      {display:'flex', flexDirection:'column', gap:16},
  header:    {display:'flex', alignItems:'center', gap:12},
  h1:        {color:'#f1f5f9', fontSize:22, margin:0},
  frameWrap: {border:'1px solid #1e293b', borderRadius:10, overflow:'hidden'},
  frameBar:  {background:'#1e293b', padding:'8px 14px',
               display:'flex', justifyContent:'space-between', alignItems:'center'},
  frameLabel:{color:'#94a3b8', fontSize:11, textTransform:'uppercase', letterSpacing:1},
  frameUrl:  {color:'#475569', fontSize:11},
  frame:     {width:'100%', height:400, border:'none', display:'block', background:'#fff'},
  actions:   {display:'flex', gap:10},
  secondary: {flex:1, padding:13, background:'#1e293b', color:'#94a3b8',
               border:'none', borderRadius:10, cursor:'pointer', fontSize:14},
  primary:   {flex:2, padding:13, background:'#10b981', color:'#fff',
               border:'none', borderRadius:10, fontSize:14, fontWeight:700, cursor:'pointer'},
};
