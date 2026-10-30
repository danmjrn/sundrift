import {serverApi} from '@/lib/server-api';
import {requireUser} from '@/lib/require-user';
import type {Booking} from '@/lib/types';
import {Trips} from '@/features/booking/Trips';
export const metadata={title:'My trips'};
export default async function Page(){await requireUser('/trips');const bookings=await serverApi<Booking[]>('/bookings');return <div className="wrap page-pad"><p className="eyebrow">A collection of good memories</p><h1 className="page-title">Your somewhere stories.</h1><Trips bookings={bookings}/></div>;}
