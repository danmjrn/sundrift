'use client';
/* eslint-disable @next/next/no-location-assign-relative-destination -- Full navigation clears prior authentication-context server payloads. */
import Image from 'next/image';
import Link from 'next/link';
import {useState} from 'react';
import type {Listing} from '@/lib/types';
import {money,typeName} from '@/lib/format';
import {api} from '@/lib/api';
import {useAuth} from './AuthProvider';
import {Icon} from './Icon';
export function StayCard({stay,query='',saved=false,onRemoved}:{stay:Listing;query?:string;saved?:boolean;onRemoved?:()=>void}) {
 const {user,loading,savedIds,setSaved}=useAuth();const isSaved=saved||savedIds.includes(stay.id);const [message,setMessage]=useState(''),[busy,setBusy]=useState(false);
 return <article className="stay-card"><div className="stay-photo"><Link href={`/stays/${stay.slug}${query}`} aria-label={`Explore ${stay.title}`}><Image src={stay.images[0]||'/images/stay-01.jpg'} alt={`${stay.title}, an illustrative ${typeName(stay.type)} interior`} fill sizes="(max-width:600px) 90vw,(max-width:1000px) 45vw,30vw"/></Link><span className="photo-label">{stay.type==='VILLA'?'Exceptional space':stay.type==='CABIN'?'Close to nature':'The considered collection'}</span><button className={'save-button '+(isSaved?'saved':'')} aria-label={`${isSaved?'Unsave':'Save'} ${stay.title}`} aria-pressed={isSaved} disabled={busy||loading} aria-busy={busy||loading} onClick={async()=>{if(!user){window.location.assign('/login?next=/wishlist');return;}setBusy(true);try{await api(`/wishlist/${stay.id}`,{method:isSaved?'DELETE':'POST'});setSaved(stay.id,!isSaved);if(isSaved)onRemoved?.();}catch(e){setMessage((e as Error).message);}finally{setBusy(false);}}}><Icon name="heart" size={19}/></button></div>
 <div className="stay-meta"><span>{stay.destination} · {typeName(stay.type)}</span>{Number(stay.rating)>0&&<span className="rating"><Icon name="star" size={13}/>{Number(stay.rating).toFixed(1)}</span>}</div><Link href={`/stays/${stay.slug}${query}`}><h3>{stay.title}</h3></Link><div className="stay-price"><span><strong>{money(stay.nightlyRate)}</strong> <span>/ night</span></span><span>{stay.capacity} guests · {stay.beds} beds</span></div>{stay.stayPrice&&<p className="stay-total">{money(stay.stayPrice.total)} total for {stay.stayPrice.nights} nights · includes fees & tax</p>}{message&&<p role="alert" className="error-text">{message}</p>}</article>;
}
