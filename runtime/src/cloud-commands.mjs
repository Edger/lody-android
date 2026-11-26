export function cloudCommands(runtime, access, stopSessions) {
 let connected=false, selected;
 return {
  connect(workspaceId,gatewayBaseUrl,userId,workspaceReadOnly) {
   if(connected)throw Error('already_connected');
   if(userId!==undefined && (typeof userId!=='string' || !userId.trim() || userId.length>128))throw Error('invalid_identity');
   access.configure({workspaceId,gatewayBaseUrl});
   connected=true;
   // The verified native identity enables presence for creation; offline probes omit it.
   // A read-only member (viewer) is offered no Status view, pin or section writes by the runtime.
   return runtime.start(workspaceId,userId,Boolean(userId),workspaceReadOnly===true);
  },
  watchSession(id,historyStart,_signal) {
   if(!connected)throw Error('not_connected');
   if(typeof id!=='string' || !/^[a-zA-Z0-9_-]{1,128}$/.test(id))throw Error('invalid_session');
   stopSessions();selected=id;
   // The dispatcher appends its AbortSignal to every command, so an omitted historyStart arrives
   // in this position; only a non-negative integer from native is a history start.
   return runtime.session(id,Number.isInteger(historyStart)&&historyStart>=0?historyStart:undefined);
  },
  closeSession(id) {
   if(selected===id){stopSessions();selected=undefined;}
   return true;
  },
  grant(value) {
   if(value!==null && (!value || typeof value.token!=='string' || typeof value.gatewayBaseUrl!=='string' || !Number.isFinite(value.expiresIn) || value.expiresIn<=0))throw Error('invalid_grant');
   runtime.grant(value);return true;
  },
 };
}
