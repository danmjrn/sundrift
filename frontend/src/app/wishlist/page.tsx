import {requireUser} from '@/lib/require-user';
import {serverApi} from '@/lib/server-api';
import type {Listing} from '@/lib/types';
import {Wishlist} from '@/features/catalogue/Wishlist';
export default async function Page(){await requireUser('/wishlist');const items=await serverApi<Listing[]>('/wishlist');return <div className="wrap page-pad"><p className="eyebrow">For another day, or this weekend</p><h1 className="page-title">Somewhere, saved.</h1><Wishlist initial={items}/></div>;}
