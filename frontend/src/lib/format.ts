export const money=(value:number|string)=>new Intl.NumberFormat('en-ZA',{style:'currency',currency:'ZAR',minimumFractionDigits:0,maximumFractionDigits:2}).format(Number(value));
export const date=(value:string)=>new Intl.DateTimeFormat('en-ZA',{day:'numeric',month:'short',year:'numeric',timeZone:'Africa/Johannesburg'}).format(new Date(value.slice(0,10)+'T12:00:00+02:00'));
export const today=()=>new Intl.DateTimeFormat('en-CA',{timeZone:'Africa/Johannesburg',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
export const policy=(name:string)=>name==='FLEXIBLE'?'Full refund at least 2 calendar days before arrival; no refund within 2 days.':'Full refund at least 7 calendar days before arrival; 50% at 2–6 days; no refund within 2 days.';
export const typeName=(value:string)=>value.toLowerCase().replaceAll('_',' ');
