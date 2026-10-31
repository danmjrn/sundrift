import {it,expect} from 'vitest';
import {safeReturnPath} from './return-path';
it('retains booking return state and rejects browser-normalised external destinations',()=>{
  expect(safeReturnPath('/stays/the-clifton-house?guests=2')).toBe('/stays/the-clifton-house?guests=2');
  for(const path of ['//example.com','/\\example.com','/\n/example.com','https://example.com','javascript:alert(1)'])expect(safeReturnPath(path)).toBe('/');
});
