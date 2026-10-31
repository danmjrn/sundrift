'use client';
import {Icon} from './Icon';
import {today} from '@/lib/format';
export function SearchBar({values={},compact=false}:{values?:Record<string,string>;compact?:boolean}) {return <form action="/search" className={'search-bar '+(compact?'compact':'')}>
 <label className="search-destination"><span>Where to?</span><div><Icon name="pin" size={18}/><input name="destination" aria-label="Destination" placeholder="Find your somewhere" defaultValue={values.destination||''} list="destinations-list"/></div></label>
 <datalist id="destinations-list">{['Cape Town','Johannesburg','Sandton','Durban','Garden Route','Drakensberg'].map(x=><option key={x}>{x}</option>)}</datalist>
 <label><span>Check in</span><input type="date" name="checkIn" aria-label="Check in" min={today()} defaultValue={values.checkIn||''}/></label><label><span>Check out</span><input type="date" name="checkOut" aria-label="Check out" min={values.checkIn||today()} defaultValue={values.checkOut||''}/></label>
 <label className="guest-field"><span>Guests</span><select name="guests" aria-label="Guests" defaultValue={values.guests||'2'}>{[1,2,3,4,5,6,8,10,12,16].map(x=><option key={x} value={x}>{x} {x===1?'guest':'guests'}</option>)}</select></label>
 <button className="search-submit" type="submit" aria-label="Find a stay"><Icon name="search"/><span>Find a stay</span></button></form>;}
