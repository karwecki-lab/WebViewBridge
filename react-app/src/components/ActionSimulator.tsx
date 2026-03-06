import React, { useState } from 'react';
import type { ActionContext, ObjectType, ActionType, UIMode } from '../types';

interface Props { onTrigger: (ctx: ActionContext) => void; }

const PRESETS: { label: string; color: string; ctx: ActionContext }[] = [
  {
    label: 'SUBSCRIPTION + ISSUE', color: '#10b981',
    ctx: { distributorId:'DIST-001', customerId:'CUST-12345', objectId:'OBJ-001',
           objectType:'SUBSCRIPTION', action:'ISSUE', lang:'en', uiMode:'light' },
  },
  {
    label: 'POLICY + WITHDRAWAL', color: '#f59e0b',
    ctx: { distributorId:'DIST-001', customerId:'CUST-12345', objectId:'POL-999',
           objectType:'POLICY', action:'WITHDRAWAL', lang:'en', uiMode:'light' },
  },
  {
    label: 'POLICY + RESIGNATION', color: '#ef4444',
    ctx: { distributorId:'DIST-001', customerId:'CUST-12345', objectId:'POL-999',
           objectType:'POLICY', action:'RESIGNATION', lang:'en', uiMode:'light' },
  },
];

const BLANK: ActionContext = {
  distributorId:'DIST-001', customerId:'CUST-12345', objectId:'OBJ-001',
  objectType:'SUBSCRIPTION', action:'ISSUE', lang:'en', uiMode:'light',
};

export const ActionSimulator: React.FC<Props> = ({ onTrigger }) => {
  const [custom, setCustom] = useState<ActionContext>(BLANK);
  const [open, setOpen] = useState(false);

  return (
    <div style={S.wrap}>
      <div style={S.header}>
        <span style={S.title}>⚡ Action Simulator</span>
        <span style={S.sub}>Simulates native SDK button triggers</span>
      </div>

      {/* Preset buttons */}
      <div style={S.row}>
        {PRESETS.map(p => (
          <button key={p.label} style={{...S.btn, background: p.color}} onClick={() => onTrigger(p.ctx)}>
            {p.label}
          </button>
        ))}
      </div>

      {/* Custom form */}
      <button style={S.toggleBtn} onClick={() => setOpen(o => !o)}>
        {open ? '▲ Hide' : '▼ Custom action'}
      </button>

      {open && (
        <div style={S.form}>
          <Row label="Distributor ID">
            <input style={S.input} value={custom.distributorId}
              onChange={e => setCustom(c => ({...c, distributorId: e.target.value}))} />
          </Row>
          <Row label="Customer ID">
            <input style={S.input} value={custom.customerId}
              onChange={e => setCustom(c => ({...c, customerId: e.target.value}))} />
          </Row>
          <Row label="Object ID">
            <input style={S.input} value={custom.objectId}
              onChange={e => setCustom(c => ({...c, objectId: e.target.value}))} />
          </Row>
          <Row label="Object Type">
            <select style={S.input} value={custom.objectType}
              onChange={e => setCustom(c => ({...c, objectType: e.target.value as ObjectType}))}>
              <option>SUBSCRIPTION</option><option>POLICY</option>
            </select>
          </Row>
          <Row label="Action">
            <select style={S.input} value={custom.action}
              onChange={e => setCustom(c => ({...c, action: e.target.value as ActionType}))}>
              <option>ISSUE</option><option>WITHDRAWAL</option><option>RESIGNATION</option>
            </select>
          </Row>
          <Row label="Language">
            <select style={S.input} value={custom.lang}
              onChange={e => setCustom(c => ({...c, lang: e.target.value}))}>
              {['en','pl','de','fr','es'].map(l => <option key={l}>{l}</option>)}
            </select>
          </Row>
          <Row label="UI Mode">
            <select style={S.input} value={custom.uiMode}
              onChange={e => setCustom(c => ({...c, uiMode: e.target.value as UIMode}))}>
              <option>light</option><option>dark</option>
            </select>
          </Row>
          <button style={{...S.btn, background:'#6366f1', marginTop: 8}} onClick={() => onTrigger(custom)}>
            Trigger Custom Action →
          </button>
        </div>
      )}
    </div>
  );
};

const Row: React.FC<{label: string; children: React.ReactNode}> = ({label, children}) => (
  <div style={{display:'flex', alignItems:'center', gap:8}}>
    <span style={{color:'#64748b', fontSize:12, width:110, flexShrink:0}}>{label}</span>
    {children}
  </div>
);

const S: Record<string,React.CSSProperties> = {
  wrap:      {background:'#1e293b', borderRadius:12, padding:20, marginBottom:16},
  header:    {display:'flex', alignItems:'baseline', gap:12, marginBottom:14},
  title:     {color:'#f1f5f9', fontWeight:700, fontSize:16},
  sub:       {color:'#475569', fontSize:12},
  row:       {display:'flex', gap:8, flexWrap:'wrap', marginBottom:10},
  btn:       {padding:'10px 18px', color:'#fff', border:'none', borderRadius:8,
               cursor:'pointer', fontSize:13, fontWeight:600, flex:1, minWidth:160},
  toggleBtn: {background:'none', border:'1px solid #334155', color:'#94a3b8',
               padding:'6px 14px', borderRadius:6, cursor:'pointer', fontSize:12},
  form:      {marginTop:14, display:'flex', flexDirection:'column', gap:10},
  input:     {flex:1, padding:'6px 10px', borderRadius:6, border:'1px solid #334155',
               background:'#0f172a', color:'#f1f5f9', fontSize:13},
};
