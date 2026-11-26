import test from 'node:test';
import assert from 'node:assert/strict';
import {cloudCommands} from '../src/cloud-commands.mjs';
import {createDispatcher} from '../src/dispatch.mjs';
test('read probe never enables presence or supplies identity for implicit Machine RPC',()=>{
 const calls=[];const runtime={start:(...args)=>calls.push(args),grant(){},setActive:value=>calls.push(value)};
 const access={configure(){},setActive(){}};
 const commands=cloudCommands(runtime,access);
 commands.connect('workspace','https://streams.example.org');
 assert.deepEqual(calls,[['workspace',undefined,false,false]]);
 assert.throws(()=>commands.connect('other','https://streams.example.org'),/already_connected/);
 assert.equal(commands.sendTurn,undefined);
});
test('session subscription validates ids and closes only the matching selection',async()=>{
 const calls=[];
 const commands=cloudCommands({start(){},session:id=>{calls.push(id);return 'watching';}}, {configure(){}},()=>calls.push('stop'));
 assert.throws(()=>commands.watchSession('s'),/not_connected/);
 commands.connect('workspace','https://streams.example.org');
 assert.throws(()=>commands.watchSession('../other'),/invalid_session/);
 await commands.watchSession('s1');await commands.watchSession('s2');
 commands.closeSession('s1');
 assert.deepEqual(calls,['stop','s1','stop','s2']);
 commands.closeSession('s2');assert.equal(calls.at(-1),'stop');
 assert.equal(commands.sendTurn,undefined);
});
test('an omitted history start never forwards the dispatcher signal to the runtime',async()=>{
 const starts=[];const replies=[];
 const commands=cloudCommands({start(){},session:(id,start)=>{starts.push([id,start]);return true;}},{configure(){}},()=>{});
 commands.connect('workspace','https://streams.example.org');
 const dispatch=createDispatcher({},reply=>replies.push(reply),commands);
 await dispatch({id:'a',method:'watchSession',args:['s1']});
 await dispatch({id:'b',method:'watchSession',args:['s2',12]});
 assert.deepEqual(starts,[['s1',undefined],['s2',12]]);
 assert.deepEqual(replies,[{type:'reply',id:'a',value:true},{type:'reply',id:'b',value:true}]);
});
test('native verified identity enables presence and rejects malformed identities',()=>{
 const calls=[];const c=cloudCommands({start:(...args)=>calls.push(args)}, {configure(){}});
 assert.throws(()=>c.connect('workspace','https://streams.example.org',{}),/invalid_identity/);
 c.connect('workspace','https://streams.example.org','user');assert.deepEqual(calls,[['workspace','user',true,false]]);
 // Only an explicit true marks a viewer read-only; anything else leaves the runtime's writes offered.
 const viewer=[];cloudCommands({start:(...args)=>viewer.push(args)}, {configure(){}}).connect('workspace','https://streams.example.org','user',true);
 assert.deepEqual(viewer,[['workspace','user',true,true]]);
 const malformed=[];cloudCommands({start:(...args)=>malformed.push(args)}, {configure(){}}).connect('workspace','https://streams.example.org','user','yes');
 assert.deepEqual(malformed,[['workspace','user',true,false]]);
});
