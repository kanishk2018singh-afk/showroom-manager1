import { db, DEFAULT_TERMS, getBusiness, getDocSetting, setSetting } from './db'
import { computeTotals } from './calc'
import { financialYear, round2, todayISO, uid } from './format'
import type {
  DocSetting,
  DocType,
  Invoice,
  Item,
  LineItem,
  Party,
  PaymentEntry,
} from './types'
import { docMeta } from './types'

// ---------------- Numbering ----------------

export const formatDocNumber = (s: DocSetting, date: string, n: number): string => {
  const serial = String(n).padStart(Math.max(1, s.digits), '0')
  return s.includeFy ? `${s.prefix}/${financialYear(date)}/${serial}` : `${s.prefix}/${serial}`
}

/** Preview of the next number (does not consume it) */
export async function peekNumber(docType: DocType, date: string): Promise<string> {
  const s = await getDocSetting(docType)
  return formatDocNumber(s, date, s.nextNumber)
}

async function allocateNumber(docType: DocType, date: string): Promise<string> {
  const s = await db.docSettings.get(docType)
  const setting: DocSetting = s ?? (await getDocSetting(docType))
  const number = formatDocNumber(setting, date, setting.nextNumber)
  await db.docSettings.put({ ...setting, nextNumber: setting.nextNumber + 1 })
  return number
}

// ---------------- Items ----------------

export const listItems = () => db.items.orderBy('name').toArray()

export async function upsertItem(item: Item): Promise<number> {
  const rec = { ...item, updatedAt: Date.now() }
  if (rec.id) {
    await db.items.put(rec)
    return rec.id
  }
  const { id: _drop, ...rest } = rec
  void _drop
  return db.items.add(rest as Item)
}

export async function deleteItem(id: number): Promise<void> {
  await db.items.delete(id)
}

export async function adjustStock(itemId: number, delta: number): Promise<void> {
  await db.transaction('rw', db.items, async () => {
    const item = await db.items.get(itemId)
    if (!item?.id) return
    await db.items.update(item.id, {
      stockQty: Math.max(0, (item.stockQty || 0) + delta),
      updatedAt: Date.now(),
    })
  })
}

export async function findItemByCode(code: string): Promise<Item | undefined> {
  const c = code.trim().toLowerCase()
  if (!c) return undefined
  const items = await db.items.toArray()
  return items.find(
    (i) => i.code.toLowerCase() === c || (i.barcode ?? '').toLowerCase() === c,
  )
}

// ---------------- Parties ----------------

export const listParties = () => db.parties.orderBy('name').toArray()

export async function upsertParty(p: Party): Promise<number> {
  if (p.id) {
    await db.parties.put(p)
    return p.id
  }
  const { id: _drop, ...rest } = p
  void _drop
  return db.parties.add(rest as Party)
}

export async function deleteParty(id: number): Promise<void> {
  await db.parties.delete(id)
}

/** All invoices (final) of a party + outstanding balance */
export async function partyInvoices(partyId: number): Promise<Invoice[]> {
  const list = await db.invoices.where('partyId').equals(partyId).toArray()
  return list.sort((a, b) => b.date.localeCompare(a.date) || b.createdAt - a.createdAt)
}

export async function balanceOf(
  partyId: number,
  shopState = '08',
): Promise<number> {
  const party = await db.parties.get(partyId)
  const invoices = await db.invoices.where('partyId').equals(partyId).toArray()
  return round2(
    (party?.openingBalance ?? 0) +
      invoices.reduce((sum, inv) => {
        if (inv.status !== 'FINAL') return sum
        const meta = docMeta(inv.docType)
        const t = computeTotals(inv, shopState)
        const net = t.grandTotal - t.paid
        if (meta.isSale) return sum + net
        if (meta.negative) return sum - net
        return sum
      }, 0),
  )
}

export async function allBalances(shopState = '08'): Promise<Map<number, number>> {
  const [parties, invoices] = await Promise.all([db.parties.toArray(), db.invoices.toArray()])
  const map = new Map<number, number>()
  parties.forEach((p) => p.id && map.set(p.id, p.openingBalance || 0))
  invoices.forEach((inv) => {
    if (inv.status !== 'FINAL' || !inv.partyId || !map.has(inv.partyId)) return
    const meta = docMeta(inv.docType)
    const t = computeTotals(inv, shopState)
    const net = t.grandTotal - t.paid
    if (meta.isSale) map.set(inv.partyId, round2((map.get(inv.partyId) ?? 0) + net))
    else if (meta.negative) map.set(inv.partyId, round2((map.get(inv.partyId) ?? 0) - net))
  })
  return map
}

