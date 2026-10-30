import Link from 'next/link';
import type {Metadata} from 'next';
import type {SearchResult} from '@/lib/types';
import {serverApi} from '@/lib/server-api';
import {SearchBar} from '@/components/SearchBar';
import {StayCard} from '@/components/StayCard';
import {Empty} from '@/components/States';
export const metadata:Metadata={title:'Explore stays'};
export default async function Search({searchParams}:{searchParams:Promise<Record<string,string|undefined>>}){
 const raw=await searchParams;const values=Object.fromEntries(Object.entries(raw).filter((entry):entry is [string,string]=>typeof entry[1]==='string'));
 const params=new URLSearchParams(values);let result:SearchResult|null=null;let error='';
 try{result=await serverApi<SearchResult>('/catalogue?'+params);}catch(e){error=(e as Error).message;}
 const page=Number(values.page||0);const pageUrl=(next:number)=>{const p=new URLSearchParams(values);p.set('page',String(next));return '/search?'+p;};
 const dateQuery=new URLSearchParams();for(const k of ['checkIn','checkOut','guests'])if(values[k])dateQuery.set(k,values[k]);
 return <div className="wrap page-pad"><p className="eyebrow">Find your somewhere</p><h1 className="page-title">{values.destination||'Somewhere extraordinary.'}</h1><SearchBar values={values} compact/>
 <div className="search-layout"><aside className="filters"><form action="/search"><div className="filter-heading"><h2>Make it yours</h2><Link href="/search">Reset</Link></div>{['destination','checkIn','checkOut','guests'].map(k=><input key={k} type="hidden" name={k} value={values[k]||''}/>)}
 <fieldset><legend>Nightly budget · ZAR</legend><div className="two-fields"><label>From<input type="number" min="0" name="min" placeholder="0" defaultValue={values.min||''}/></label><label>To<input type="number" min="0" name="max" placeholder="Any" defaultValue={values.max||''}/></label></div></fieldset>
 <fieldset><legend>Your kind of space</legend><select name="type" aria-label="Accommodation type" defaultValue={values.type||''}><option value="">All accommodation</option>{['APARTMENT','VILLA','CABIN','SUITE','HOTEL_ROOM'].map(t=><option key={t} value={t}>{t.toLowerCase().replace('_',' ')}</option>)}</select></fieldset>
 <fieldset><legend>The essentials</legend><label>Amenities<select name="amenities" defaultValue={values.amenities||''}><option value="">Any amenities</option>{['Parking','Wi-Fi','Pool','Kitchen','Ocean view','Fireplace','Workspace','Garden'].map(a=><option key={a}>{a}</option>)}</select></label></fieldset>
 <fieldset><legend>Guest rating</legend><select name="rating" aria-label="Minimum rating" defaultValue={values.rating||'0'}><option value="0">All ratings</option><option value="4">4+ · Lovely</option><option value="4.5">4.5+ · Exceptional</option><option value="5">5 · A favourite</option></select></fieldset>
 <fieldset><legend>Sort by</legend><select name="sort" aria-label="Sort stays" defaultValue={values.sort||'recommended'}><option value="recommended">Recommended</option><option value="price_asc">Price: low to high</option><option value="price_desc">Price: high to low</option><option value="rating">Highest rated</option></select></fieldset><button className="button full" type="submit">Apply filters</button></form></aside>
 <section className="results"><div className="result-heading"><p>{result?`${result.items.length}${result.hasMore?'+':''} considered stays`:'Your search'}</p><span className="small muted">One whole space. All yours.</span></div><div className="selected-filters">{Object.entries(values).filter(([k,v])=>['amenities','type','min','max','rating','guests'].includes(k)&&v).map(([k,v])=><span key={k}>{k}: {v.replaceAll('_',' ').toLowerCase()}</span>)}</div>{result&&result.items.length?<div className="stay-grid results-grid">{result.items.map(stay=><StayCard key={stay.id} stay={stay} query={'?'+dateQuery}/>)}</div>:<Empty title={error?'A small detour.':'A different somewhere?'} href="/search" label="Clear filters">{error||'No stays match these dates and filters. Try a wider budget, nearby destination or another weekend.'}</Empty>}
 {result&&(page>0||result.hasMore)&&<nav className="pagination" aria-label="Search pages">{page>0&&<Link className="button secondary" href={pageUrl(page-1)}>Previous</Link>}<span>Page {page+1}</span>{result.hasMore&&<Link className="button secondary" href={pageUrl(page+1)}>Next stays →</Link>}</nav>}</section></div></div>;
}
