import {describe,it,expect} from 'vitest';
import {date,money,policy,typeName} from './format';
describe('guest-facing pricing and dates',()=>{
 it('labels ZAR using South African formatting',()=>{expect(money(2850)).toMatch(/R/);expect(money(2850).replace(/\s/g,'')).toContain('2850');});
 it('preserves accommodation dates independent of the host timezone',()=>{expect(date('2026-11-06')).toMatch(/6.*Nov.*2026/);});
 it('explains both refund boundaries',()=>{expect(policy('FLEXIBLE')).toContain('2 calendar days');expect(policy('MODERATE')).toContain('2–6 days');});
 it('renders supported unit types',()=>{expect(typeName('HOTEL_ROOM')).toBe('hotel room');});
});
