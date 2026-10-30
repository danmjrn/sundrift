/** Canonicalise a login return URL without allowing a different browser origin. */
export function safeReturnPath(value:string):string {
  if(!value.startsWith('/'))return '/';
  try {
    const base='https://noirstay.invalid';
    const parsed=new URL(value,base);
    return parsed.origin===base?parsed.pathname+parsed.search+parsed.hash:'/';
  } catch { return '/'; }
}
