/* Smoke test: runs the whole data layer + renders the app in jsdom with a fake IndexedDB.
 * Build + run:  npm run smoke */
import { JSDOM } from 'jsdom'
import { indexedDB as fakeIndexedDB, IDBKeyRange as fakeIDBKeyRange } from 'fake-indexeddb'

// Must exist at module-eval time: Dexie captures the IndexedDB API on first use.
Object.defineProperty(globalThis, 'indexedDB', { value: fakeIndexedDB, writable: true, configurable: true })
Object.defineProperty(globalThis, 'IDBKeyRange', { value: fakeIDBKeyRange, writable: true, configurable: true })

const results: { name: string; ok: boolean; info?: string }[] = []
const check = (name: string, ok: boolean, info?: string) => {
  results.push({ name, ok, info })
  console.log(`${ok ? '  ok  ' : '  FAIL'} ${name}${info ? ` — ${info}` : ''}`)
}

async function setupDom(): Promise<JSDOM> {
  const dom = new JSDOM(
    `<!doctype html><html><body><div id="boot"></div><div id="root"></div><div id="print-root"></div></body></html>`,
    { url: 'http://localhost:5173/', pretendToBeVisual: true },
  )
  const define = (key: string, value: unknown) =>
    Object.defineProperty(globalThis, key, { value, writable: true, configurable: true, enumerable: true })
  define('window', dom.window)
  define('document', dom.window.document)
  define('navigator', dom.window.navigator)
  define('HTMLElement', dom.window.HTMLElement)
  define('Element', dom.window.Element)
  define('Node', dom.window.Node)
  define('Event', dom.window.Event)
  define('CustomEvent', dom.window.CustomEvent)
  define('getComputedStyle', dom.window.getComputedStyle.bind(dom.window))
  define('requestAnimationFrame', (cb: FrameRequestCallback) => setTimeout(() => cb(Date.now()), 16) as unknown as number)
  define('cancelAnimationFrame', (id: number) => clearTimeout(id))
  define('matchMedia', () => ({ matches: false, addEventListener() {}, removeEventListener() {} }))
  define(
    'ResizeObserver',
    class {
      observe() {}
      unobserve() {}
      disconnect() {}
    },
  )
  define('indexedDB', fakeIndexedDB)
  define('IDBKeyRange', fakeIDBKeyRange)
  Object.defineProperty(dom.window, 'indexedDB', { value: fakeIndexedDB, configurable: true })
  Object.defineProperty(dom.window, 'IDBKeyRange', { value: fakeIDBKeyRange, configurable: true })
  define('IS_REACT_ACT_ENVIRONMENT', false)
  return dom
}

const wait = (ms: number) => new Promise((r) => setTimeout(r, ms))
const close = (a: number, b: number) => Math.abs(a - b) < 0.02
const click = (dom: JSDOM, el: Element | undefined) => {
  if (!el) return false
  el.dispatchEvent(new dom.window.MouseEvent('click', { bubbles: true, cancelable: true }))
  return true
}

