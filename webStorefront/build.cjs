'use strict';
const fs=require('node:fs'),path=require('node:path'),zlib=require('node:zlib');
const root=__dirname,output=path.join(root,'dist');
async function main() {
  // The only removed directory is this module's generated dist folder.
  if(path.resolve(output)!==path.join(path.resolve(root),'dist'))throw Error('Invalid output path.');
  fs.rmSync(output,{recursive:true,force:true});fs.mkdirSync(path.join(output,'assets'),{recursive:true});
  fs.copyFileSync(path.join(root,'index.html'),path.join(output,'index.html'));
  const publicRoot=path.join(root,'public');
  function rejectLinks(directory) {
    for(const entry of fs.readdirSync(directory,{withFileTypes:true})) {
      const file=path.join(directory,entry.name);
      if(entry.isSymbolicLink())throw Error('Public assets must not contain filesystem links.');
      if(entry.isDirectory())rejectLinks(file);
    }
  }
  if(fs.existsSync(publicRoot)){rejectLinks(publicRoot);fs.cpSync(publicRoot,output,{recursive:true});}
  await require('esbuild').build({entryPoints:[path.join(root,'src/main.ts')],bundle:true,minify:true,
    outfile:path.join(output,'assets/app.js'),platform:'browser',format:'esm',target:'es2022',legalComments:'none',external:['/brand/*']});
  function compress(directory) {
    for(const entry of fs.readdirSync(directory,{withFileTypes:true})) {
      const file=path.join(directory,entry.name);
      if(entry.isDirectory())compress(file);
      else if(/\.(js|css|html)$/.test(entry.name))fs.writeFileSync(file+'.gz',zlib.gzipSync(fs.readFileSync(file)));
    }
  }
  compress(output);console.log('Built MZ Botanicals web storefront.');
}
main().catch(()=>{console.error('Web storefront build failed.');process.exitCode=1;});
