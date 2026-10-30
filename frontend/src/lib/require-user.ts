import {redirect} from 'next/navigation';
import {serverApi} from './server-api';
import type {User} from './types';
export async function requireUser(next:string){const user=await serverApi<User>('/auth/me').catch(()=>null);if(!user)redirect('/login?next='+encodeURIComponent(next));return user;}
