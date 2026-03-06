import React, { useState } from 'react';
import type { ActionContext } from '../types';
import { ContextCard } from '../components/ContextCard';
import { sdkBridge } from '../services/SdkBridge';

export const PolicyWithdrawalPage: React.FC<{ ctx: ActionContext }> = ({ ctx }) => {
  const [done, setDone] = useState(false);

  const confirm = () => {
    setDone(true);
    sdkBridge.returnResult('WITHDRAWN', { objectId: ctx.objectId, objectType: ctx.objectType });
  };

  return (
    <div style={S.page}>
      <div style={S.header}>
        <h1 style={S.h1}>Policy Withdrawal</h1>
        <Badge>WITHDRAWAL</Badge>
      </div>

      <ContextCard ctx={ctx} title="Policy context" />

      <InfoBox>
        <p style={S.text}>
          You are requesting withdrawal from policy <strong style={{color:'#fbbf24'}}>{ctx.objectId}</strong>.
          This will terminate the policy and all associated coverage.
        </p>
        <ul style={S.list}>
          <li>Withdrawal is effective immediately upon confirmation.</li>
          <li>A confirmation email will be sent within 24 hours.</li>
          <li>Pending claims are not affected by this action.</li>
        </ul>
      </InfoBox>

      <AttrTable rows={[
        ['Policy ID',    ctx.objectId],
        ['Object Type',  ctx.objectType],
        ['Distributor',  ctx.distributorId],
        ['Language',     ctx.lang.toUpperCase()],
        ['UI Mode',      ctx.uiMode],
      ]} />

      {done
        ? <div style={S.success}>✅ Withdrawal confirmed successfully.</div>
        : (
          <div style={S.btns}>
            <button style={S.cancel} onClick={() => sdkBridge.navigateBack()}>Cancel</button>
            <button style={S.danger}  onClick={confirm}>Confirm Withdrawal</button>
          </div>
        )
      }
    </div>
  );
};

// ── Shared sub-components ────────────────────────────────────

const Badge: React.FC<{children?: React.ReactNode}> = ({children}) => (
  <span style={{background:'#f59e0b', color:'#000', borderRadius:6,
    padding:'4px 12px', fontSize:12, fontWeight:700}}>{children}</span>
);

const InfoBox: React.FC<{children: React.ReactNode}> = ({children}) => (
  <div style={{background:'#0f172a', border:'1px solid #334155', borderRadius:10, padding:16}}>
    {children}
  </div>
);

const AttrTable: React.FC<{rows: [string,string][]}> = ({rows}) => (
  <div style={{background:'#0f172a', border:'1px solid #1e293b', borderRadius:10}}>
    {rows.map(([label, value], i) => (
      <div key={i} style={{display:'flex', justifyContent:'space-between',
        padding:'10px 16px', borderBottom: i < rows.length-1 ? '1px solid #1e293b' : 'none'}}>
        <span style={{color:'#64748b', fontSize:13}}>{label}</span>
        <span style={{color:'#f1f5f9', fontSize:13, fontWeight:600}}>{value}</span>
      </div>
    ))}
  </div>
);

const S: Record<string,React.CSSProperties> = {
  page:    {display:'flex', flexDirection:'column', gap:16},
  header:  {display:'flex', alignItems:'center', gap:12},
  h1:      {color:'#f1f5f9', fontSize:22, margin:0},
  text:    {color:'#94a3b8', fontSize:13, lineHeight:1.6, marginBottom:10},
  list:    {color:'#94a3b8', fontSize:13, paddingLeft:20, lineHeight:2, margin:0},
  btns:    {display:'flex', gap:10},
  cancel:  {flex:1, padding:13, background:'#1e293b', color:'#94a3b8',
             border:'none', borderRadius:10, cursor:'pointer'},
  danger:  {flex:2, padding:13, background:'#ef4444', color:'#fff',
             border:'none', borderRadius:10, fontWeight:700, cursor:'pointer'},
  success: {background:'#064e3b', color:'#6ee7b7', padding:16,
             borderRadius:10, textAlign:'center', fontSize:14},
};
