'use strict';
const http=require('node:http'),fs=require('node:fs'),path=require('node:path');
const CSP="default-src 'self'; script-src 'self' https://apis.google.com https://www.gstatic.com; style-src 'self' 'unsafe-inline'; img-src 'self' https: data:; font-src 'self'; connect-src 'self' https://*.googleapis.com https://*.firebaseio.com wss://*.firebaseio.com https://*.firebaseapp.com; frame-src https://killrsgenetics-461300.firebaseapp.com https://accounts.google.com; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";
const MIME={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8',
  '.json':'application/json; charset=utf-8','.png':'image/png','.webp':'image/webp','.jpg':'image/jpeg','.jpeg':'image/jpeg',
  '.svg':'image/svg+xml','.ico':'image/x-icon','.woff2':'font/woff2','.ttf':'font/ttf','.otf':'font/otf'};
function createStorefrontServer({directory=path.join(__dirname,'dist')}={}) {
  const root=fs.realpathSync(directory),index=path.join(root,'index.html');
  if(!fs.statSync(index).isFile())throw Error('Build the storefront before starting the server.');
  return http.createServer((request,response)=>{
    response.setHeader('Content-Security-Policy',CSP);
    response.setHeader('X-Content-Type-Options','nosniff');response.setHeader('X-Frame-Options','DENY');
    response.setHeader('Referrer-Policy','strict-origin-when-cross-origin');
    response.setHeader('Cross-Origin-Opener-Policy','same-origin-allow-popups');
    response.setHeader('Permissions-Policy','camera=(), microphone=(), geolocation=()');
    response.setHeader('Strict-Transport-Security','max-age=31536000');
    const finish=(status,body)=>{response.writeHead(status,{'Content-Type':'text/plain; charset=utf-8','Cache-Control':'no-store'});response.end(request.method==='HEAD'?undefined:body);};
    if(!['GET','HEAD'].includes(request.method)){response.setHeader('Allow','GET, HEAD');finish(405,'Method not allowed.');return;}
    let pathname;
    try{pathname=decodeURIComponent(new URL(request.url,'http://localhost').pathname);}catch(_){finish(400,'Invalid request.');return;}
    if(pathname.includes('\0') || pathname.includes('\\') || pathname.split('/').some(part=>part.startsWith('.'))){finish(404,'Not found.');return;}
    if(pathname==='/healthz') {
      response.writeHead(200,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'});
      response.end(request.method==='HEAD'?undefined:JSON.stringify({status:'ok',service:'mz-botanicals-web',checkoutEnabled:false}));return;
    }
    let file=path.resolve(root,'.'+pathname);
    if(file!==root && !file.startsWith(root+path.sep)){finish(404,'Not found.');return;}
    try {
      if(!fs.statSync(file).isFile())throw Error('Not a file.');
      file=fs.realpathSync(file);
      if(!file.startsWith(root+path.sep)){finish(404,'Not found.');return;}
    } catch(_) {
      if(path.extname(pathname) || pathname.startsWith('/assets/') || pathname.startsWith('/brand/')){finish(404,'Not found.');return;}
      file=index;
    }
    const headers={'Content-Type':MIME[path.extname(file).toLowerCase()] || 'application/octet-stream',
      'Cache-Control':file.includes(path.sep+'brand'+path.sep)?'public, max-age=86400':'no-cache','Vary':'Accept-Encoding'};
    if(/\bgzip\b/.test(request.headers['accept-encoding']||'') && fs.existsSync(file+'.gz')) {
      headers['Content-Encoding']='gzip';file+='.gz';
    }
    response.writeHead(200,headers);
    if(request.method==='HEAD'){response.end();return;}
    const stream=fs.createReadStream(file);stream.on('error',()=>response.destroy());stream.pipe(response);
  });
}
if(require.main===module) {
  const raw=process.env.PORT || '8080',port=Number(raw);
  if(!/^\d+$/.test(raw) || !Number.isInteger(port) || port<1 || port>65535)throw Error('Invalid server port.');
  createStorefrontServer().listen(port,'0.0.0.0',()=>console.log('MZ Botanicals storefront is listening on port '+port));
}
module.exports={createStorefrontServer};
