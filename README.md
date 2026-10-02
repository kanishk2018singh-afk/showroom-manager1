<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Showroom Manager 🏪

## 📱 Android APK — seedha download karein

**⬇️ https://github.com/kanishk2018singh-afk/showroom-manager1/releases/download/apk-latest/app-debug.apk**

Phone me: link kholein → APK download → **"Install anyway" / unknown sources allow** karein.

Install ke baad **2 icon** milenge:
- **Showroom Manager** → MyBillBook jaisa billing app (pura app APK ke andar bundled — internet ke bina bhi chalta hai; print, WhatsApp share, backup sab)
- **Showroom Catalogue (AI)** → purani app (product catalogue + AI tools)

Har push par APK khud ban kar isi link par update ho jata hai (workflow: `.github/workflows/build-apk.yml`).

> ### 🌐 App browser me kholein
>
> **1) GitHub Pages (recommended, clean link):**
> Settings → Pages → Source: *Deploy from a branch* → Branch: `arena/01a0fba1-showroom-manager1`, folder `/docs` → Save.
> Phir link: **https://kanishk2018singh-afk.github.io/showroom-manager1/**
>
> **2) Bina setup, abhi ke liye (ek extra click):**
> https://rawcdn.githack.com/kanishk2018singh-afk/showroom-manager1/v2.0.0/docs/index.html
> (pehle "One more step → Open the page" aata hai, us par click karein)
>
> **3) Ek-file offline version:** `docs/showroom-manager-app.html` — download karke browser me kholein (server ki zarurat nahi).
>
> ⚠️ Note: jsDelivr/CDN par HTML files `text/plain` me milti hain (security policy), isliye un links par source code dikhta hai — app nahi.

Do hisse hain is repo me:

| Folder | Kya hai | Kaise chalayein |
| --- | --- | --- |
| `webapp/` | **MyBillBook जैसी Billing app (Web/PWA)** — GST invoice, estimate, challan, credit note, **purchase bill**, barcode billing, udhaar khata, **payments in/out**, **expenses**, **aging + day book reports**, UPI QR, WhatsApp bill, print (A4 + thermal), **🔐 users + PIN login**, **🏢 multi-company (firm) create & switch**, **☁️ Google/Email login + cloud sync** | `cd webapp && npm install && npm run dev` |
| `app/` | Purani Android app (Kotlin + Compose) — product catalogue, pricing, AI studio | Android Studio, ya GitHub Actions se APK |
| `web/` | Purana single-file web catalogue demo (`web/index.html`) | browser me khol lein |

Web app ke baare me poori jankari: [`webapp/README.md`](webapp/README.md)

**Web app chalayein / test karein (repo root se):**

```bash
npm install --prefix webapp   # sirf pehli baar (ya seedha npm run smoke — wo khud install kar leta hai)
npm run dev                   # http://localhost:5173
npm run smoke                 # 79 automated checks (data + UI) — ek hi command me
```

> 🧪 Terminal nahi chahiye? App ke andar **Settings → App self-test** hai (32 checks, browser me, aapka data safe).
> Details + troubleshooting: [`webapp/README.md`](webapp/README.md#-testing-smoke-test)

**Phone me install:** app ko Chrome me kholkar menu (⋮) → "Install app" / "Add to Home screen" — home screen par icon aa jayega aur bina internet bhi chalega. Data phone ke andar (IndexedDB) hi rehta hai.

**Deploy:** `main` branch par push karne par `.github/workflows/deploy-webapp.yml` GitHub Pages par PWA deploy kar deta hai (HTTPS link milta hai jisse phone me install ho jata hai).

---

# Run and deploy your AI Studio app (purani Android app)

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/313276e6-a36e-44f2-8797-acfd1de9a54e

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
7. If you have already published your app in AI Studio, please [request upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset) in Google Play Console.
