import {requireUser} from '@/lib/require-user';
import {Account} from '@/features/identity/Account';
export default async function Page(){const user=await requireUser('/account');return <div className="wrap narrow page-pad"><p className="eyebrow">A little more you</p><h1 className="page-title">Your space.</h1><Account user={user}/></div>;}
