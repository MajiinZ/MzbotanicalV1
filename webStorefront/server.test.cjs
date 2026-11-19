'use strict';
const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),os=require('node:os'),path=require('node:path');
const {createStorefrontServer}=require('./server.cjs');
async function fixture(t) {
  const directory=fs.mkdtempSync(path.join(os.tmpdir(),'mz-web-test-'));
  fs.writeFileSync(path.join(directory,'index.html'),'<!doctype html><title>MZ Botanicals</title>');
  fs.mkdirSync(path.join(directory,'assets'));fs.writeFileSync(path.join(directory,'assets/app.js'),'console.log("web");');
  const server=createStorefrontServer({directory});await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  t.after(async()=>{
    await new Promise(resolve=>{server.close(resolve);server.closeAllConnections();});
    const resolved=path.resolve(directory),allowed=path.resolve(os.tmpdir())+path.sep;
    if(!resolved.startsWith(allowed) || !path.basename(resolved).startsWith('mz-web-test-'))throw Error('Unsafe test cleanup.');
    fs.rmSync(resolved,{recursive:true,force:true});
  });
  return 'http://127.0.0.1:'+server.address().port;
}
test('Cloud Run health endpoint identifies the real storefront with checkout disabled',async t=>{
  const base=await fixture(t),response=await fetch(base+'/healthz');
  assert.equal(response.status,200);assert.deepEqual(await response.json(),{status:'ok',service:'mz-botanicals-web',checkoutEnabled:false});
});
test('SPA routes and browser assets are served with correct content and authentication-compatible security headers',async t=>{
  const base=await fixture(t),page=await fetch(base+'/orders');
  assert.equal(page.status,200);assert((await page.text()).includes('MZ Botanicals'));
  assert(page.headers.get('content-security-policy').includes("object-src 'none'"));
  assert.equal(page.headers.get('cross-origin-opener-policy'),'same-origin-allow-popups');
  const asset=await fetch(base+'/assets/app.js');assert.equal(asset.status,200);assert(asset.headers.get('content-type').startsWith('text/javascript'));
  assert.equal(asset.headers.get('cache-control'),'no-cache');
  const head=await fetch(base+'/',{method:'HEAD'});assert.equal(head.status,200);assert.equal(await head.text(),'');
});
test('server rejects writes and never exposes filesystem paths or substitutes HTML for missing JS',async t=>{
  const base=await fixture(t);
  assert.equal((await fetch(base+'/',{method:'POST',body:'private'})).status,405);
  for(const url of ['/assets/missing.js','/.env','/%2e%2e%2fserver.cjs','/%00','/brand/missing.png','/..%5csecret']) {
    const response=await fetch(base+url);assert.notEqual(response.status,200,url);assert(!(await response.text()).includes('require('));
  }
});
