import {Login} from '@/features/identity/Login';
export const metadata={title:'Make yourself at home'};
export default async function LoginPage({searchParams}:{searchParams:Promise<{next?:string}>}){return <Login next={(await searchParams).next||'/'}/>;}
