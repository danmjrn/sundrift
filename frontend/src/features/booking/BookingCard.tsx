'use client';
import {useEffect,useRef,useState} from 'react';
import Link from 'next/link';
import {useRouter} from 'next/navigation';
import type {Booking,Listing,Price,Block} from '@/lib/types';
import {api,key} from '@/lib/api';
import {money,today,policy} from '@/lib/format';
import {useAuth} from '@/components/AuthProvider';
import {PriceBreakdown} from '@/components/PriceBreakdown';
import {Icon} from '@/components/Icon';
export function BookingCard({stay,values}:{stay:Listing;values:Record<string,string|undefined>}){
 const {user}=useAuth();const router=useRouter();const [checkIn,setIn]=useState(values.checkIn||''),[checkOut,setOut]=useState(values.checkOut||''),[guests,setGuests]=useState(Number(values.guests)||2);
 const [preview,setPreview]=useState<{available:boolean;price:Price}|null>(null),[error,setError]=useState(''),[busy,setBusy]=useState(false),[loading,setLoading]=useState(false);
 const idem=useRef('');
 useEffect(()=>{if(!checkIn||!checkOut){return;}let active=true;const params=new URLSearchParams({checkIn,checkOut,guests:String(guests)});
  void api<{available:boolean;price:Price}>(`/catalogue/units/${stay.id}/price?${params}`).then(x=>{if(active){setPreview(x);setError('');setLoading(false);}}).catch(e=>{if(active){setError((e as Error).message);setPreview(null);setLoading(false);}});return()=>{active=false;};
 },[stay.id,checkIn,checkOut,guests]);
 const change=(fn:()=>void)=>{fn();setPreview(null);setLoading(true);setError('');idem.current='';};
 const next=`/stays/${stay.slug}?${new URLSearchParams({checkIn,checkOut,guests:String(guests)})}`;
 async function reserve(){setBusy(true);setError('');try{
  const quote=await api<{id:string}>('/quotes',{method:'POST',body:JSON.stringify({listingId:stay.id,checkIn,checkOut,guests})});
  idem.current=idem.current||key();const booking=await api<Booking>('/bookings',{method:'POST',headers:{'Idempotency-Key':idem.current},body:JSON.stringify({quoteId:quote.id})});router.push('/checkout/'+booking.id);
 }catch(e){setError((e as Error).message);idem.current='';}finally{setBusy(false);}}
 return <aside id="reserve" className="booking-card"><div className="booking-price"><span><strong>{money(stay.nightlyRate)}</strong> / night</span><span><Icon name="star" size={13}/>{Number(stay.rating||0).toFixed(1)}</span></div><p className="small muted">A little space that’s entirely yours.</p>
 <div className="booking-dates"><label>Check in<input aria-label="Stay check in" type="date" min={today()} value={checkIn} onChange={e=>change(()=>setIn(e.target.value))}/></label><label>Check out<input aria-label="Stay check out" type="date" min={checkIn||today()} value={checkOut} onChange={e=>change(()=>setOut(e.target.value))}/></label></div><label className="booking-guests">Guests<select value={guests} onChange={e=>change(()=>setGuests(Number(e.target.value)))}>{Array.from({length:stay.capacity},(_,i)=><option key={i} value={i+1}>{i+1} {i===0?'guest':'guests'}</option>)}</select></label>
 {loading&&checkIn&&checkOut&&<p aria-live="polite">Checking availability and price…</p>}{preview&&<><PriceBreakdown price={preview.price}/>{!preview.available&&<p className="error-text" role="alert">These dates are unavailable. Try another weekend.</p>}</>}{error&&<p className="error-text" role="alert">{error}</p>}
 {user?.role==='GUEST'?<button className="button full" disabled={!preview?.available||busy} onClick={reserve}>{busy?'Holding your dates…':'Continue to reservation'}<Icon name="arrow" size={17}/></button>:<Link className="button full" href={'/login?next='+encodeURIComponent(next)}>{user?'Enter as demo guest':'Sign in to reserve'}<Icon name="arrow" size={17}/></Link>}
 <p className="booking-note">No payment yet. Your dates are held for 10 minutes at checkout.</p><div className="policy-small"><Icon name="shield" size={17}/><span>{policy(stay.cancellationPolicy)}</span></div><a className="mobile-reserve" href="#reserve">{money(stay.nightlyRate)} / night <span>Choose dates ↑</span></a></aside>;
}
export function AvailabilityCalendar({listingId,refreshToken=''}:{listingId:string;refreshToken?:string}){
 const signature=listingId+':'+refreshToken;
 const [result,setResult]=useState<{signature:string;blocks:Block[]}|null>(null),[offset,setOffset]=useState(0),[failure,setFailure]=useState<{signature:string;message:string}|null>(null);
 const blocks=result?.signature===signature?result.blocks:null;const error=failure?.signature===signature?failure.message:'';
 useEffect(()=>{let active=true;void api<Block[]>(`/catalogue/units/${listingId}/availability`).then(items=>{if(active)setResult({signature,blocks:items});}).catch(e=>{if(active)setFailure({signature,message:(e as Error).message});});return()=>{active=false;};},[listingId,signature]);
 const base=new Date(today()+'T12:00:00');base.setDate(1);base.setMonth(base.getMonth()+offset);const count=new Date(base.getFullYear(),base.getMonth()+1,0).getDate();
 const iso=(day:number)=>`${base.getFullYear()}-${String(base.getMonth()+1).padStart(2,'0')}-${String(day).padStart(2,'0')}`;
 return <div className="availability-calendar"><div className="calendar-heading"><h3>{base.toLocaleDateString('en-ZA',{month:'long',year:'numeric'})}</h3><div><button className="icon-button" aria-label="Previous month" disabled={offset===0} onClick={()=>setOffset(offset-1)}>←</button><button className="icon-button" aria-label="Next month" disabled={offset===11} onClick={()=>setOffset(offset+1)}>→</button></div></div>{blocks?<table className="calendar-table" aria-label="Availability by night"><thead><tr>{['Sun','Mon','Tue','Wed','Thu','Fri','Sat'].map(d=><th scope="col" key={d}>{d}</th>)}</tr></thead><tbody>{Array.from({length:Math.ceil((base.getDay()+count)/7)},(_,week)=><tr key={week}>{Array.from({length:7},(_,weekday)=>{const day=week*7+weekday-base.getDay()+1;if(day<1||day>count)return <td key={weekday}/>;const value=iso(day);const unavailable=blocks.some(b=>value>=b.checkIn&&value<b.checkOut)||value<today();return <td key={weekday} aria-label={`${value} ${unavailable?'unavailable':'available'}`}><span className={'calendar-day '+(unavailable?'unavailable':'')}>{day}</span></td>;})}</tr>)}</tbody></table>:<p role="status">{error?'Availability could not be loaded. Refresh to try again.':'Loading availability…'}</p>}<p className="small muted"><span className="calendar-key"/>Unavailable nights · checkout can fall on the next stay’s arrival day.</p>{error&&<p role="alert">{error}</p>}</div>;
}
