'use strict';
const fs=require('node:fs'),path=require('node:path'),{spawnSync}=require('node:child_process');
async function main() {
  const tests=[path.join(__dirname,'server.test.cjs')],domain=path.join(__dirname,'src/domain.test.ts');
  if(fs.existsSync(domain)) {
    const directory=path.join(__dirname,'.test-build');fs.mkdirSync(directory,{recursive:true});
    const bundled=path.join(directory,'domain.test.cjs');
    await require('esbuild').build({entryPoints:[domain],bundle:true,outfile:bundled,platform:'node',format:'cjs',target:'node22'});
    tests.push(bundled);
  }
  const result=spawnSync(process.execPath,['--test',...tests],{stdio:'inherit'});process.exitCode=result.status??1;
}
main().catch(()=>{console.error('Unable to run storefront tests.');process.exitCode=1;});
