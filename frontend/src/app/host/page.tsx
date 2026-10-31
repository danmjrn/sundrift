import {requireUser} from '@/lib/require-user';
import {serverApi} from '@/lib/server-api';
import type {HostData} from '@/lib/types';
import {HostDashboard} from '@/features/management/HostDashboard';
import {Empty} from '@/components/States';
export default async function Page(){const user=await requireUser('/host');if(user.role!=='HOST')return <div className="wrap page-pad"><Empty title="A host’s point of view." href="/login?next=/host" label="Enter as demo host">Switch to your demo host account to manage its stays and reservations.</Empty></div>;const data=await serverApi<HostData>('/host');return <div className="wrap page-pad"><HostDashboard initial={data}/></div>;}
