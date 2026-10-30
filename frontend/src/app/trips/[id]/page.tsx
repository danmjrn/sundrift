import {notFound} from 'next/navigation';
import {serverApi} from '@/lib/server-api';
import {requireUser} from '@/lib/require-user';
import type {Booking} from '@/lib/types';
import {TripDetail} from '@/features/booking/TripDetail';
export default async function Page({params}:{params:Promise<{id:string}>}){const {id}=await params;await requireUser('/trips/'+id);const booking=await serverApi<Booking>('/bookings/'+id).catch(()=>null);if(!booking)notFound();return <TripDetail initial={booking}/>;}
