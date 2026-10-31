'use client';
/* eslint-disable @next/next/no-location-assign-relative-destination -- Full navigation clears prior authentication-context server payloads. */
import Link from 'next/link';
import {useState} from 'react';
import {useAuth} from './AuthProvider';
import {Icon} from './Icon';
import {api} from '@/lib/api';
export function Wordmark(){return <span className="wordmark"><svg width="28" height="32" viewBox="0 0 28 32" fill="none" aria-hidden="true"><path d="M2 23h24M5 28h18M6 19a8 8 0 0 1 16 0M14 2v5M2 8l4 4M26 8l-4 4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/></svg>sundrift<span className="wordmark-dot">.</span></span>;}
export function Header(){const {user,loading,demo}=useAuth();const [open,setOpen]=useState(false);return <>
 <header className="site-header wrap"><Link href="/" aria-label="Sundrift home"><Wordmark/></Link><nav className="desktop-nav" aria-label="Main navigation"><Link href="/search">Explore stays</Link><Link href="/#destinations">Destinations</Link><Link href="/about">Our approach</Link></nav><div className="header-actions">{demo&&<Link className="demo-pill" href="/login"><span/>Portfolio demo</Link>}<Link href={user?'/account':'/login'} className="account-link" aria-label={loading?'Account':user?'Your space: account':'Sign in'}><Icon name="users" size={18}/><span>{loading?'Account':user?'Your space':'Sign in'}</span></Link><button className="icon-button mobile-menu" onClick={()=>setOpen(!open)} aria-expanded={open} aria-label="Open navigation"><Icon name={open?'close':'menu'}/></button></div></header>
 {open&&<nav className="mobile-nav wrap" aria-label="Mobile navigation" onClick={()=>setOpen(false)}><Link href="/search">Explore stays</Link><Link href="/trips">My trips</Link><Link href="/wishlist">Saved stays</Link><Link href="/host">Host studio</Link><Link href="/admin">Administration</Link></nav>}
 {user&&<div className="account-bar"><div className="wrap"><span>{user.demo?'Your private demo · ':''}{user.role.toLowerCase()} space</span><nav aria-label="Account navigation"><Link href="/trips">My trips</Link><Link href="/wishlist">Saved</Link><Link href="/host">Host studio</Link><Link href="/admin">Admin</Link><button onClick={async()=>{await api('/auth/logout',{method:'POST'});window.location.assign('/');}}>Sign out</button></nav></div></div>}
 </>;}
