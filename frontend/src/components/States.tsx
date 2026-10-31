import Link from 'next/link';
import {Icon} from './Icon';
export function Empty({title,children,href,label}:{title:string;children:React.ReactNode;href?:string;label?:string}){return <div className="empty-state"><Icon name="home" size={30}/><h2>{title}</h2><p>{children}</p>{href&&<Link className="button" href={href}>{label||'Explore stays'}<Icon name="arrow" size={16}/></Link>}</div>;}
export function Notice({children}:{children:React.ReactNode}){return <div className="notice"><Icon name="shield" size={18}/><p>{children}</p></div>;}
