import {notFound,redirect} from 'next/navigation';
import {requireUser} from '@/lib/require-user';
import {serverApi} from '@/lib/server-api';
import type {HostData} from '@/lib/types';
import {ListingForm} from '@/features/management/ListingForm';
export default async function Page({params}:{params:Promise<{id:string}>}){const {id}=await params;const user=await requireUser('/host/listings/'+id);if(user.role!=='HOST')redirect('/login?next=/host');const data=await serverApi<HostData>('/host');const listing=id==='new'?null:data.listings.find(l=>l.id===id);if(id!=='new'&&!listing)notFound();return <div className="wrap narrow page-pad"><ListingForm initial={listing||null}/></div>;}