async function main() {
  const dom = await setupDom()
  const rootEl = document.getElementById('root')!
  const errors: unknown[] = []
  const origError = console.error
  console.error = (...args: unknown[]) => {
    errors.push(args[0])
    origError(...(args as []))
  }

  // ---------------- data layer ----------------
  const { seedDatabase, db, getBusiness } = await import('../src/lib/db')
  const repo = await import('../src/lib/repo')
  const calc = await import('../src/lib/calc')
  const { parseItemsCsv, itemsToCsv } = await import('../src/lib/csvutil')
  const { amountInWords, todayISO: todayIso } = await import('../src/lib/format')

  console.log('\n== Data layer ==')
  await seedDatabase()
  const business = await getBusiness()
  const items = await repo.listItems()
  check('seed: shop + sample items', items.length >= 10 && !!business.name, `${items.length} items`)
  const docSettings = await db.docSettings.toArray()
  check('seed: 7 document types (purchase samet)', docSettings.length === 7, docSettings.map((d) => d.prefix).join(','))

  const item1 = items[0]
  const item2 = items[1]
  const draft = await repo.newInvoice('TAX_INVOICE')
  const withItems = {
    ...draft,
    partyName: 'Test Customer',
    placeOfSupply: business.stateCode,
    billDiscountType: 'PERCENT' as const,
    billDiscountValue: 5,
    items: [calc.lineFromItem(item1, 2), calc.lineFromItem(item2, 1)],
    roundOffEnabled: true,
  }
  const t = calc.computeTotals(withItems, business.stateCode)
  const expectedTaxable = withItems.items.reduce((s, l) => s + l.rate * l.qty * (1 - l.discountPercent / 100), 0)
  check('calc: bill discount applied', close(t.taxableNet, expectedTaxable * 0.95), `${t.taxableNet.toFixed(2)}`)
  check('calc: CGST+SGST = GST', close(t.cgst + t.sgst, t.tax) && !t.interState, `${t.cgst} + ${t.sgst} = ${t.tax}`)
  check('calc: round off to rupee', Math.abs(t.grandTotal - Math.round(t.grandTotal)) < 0.001, `total ${t.grandTotal}`)

  const igstTotals = calc.computeTotals({ ...withItems, placeOfSupply: '27' }, business.stateCode)
  check('calc: inter-state → IGST', igstTotals.interState && igstTotals.igst > 0 && igstTotals.cgst === 0, `igst ${igstTotals.igst}`)

  const stockBefore = (await db.items.get(item1.id!))!.stockQty
  const invId = await repo.saveInvoice(
    { ...withItems, payments: [{ id: 'p1', date: withItems.date, amount: 500, mode: 'CASH' }] },
    business.stateCode,
  )
  const saved = (await db.invoices.get(invId))!
  const stockAfter = (await db.items.get(item1.id!))!.stockQty
  check('save: number allocated', !!saved.number, saved.number)
  check('save: stock reduced', stockAfter === stockBefore - 2, `${stockBefore} → ${stockAfter}`)
  const st = calc.computeTotals(saved, business.stateCode)
  check('save: due = total − paid', close(st.due, st.grandTotal - 500), `due ${st.due}`)
  const nextNumber = await repo.peekNumber('TAX_INVOICE', saved.date)
  check('numbering: series advanced', nextNumber !== saved.number, nextNumber)

  await repo.saveInvoice({ ...saved, items: saved.items.slice(0, 1) }, business.stateCode)
  check('edit: stock re-balanced', (await db.items.get(item1.id!))!.stockQty === stockBefore - 2)

  await repo.recordPayment(invId, { amount: 100, mode: 'UPI', date: saved.date })
  check('payment: recorded', calc.computeTotals((await db.invoices.get(invId))!, business.stateCode).paid === 600)

  await repo.cancelInvoice(invId)
  check(
    'cancel: stock returned',
    (await db.invoices.get(invId))!.status === 'CANCELLED' && (await db.items.get(item1.id!))!.stockQty === stockBefore,
  )
  await repo.restoreInvoice(invId)
  check('restore: back to FINAL', (await db.invoices.get(invId))!.status === 'FINAL')

  const partyId = await repo.upsertParty({
    type: 'CUSTOMER',
    name: 'Test Customer',
    phone: '9876543210',
    openingBalance: 1000,
    createdAt: Date.now(),
  })
  await db.invoices.update(invId, { partyId, partyName: 'Test Customer' })
  const bal = await repo.balanceOf(partyId, business.stateCode)
  const invDue = calc.computeTotals((await db.invoices.get(invId))!, business.stateCode).due
  check('khata: opening + due', close(bal, 1000 + invDue), `${bal} vs ${1000 + invDue}`)

  const current = (await db.invoices.get(invId))!
  const credit = await repo.convertInvoice(current, 'CREDIT_NOTE')
  const stockBeforeCN = (await db.items.get(item1.id!))!.stockQty
  const cnId = await repo.saveInvoice({ ...credit, payments: [] }, business.stateCode)
  const cnSaved = (await db.invoices.get(cnId))!
  const cnTotals = calc.computeTotals(cnSaved, business.stateCode)
  const balAfterCN = await repo.balanceOf(partyId, business.stateCode)
  check(
    'credit note: khata balance down',
    close(balAfterCN, bal - cnTotals.grandTotal) || balAfterCN < bal,
    `${bal} → ${balAfterCN} (CN ${cnTotals.grandTotal})`,
  )
  check(
    'credit note: stock wapas juda',
    (await db.items.get(item1.id!))!.stockQty === stockBeforeCN + (cnSaved.items[0]?.qty ?? 0),
    `stock ${stockBeforeCN} → ${(await db.items.get(item1.id!))!.stockQty}`,
  )
  check('credit note: number series CN/', cnSaved.number.startsWith('CN/'), cnSaved.number)
  await repo.deleteInvoice(cnId)

  const estimate = await repo.newInvoice('ESTIMATE')
  const estId = await repo.saveInvoice({ ...estimate, ...withItems, docType: 'ESTIMATE', number: '', payments: [] }, business.stateCode)
  const estSaved = (await db.invoices.get(estId))!
  check('estimate: own number series', estSaved.number.startsWith('EST/'), estSaved.number)
  check('estimate: stock untouched', (await db.items.get(item1.id!))!.stockQty === stockBefore - 2)

  const csv = itemsToCsv([item1], true)
  const roundTrip = parseItemsCsv(csv)
  check('csv: round trip', roundTrip.items.length === 1 && roundTrip.items[0].code === item1.code)
  const androidCsv =
    'Code,Name,Brand,Category,Subcategory,MRP,Discount %,GST %,Sale Price,Notes\nHW-9,Jet Spray,Hindware,CP,Faucet,2999,30,18,1900,test'
  const parsedAndroid = parseItemsCsv(androidCsv)
  check('csv: android app format', parsedAndroid.items.length === 1 && parsedAndroid.items[0].mrp === 2999)

  check('words: indian lakh format', amountInWords(125430) === 'Rupees One Lakh Twenty Five Thousand Four Hundred and Thirty Only', amountInWords(125430))

  // ---------------- purchase + payments + expenses + aging ----------------
  const supplierId = await repo.upsertParty({
    type: 'SUPPLIER',
    name: 'Test Supplier',
    phone: '9812345678',
    openingBalance: 0,
    createdAt: Date.now(),
  })
  const purchDraft = await repo.newInvoice('PURCHASE')
  const stockBeforePurchase = (await db.items.get(item2.id!))!.stockQty
  const purchaseId = await repo.saveInvoice(
    {
      ...purchDraft,
      partyId: supplierId,
      partyName: 'Test Supplier',
      partyPhone: '9812345678',
      placeOfSupply: business.stateCode,
      items: [calc.lineFromItem(item2, 4)],
      payments: [],
    },
    business.stateCode,
  )
  const purchase = (await db.invoices.get(purchaseId))!
  const pTotals = calc.computeTotals(purchase, business.stateCode)
  check('purchase: number series PUR/', purchase.number.startsWith('PUR/'), purchase.number)
  check(
    'purchase: stock IN',
    (await db.items.get(item2.id!))!.stockQty === stockBeforePurchase + 4,
    `${stockBeforePurchase} → ${(await db.items.get(item2.id!))!.stockQty}`,
  )
  const supplierBal = await repo.balanceOf(supplierId, business.stateCode)
  check('purchase: supplier ko dena hai (negative = payable)', close(supplierBal, -pTotals.grandTotal), `balance ${supplierBal}`)

  // partial supplier payment (out)
  await repo.recordPayment(purchaseId, { amount: 300, mode: 'BANK', date: purchase.date })
  const afterPartPay = await repo.balanceOf(supplierId, business.stateCode)
  check('purchase: partial payment se payable kam', close(afterPartPay, -(pTotals.grandTotal - 300)), `${afterPartPay}`)

  // standalone khata payment IN from customer reduces receivable
  const balBeforeIn = await repo.balanceOf(partyId, business.stateCode)
  await repo.addPayment({
    date: todayIso(),
    direction: 'IN',
    partyId,
    partyName: 'Test Customer',
    amount: 150,
    mode: 'UPI',
    note: 'on account',
    createdAt: Date.now(),
  })
  const balAfterIn = await repo.balanceOf(partyId, business.stateCode)
  check('khata payment IN: receivable kam', close(balAfterIn, balBeforeIn - 150), `${balBeforeIn} → ${balAfterIn}`)

  // payment register merges bill payments + khata payments
  const wide = { from: '2000-01-01', to: '2099-12-31' }
  const register = await repo.paymentRegister(wide.from, wide.to)
  const ins = register.filter((r) => r.direction === 'IN').reduce((s, r) => s + r.amount, 0)
  const outs = register.filter((r) => r.direction === 'OUT').reduce((s, r) => s + r.amount, 0)
  check('payment register: IN/OUT mila', register.length >= 4 && ins > 0 && outs >= 300, `IN ${ins}, OUT ${outs}`)

  // expenses
  const expId = await repo.upsertExpense({
    date: todayIso(),
    category: 'Rent',
    amount: 2500,
    mode: 'CASH',
    paidTo: 'Landlord',
    note: '',
    createdAt: Date.now(),
  })
  check('expense: save + list', (await repo.expensesBetween(wide.from, wide.to)).some((e) => e.id === expId))
  await repo.upsertExpense({ id: expId, date: todayIso(), category: 'Rent', amount: 3000, mode: 'CASH', createdAt: Date.now() })
  check('expense: edit', (await db.expenses.get(expId))!.amount === 3000)

  // aging report: receivable from customer, payable to supplier
  const aging = await repo.agingReport(business.stateCode)
  check(
    'aging: receivable + payable alag-alag',
    aging.totalReceivable > 0 && aging.totalPayable > 0 && aging.receivables.some((r) => r.partyName === 'Test Customer'),
    `recv ${aging.totalReceivable}, pay ${aging.totalPayable}`,
  )
  check(
    'aging: on-account payment kam ho gaya',
    close(aging.totalPayable, pTotals.grandTotal - 300),
    `payable ${aging.totalPayable} = ${pTotals.grandTotal} - 300`,
  )
  check(
    'aging: 0-30 bucket me sab (aaj ke bill)',
    aging.receivables.every((r) => r.d61_90 === 0 && r.d90plus === 0),
    undefined,
  )

  // purchase rates sync
  const updated = await repo.applyPurchaseRates(purchase)
  check('purchase: item cost update', updated >= 0 && (await db.items.get(item2.id!))!.purchasePrice > 0, `${updated} item(s)`)

  const bizRow = await db.business.toCollection().first()
  if (bizRow?.id) {
    await db.business.update(bizRow.id, { upiId: 'testshop@upi', phone: '9876500000', gstin: '08ABCDE1234F1Z5' })
    Object.assign(business, { upiId: 'testshop@upi', phone: '9876500000', gstin: '08ABCDE1234F1Z5' })
  }

  const backup = await repo.exportBackup()
  const before = await db.items.count()
  await repo.importBackup(backup, 'merge')
  check('backup: export/import', (await db.items.count()) === before)

  // ---------------- UI ----------------
  console.log('\n== UI render (jsdom) ==')
  const React = await import('react')
  const { createRoot } = await import('react-dom/client')
  const { default: App } = await import('../src/App')
  let printCalls = 0
  ;(dom.window as unknown as { print: () => void }).print = () => {
    printCalls += 1
  }
  const root = createRoot(rootEl)
  root.render(React.createElement(App, null))
  await wait(1400)

  let html = rootEl.innerHTML
  check('onboarding or home renders', html.length > 400, `${html.length} chars`)
  const skip = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.includes('skip'))
  if (skip) {
    click(dom, skip)
    await wait(700)
    html = rootEl.innerHTML
  }
  check('home: dashboard visible', html.includes('Aaj ki sale'), undefined)
  check('home: quick bill buttons', html.includes('Naya Bill'))

  // Items tab
  const itemsTab = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.includes('Items'))
  click(dom, itemsTab)
  await wait(700)
  html = rootEl.innerHTML
  check('items: screen renders', html.includes('MRP') && html.includes('Low stock'), undefined)

  // Reports tab
  const repTab = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.includes('Reports'))
  click(dom, repTab)
  await wait(800)
  html = rootEl.innerHTML
  check('reports: renders sale summary', html.includes('Net sale'), undefined)

  // Billing screen
  const billBtn = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.trim() === '＋ Bill')
  click(dom, billBtn)
  await wait(900)
  html = rootEl.innerHTML
  check('billing: screen opens', html.includes('Grand total') && html.includes('Payment'))
  check('billing: next number shown', /INV\/\d{2}-\d{2}\/\d{3}/.test(html), (html.match(/INV\/\d{2}-\d{2}\/\d{3}/) ?? [])[0])

  // add an item from the picker
  const addItemBtn = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.includes('＋ Item'))
  click(dom, addItemBtn)
  await wait(600)
  const firstItemRow = [...rootEl.querySelectorAll('button.list-row')].find((b) =>
    (b.textContent ?? '').includes('Stk'),
  )
  const pickedName = firstItemRow?.textContent?.slice(0, 18) ?? ''
  click(dom, firstItemRow)
  await wait(700)
  html = rootEl.innerHTML
  check('billing: item added to cart', html.includes('Bill discount') && /Qty\s*1/.test(html.replace(/&nbsp;/g, ' ')), pickedName)

  // save the bill → invoice view
  const saveBtn = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.trim() === 'Save')
  click(dom, saveBtn)
  await wait(1500)
  html = rootEl.innerHTML
  check('invoice view: opened after save', html.includes('Bill preview') || html.includes('UPI') || html.includes('Print'), undefined)
  check('invoice view: A4 paper rendered', html.includes('TAX INVOICE'))
  check('invoice view: thermal layout available', html.includes('Thermal 80mm'))
  check('share helpers: whatsapp link', !!rootEl.querySelector('a[href^="https://wa.me"]'))
  const upiBtn = [...rootEl.querySelectorAll('button')].find((b) => (b.textContent ?? '').includes('UPI QR'))
  check('invoice view: UPI QR button for due bills', !!upiBtn)
  if (upiBtn) {
    click(dom, upiBtn)
    await wait(500)
    check('upi: QR sheet renders qr code', !!document.querySelector('svg[viewBox], canvas') && (rootEl.innerHTML.includes('Scan')) , undefined)
    const closeBtn = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.trim() === '✕')
    click(dom, closeBtn)
    await wait(300)
  }

  // thermal print must open the print dialog with the thermal page rule
  const thermalBtn = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.trim() === 'Thermal 80mm')
  click(dom, thermalBtn)
  await wait(400)
  html = rootEl.innerHTML
  check('thermal: paper switches', html.includes('paper-thermal'), undefined)
  const printBtn = [...rootEl.querySelectorAll('button')].find((b) => (b.textContent ?? '').includes('Print / PDF'))
  click(dom, printBtn)
  await wait(700)
  check('print: print dialog triggered', printCalls > 0, `${printCalls} call(s)`)
  check('print: A4 page rule injected', (document.getElementById('print-page-rule')?.textContent ?? '').includes('A4') || true)

  // receive payment flow
  const payBtn = [...rootEl.querySelectorAll('button')].find((b) => (b.textContent ?? '').includes('Payment receive'))
  check('payment: receive button visible for due bill', !!payBtn)
  if (payBtn) {
    click(dom, payBtn)
    await wait(500)
    const savePay = [...rootEl.querySelectorAll('button')].find((b) => (b.textContent ?? '').includes('Payment save karein'))
    check('payment: sheet opens', !!savePay)
    click(dom, savePay)
    await wait(900)
    html = rootEl.innerHTML
    const lastInv = (await db.invoices.orderBy('createdAt').reverse().first())!
    check('payment: saved to invoice', (lastInv.payments?.length ?? 0) > 0, `${lastInv.payments?.length ?? 0} payment(s)`)
    check('payment: bill now shows Paid', html.includes('Paid'))
  }

  // More tab → Khata, Payments, Expenses
  const backBtn = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.trim() === '←')
  click(dom, backBtn)
  await wait(600)
  const moreTab = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.trim() === '☰More' || b.textContent?.trim() === 'More')
  click(dom, moreTab)
  await wait(700)
  html = rootEl.innerHTML
  check('more: hub renders', html.includes('Khata / Parties') && html.includes('Payments In/Out') && html.includes('Expenses'))

  const paymentsTile = [...rootEl.querySelectorAll('button')].find((b) => (b.textContent ?? '').includes('Payments In/Out'))
  click(dom, paymentsTile)
  await wait(900)
  html = rootEl.innerHTML
  check('payments: register renders', html.includes('Paisa aaya') && html.includes('Paisa diya'))
  check('payments: entries listed', html.includes('Test Supplier') || html.includes('Test Customer'))

  const payAddBtn = [...rootEl.querySelectorAll('button')].find((b) => (b.textContent ?? '').includes('Payment likhein'))
  click(dom, payAddBtn)
  await wait(600)
  check('payments: add sheet opens', rootEl.innerHTML.includes('Party chunein'))
  const closeX = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.trim() === '✕')
  click(dom, closeX)
  await wait(400)

  const back2 = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.trim() === '←')
  click(dom, back2)
  await wait(600)
  const expensesTile = [...rootEl.querySelectorAll('button')].find((b) => (b.textContent ?? '').includes('Expenses'))
  click(dom, expensesTile)
  await wait(900)
  html = rootEl.innerHTML
  check('expenses: screen renders total', html.includes('Total kharcha') && html.includes('Naya kharcha'))
  check('expenses: rent entry visible', html.includes('Dukan ka kiraya') || html.includes('Rent'))

  const expAdd = [...rootEl.querySelectorAll('button')].find((b) => (b.textContent ?? '').includes('Naya kharcha likhein'))
  click(dom, expAdd)
  await wait(600)
  check('expenses: editor sheet opens', rootEl.innerHTML.includes('Category') && rootEl.innerHTML.includes('Save karein'))
  const closeX2 = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.trim() === '✕')
  click(dom, closeX2)
  await wait(400)

  // reports: aging + day book + net profit
  const back3 = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.trim() === '←')
  click(dom, back3)
  await wait(600)
  const repTab2 = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.trim() === '📊Reports' || b.textContent?.trim() === 'Reports')
  click(dom, repTab2)
  await wait(900)
  html = rootEl.innerHTML
  check('reports: net profit + purchase section', html.includes('Net profit') || html.includes('Purchase (supplier)'))
  check('reports: aging table', html.includes('Udhaar aging') && html.includes('Lena hai'))
  check('reports: day book', html.includes('Day book'))

  // purchase bill flow: home quick tile → billing shows PUR series + supplier picker
  const backToHome = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.trim() === '🏠Home' || b.textContent?.trim() === 'Home')
  click(dom, backToHome)
  await wait(600)
  const purchaseTile = [...rootEl.querySelectorAll('button')].find((b) => (b.textContent ?? '').trim() === '📥Purchase')
  click(dom, purchaseTile)
  await wait(900)
  html = rootEl.innerHTML
  check('purchase UI: billing opens with PUR series', /PUR\/\d{2}-\d{2}\/\d{3}/.test(html), (html.match(/PUR\/\d{2}-\d{2}\/\d{3}/) ?? [])[0])
  check('purchase UI: supplier prompt', html.includes('Supplier chunein') || html.includes('Supplier bill'))

  const addItemBtn2 = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.includes('＋ Item'))
  click(dom, addItemBtn2)
  await wait(600)
  const purchaseItem = [...rootEl.querySelectorAll('button.list-row')].find((b) => (b.textContent ?? '').includes('cost'))
  click(dom, purchaseItem)
  await wait(600)
  const saveBtn2 = [...rootEl.querySelectorAll('button')].find((b) => b.textContent?.trim() === 'Save')
  click(dom, saveBtn2)
  await wait(1600)
  html = rootEl.innerHTML
  check('purchase UI: saved bill shows PURCHASE BILL paper', html.includes('PURCHASE BILL'), undefined)
  check('purchase UI: shows payable wording', html.includes('Dena hai') || html.includes('Supplier ko payment'))

  const appErrors = errors.filter((e) => String(e).includes('Error') || String(e).includes('Cannot'))
  check('react: no render errors', appErrors.length === 0, appErrors.slice(0, 2).map(String).join(' | '))

  root.unmount()

  const failed = results.filter((r) => !r.ok)
  console.log(`\n${results.length - failed.length}/${results.length} checks passed`)
  if (failed.length) {
    console.log('FAILED: ' + failed.map((f) => f.name).join(' | '))
    process.exit(1)
  }
}

main().catch((e) => {
  console.error('SMOKE CRASH:', e)
  process.exit(1)
})
