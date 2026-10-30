'use client';
import {createContext,useContext,useEffect,useState,type ReactNode} from 'react';
import {api} from '@/lib/api';
import type {User,Listing} from '@/lib/types';
const Context=createContext<{user:User|null;loading:boolean;demo:boolean;savedIds:string[];setSaved:(id:string,saved:boolean)=>void;refresh:()=>Promise<void>}>({user:null,loading:true,demo:false,savedIds:[],setSaved:()=>{},refresh:async()=>{}});
export function AuthProvider({children}:{children:ReactNode}) {
 const [user,setUser]=useState<User|null>(null),[loading,setLoading]=useState(true),[demo,setDemo]=useState(false),[savedIds,setIds]=useState<string[]>([]);
 async function refresh(){const current=await api<User>('/auth/me').catch(()=>null);setUser(current);if(current){const items=await api<Listing[]>('/wishlist');setIds(items.map(x=>x.id));}else setIds([]);setLoading(false);}
 useEffect(()=>{void api<User>('/auth/me').then(async current=>{setUser(current);const items=await api<Listing[]>('/wishlist');setIds(items.map(x=>x.id));}).catch(()=>setUser(null)).finally(()=>setLoading(false));void api<{enabled:boolean}>('/demo/status').then(x=>setDemo(x.enabled)).catch(()=>setDemo(false));},[]);
 const setSaved=(id:string,saved:boolean)=>setIds(current=>saved?[...new Set([...current,id])]:current.filter(x=>x!==id));
 return <Context.Provider value={{user,loading,demo,savedIds,setSaved,refresh}}>{children}</Context.Provider>;
}
export const useAuth=()=>useContext(Context);
