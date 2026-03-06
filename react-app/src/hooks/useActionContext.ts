import { useState, useEffect } from 'react';
import type { ActionContext } from '../types';
import { sdkBridge } from '../services/SdkBridge';

/**
 * Returns the current ActionContext, either injected by the native bridge
 * or simulated via ActionSimulator in dev mode.
 * Re-renders automatically when context changes.
 */
export function useActionContext(): ActionContext | null {
  const [ctx, setCtx] = useState<ActionContext | null>(sdkBridge.getContext());
  useEffect(() => sdkBridge.subscribe(setCtx), []);
  return ctx;
}