// ---------------- Invoices ----------------

async function applyStockEffect(inv: Invoice, direction: 1 | -1): Promise<void> {
  const meta = docMeta(inv.docType)
  if (!meta.stockOut && !meta.stockIn) return
  const sign = (meta.stockOut ? -1 : 1) * direction
  for (const line of inv.items) {
    if (!line.itemId) continue
    const item = await db.items.get(line.itemId)
    if (!item?.id) continue
    await db.items.update(item.id, {
      stockQty: Math.max(0, (item.stockQty || 0) + sign * line.qty),
      updatedAt: Date.now(),
    })
  }
}

export async function saveInvoice(inv: Invoice, shopState = '08'): Promise<number> {
  void shopState
  return db.transaction('rw', db.invoices, db.docSettings, db.items, async () => {
    const now = Date.now()
    let id = inv.id
    const existing = id ? await db.invoices.get(id) : undefined
    if (existing) await applyStockEffect(existing, -1)

    const number = inv.number?.trim() ? inv.number.trim() : await allocateNumber(inv.docType, inv.date)
    const rec: Invoice = {
      ...inv,
      number,
      status: inv.status ?? 'FINAL',
      createdAt: existing?.createdAt ?? inv.createdAt ?? now,
      updatedAt: now,
    }
    if (existing?.id) {
      await db.invoices.put({ ...rec, id: existing.id })
      id = existing.id
    } else {
      const { id: _drop, ...rest } = rec
      void _drop
      id = await db.invoices.add(rest as Invoice)
    }

    const saved = await db.invoices.get(id)
    if (saved) await applyStockEffect(saved, 1)

    // link converted document
    if (inv.fromId) {
      await db.invoices.update(inv.fromId, { convertedToId: id, updatedAt: now })
    }
    return id
  })
}

export async function deleteInvoice(id: number): Promise<void> {
  await db.transaction('rw', db.invoices, db.items, async () => {
    const inv = await db.invoices.get(id)
    if (inv) await applyStockEffect(inv, -1)
    await db.invoices.delete(id)
  })
}

export async function cancelInvoice(id: number): Promise<void> {
  await db.transaction('rw', db.invoices, db.items, async () => {
    const inv = await db.invoices.get(id)
    if (!inv?.id) return
    if (inv.status === 'FINAL') await applyStockEffect(inv, -1)
    await db.invoices.update(id, { status: 'CANCELLED', updatedAt: Date.now() })
  })
}

export async function restoreInvoice(id: number): Promise<void> {
  await db.transaction('rw', db.invoices, db.items, async () => {
    const inv = await db.invoices.get(id)
    if (!inv?.id) return
    if (inv.status !== 'FINAL') await applyStockEffect(inv, 1)
    await db.invoices.update(id, { status: 'FINAL', updatedAt: Date.now() })
  })
}

export async function recordPayment(
  invoiceId: number,
  payment: Omit<PaymentEntry, 'id'>,
): Promise<void> {
  const inv = await db.invoices.get(invoiceId)
  if (!inv) return
  const entry: PaymentEntry = { ...payment, id: uid() }
  await db.invoices.update(invoiceId, {
    payments: [...(inv.payments ?? []), entry],
    updatedAt: Date.now(),
  })
}

export async function removePayment(invoiceId: number, paymentId: string): Promise<void> {
  const inv = await db.invoices.get(invoiceId)
  if (!inv) return
  await db.invoices.update(invoiceId, {
    payments: (inv.payments ?? []).filter((p) => p.id !== paymentId),
    updatedAt: Date.now(),
  })
}

export const listInvoices = (): Promise<Invoice[]> =>
  db.invoices.orderBy('createdAt').reverse().toArray()

export const invoicesBetween = (from: string, to: string): Promise<Invoice[]> =>
  db.invoices
    .where('date')
    .between(from, to, true, true)
    .toArray()
    .then((list) => list.sort((a, b) => a.date.localeCompare(b.date) || a.createdAt - b.createdAt))

