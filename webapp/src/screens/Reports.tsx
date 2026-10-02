import { useMemo, useState } from 'react'
import { useLiveQuery } from 'dexie-react-hooks'
import { db } from '../lib/db'
import { computeTotals, itemMargin } from '../lib/calc'
import { download, financialYear, fmtDate, lastNDays, money, monthEnd, monthStart, num, round2, todayISO } from '../lib/format'
import { csvEscape, invoicesToCsv } from '../lib/csvutil'
import type { Business, Invoice, Item } from '../lib/types'
import { DOC_TYPES, PAYMENT_MODES, docMeta } from '../lib/types'
import { ChipRow, EmptyState, StatBox, toast } from '../components/ui'

type RangeKey = 'today' | 'week' | 'month' | 'fy' | 'all' | 'custom'

export function ReportsScreen({ business }: { business: Business }) {
  const [range, setRange] = useState<RangeKey>('month')
  const [custom, setCustom] = useState({ from: monthStart(), to: todayISO() })

  const invoices = useLiveQuery(() => db.invoices.orderBy('date').toArray(), [], [] as Invoice[])
  const items = useLiveQuery(() => db.items.toArray(), [], [] as Item[])

  const { from, to } = useMemo(() => {
    if (range === 'today') return { from: todayISO(), to: todayISO() }
    if (range === 'week') return lastNDays(7)
    if (range === 'month') return { from: monthStart(), to: monthEnd() }
    if (range === 'fy') {
      const startYear = Number('20' + financialYear(todayISO()).slice(0, 2))
      return { from: `${startYear}-04-01`, to: `${startYear + 1}-03-31` }
    }
    if (range === 'all') return { from: '2000-01-01', to: '2099-12-31' }
    return custom
  }, [range, custom])

  const report = useMemo(() => {
    const inRange = invoices.filter((i) => i.date >= from && i.date <= to && i.status === 'FINAL')
    let sale = 0
    let taxable = 0
    let tax = 0
    let discount = 0
    let profit = 0
    let qty = 0
    let bills = 0
    let returned = 0
    const dayMap = new Map<string, number>()
    const itemMap = new Map<string, { name: string; qty: number; amount: number; profit: number }>()
    const partyMap = new Map<string, { name: string; amount: number; due: number; bills: number }>()
    const modeMap = new Map<string, number>()
    const docMap = new Map<string, { count: number; amount: number }>()
    const hsnMap = new Map<string, { hsn: string; taxable: number; tax: number; rate: number; qty: number }>()

    inRange.forEach((inv) => {
      const meta = docMeta(inv.docType)
      const t = computeTotals(inv, business.stateCode)
      const docRow = docMap.get(meta.label) ?? { count: 0, amount: 0 }
      docRow.count += 1
      docRow.amount = round2(docRow.amount + t.grandTotal)
      docMap.set(meta.label, docRow)

      inv.payments.forEach((p) => modeMap.set(p.mode, round2((modeMap.get(p.mode) ?? 0) + p.amount)))

      if (!meta.isSale && !meta.negative) return

      const sign = meta.negative ? -1 : 1
      sale += sign * t.grandTotal
      taxable += sign * t.taxableNet
      tax += sign * t.tax
      discount += sign * (t.lineDiscount + t.billDiscount)
      profit += sign * t.profit
      qty += sign * t.totalQty
      bills += 1
      if (meta.negative) returned += t.grandTotal

      const dayKey = inv.date
      dayMap.set(dayKey, round2((dayMap.get(dayKey) ?? 0) + sign * t.grandTotal))

      inv.items.forEach((l, i) => {
        const key = l.code || l.name
        const row = itemMap.get(key) ?? { name: l.name, qty: 0, amount: 0, profit: 0 }
        row.qty += sign * l.qty
        row.amount = round2(row.amount + sign * (t.lines[i]?.total ?? 0))
        row.profit = round2(row.profit + sign * ((t.lines[i]?.taxableAfterBillDiscount ?? 0) - l.costPrice * l.qty))
        itemMap.set(key, row)

        if (!meta.noTax) {
          const hsnKey = `${l.hsn || '-'}|${l.gstPercent}`
          const h = hsnMap.get(hsnKey) ?? { hsn: l.hsn || '-', taxable: 0, tax: 0, rate: l.gstPercent, qty: 0 }
          h.taxable = round2(h.taxable + sign * (t.lines[i]?.taxableAfterBillDiscount ?? 0))
          h.tax = round2(h.tax + sign * (t.lines[i]?.tax ?? 0))
          h.qty += sign * l.qty
          hsnMap.set(hsnKey, h)
        }
      })

      const pKey = inv.partyName || 'Cash Sale'
      const pRow = partyMap.get(pKey) ?? { name: pKey, amount: 0, due: 0, bills: 0 }
      pRow.amount = round2(pRow.amount + sign * t.grandTotal)
      pRow.due = round2(pRow.due + sign * t.due)
      pRow.bills += 1
      partyMap.set(pKey, pRow)
    })

    const days = [...dayMap.entries()].sort((a, b) => a[0].localeCompare(b[0]))
    const maxDay = Math.max(1, ...days.map(([, v]) => v))
    const stockValue = items.reduce((s, i) => s + i.purchasePrice * i.stockQty, 0)
    const saleValue = items.reduce(
      (s, i) => s + i.mrp * (1 - i.discountPercent / 100) * (1 + i.gstPercent / 100) * i.stockQty,
      0,
    )

    return {
      sale: round2(sale),
      taxable: round2(taxable),
      tax: round2(tax),
      discount: round2(discount),
      profit: round2(profit),
      qty: round2(qty),
      bills,
      returned: round2(returned),
      avg: bills ? round2(sale / bills) : 0,
      days,
      maxDay,
      topItems: [...itemMap.values()].sort((a, b) => b.amount - a.amount).slice(0, 12),
      topParties: [...partyMap.values()].sort((a, b) => b.amount - a.amount).slice(0, 12),
      modes: PAYMENT_MODES.map((m) => ({ ...m, amount: round2(modeMap.get(m.key) ?? 0) })).filter((m) => m.amount > 0),
      docs: [...docMap.entries()],
      hsn: [...hsnMap.values()].sort((a, b) => b.taxable - a.taxable),
      stockValue: round2(stockValue),
      stockSaleValue: round2(saleValue),
      lowStock: items.filter((i) => i.stockQty <= i.lowStockAlert),
      marginItems: items
        .map((i) => ({ item: i, margin: itemMargin(i.mrp, i.discountPercent, i.purchasePrice) }))
        .sort((a, b) => b.margin.percent - a.margin.percent),
    }
  }, [invoices, items, from, to, business.stateCode])

  const rangeInvoices = invoices.filter((i) => i.date >= from && i.date <= to)

  return (
    <div className="flex-1 px-3 pb-24 pt-3">
      <ChipRow>
        {(
          [
            ['today', 'Aaj'],
            ['week', '7 din'],
            ['month', 'Is mahine'],
            ['fy', 'Is saal (FY)'],
            ['all', 'Sab'],
            ['custom', 'Custom'],
          ] as [RangeKey, string][]
        ).map(([k, label]) => (
          <button key={k} className="chip" data-active={range === k} onClick={() => setRange(k)}>
            {label}
          </button>
        ))}
      </ChipRow>
      {range === 'custom' ? (
        <div className="mt-1 grid grid-cols-2 gap-2">
          <input type="date" className="input" value={custom.from} onChange={(e) => setCustom({ ...custom, from: e.target.value })} />
          <input type="date" className="input" value={custom.to} onChange={(e) => setCustom({ ...custom, to: e.target.value })} />
        </div>
      ) : null}

      <div className="mt-2 rounded-2xl bg-gradient-to-br from-brand-700 to-brand-900 p-4 text-white shadow-lg">
        <div className="text-[11px] font-semibold uppercase tracking-wide text-brand-200">
          Net sale ({fmtDate(from)} – {fmtDate(to)})
        </div>
        <div className="num mt-0.5 text-3xl font-extrabold">{money(report.sale)}</div>
        <div className="mt-1 flex gap-3 text-[11px] text-brand-100">
          <span>{num(report.bills, 0)} bills</span>
          <span>Avg {money(report.avg, 0)}</span>
          <span>{num(report.qty, 0)} qty</span>
          {report.returned ? <span>Return {money(report.returned, 0)}</span> : null}
        </div>
      </div>

      <div className="mt-2 grid grid-cols-2 gap-2">
        <StatBox label="Taxable value" value={money(report.taxable)} />
        <StatBox label="GST collect hua" value={money(report.tax)} tone="brand" />
        <StatBox label="Discount diya" value={money(report.discount)} tone="due" />
        <StatBox label="Anumanit profit" value={money(report.profit)} tone="money" sub="taxable − purchase cost" />
      </div>

      {/* Day wise */}
      <div className="section-title mt-4">
        <span>Din-wise sale</span>
      </div>
      {report.days.length === 0 ? (
        <EmptyState icon="📊" title="Is period me koi sale nahi" hint="Range badalkar dekhein ya naya bill banayein." />
      ) : (
        <div className="card">
          <div className="flex h-36 items-end gap-1">
            {report.days.slice(-30).map(([d, v]) => (
              <div key={d} className="group flex flex-1 flex-col items-center justify-end gap-1">
                <div className="num text-[8px] font-bold text-slate-500">{v >= 1000 ? `${Math.round(v / 1000)}k` : Math.round(v)}</div>
                <div
                  className="w-full rounded-t bg-brand-500"
                  style={{ height: `${Math.max(4, (v / report.maxDay) * 100)}%` }}
                  title={`${fmtDate(d)} • ${money(v)}`}
                />
                <div className="text-[8px] text-slate-400">{d.slice(8)}</div>
              </div>
            ))}
          </div>
          <div className="mt-1 text-center text-[10px] text-slate-400">Aakhri {Math.min(30, report.days.length)} din</div>
        </div>
      )}

      {/* Doc type + payments */}
      <div className="section-title mt-4">
        <span>Document type</span>
      </div>
      <div className="card-flat overflow-hidden">
        {DOC_TYPES.map((d) => {
          const row = report.docs.find(([label]) => label === d.label)
          return (
            <div key={d.key} className="flex items-center justify-between border-b border-slate-100 px-3 py-2 text-[13px] last:border-0">
              <span className="font-semibold text-slate-700">
                {d.icon} {d.label}
              </span>
              <span className="num text-slate-600">
                {num(row?.[1].count ?? 0, 0)} • {money(row?.[1].amount ?? 0, 0)}
              </span>
            </div>
          )
        })}
      </div>

      <div className="section-title mt-4">
        <span>Payment mode</span>
      </div>
      <div className="card-flat overflow-hidden">
        {report.modes.length === 0 ? (
          <div className="px-3 py-3 text-[12px] text-slate-500">Is period me koi payment record nahi hua.</div>
        ) : (
          report.modes.map((m) => (
            <div key={m.key} className="flex items-center justify-between border-b border-slate-100 px-3 py-2 text-[13px] last:border-0">
              <span className="font-semibold text-slate-700">
                {m.label} <span className="text-slate-400">({m.hi})</span>
              </span>
              <span className="num font-bold">{money(m.amount)}</span>
            </div>
          ))
        )}
      </div>

      {/* Top items */}
      <div className="section-title mt-4">
        <span>Top items (sale ke hisab se)</span>
      </div>
      <div className="card-flat overflow-hidden">
        {report.topItems.length === 0 ? (
          <div className="px-3 py-3 text-[12px] text-slate-500">Data nahi hai.</div>
        ) : (
          report.topItems.map((it, idx) => (
            <div key={idx} className="flex items-center gap-2 border-b border-slate-100 px-3 py-2 last:border-0">
              <span className="w-5 text-[11px] font-bold text-slate-400">{idx + 1}</span>
              <span className="min-w-0 flex-1 truncate text-[13px] font-semibold text-slate-800">{it.name}</span>
              <span className="num text-[11px] text-slate-500">{num(it.qty, 0)} qty</span>
              <span className="num w-20 text-right text-[13px] font-bold">{money(it.amount, 0)}</span>
            </div>
          ))
        )}
      </div>

      {/* Top parties */}
      <div className="section-title mt-4">
        <span>Top parties</span>
      </div>
      <div className="card-flat overflow-hidden">
        {report.topParties.length === 0 ? (
          <div className="px-3 py-3 text-[12px] text-slate-500">Data nahi hai.</div>
        ) : (
          report.topParties.map((p, idx) => (
            <div key={idx} className="flex items-center gap-2 border-b border-slate-100 px-3 py-2 last:border-0">
              <span className="min-w-0 flex-1 truncate text-[13px] font-semibold text-slate-800">{p.name}</span>
              <span className="num text-[11px] text-slate-500">{num(p.bills, 0)} bills</span>
              {p.due > 0.5 ? <span className="num w-20 text-right text-[12px] font-bold text-due">Baki {money(p.due, 0)}</span> : null}
              <span className="num w-20 text-right text-[13px] font-bold">{money(p.amount, 0)}</span>
            </div>
          ))
        )}
      </div>

      {/* GST / HSN */}
      <div className="section-title mt-4">
        <span>GST / HSN summary (GSTR-1 jaisa)</span>
      </div>
      <div className="card-flat overflow-auto">
        {report.hsn.length === 0 ? (
          <div className="px-3 py-3 text-[12px] text-slate-500">Is period me koi taxable sale nahi.</div>
        ) : (
          <table className="w-full text-[12px]">
            <thead className="bg-slate-50 text-[10px] uppercase text-slate-500">
              <tr>
                <th className="px-2 py-1.5 text-left">HSN</th>
                <th className="px-2 py-1.5 text-right">Qty</th>
                <th className="px-2 py-1.5 text-right">Taxable</th>
                <th className="px-2 py-1.5 text-right">GST%</th>
                <th className="px-2 py-1.5 text-right">CGST</th>
                <th className="px-2 py-1.5 text-right">SGST</th>
              </tr>
            </thead>
            <tbody>
              {report.hsn.map((h, i) => (
                <tr key={i} className="border-t border-slate-100">
                  <td className="px-2 py-1.5 font-semibold">{h.hsn}</td>
                  <td className="num px-2 py-1.5 text-right">{num(h.qty, 0)}</td>
                  <td className="num px-2 py-1.5 text-right">{num(h.taxable)}</td>
                  <td className="num px-2 py-1.5 text-right">{num(h.rate, 0)}%</td>
                  <td className="num px-2 py-1.5 text-right">{num(round2(h.tax / 2))}</td>
                  <td className="num px-2 py-1.5 text-right">{num(round2(h.tax - h.tax / 2))}</td>
                </tr>
              ))}
              <tr className="border-t-2 border-slate-200 font-bold">
                <td className="px-2 py-1.5" colSpan={2}>
                  Total
                </td>
                <td className="num px-2 py-1.5 text-right">{num(report.taxable)}</td>
                <td />
                <td className="num px-2 py-1.5 text-right">{num(round2(report.tax / 2))}</td>
                <td className="num px-2 py-1.5 text-right">{num(round2(report.tax / 2))}</td>
              </tr>
            </tbody>
          </table>
        )}
      </div>

      {/* Stock */}
      <div className="section-title mt-4">
        <span>Stock summary</span>
      </div>
      <div className="grid grid-cols-2 gap-2">
        <StatBox label="Stock value (cost)" value={money(report.stockValue)} />
        <StatBox label="Stock sale value" value={money(report.stockSaleValue)} tone="money" />
      </div>
      {report.lowStock.length ? (
        <div className="card mt-2">
          <div className="text-[13px] font-bold text-due">⚠️ {report.lowStock.length} item low/out of stock</div>
          <div className="mt-1 text-[11px] text-slate-600">
            {report.lowStock.slice(0, 8).map((i) => `${i.name} (${i.stockQty})`).join(', ')}
            {report.lowStock.length > 8 ? ' …' : ''}
          </div>
        </div>
      ) : null}

      {/* Exports */}
      <div className="section-title mt-4">
        <span>Report export</span>
      </div>
      <div className="flex flex-col gap-2">
        <button
          className="btn btn-outline btn-block"
          onClick={() => {
            if (!rangeInvoices.length) return toast('Is period me koi bill nahi', 'error')
            download(`sales-register-${from}-to-${to}.csv`, invoicesToCsv(rangeInvoices, business))
            toast('Sales register CSV download ho gaya', 'success')
          }}
        >
          ⬇ Sales register (sab bills) CSV
        </button>
        <button
          className="btn btn-outline btn-block"
          onClick={() => {
            const rows = [
              ['HSN', 'Qty', 'Taxable', 'GST %', 'CGST', 'SGST'],
              ...report.hsn.map((h) => [h.hsn, String(h.qty), h.taxable.toFixed(2), String(h.rate), (h.tax / 2).toFixed(2), (h.tax / 2).toFixed(2)]),
            ]
            download(`gst-hsn-summary-${from}-to-${to}.csv`, rows.map((r) => r.map(csvEscape).join(',')).join('\n'))
            toast('GST summary download ho gayi', 'success')
          }}
        >
          ⬇ GST / HSN summary CSV
        </button>
        <button
          className="btn btn-outline btn-block"
          onClick={() => {
            const rows = [
              ['Item', 'Qty', 'Sale amount', 'Profit'],
              ...report.topItems.map((i) => [i.name, String(i.qty), i.amount.toFixed(2), i.profit.toFixed(2)]),
            ]
            download(`item-wise-sale-${from}-to-${to}.csv`, rows.map((r) => r.map(csvEscape).join(',')).join('\n'))
            toast('Item-wise report download ho gayi', 'success')
          }}
        >
          ⬇ Item-wise sale CSV
        </button>
        <button
          className="btn btn-outline btn-block"
          onClick={() => {
            const rows = [
              ['Party', 'Bills', 'Sale amount', 'Baki'],
              ...report.topParties.map((p) => [p.name, String(p.bills), p.amount.toFixed(2), p.due.toFixed(2)]),
            ]
            download(`party-wise-${from}-to-${to}.csv`, rows.map((r) => r.map(csvEscape).join(',')).join('\n'))
            toast('Party-wise report download ho gayi', 'success')
          }}
        >
          ⬇ Party-wise sale & baki CSV
        </button>
      </div>
      <div className="h-6" />
    </div>
  )
}
