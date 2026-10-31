import {test,expect} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {writeFile} from 'node:fs/promises';
test('responsive real screenshots, image loading, accessible catalogue and calendar',async({page})=>{
 const sizes=[{name:'desktop',width:1440,height:1000},{name:'tablet',width:768,height:1024},{name:'phone',width:390,height:844}];
 const measured:{viewport:string;component:string;foreground:string;background:string;ratio:number;minimum:number}[]=[];
 function ratio(a:string,b:string){const lum=(rgb:string)=>{const c=(rgb.match(/[0-9.]+/g)||[]).slice(0,3).map(Number).map(x=>x/255).map(x=>x<=0.04045?x/12.92:Math.pow((x+0.055)/1.055,2.4));return 0.2126*c[0]+0.7152*c[1]+0.0722*c[2];};const x=lum(a),y=lum(b);return(Math.max(x,y)+0.05)/(Math.min(x,y)+0.05);}
 function check(viewport:string,component:string,foreground:string,background:string,minimum:number){const actual=ratio(foreground,background);measured.push({viewport,component,foreground,background,ratio:actual,minimum});expect(actual,`${viewport} ${component}`).toBeGreaterThanOrEqual(minimum);}
 const failures:string[]=[];page.on('pageerror',e=>failures.push(e.message));
 for(const size of sizes){
  await page.setViewportSize(size);await page.goto('/');await page.getByRole('main').locator('.hero-image img').filter({visible:true}).evaluate(async(image:HTMLImageElement)=>{if(!image.complete)await new Promise(resolve=>image.addEventListener('load',resolve,{once:true}));});
  expect(await page.getByRole('main').locator('.hero-image img').filter({visible:true}).evaluate((image:HTMLImageElement)=>image.naturalWidth)).toBeGreaterThan(0);
  await page.getByRole('main').locator('img').evaluateAll(async images=>{await Promise.all(images.filter(image=>{const r=image.getBoundingClientRect();return r.top<innerHeight&&r.bottom>0;}).map(image=>(image as HTMLImageElement).decode()));});
  await expect(page.locator('body')).toHaveJSProperty('scrollWidth',size.width);
  await page.screenshot({path:`../docs/screenshots/home-${size.name}-viewport.png`,scale:'css'});
  const group=await page.getByRole('main').locator('.search-bar').filter({visible:true}).evaluate(el=>{const s=getComputedStyle(el);return {background:s.backgroundColor,border:s.borderTopColor};});
  const search=await page.getByRole('main').getByLabel('Destination',{exact:true}).filter({visible:true}).evaluate(el=>({text:getComputedStyle(el).color,placeholder:getComputedStyle(el,'::placeholder').color}));
  check(size.name,'search input group boundary',group.border,group.background,3);check(size.name,'search input text',search.text,group.background,4.5);check(size.name,'search placeholder',search.placeholder,group.background,4.5);
  const home=await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21a','wcag21aa']).analyze();expect(home.violations).toEqual([]);
  await page.goto('/stays/the-clifton-house?checkIn='+new Date(Date.now()+70*86400000).toISOString().slice(0,10)+'&checkOut='+new Date(Date.now()+72*86400000).toISOString().slice(0,10)+'&guests=2');
  await expect(page.getByRole('main').locator('.booking-card .price-breakdown').filter({visible:true})).toBeVisible();await page.screenshot({path:`../docs/screenshots/stay-${size.name}.png`,fullPage:true,scale:'css'});
  await expect(page.locator('body')).toHaveJSProperty('scrollWidth',size.width);
  const input=await page.getByRole('main').getByLabel('Stay check in',{exact:true}).filter({visible:true}).evaluate(el=>{const s=getComputedStyle(el);return {text:s.color,background:s.backgroundColor,border:s.borderTopColor};});
  check(size.name,'booking input text',input.text,input.background,4.5);check(size.name,'booking input boundary',input.border,input.background,3);
  const disabled=await page.getByRole('button',{name:'Previous month',exact:true}).evaluate(el=>{const s=getComputedStyle(el);return {text:s.color,background:s.backgroundColor};});check(size.name,'disabled control text',disabled.text,disabled.background,4.5);
  await page.getByRole('main').getByLabel('Stay check in',{exact:true}).filter({visible:true}).focus();
  const checkout=page.getByRole('main').getByLabel('Stay check out',{exact:true}).filter({visible:true});
  // Native date inputs expose several keyboard segments; Tab must reach the next field.
  for(let step=0;step<8;step++){await page.keyboard.press('Tab');if(await checkout.evaluate(el=>el===document.activeElement))break;}
  await expect(checkout).toBeFocused();
  const focus=await page.getByRole('main').getByLabel('Stay check out',{exact:true}).filter({visible:true}).evaluate(el=>{const s=getComputedStyle(el);return {outline:s.outlineColor,background:getComputedStyle(document.body).backgroundColor};});check(size.name,'keyboard focus outline',focus.outline,focus.background,3);
  const stay=await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21a','wcag21aa']).analyze();expect(stay.violations).toEqual([]);
  await page.getByRole('button',{name:'View all photographs',exact:true}).click();await expect(page.getByRole('dialog')).toBeVisible();await page.keyboard.press('Escape');await expect(page.getByRole('dialog')).not.toBeVisible();
 }
 await writeFile('../docs/contrast-results.json',JSON.stringify(measured,null,2)+'\n');
 expect(failures).toEqual([]);
});
