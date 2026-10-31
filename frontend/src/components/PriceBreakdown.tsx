import type {Price} from '@/lib/types';
import {money} from '@/lib/format';
export function PriceBreakdown({price}:{price:Price}){return <dl className="price-breakdown"><div><dt>{money(price.nightlyRate)} × {price.nights} nights</dt><dd>{money(price.subtotal)}</dd></div><div><dt>Cleaning fee</dt><dd>{money(price.cleaningFee)}</dd></div><div><dt>Service fee · 8%</dt><dd>{money(price.serviceFee)}</dd></div><div><dt>VAT · 15%</dt><dd>{money(price.tax)}</dd></div><div className="total"><dt>Total <small>ZAR</small></dt><dd>{money(price.total)}</dd></div></dl>;}