/** Build a fresh draft invoice (also used for "convert to" and "duplicate") */
export async function newInvoice(docType: DocType, date = todayISO()): Promise<Invoice> {
  const setting = await getDocSetting(docType)
  return {
    docType,
    number: '',
    date,
    partyName: '',
    placeOfSupply: '',
    items: [],
    billDiscountType: 'PERCENT',
    billDiscountValue: 0,
    extraCharges: [],
    roundOffEnabled: true,
    notes: '',
    status: 'FINAL',
    payments: [],
    terms: setting.terms || DEFAULT_TERMS,
    createdAt: Date.now(),
    updatedAt: Date.now(),
  }
}

export async function duplicateInvoice(inv: Invoice, date = todayISO()): Promise<Invoice> {
  const draft = await newInvoice(inv.docType, date)
  return {
    ...draft,
    partyId: inv.partyId,
    partyName: inv.partyName,
    partyPhone: inv.partyPhone,
    partyGstin: inv.partyGstin,
    partyAddress: inv.partyAddress,
    placeOfSupply: inv.placeOfSupply,
    items: inv.items.map((l) => ({ ...l, id: uid() })),
    billDiscountType: inv.billDiscountType,
    billDiscountValue: inv.billDiscountValue,
    extraCharges: inv.extraCharges.map((c) => ({ ...c })),
    roundOffEnabled: inv.roundOffEnabled,
    notes: inv.notes,
    terms: inv.terms,
  }
}

export async function convertInvoice(inv: Invoice, to: DocType): Promise<Invoice> {
  const draft = await newInvoice(to)
  return {
    ...draft,
    fromId: inv.id,
    partyId: inv.partyId,
    partyName: inv.partyName,
    partyPhone: inv.partyPhone,
    partyGstin: inv.partyGstin,
    partyAddress: inv.partyAddress,
    placeOfSupply: inv.placeOfSupply || '',
    items: inv.items.map((l: LineItem) => ({ ...l, id: uid() })),
    billDiscountType: inv.billDiscountType,
    billDiscountValue: inv.billDiscountValue,
    extraCharges: inv.extraCharges.map((c) => ({ ...c })),
    roundOffEnabled: inv.roundOffEnabled,
    notes: inv.notes,
    terms: inv.terms,
  }
}

// ---------------- Backup ----------------

export async function exportBackup(): Promise<string> {
  const [business, items, parties, invoices, docSettings, appSettings] = await Promise.all([
    db.business.toArray(),
    db.items.toArray(),
    db.parties.toArray(),
    db.invoices.toArray(),
    db.docSettings.toArray(),
    db.appSettings.toArray(),
  ])
  return JSON.stringify(
    {
      app: 'showroom-manager',
      version: 2,
      exportedAt: new Date().toISOString(),
      business,
      items,
      parties,
      invoices,
      docSettings,
      appSettings,
    },
    null,
    2,
  )
}

export async function importBackup(json: string, mode: 'replace' | 'merge' = 'merge'): Promise<void> {
  const data = JSON.parse(json)
  if (Array.isArray(data.business) && data.business.length) {
    if (mode === 'replace') await db.business.clear()
    const existing = await db.business.count()
    if (existing === 0) await db.business.bulkAdd(data.business)
  }
  await db.transaction('rw', db.items, db.parties, db.invoices, db.docSettings, db.appSettings, async () => {
    if (mode === 'replace') {
      await Promise.all([
        db.items.clear(),
        db.parties.clear(),
        db.invoices.clear(),
        db.docSettings.clear(),
        db.appSettings.clear(),
      ])
    }
    for (const table of ['items', 'parties', 'invoices', 'docSettings', 'appSettings'] as const) {
      const rows = data[table]
      if (!Array.isArray(rows)) continue
      for (const row of rows) {
        if (table === 'items') await db.items.put(row)
        else if (table === 'parties') await db.parties.put(row)
        else if (table === 'invoices') await db.invoices.put(row)
        else if (table === 'docSettings') await db.docSettings.put(row)
        else await db.appSettings.put(row)
      }
    }
  })
}

export async function wipeAllData(): Promise<void> {
  await Promise.all([
    db.items.clear(),
    db.parties.clear(),
    db.invoices.clear(),
    db.docSettings.clear(),
    db.appSettings.clear(),
    db.business.clear(),
  ])
  await setSetting('onboarded', 'no')
}

export { getBusiness }
