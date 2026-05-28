# Názov témy

**Savr Platform**

## Stručný popis témy

Savr Platform je informačný systém zameraný na znižovanie plytvania potravinami prostredníctvom evidencie, publikovania, vyhľadávania a objednávania prebytočných potravín z reštaurácií, kaviarní a obchodov. Registrovaní používatelia môžu vyhľadávať dostupné ponuky, objednať ich, sledovať stav svojich objednávok a po vyzdvihnutí ohodnotiť skúsenosť s prevádzkou. Používateľ môže zároveň vytvoriť a spravovať jednu alebo viac prevádzok, prostredníctvom ktorých publikuje ponuky potravín určených na vyzdvihnutie v definovanom čase a na definovanom mieste. Systém zabezpečuje správu životného cyklu ponúk a objednávok, notifikácie o dôležitých zmenách, analytiku prevádzky, support chat a AI asistenta pre prípravu ponuky z obrázka.

V aktuálne implementovanej verzii administrácia pokrýva najmä schvaľovanie čakajúcich prevádzok. Blokovanie používateľov ani zamietanie prevádzok nie sú súčasťou aktuálne exponovaného API a frontendového flow.

## Zoznam požiadaviek

| ID | Požiadavka |
|----|------------|
| RQ01 | Systém umožní registráciu používateľa do systému. |
| RQ02 | Systém umožní prihlásenie používateľa a výmenu autentizačného tokenu pre frontend. |
| RQ03 | Systém umožní používateľovi vytvoriť prevádzku (`Business`). |
| RQ04 | Systém umožní používateľovi zobraziť, upraviť a zmazať vlastné prevádzky. |
| RQ05 | Systém bude evidovať stav prevádzky a zaradi ju do schvaľovacieho procesu. |
| RQ06 | Systém umožní administrátorovi zobraziť čakajúce prevádzky a schváliť ich. |
| RQ07 | Systém umožní prevádzke nahrať ikonu prevádzky a verejne ju zobrazovať. |
| RQ08 | Systém umožní prevádzke vytvoriť a zverejniť ponuku prebytočných potravín. |
| RQ09 | Systém umožní prevádzke zobraziť, upraviť a odstrániť vlastnú ponuku. |
| RQ10 | Systém umožní prevádzke opätovne použiť existujúcu ponuku a generovať opakované výskyty ponúk. |
| RQ11 | Systém umožní prevádzke nahrať finálny obrázok ponuky a verejne ho zobrazovať. |
| RQ12 | Systém umožní používateľovi vyhľadávať a filtrovať dostupné ponuky v marketplace. |
| RQ13 | Systém umožní používateľovi zobraziť detail ponuky vrátane ceny, miesta a času vyzdvihnutia. |
| RQ14 | Systém umožní používateľovi vytvoriť objednávku z dostupnej ponuky. |
| RQ15 | Systém umožní používateľovi zobraziť svoje objednávky a pickup pass. |
| RQ16 | Systém umožní prevádzke alebo administrátorovi potvrdiť vyzdvihnutie objednávky pomocou pickup tokenu. |
| RQ17 | Systém umožní používateľovi po vyzdvihnutí ohodnotiť prevádzku. |
| RQ18 | Systém upozorní používateľa na dôležité zmeny prostredníctvom notifikácií a umožní ich čítať. |
| RQ19 | Systém umožní prevádzke zobraziť analytiku prevádzky a ponúk. |
| RQ20 | Systém umožní používateľovi komunikovať so support chat asistentom. |
| RQ21 | Systém umožní prevádzke vytvoriť draft ponuky z obrázka a vygenerovať ilustračný cover obrázok pomocou AI. |

---

## Slovník pojmov

