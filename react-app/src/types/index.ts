export type ObjectType = 'POLICY' | 'SUBSCRIPTION';
export type ActionType = 'ISSUE' | 'WITHDRAWAL' | 'RESIGNATION';
export type UIMode    = 'dark' | 'light';

export interface ActionContext {
  distributorId: string;
  customerId:    string;
  objectId:      string;
  objectType:    ObjectType;
  action:        ActionType;
  lang:          string;
  uiMode:        UIMode;
  extraParams?:  Record<string, string>;
}

export interface BridgeMessage {
  action: string;
  token:  string;
  params: Record<string, string>;
}

export type Route = 'subscription-issue' | 'policy-withdrawal' | 'policy-resignation' | null;

export function resolveRoute(ctx: ActionContext): Route {
  if (ctx.objectType === 'SUBSCRIPTION' && ctx.action === 'ISSUE')       return 'subscription-issue';
  if (ctx.objectType === 'POLICY'       && ctx.action === 'WITHDRAWAL')  return 'policy-withdrawal';
  if (ctx.objectType === 'POLICY'       && ctx.action === 'RESIGNATION') return 'policy-resignation';
  return null;
}
