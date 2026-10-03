# Android: Firebase Auth + Firestore Sync (offline-first)

Ye document Showroom Manager Android app me juda hua **login + cloud sync** system samjhata hai —
kya kaam karta hai, Firestore me data kahan jata hai, aur device par kaise test karein.

---

## 1. Architecture (locked)

```
Compose UI  →  ShowroomViewModel  →  ShowroomRepository  →  Room (primary local DB)
                                                          ↕
                                              FirestoreSync (offline-first engine)
                                                          ↕
                                              Cloud Firestore (users/{uid}/...)
```

- **Room hi primary database hai.** App bina internet bhi poori chalti hai.
- Firestore optional hai — login + internet ho to data cloud me bhi jata hai.
- Duplicate/parallel data layer nahi banaya gaya; purane `ShowroomRepository` ke methods waise hi kaam karte hain.

## 2. Firestore structure

```
users/{uid}                        → profile (naam, email, lastSyncTime)
users/{uid}/companies/{docId}      → company/firm
users/{uid}/categories/{docId}     → category
users/{uid}/products/{docId}       → product (catalogue)
```

- `{uid}` = Firebase Auth ka UID (email / Google / anonymous — sabhi).
- **docId = stable local id**: product → `{companyId}_{code}`, company/category → naam (lowercase, `/` → `_`).
  Isse har device par same record ka same document banta hai (duplicate nahi).
- Room me timestamps **Long** hi hain. Cloud me `updatedAt` (Firestore Timestamp) + `updatedAtMillis` (Long)
  dono likhe jate hain, aur padhte waqt `Timestamp.toRoomLong()` / `Long.toFirestoreTimestamp()` helpers use hote hain.

## 3. Sync cycle (offline-first)

1. User app me kuch bhi save karta hai → **Room me likha jata hai** (UI turant update).
2. Row par `isSynced = false` mark hota hai (pending upload).
3. Best-effort upload Firestore par jata hai (debounce ~2s, batch 400 docs).
4. **Sirf safal write ke baad** `isSynced = true` hota hai.
5. Internet/config/permission fail ho → data Room me safe rehta hai, `isSynced = false` hi rehta hai.
6. **Retry automatic**: internet wapas aate hi (network callback) + har 2 minute ka loop + app start par.
7. **Delete**: local row turant delete, aur `sync_queue` table me pending delete — internet aane par cloud se bhi hat jata hai.

## 4. Conflict resolution (sirf `updatedAt`)

| Local | Cloud | Result |
|---|---|---|
| purana | naya | Cloud se Room me update |
| naya | purana | Local cloud par upload |
| same | same | Kuch nahi hota |

Naya data kabhi blindly overwrite nahi hota. Pending (`isSynced = false`) rows par cloud purani ho to upload hone se pehle merge check hota hai.

## 5. Login / Auth

- Login hone tak **login screen** dikhti hai; login ke turant baad dashboard + initial sync (pull → merge → upload).
- Password reset email: "Password bhool gaye? Reset email bhejein" → Firebase reset mail.
- Password kabhi Room/Firestore me save **nahi** hota (sirf Firebase Auth ke paas).
- Session persistent hai (app band karke kholne par login bana rehta hai). Session detection
  `FirebaseAuthManager.authStateFlow` se hoti hai — cold start par Firebase jab session restore karta hai,
  app usi waqt dashboard dikhati hai (bech me chhota "Login check ho raha hai…" indicator) aur
  **har naye uid ke liye sirf ek baar** initial sync chalta hai (duplicate sync nahi).
- `google-services.json` na ho / Firebase configure na ho → app crash nahi karti, **poora offline** chalti hai, aur
  login screen par ek clear message dikhta hai. "Login ke bina chalayein (offline)" option bhi hai.
- Google Sign-In ke liye Firebase Console me **Google provider on** + **SHA-1** add hona chahiye.

## 6. Security

- `firestore.rules` (repo root): har user sirf **apne** `users/{uid}/...` data ko read/write kar sakta hai.
- `allow read, write: if true` jaisa public rule kahin nahi hai; baaki sab default-deny.
- Deploy: `firebase deploy --only firestore:rules` (ya Console → Firestore → Rules me paste + Publish).

## 7. Setup checklist (ek baar)

1. `app/google-services.json` repo me hai (project `showroom-manager-c0856`, package `com.aistudio.showroommanager.pqlmvs`) — ✅
2. Firebase Console → Authentication → **Email/Password** enable karein (Google chahiye to Google bhi; Guest ke liye Anonymous).
3. Google Sign-In ke liye: Project settings → apna app → **SHA-1** add karein (debug + release), Google provider enable.
4. Firestore Database banayein (production mode) aur `firestore.rules` publish karein.
5. Areas (region) apne hisaab se — rules region-agnostic hain.

