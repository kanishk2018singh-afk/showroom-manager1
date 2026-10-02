import { useCallback, useEffect, useState } from 'react'
import { db, getBusiness } from './lib/db'
import { newInvoice } from './lib/repo'
import { fmtDate, todayISO } from './lib/format'
import type { Business, DocType, Invoice } from './lib/types'
import { Toaster, Fab, toast } from './components/ui'
import { HomeScreen } from './screens/Home'
import { ItemsScreen } from './screens/Items'
import { InvoicesScreen } from './screens/Invoices'
import { ReportsScreen } from './screens/Reports'
import { SettingsScreen } from './screens/Settings'
import { PartiesScreen } from './screens/Parties'
import { BillingScreen } from './screens/Billing'
import { InvoiceView } from './screens/InvoiceView'
import { Onboarding } from './screens/Onboarding'

type SubRoute =
  | { name: 'none' }
  | { name: 'billing'; draft: Invoice }
  | { name: 'view'; id: number }
  | { name: 'parties' }

const TABS = [
  { icon: '🏠', label: 'Home' },
  { icon: '📦', label: 'Items' },
  { icon: '🧾', label: 'Bills' },
  { icon: '📊', label: 'Reports' },
  { icon: '⚙️', label: 'Settings' },
]

export default function App() {
  const [business, setBusiness] = useState<Business | null>(null)
  const [onboarded, setOnboarded] = useState<boolean | null>(null)
  const [tab, setTab] = useState(0)
  const [route, setRoute] = useState<SubRoute>({ name: 'none' })
  const [lowStockFocus, setLowStockFocus] = useState(false)
  const [installEvt, setInstallEvt] = useState<{ prompt: () => Promise<void> } | null>(null)
  const [seeded, setSeeded] = useState(false)

  const loadBusiness = useCallback(async () => {
    const [b, setting] = await Promise.all([getBusiness(), db.appSettings.get('onboarded')])
    setBusiness(b)
    setOnboarded(setting?.value === 'yes')
  }, [])

  useEffect(() => {
    void loadBusiness()
    void db.items.count().then((c) => setSeeded(c > 0))
  }, [loadBusiness])

  // Android back button / browser back closes the open sub-screen
  useEffect(() => {
    const onPop = () => setRoute((r) => (r.name === 'none' ? r : { name: 'none' }))
    window.addEventListener('popstate', onPop)
    return () => window.removeEventListener('popstate', onPop)
  }, [])

  useEffect(() => {
    const handler = (e: Event) => {
      e.preventDefault()
      setInstallEvt(e as unknown as { prompt: () => Promise<void> })
    }
    window.addEventListener('beforeinstallprompt', handler)
    return () => window.removeEventListener('beforeinstallprompt', handler)
  }, [])

  const openRoute = (r: SubRoute) => {
    try {
      history.pushState({ sub: r.name }, '')
    } catch {
      /* ignore */
    }
    setRoute(r)
  }

  const openNewBill = async (docType: DocType) => {
    if (!business) return
    const draft = await newInvoice(docType)
    if (!draft.placeOfSupply) draft.placeOfSupply = business.stateCode
    openRoute({ name: 'billing', draft })
  }

  const install = async () => {
    if (!installEvt) return
    await installEvt.prompt()
    setInstallEvt(null)
  }

  if (!business || onboarded === null) {
    return (
      <div className="app-shell items-center justify-center">
        <div className="mt-24 text-center text-sm text-slate-500">Showroom Manager load ho raha hai…</div>
      </div>
    )
  }

  if (!onboarded) {
    return (
      <>
        <Onboarding business={business} onDone={() => void loadBusiness()} />
        <Toaster />
      </>
    )
  }

  if (route.name === 'billing') {
    return (
      <div className="app-shell">
        <BillingScreen
          draft={route.draft}
          business={business}
          onBack={() => setRoute({ name: 'none' })}
          onSaved={(id) => {
            setRoute({ name: 'view', id })
            setTab(2)
          }}
        />
        <Toaster />
      </div>
    )
  }

  if (route.name === 'view') {
    return (
      <div className="app-shell">
        <InvoiceView
          invoiceId={route.id}
          business={business}
          onBack={() => setRoute({ name: 'none' })}
          onEdit={(inv) => setRoute({ name: 'billing', draft: inv })}
          onConverted={(draft) => {
            toast('Naya document banaya gaya — check karke save karein')
            setRoute({ name: 'billing', draft })
          }}
        />
        <Toaster />
      </div>
    )
  }

  if (route.name === 'parties') {
    return (
      <div className="app-shell">
        <div className="topbar">
          <button className="btn btn-sm bg-white/10 text-white hover:bg-white/20" onClick={() => setRoute({ name: 'none' })}>
            ←
          </button>
          <div className="flex-1">
            <div className="text-sm font-bold">Khata / Parties</div>
            <div className="text-[11px] text-brand-200">Customer & supplier udhaar</div>
          </div>
        </div>
        <PartiesScreen business={business} onOpenInvoice={(id) => openRoute({ name: 'view', id })} />
        <Toaster />
      </div>
    )
  }

  const title =
    tab === 0 ? business.name : tab === 1 ? 'Items / Stock' : tab === 2 ? 'Bills' : tab === 3 ? 'Reports' : 'Settings'

  return (
    <div className="app-shell">
      <div className="topbar">
        <div className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-white/15 text-lg">🏪</div>
        <div className="min-w-0 flex-1">
          <div className="truncate text-sm font-bold">{title}</div>
          <div className="truncate text-[11px] text-brand-200">
            {seeded ? `${fmtDate(todayISO())}` : 'Setup…'}
            {business.gstin ? ` • GSTIN ${business.gstin}` : ''}
          </div>
        </div>
        <button className="btn btn-sm bg-white/10 text-white hover:bg-white/20" onClick={() => void openNewBill('TAX_INVOICE')}>
          ＋ Bill
        </button>
        <button
          className="btn btn-sm bg-white/10 text-white hover:bg-white/20"
          onClick={() => {
            setTab(4)
            setRoute({ name: 'none' })
          }}
        >
          ⚙️
        </button>
      </div>

      {tab === 0 ? (
        <HomeScreen
          business={business}
          lowStockOnly
          onNewBill={(d) => void openNewBill(d)}
          onOpenInvoice={(id) => openRoute({ name: 'view', id })}
          onGoItems={() => {
            setLowStockFocus(true)
            setTab(1)
          }}
          onGoReports={() => setTab(3)}
          onGoParties={() => openRoute({ name: 'parties' })}
        />
      ) : null}

      {tab === 1 ? <ItemsScreen business={business} focusLowStock={lowStockFocus} /> : null}

      {tab === 2 ? (
        <InvoicesScreen business={business} onOpen={(id) => openRoute({ name: 'view', id })} onNewBill={(d) => void openNewBill(d)} />
      ) : null}

      {tab === 3 ? <ReportsScreen business={business} /> : null}

      {tab === 4 ? (
        <SettingsScreen
          business={business}
          onBusinessChange={() => void loadBusiness()}
          onOpenParties={() => openRoute({ name: 'parties' })}
          installPrompt={installEvt}
          onInstall={() => void install()}
        />
      ) : null}

      {tab !== 2 ? <Fab label="Naya Bill" onClick={() => void openNewBill('TAX_INVOICE')} /> : null}

      <nav className="tabbar no-print">
        {TABS.map((t, i) => (
          <button key={t.label} className="tab-item" data-active={tab === i} onClick={() => setTab(i)}>
            <span className="text-lg leading-none">{t.icon}</span>
            <span>{t.label}</span>
          </button>
        ))}
      </nav>

      <Toaster />
    </div>
  )
}
