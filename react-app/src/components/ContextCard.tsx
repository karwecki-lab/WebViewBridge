import React from 'react';
import type { ActionContext } from '../types';

const REDACTED = new Set(['customerId']);

export const ContextCard: React.FC<{ ctx: ActionContext; title?: string }> = ({
  ctx, title = 'Action Context',
}) => (
  <div style={S.card}>
    <p style={S.label}>{title}</p>
    <div style={S.grid}>
      {(Object.entries(ctx) as [string, unknown][])
        .filter(([k]) => k !== 'extraParams')
        .map(([k, v]) => (
          <React.Fragment key={k}>
            <span style={S.key}>{k}</span>
            <span style={S.val}>{REDACTED.has(k) ? '••••' : String(v)}</span>
          </React.Fragment>
        ))}
    </div>
  </div>
);

const S: Record<string,React.CSSProperties> = {
  card:  {background:'#0f172a', border:'1px solid #1e293b', borderRadius:10, padding:14},
  label: {color:'#475569', fontSize:11, textTransform:'uppercase', letterSpacing:1, margin:'0 0 10px'},
  grid:  {display:'grid', gridTemplateColumns:'auto 1fr', columnGap:16, rowGap:6},
  key:   {color:'#64748b', fontSize:12},
  val:   {color:'#f1f5f9', fontSize:13, fontWeight:600},
};