## 8. Test scenarios (13)

| # | Scenario | Steps | Expected |
|---|---|---|---|
| 1 | Naya account (email) | Login screen → Sign up → email/password/naam | Account ban jata hai, dashboard khulta hai, initial sync chalta hai |
| 2 | Wapas login | App band karke kholo → sign out → wapas sign in | Data wapas aa jata hai (persistent session + cloud pull) |
| 2b | Cold-start session restore | Login karke app band karo → wapas kholo | Chhota "Login check ho raha hai…" → seedha dashboard (login screen nahi), auto sync |
| 3 | Galat password | Galat password se login | Friendly error, crash nahi, data safe |
| 4 | Password reset | "Password bhool gaye?" → email daalo | Reset email ka message (mail inbox me aata hai) |
| 5 | Offline login-bypass | "Login ke bina chalayein (offline)" | App normal chalti hai, sirf cloud features off |
| 6 | Offline me save | Airplane mode → naya product add | Product turant dikhta hai; "⏳ N pending" dikhta hai |
| 7 | Auto upload | Airplane mode off | Kuch seconds/minutes me pending 0 ho jata hai |
| 8 | App band karke offline save | Offline me add → app kill → wapas kholo (online) | Data Room me tha, ab cloud par chala gaya |
| 9 | Do device sync | Device A par add → Device B par login/sync | Device B par wahi product aa jata hai |
| 10 | Conflict (cloud naya) | Device A par edit (online), Device B par purana data hold karke pull | Cloud ka naya version jeetta hai |
| 11 | Conflict (local naya) | Device B offline edit → online | Local version cloud par jata hai (naya data overwrite nahi hota) |
| 12 | Delete sync | Product delete (online) | Cloud se bhi hat jata hai; offline delete baad me apply hota hai |
| 13 | Config ke bina app | `google-services.json` hata kar build | App chalti hai (offline), login screen par clear message, koi crash nahi |

> Firebase auth/sync ka asli network test sirf device/emulator par hota hai (sandbox me Google domains blocked hain),
> isliye CI par **build + compile** verify hota hai aur runtime scenarios aap device par upar ke steps se check kar sakte hain.

## 9. Room schema (v3)

- `products`: + `isSynced` (Boolean, default false)
- `companies`: + `updatedAt` (Long) + `isSynced`
- `categories`: + `updatedAt` (Long) + `isSynced`
- `sync_queue` (nayi table): pending deletes (`collectionName`, `docId`, `operation`, `attempts`)

`MIGRATION_2_3` safe hai — purana data delete nahi hota (fallback destructive migration sirf emergency ke liye hai).

## 10. Signing keys aur Google Sign-In (SHA-1)

Downloadable APK **stable debug key** se sign hota hai (`showroom-debug.keystore`, repo me committed):

| Key | SHA-1 | SHA-256 |
|---|---|---|
| `showroom-debug.keystore` (CI/download APK) | `3F:F9:A2:D0:20:10:0D:EA:D4:D1:50:1D:D6:2E:B4:5D:EB:60:90:6F` | `90:9D:8A:B1:7D:0C:CF:DA:8B:C1:45:65:1D:51:F2:65:5A:F4:F5:FF:4C:45:DC:BC:F9:A8:27:CD:3D:27:97:41` |

- **Google Sign-In** ke liye jo key se APK sign hua hai, uska SHA-1 Firebase Console me hona chahiye:
  Project settings → aapka Android app → *Add fingerprint* → SHA-1 paste → Save → naya `google-services.json`
  download karke `app/` me rakhein (aur repo me commit karein).
- Agar aap **apne computer par** build karte hain (Android Studio / `gradlew.bat`), wahan aapki local debug key
  lagti hai (jaise `f1efa837745366852627fce5070b74c6dad8beb1`) — uska SHA-1 bhi usi jagah add karein.
- Email/password login in dono ke bina bhi chalta hai; SHA-1 sirf **Google** button ke liye chahiye.

## 11. Ek device par doosra account (jaan lena zaroori)

Sync merge natural keys par hota hai (product → `companyId + code`, party/company/category → naam). Agar ek hi device par
**doosre account** se login karein, to local Room data us naye account ke cloud me bhi merge ho jayega (delete kuch nahi hota).
Personal use (ek hi account) me ye normal hai; agar doosra account use karna ho to pehle Settings se data export/backup le lein.
