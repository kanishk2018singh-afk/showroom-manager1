# Showroom Manager — Billing Edition (Web App / PWA)

MyBillBook जैसा **showroom billing app** — GST invoice, estimate, barcode billing, stock aur reports.
पूरा offline चलता है, data आपके phone/browser में ही रहता है (कोई login नहीं, कोई server नहीं)।

यह `webapp/` folder उसी repo की Android app (`app/`) का **web version** है — phone में "Add to Home screen" करके
native app की तरह install किया जा सकता है।

---

## ✨ क्या-क्या है (Billing strong)

### 🧾 Billing (मुख्य)
- **Tax Invoice / पक्का बिल (GST)** — CGST+SGST या IGST (place of supply के हिसाब से) automatic
- **Estimate / Quotation**, **Proforma Invoice**, **Delivery Challan**, **Bill of Supply** (बिना GST), **Credit Note / Sales Return**
- **Item-wise discount % + Bill-level discount** (₹ या %), extra charges (freight/hamali), round-off
- **Barcode / quick billing** — barcode scan करके या नाम/code से item tap करके 2 second में bill
- USB/Bluetooth barcode scanner (keyboard-type) भी चलता है — code box में type करके Enter
- **Bill number series** per document type — जैसे `INV/25-26/001`, `EST/25-26/007` (Settings से बदल सकते हैं)
- **Payment tracking** — पूरा paid / आधा / **उधार (credit)**; cash, UPI, card, bank, cheque
- **WhatsApp पर bill** — पूरे bill का text message, या bill की **image** (native share sheet)
- **UPI QR** — बाकी amount का QR bill पर छपता है (PhonePe/GPay/Paytm scan करके pay)
- **Print / PDF** — A4 invoice (professional layout) और **80mm thermal printer** layout
- Estimate → Tax Invoice **convert**, bill duplicate, credit note बनाना, bill cancel (stock वापस) / delete

### 📦 Items & Stock
- Item master: code, barcode, brand, category, sub-category, HSN, unit, MRP, discount %, GST %, purchase rate, stock, low-stock alert
- Sale price + **margin per piece** live calculation
- Stock खुद कट जाता है bill पर (cancel/credit note पर वापस जुड़ जाता है)
- **CSV import/export** (Excel) — Android app की पुरानी CSV file भी चलती है; template भी download हो सकता है

### 👥 Khata / Parties
- Customer & Supplier list, **किसका कितना बाकी है** (udhaar khata)
- Ledger — party के सारे bills, total business, paid, बाकी
- **WhatsApp payment reminder** एक tap में
- Opening balance (पुराना बकाया) support

### 📊 Reports
- Net sale, taxable value, GST collected, discount दिया, अनुमानित **profit**, bill count, avg bill
- **Din-wise sale chart**, top items, top parties, payment-mode summary, document-type summary
- **GST / HSN summary** (GSTR-1 जैसा) — CGST/SGST breakdown
- Stock value report + low-stock list
- सब reports **CSV export** (sales register, GST summary, item-wise, party-wise)

### ⚙️ Settings
- दुकान की details (नाम, address, phone, GSTIN, state, UPI ID, bank) + **logo & signature upload**
- Per-document number series, default terms & conditions
- **Backup / restore** (JSON file) + full data reset
- App install (PWA) button

---

## 🚀 चलाने का तरीका (development)

```bash
cd webapp
npm install
npm run dev        # http://localhost:5173
```

दूसरे device/network से खोलने के लिए server पहले से `0.0.0.0` पर bind है।

Production build:

```bash
npm run build      # dist/ बनेगा (offline PWA + service worker के साथ)
npm run preview    # build को local test
```

## 📱 Phone में app की तरह install कैसे करें
1. App को Android **Chrome** में खोलें (HTTPS link होना चाहिए)
2. Menu (⋮) → **“Install app” / “Add to Home screen”**
3. Home screen पर 🏪 icon आ जाएगा — बिना internet भी खुलेगा

> Data browser storage (IndexedDB) में रहता है। इसलिए महीने में एक बार
> **Settings → Backup** से JSON file ज़रूर निकाल लें। Browser data clear करने पर data चला जाएगा।

## 🌐 Deploy
GitHub Actions workflow `.github/workflows/deploy-webapp.yml` — `main` branch पर push होने पर webapp build होकर
**GitHub Pages** पर deploy हो जाता है (https://<user>.github.io/showroom-manager1/)।
इसी HTTPS link को phone में खोलकर install किया जा सकता है।

## 🧪 Testing (smoke test)

Poore billing flow ka automated test hai (jsdom + fake IndexedDB) — isi se data layer aur UI dono check hote hain:

```bash
npm run smoke
```

Ye 46 checks chalata hai: invoice maths (GST/CGST/SGST/IGST, bill discount, round off), number series,
stock cut/restore, payment recording, khata balance, credit note, CSV import/export, backup-restore,
aur UI flow (home → items → reports → billing → item add → save → invoice view → print → UPI QR → payment).

## 🧱 Tech
Vite + React 19 + TypeScript + Tailwind CSS 4 + Dexie (IndexedDB) + vite-plugin-pwa + qrcode.react + html2canvas-pro

```
src/
  lib/        types, db (Dexie), repo (billing logic), calc (GST maths), format, doc, csv, print
  components/ UI primitives, InvoicePaper (A4 + thermal), BarcodeScanner, pickers
  screens/    Home, Billing, InvoiceView, Invoices, Items, Parties, Reports, Settings, Onboarding
```
