import React, { useState } from 'react';
import type { ActionContext } from '../types';
import { ContextCard } from '../components/ContextCard';
import { sdkBridge } from '../services/SdkBridge';

type Step = 'info' | 'confirm' | 'done';

export const PolicyResignationPage: React.FC<{ ctx: ActionContext }> = ({ ctx }) => {
  const [step, setStep] = useState<Step>('info');
  const [input, setInput] = useState('');

  const resign = () => {
    setStep('done');
    sdkBridge.returnResult('RESIGNED', { objectId: ctx.objectId });
  };

  return (
    <div style={S.page}>
      <div style={S.header}>
        <h1 style={S.h1}>Policy Resignation</h1>
        <Badge>RESIGNATION</Badge>
      </div>

      <ContextCard ctx={ctx} title="Policy context" />

      {step === 'info' && (
        <>
          <div style={S.warning}>
            <p style={S.warnTitle}>⚠️ Important — Please Read Before Proceeding</p>
            <p style={S.text}>
              You are about to resign from policy <strong style={{color:'#fbbf24'}}>{ctx.objectId}</strong>.
              This is a <strong style={{color:'#fbbf24'}}>permanent</strong> action.
            </p>
            <ul style={S.list}>
              <li>All coverage ends on the resignation effective date.</li>
              <li>No refund is available after the 14-day cooling-off period.</li>
              <li>This action cannot be reversed once confirmed.</li>
            </ul>
          </div>

          <AttrTable rows={[
            ['Policy ID',   ctx.objectId],
            ['Object Type', ctx.objectType],
            ['Action',      ctx.action],
            ['Distributor', ctx.distributorId],
            ['Language',    ctx.lang.toUpperCase()],
            ['UI Mode',     ctx.uiMode],
          ]} />

          <div style={S.btns}>
            <button style={S.cancel} onClick={() => sdkBridge.navigateBack()}>← Back</button>
            <button style={S.warn}   onClick={() => setStep('confirm')}>Proceed →</button>
          </div>
        </>
      )}

      {step === 'confirm' && (
        <div style={S.confirmBox}>
          <p style={S.confirmTitle}>Final Confirmation</p>
          <p style={S.text}>Type <strong style={{color:'#fbbf24'}}>RESIGN</strong> to confirm.</p>
          <input
            style={S.confirmInput}
            placeholder="Type RESIGN"
            value={input}
            onChange={e => setInput(e.target.value)}
          />
          <div style={S.btns}>
            <button style={S.cancel} onClick={() => setStep('info')}>Cancel</button>
            <button
              style={{...S.danger, opacity: input === 'RESIGN' ? 1 : 0.4,
                cursor: input === 'RESIGN' ? 'pointer' : 'not-allowed'}}
              disabled={input !== 'RESIGN'}
              onClick={resign}>
              Confirm Resignation
            </button>
          </div>
        </div>
      )}

      {step === 'done' && (
        <div style={S.success}>
          ✅ Resignation for policy <strong>{ctx.objectId}</strong> has been submitted.
        </div>
      )}
    </div>
  );
};

const Badge: React.FC<{children?: React.ReactNode}> = ({children}) => (
  <span style={{background:'#ef4444', color:'#fff', borderRadius:6,
    padding:'4px 12px', fontSize:12, fontWeight:700}}>{children}</span>
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
  page:         {display:'flex', flexDirection:'column', gap:16},
  header:       {display:'flex', alignItems:'center', gap:12},
  h1:           {color:'#f1f5f9', fontSize:22, margin:0},
  warning:      {background:'#1c1917', border:'1px solid #78350f', borderRadius:10, padding:16},
  warnTitle:    {color:'#fbbf24', fontSize:15, fontWeight:700, marginBottom:8},
  text:         {color:'#94a3b8', fontSize:13, lineHeight:1.6},
  list:         {color:'#94a3b8', fontSize:13, paddingLeft:20, lineHeight:2},
  btns:         {display:'flex', gap:10},
  cancel:       {flex:1, padding:13, background:'#1e293b', color:'#94a3b8',
                  border:'none', borderRadius:10, cursor:'pointer'},
  warn:         {flex:2, padding:13, background:'#d97706', color:'#fff',
                  border:'none', borderRadius:10, fontWeight:700, cursor:'pointer'},
  danger:       {flex:2, padding:13, background:'#dc2626', color:'#fff',
                  border:'none', borderRadius:10, fontWeight:700, transition:'opacity .2s'},
  confirmBox:   {background:'#1c1917', border:'1px solid #78350f', borderRadius:10,
                  padding:20, display:'flex', flexDirection:'column', gap:14},
  confirmTitle: {color:'#fbbf24', fontSize:16, fontWeight:700},
  confirmInput: {padding:'10px 14px', background:'#0f172a', border:'1px solid #475569',
                  borderRadius:8, color:'#f1f5f9', fontSize:14},
  success:      {background:'#064e3b', color:'#6ee7b7', padding:18,
                  borderRadius:10, textAlign:'center', fontSize:14},
};
