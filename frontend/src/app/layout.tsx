import type {Metadata} from 'next';
import {AuthProvider} from '@/components/AuthProvider';
import {Header} from '@/components/Header';
import {Footer} from '@/components/Footer';
import './globals.css';
export const metadata:Metadata={metadataBase:new URL(process.env.PUBLIC_ORIGIN||'http://localhost:3000'),title:{default:'NoirStay · Find somewhere worth staying.',template:'%s · NoirStay'},description:'Considered places. Unforgettable stays. Explore a fictional South African accommodation booking demo by Daniel Jr Nkulu, Shape It Solutions.',openGraph:{images:['/social-preview.svg']}};
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="en-ZA"><body><a href="#main" className="skip-link">Skip to content</a><AuthProvider><Header/><main id="main">{children}</main><Footer/></AuthProvider></body></html>;}
