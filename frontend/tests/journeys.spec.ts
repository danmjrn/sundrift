import AxeBuilder from '@axe-core/playwright';
import {test,expect,type Page} from '@playwright/test';
let browserErrors:string[]=[];
test.beforeEach(async({page})=>{browserErrors=[];page.on('pageerror',e=>browserErrors.push(e.message));page.on('response',r=>{if(r.status()>=500)browserErrors.push(`${r.status()} ${r.url()}`);});});
test.afterEach(async()=>{expect(browserErrors).toEqual([]);});
async function audit(page:Page){const result=await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21a','wcag21aa']).analyze();expect(result.violations).toEqual([]);}
function future(days:number){const d=new Date();d.setUTCDate(d.getUTCDate()+days);return d.toISOString().slice(0,10);}
async function demo(page:Page,role:string,next='/'){await page.goto('/login?next='+encodeURIComponent(next));await page.getByRole('button',{name:'Demo '+role,exact:true}).click();await expect(page).not.toHaveURL(/\/login/);}
test('guest search, wishlist, persisted checkout, host reservation and cancellation',async({page},testInfo)=>{
 await demo(page,'guest','/search');
 await page.getByLabel('Destination',{exact:true}).fill('Cape Town');await page.getByLabel('Check in',{exact:true}).fill(future(60));await page.getByLabel('Check out',{exact:true}).fill(future(62));await page.getByRole('button',{name:'Find a stay'}).click();
 await page.getByRole('combobox',{name:'Amenities',exact:true}).selectOption('Parking');await page.getByLabel('To',{exact:true}).fill('4000');await page.getByRole('button',{name:'Apply filters'}).click();
 await expect(page.locator('.stay-card').first()).toBeVisible();await page.locator('.stay-card').first().getByRole('button',{name:/^Save /}).click();await expect(page.locator('.stay-card').first().getByRole('button',{name:/^Unsave /})).toBeVisible();
 await page.locator('.stay-card').first().getByRole('link',{name:/^Explore /}).click();await expect(page.locator('.booking-card .price-breakdown')).toBeVisible();
 await page.getByRole('button',{name:'Continue to reservation'}).click();await expect(page).toHaveURL(/\/checkout\//);
 await page.screenshot({path:`../docs/screenshots/checkout-${testInfo.project.name}.png`,fullPage:true,scale:'css'});await audit(page);await page.getByRole('checkbox').check();await page.getByRole('button',{name:'Confirm simulated reservation'}).click();await expect(page).toHaveURL(/\/trips\//);
 await expect(page.getByText('confirmed',{exact:true})).toBeVisible();const tripUrl=page.url();const reference=(await page.locator('main>.wrap>.eyebrow').innerText()).trim();await page.reload();await expect(page.getByText('confirmed',{exact:true})).toBeVisible();
 await demo(page,'host','/host');await expect(page.getByRole('cell',{name:reference,exact:true})).toBeVisible();await page.screenshot({path:`../docs/screenshots/host-${testInfo.project.name}.png`,fullPage:true,scale:'css'});
 await page.getByLabel('Block from').fill(future(80));await page.getByLabel('Until, exclusive').fill(future(82));await page.getByRole('button',{name:'Block dates',exact:true}).click();await expect(page.getByText('Your dates have been blocked.')).toBeVisible();await page.locator('.block-row').last().getByRole('button',{name:'Unblock',exact:true}).click();await expect(page.getByText('Dates released.')).toBeVisible();
 await demo(page,'guest',new URL(tripUrl).pathname);await audit(page);await page.getByRole('button',{name:'Preview cancellation'}).click();await expect(page.getByText('Your simulated refund:')).toBeVisible();await page.screenshot({path:`../docs/screenshots/trip-${testInfo.project.name}.png`,fullPage:true,scale:'css'});await page.getByRole('button',{name:'Confirm cancellation'}).click();await expect(page.getByText(/Cancellation saved/)).toBeVisible();await page.reload();await expect(page.getByText('cancelled',{exact:true})).toBeVisible();
 await page.goto('/trips');await page.getByRole('tab',{name:'Completed',exact:true}).click();await page.locator('.trip-row').filter({hasText:'An open-sky kind of stay'}).click();await page.getByLabel('Your review',{exact:true}).fill('A peaceful fictional stay, reviewed through the real guest journey.');await page.getByRole('button',{name:'Share review',exact:true}).click();await expect(page.getByText('Your review has been saved. One review is allowed per completed stay.')).toBeVisible();await page.reload();await expect(page.getByText('Your words are part of the story.')).toBeVisible();
 await page.goto('/wishlist');await expect(page.locator('.stay-card')).toHaveCount(1);await page.reload();await expect(page.locator('.stay-card').getByRole('button',{name:/^Unsave /})).toBeVisible();
});
test('host listing creation, real image upload, admin publication and audit',async({page},testInfo)=>{
 await demo(page,'host','/host/listings/new');const title='The browser test cabin '+Date.now();
 await page.getByLabel('Stay title',{exact:true}).fill(title);await page.getByLabel('The story of your stay').fill('A fictional cabin created through the real host workflow.');await page.getByRole('button',{name:'Save draft',exact:true}).click();await expect(page.getByText(/Draft saved/)).toBeVisible();
 await page.getByLabel('Add a photograph').setInputFiles('public/images/stay-02.jpg');await page.getByRole('button',{name:'Upload photograph'}).click();await expect(page.getByText('Photograph uploaded and saved.')).toBeVisible();await page.getByRole('button',{name:'Submit for publication'}).click();await expect(page.getByText(/Submitted for administrator review/)).toBeVisible();
 await demo(page,'admin','/admin');await page.screenshot({path:`../docs/screenshots/admin-${testInfo.project.name}.png`,fullPage:true,scale:'css'});const row=page.getByRole('row').filter({has:page.getByRole('cell',{name:title,exact:true})});await row.getByRole('button',{name:'Approve & publish'}).click();await expect(row.getByRole('cell',{name:'published',exact:true})).toBeVisible();
 await row.getByRole('button',{name:'Unpublish',exact:true}).click();await expect(row.getByRole('cell',{name:'unpublished',exact:true})).toBeVisible();await page.getByRole('button',{name:'Audit',exact:true}).click();await expect(page.getByRole('cell',{name:'unpublished',exact:true})).toBeVisible();
});
test('protected routes, keyboard navigation, error state and narrow layout',async({page},testInfo)=>{
 await page.goto('/trips');await expect(page).toHaveURL(/\/login/);
 await page.goto('/');await page.screenshot({path:`../docs/screenshots/home-${testInfo.project.name}.png`,fullPage:true,scale:'css'});await page.keyboard.press('Tab');await expect(page.getByRole('link',{name:'Skip to content'})).toBeFocused();
 await expect(page.locator('body')).toHaveJSProperty('scrollWidth',await page.locator('body').evaluate(x=>x.clientWidth));
 await page.goto('/search?destination=NoSuchPlace');await expect(page.getByRole('heading',{name:'A different somewhere?'})).toBeVisible();
 await page.goto('/search?checkIn=2020-01-01&checkOut=2020-01-03');await expect(page.getByRole('main').getByText(/Choose 1–30 nights/)).toBeVisible();
});