| **Pojem** | **Anglický názov** | **Definícia** |
|-----------|--------------------|---------------|
| **Používateľ** | User | Registrovaný používateľ systému, ktorý môže vyhľadávať ponuky, vytvárať objednávky a zároveň vlastniť prevádzky. |
| **Prevádzka** | Business | Subjekt reprezentujúci reštauráciu, obchod alebo kaviareň. Patrí konkrétnemu používateľovi a slúži na publikovanie ponúk. |
| **Stav prevádzky** | BusinessStatus | Stav prevádzky (`PENDING`, `ACTIVE`, `BLOCKED`, `REJECTED`). V aktuálnom produkte je hlavným administrátorským flow schválenie `PENDING` prevádzky. |
| **Administrátor** | Administrator | Používateľ s oprávneniami na schvaľovanie prevádzok a vybrané administratívne zásahy v systéme. |
| **Rola** | UserRole | Typ oprávnenia (`USER`, `ADMIN`). |
| **Stav používateľa** | UserStatus | Stav používateľa (`ACTIVE`, `BLOCKED`, `DELETED`). Samostatný use case na blokovanie používateľa nie je súčasťou aktuálneho API/UI flow. |
| **Adresa** | Address | Hodnotový objekt reprezentujúci adresu prevádzky alebo miesta vyzdvihnutia. |
| **Marketplace** | Marketplace | Verejná časť systému určená na vyhľadávanie a prehliadanie dostupných ponúk. |
| **Ponuka** | Offer | Záznam o dostupných potravinách na objednanie. Obsahuje obchodné údaje, cenu, množstvo, alergény, miesto a čas vyzdvihnutia. |
| **Obrázok ponuky** | OfferImage | Verejne dostupný obrázok ponuky, nahratý používateľom alebo pripravený pomocou AI asistenta. |
| **Položka ponuky** | OfferItem | Jednotlivá položka ponuky. |
| **Stav ponuky** | OfferStatus | Stav ponuky v systéme. Nadobúda hodnoty `DRAFT`, `AVAILABLE`, `RESERVED`, `PICKED_UP`, `SOLD_OUT`, `EXPIRED`, `CANCELLED`. |
| **Objednávka** | Order | Záznam o objednaní ponuky používateľom. |
| **Stav objednávky** | OrderStatus | Stav objednávky v systéme. Nadobúda hodnoty `ACTIVE`, `CANCELLED`, `PICKED_UP`, `NO_SHOW`. |
| **Miesto vyzdvihnutia** | PickupLocation | Miesto vyzdvihnutia ponuky, ktoré môže byť odlišné od adresy prevádzky. |
| **Čas vyzdvihnutia** | PickupTimeWindow | Časový interval, v ktorom si môže používateľ objednávku prevziať. |
| **Pickup pass** | PickupPass | Potvrdenie rezervácie obsahujúce pickup token potrebný pri vyzdvihnutí. |
| **Potvrdenie vyzdvihnutia** | PickupConfirmation | Potvrdenie úspešného vyzdvihnutia objednávky. |
| **Hodnotenie** | Review | Spätná väzba používateľa na prevádzku po úspešnom vyzdvihnutí objednávky. |
| **Notifikácia** | Notification | Správa doručená používateľovi pri zmene stavu objednávky, ponuky alebo prevádzky. |
| **Support chat** | Support Chat | Konverzačný asistent poskytujúci odpovede a kontextové informácie o systéme a objednávkach. |
| **AI asistent ponuky** | Offer Assistant | Pomocný nástroj, ktorý z obrázka pripraví draft ponuky a vie vygenerovať ilustračný cover obrázok. |

---

## Zoznam prípadov použitia

- UC-01 Registrácia používateľa
- UC-02 Prihlásenie používateľa
- UC-03 Vytvorenie a správa prevádzky
- UC-04 Schvaľovanie prevádzky
- UC-05 Nahratie ikony prevádzky
- UC-06 Vytvorenie a správa ponuky
- UC-07 Opakovanie ponuky
- UC-08 Vyhľadávanie ponúk
- UC-09 Vytvorenie objednávky
- UC-10 Zobrazenie objednávok a pickup passu
- UC-11 Potvrdenie vyzdvihnutia
- UC-12 Hodnotenie prevádzky po vyzdvihnutí
- UC-13 Notifikácie
- UC-14 Analytika prevádzky
- UC-15 Support chat
- UC-16 AI draft ponuky z obrázka
- UC-17 Generovanie cover obrázka ponuky
- UC-18 Nahratie finálneho obrázka ponuky

---

## Detailne rozpracované prípady použitia

### UC-03-001 Vytvorenie prevádzky

#### Účel
Vytvoriť novú prevádzku, prostredníctvom ktorej bude môcť používateľ po schválení publikovať ponuky prebytočných potravín.

#### Používateľ
Používateľ.

#### Vstupné podmienky
Používateľ je prihlásený do systému a má aktívny účet.

#### Výstup
V systéme pribudla nová prevádzka priradená danému používateľovi. Prevádzka má stav `PENDING`.

