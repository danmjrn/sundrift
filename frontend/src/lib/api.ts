export class ApiError extends Error { constructor(message:string, public status:number, public code:string) {super(message);} }
export async function api<T>(path:string, options:RequestInit={}):Promise<T> {
  const headers=new Headers(options.headers);
  if(options.body && !(options.body instanceof FormData)) headers.set('Content-Type','application/json');
  if(options.method && !['GET','HEAD'].includes(options.method)) {
    const token=await fetch('/api/v1/auth/csrf',{cache:'no-store',credentials:'same-origin'}).then(r=>r.json()) as {token:string;headerName:string};
    headers.set(token.headerName,token.token);
  }
  const response=await fetch('/api/v1'+path,{...options,headers,credentials:'same-origin',cache:'no-store'});
  if(!response.ok) { const error=await response.json().catch(()=>({message:'The service is unavailable. Please try again.',code:'NETWORK'}));throw new ApiError(error.message,response.status,error.code); }
  if(response.status===204)return undefined as T;
  return response.json() as Promise<T>;
}
export const key=()=>crypto.randomUUID();
