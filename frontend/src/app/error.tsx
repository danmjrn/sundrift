'use client';
export default function ErrorPage({reset}:{reset:()=>void}){return <div className="wrap page-pad empty-state"><h1>A small detour.</h1><p>We couldn’t reach this page. Your persisted reservations are safe. Try again in a moment.</p><button className="button" onClick={reset}>Try again</button></div>;}