#### Postup
1. Používateľ otvorí formulár na vytvorenie prevádzky.
2. Systém zobrazí formulár na zadanie údajov o prevádzke.
3. Používateľ zadá názov prevádzky, popis a adresu prevádzky.
4. Používateľ zvolí uloženie prevádzky.
5. Systém overí povinné údaje a správnosť zadaných hodnôt.
6. Systém vytvorí novú prevádzku, priradí ju prihlásenému používateľovi a nastaví jej stav `PENDING`.
7. Systém zobrazí potvrdenie o úspešnom vytvorení prevádzky.
8. Systém informuje používateľa, že prevádzka musí byť schválená administrátorom pred publikovaním ponúk.

#### Alternatívy

- `1a.` Používateľ nie je prihlásený do systému.
  - `1a1.` Systém nepovolí vytvorenie prevádzky.
  - `1a2.` Systém vyžiada prihlásenie používateľa.

- `5a.` Používateľ nevyplní povinný údaj.
  - `5a1.` Systém informuje používateľa o chýbajúcich povinných údajoch.
  - `5a2.` Systém nepovolí vytvorenie prevádzky.

- `5b.` Používateľ zadá neplatné údaje adresy.
  - `5b1.` Systém informuje používateľa o nesprávne zadaných údajoch adresy.
  - `5b2.` Systém vyžiada opravu údajov.

- `5c.` Používateľ už má prevádzku s rovnakým názvom.
  - `5c1.` Systém informuje používateľa, že prevádzka s daným názvom už existuje.
  - `5c2.` Systém nepovolí vytvorenie prevádzky.

---

### UC-06-001 Vytvorenie a zverejnenie ponuky

#### Účel
Vytvoriť a zverejniť novú ponuku prebytočných potravín patriacu do konkrétnej prevádzky.

#### Používateľ
Používateľ – vlastník prevádzky.

#### Vstupné podmienky
Používateľ je prihlásený do systému. Používateľ vlastní konkrétnu prevádzku a táto prevádzka má stav `ACTIVE`.

#### Výstup
V systéme pribudla nová ponuka v stave `AVAILABLE`, priradená ku konkrétnej prevádzke a viditeľná vo vyhľadávaní.

#### Postup
1. Používateľ otvorí detail svojej prevádzky.
2. Systém zobrazí údaje o prevádzke a dostupné akcie.
3. Používateľ zvolí možnosť vytvoriť novú ponuku.
4. Systém zobrazí formulár na zadanie údajov o ponuke.
5. Používateľ zadá názov ponuky, popis, cenu, množstvo, alergény, miesto vyzdvihnutia, časové okno vyzdvihnutia a prípadne obrázok ponuky.
6. Používateľ zvolí uloženie ponuky.
7. Systém overí povinné údaje, platnosť časového okna a správnosť zadaného miesta vyzdvihnutia.
8. Systém vytvorí novú ponuku v stave `AVAILABLE` a priradí ju danej prevádzke.
9. Systém zobrazí potvrdenie o úspešnom vytvorení ponuky.
10. Systém zaradí ponuku medzi dostupné ponuky vo vyhľadávaní.

#### Alternatívy

- `1a.` Používateľ sa pokúsi vytvoriť ponuku pre prevádzku, ktorú nevlastní.
  - `1a1.` Systém akciu zamietne z dôvodu nedostatočných oprávnení.

- `3a.` Prevádzka používateľa nie je v stave `ACTIVE`.
  - `3a1.` Systém nepovolí vytvorenie ponuky.
  - `3a2.` Systém informuje používateľa, že prevádzka ešte nebola schválená.

- `5a.` Používateľ nevyplní povinné údaje ponuky.
  - `5a1.` Systém vypíše validačné chyby.
  - `5a2.` Systém nepovolí uloženie ponuky.

- `7a.` Čas vyzdvihnutia je neplatný.
  - `7a1.` Systém informuje používateľa o neplatnom časovom intervale.
  - `7a2.` Systém vyžiada opravu údajov.

- `7b.` Miesto vyzdvihnutia nie je zadané správne.
  - `7b1.` Systém informuje používateľa o nesprávnych údajoch miesta vyzdvihnutia.
  - `7b2.` Systém vyžiada opravu údajov.

---

### UC-09-001 Vytvorenie objednávky

#### Účel
Vytvoriť objednávku dostupnej ponuky prebytočných potravín na vyzdvihnutie.

