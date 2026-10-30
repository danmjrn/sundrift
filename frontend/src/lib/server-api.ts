import 'server-only';
import {cookies} from 'next/headers';
import {ApiError} from './api';
export async function serverApi<T>(path:string):Promise<T> {
  const jar=await cookies();
  const response=await fetch((process.env.API_INTERNAL_URL || 'http://127.0.0.1:8080')+'/api/v1'+path,{headers:{Cookie:jar.toString()},cache:'no-store'});
  if(!response.ok) { const body=await response.json().catch(()=>({message:'The service is unavailable.',code:'NETWORK'}));throw new ApiError(body.message,response.status,body.code); }
  return response.json() as Promise<T>;
}
