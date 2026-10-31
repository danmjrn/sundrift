'use client';
import {useState} from 'react';
import type {Listing} from '@/lib/types';
import {StayCard} from '@/components/StayCard';
import {Empty} from '@/components/States';
export function Wishlist({initial}:{initial:Listing[]}){const [items,setItems]=useState(initial);return items.length?<div className="stay-grid">{items.map(stay=><StayCard key={stay.id} stay={stay} saved onRemoved={()=>setItems(items.filter(x=>x.id!==stay.id))}/>)}</div>:<Empty title="Keep a little possibility." href="/search">Save the places that catch your eye. You’ll find them here when the moment is right.</Empty>;}