#### Používateľ
Používateľ.

#### Vstupné podmienky
Používateľ je prihlásený do systému. Vybraná ponuka existuje a je v stave `AVAILABLE`.

#### Výstup
V systéme pribudla nová objednávka v stave `ACTIVE`. Stav ponuky sa zmenil podľa zostávajúceho množstva. Používateľ získal pickup pass a obe strany boli informované o zmene stavu.

#### Postup
1. Používateľ vyhľadá dostupné ponuky.
2. Systém zobrazí zoznam dostupných ponúk.
3. Používateľ zvolí konkrétnu ponuku.
4. Systém zobrazí detail ponuky vrátane položiek, ceny, miesta vyzdvihnutia a času vyzdvihnutia.
5. Používateľ zvolí možnosť vytvoriť objednávku.
6. Systém overí, že ponuka je stále dostupná.
7. Systém vytvorí objednávku v stave `ACTIVE` a priradí ju danému používateľovi a danej ponuke.
8. Systém upraví dostupné množstvo ponuky a pripraví pickup pass s pickup tokenom.
9. Systém zobrazí používateľovi potvrdenie o úspešnom vytvorení objednávky.
10. Systém odošle notifikáciu používateľovi a vlastníkovi prevádzky.

#### Alternatívy

- `1a.` Používateľ nie je prihlásený do systému.
  - `1a1.` Systém nepovolí vytvorenie objednávky.
  - `1a2.` Systém vyžiada prihlásenie používateľa.

- `6a.` Ponuka už nie je dostupná.
  - `6a1.` Systém objednávku nevytvorí.
  - `6a2.` Systém zobrazí používateľovi informáciu o nedostupnosti ponuky.

- `8a.` Systém zistí konflikt stavu ponuky pri ukladaní objednávky.
  - `8a1.` Systém objednávku nevytvorí.
  - `8a2.` Systém obnoví detail ponuky a zobrazí aktuálny stav.

- `10a.` Notifikáciu vlastníkovi prevádzky nie je možné doručiť.
  - `10a1.` Systém uloží objednávku aj napriek tomu.
  - `10a2.` Systém zaznamená neúspešné doručenie notifikácie.

## UML Diagram

Diagram je dostupný v repozitári ako samostatný podklad.

## Obchodné pravidlá (Business Rules)

1. Používateľ sa musí prihlásiť do systému, aby mohol vytvárať prevádzky, ponuky a objednávky.
2. Používateľ môže vytvoriť a spravovať nula alebo viac prevádzok (`Business`).
3. Každá prevádzka patrí presne jednému používateľovi a nemôže existovať bez vlastníka.
4. Novovytvorená prevádzka má stav `PENDING` a musí byť schválená administrátorom, aby mohla publikovať ponuky.
5. Aktuálne exponovaný administrátorský flow pokrýva zobrazenie čakajúcich prevádzok a ich schválenie.
6. Ponuku je možné vytvoriť iba pre prevádzku v stave `ACTIVE`.
7. Ponuka musí obsahovať korektné obchodné údaje, miesto vyzdvihnutia a časové okno vyzdvihnutia.
8. Časové okno vyzdvihnutia musí spĺňať podmienku `from < to`.
9. Ponuku je možné objednať iba v stave `AVAILABLE`.
10. Jedna objednávka sa viaže presne na jednu ponuku a jedného používateľa.
11. Pri vytvorení objednávky vzniká pickup pass s pickup tokenom potrebným pri vyzdvihnutí.
12. Vyzdvihnutie objednávky je viazané na správny pickup token a vhodný stav objednávky.
13. Pri potvrdení vyzdvihnutia musí vzniknúť záznam `PickupConfirmation`.
14. Hodnotenie (`Review`) je možné vytvoriť až po úspešnom vyzdvihnutí objednávky a najviac raz pre jednu objednávku.
15. Zmena stavu ponuky, objednávky alebo prevádzky môže vyvolať vytvorenie notifikácie.
16. Prevádzka môže opätovne použiť existujúcu ponuku a pri zapnutom auto-repeat generovať ďalšie výskyty.
17. Obrázky prevádzok a ponúk sú uložené oddelene od jadra domény a publikované cez media storage adaptéry.
18. Support chat a AI asistent ponuky využívajú externého AI poskytovateľa, ale doménový flow je navrhnutý provider-agnosticky.
