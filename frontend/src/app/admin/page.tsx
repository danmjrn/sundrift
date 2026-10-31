import {requireUser} from '@/lib/require-user';
import {serverApi} from '@/lib/server-api';
import type {AdminData} from '@/lib/types';
import {AdminDashboard} from '@/features/management/AdminDashboard';
import {Empty} from '@/components/States';
export default async function Page(){const user=await requireUser('/admin');if(user.role!=='ADMIN')return <div className="wrap page-pad"><Empty title="A little care behind the scenes." href="/login?next=/admin" label="Enter as demo admin">Switch to your demo administrator to review the stays in your private context.</Empty></div>;const data=await serverApi<AdminData>('/admin');return <div className="wrap page-pad"><AdminDashboard initial={data}/></div>;}
