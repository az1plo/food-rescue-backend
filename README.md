# Názov témy

**Food Rescue Platform**

## Stručný popis témy

Food Rescue Platform je informačný systém zameraný na znižovanie plytvania potravinami prostredníctvom evidencie, publikovania, vyhľadávania a rezervácie prebytočných potravín z reštaurácií, kaviarní a obchodov. Prevádzky môžu vytvárať ponuky potravín určených na vyzdvihnutie v definovanom čase a na definovanom mieste. Registrovaní používatelia môžu tieto ponuky vyhľadávať, rezervovať a sledovať stav svojich rezervácií. Systém zároveň zabezpečuje správu životného cyklu ponúk a rezervácií, notifikácie o dôležitých zmenách a základnú administráciu používateľov a obsahu.

## Zoznam požiadaviek (Use Cases / Requirements)

| ID | Požiadavka |
|----|------------|
| RQ01 | Systém umožní registráciu používateľa alebo prevádzky do systému. |
| RQ02 | Systém umožní prihlásenie používateľa do systému. |
| RQ03 | Systém umožní prevádzke vytvoriť a zverejniť ponuku prebytočných potravín. |
| RQ04 | Systém umožní prevádzke upraviť vlastnú ponuku. |
| RQ05 | Systém umožní prevádzke zrušiť vlastnú ponuku. |
| RQ06 | Systém umožní používateľovi vyhľadávať dostupné ponuky podľa zvolených kritérií. |
| RQ07 | Systém umožní používateľovi rezervovať dostupnú ponuku. |
| RQ08 | Systém umožní používateľovi zrušiť aktívnu rezerváciu pred koncom času vyzdvihnutia. |
| RQ09 | Systém bude evidovať miesto a čas vyzdvihnutia ku každej ponuke. |
| RQ10 | Systém umožní prevádzke potvrdiť vyzdvihnutie rezervovanej ponuky. |
| RQ11 | Systém umožní používateľovi zobraziť históriu svojich rezervácií. |
| RQ12 | Systém upozorní používateľa na zmenu stavu rezervácie alebo ponuky. |
| RQ13 | Systém umožní prevádzke označiť ponuku ako vypredanú alebo nedostupnú pred koncom časového okna. |
| RQ14 | Systém umožní administrátorovi spravovať používateľov. |
| RQ15 | Systém umožní administrátorovi spravovať ponuky. |
| RQ16 | Systém umožní administrátorovi zablokovať konto používateľa pri porušení pravidiel. |

## Slovník pojmov

| **Pojem** | **Anglický názov** | **Definícia** |
|-----------|--------------------|---------------|
| **Používateľ** | User | Registrovaný používateľ systému, ktorý môže vyhľadávať a rezervovať ponuky prebytočných potravín. |
| **Prevádzka** | Seller | Registrovaný subjekt (reštaurácia, obchod, kaviareň), ktorý publikuje ponuky prebytočných potravín. |
| **Administrátor** | Administrator | Používateľ s rozšírenými oprávneniami na správu používateľov, ponúk a dohľad nad systémom. |
| **Konto** | Account | Identita používateľa v systéme obsahujúca prihlasovacie údaje, profilové údaje a stav konta. |
| **Rola** | Role | Typ oprávnenia používateľa v systéme. Nadobúda hodnoty `USER`, `SELLER`, `ADMIN`. |
| **Stav konta** | AccountStatus | Stav konta v systéme. Nadobúda hodnoty napr. `ACTIVE`, `BLOCKED`, `DELETED`. |
| **Ponuka** | Offer | Záznam o dostupných prebytočných potravinách určených na rezerváciu a vyzdvihnutie. |
| **Položka ponuky** | OfferItem | Jednotlivá položka tvoriaca obsah ponuky, napr. typ jedla alebo balíčka a jeho množstvo. |
| **Stav ponuky** | OfferStatus | Stav ponuky v systéme. Nadobúda hodnoty `DRAFT`, `AVAILABLE`, `RESERVED`, `PICKED_UP`, `SOLD_OUT`, `EXPIRED`, `CANCELLED`. |
| **Rezervácia** | Reservation | Záznam o rezervovaní konkrétnej ponuky konkrétnym používateľom. |
| **Stav rezervácie** | ReservationStatus | Stav rezervácie v systéme. Nadobúda hodnoty `ACTIVE`, `CANCELLED`, `PICKED_UP`, `NO_SHOW`. |
| **Miesto vyzdvihnutia** | PickupLocation | Miesto alebo adresa, na ktorej si používateľ rezervovanú ponuku vyzdvihne. |
| **Čas vyzdvihnutia** | PickupTimeWindow | Časový interval, počas ktorého je možné ponuku vyzdvihnúť. |
| **Potvrdenie vyzdvihnutia** | PickupConfirmation | Záznam o tom, že prevádzka potvrdila úspešné prevzatie rezervovanej ponuky používateľom. |
| **Notifikácia** | Notification | Správa doručená používateľovi pri významnej zmene stavu ponuky alebo rezervácie. |

## Zoznam prípadov použitia

- **UC-01-001** Registrácia používateľa
- **UC-02-001** Prihlásenie používateľa
- **UC-03-001** Vytvorenie a zverejnenie ponuky
- **UC-04-001** Úprava vlastnej ponuky
- **UC-05-001** Zrušenie vlastnej ponuky
- **UC-06-001** Vyhľadávanie dostupných ponúk
- **UC-07-001** Rezervácia ponuky
- **UC-08-001** Zrušenie rezervácie
- **UC-09-001** Potvrdenie vyzdvihnutia
- **UC-10-001** Zobrazenie histórie rezervácií
- **UC-11-001** Notifikácia o zmene stavu
- **UC-12-001** Označenie ponuky ako vypredanej
- **UC-13-001** Správa používateľov administrátorom
- **UC-14-001** Správa ponúk administrátorom
- **UC-15-001** Zablokovanie konta používateľa

## Prípady použitia

### UC-01-001 Registrácia používateľa

#### Účel
Založiť nové konto v systéme pre používateľa alebo prevádzku.

#### Používateľ
Návštevník systému.

#### Vstupné podmienky
Používateľ nie je prihlásený a ešte nemá vytvorené konto s daným e-mailom.

#### Výstup
V systéme pribudlo nové konto s rolou `USER` alebo `SELLER` a stavom `ACTIVE`.

#### Postup
1. Používateľ otvorí registračný formulár.
2. Používateľ zadá meno, e-mail, heslo a zvolí rolu.
3. Systém overí povinné údaje, formát e-mailu a silu hesla.
4. Systém overí, či konto s daným e-mailom ešte neexistuje.
5. Systém vytvorí nové konto a uloží čas registrácie.
6. Systém zobrazí potvrdenie o úspešnej registrácii.

#### Alternatívy
**3a. Používateľ nevyplní povinné údaje alebo zadá neplatný formát.**  
3a1. Systém zvýrazní chybné polia a nedovolí odoslať formulár.

**4a. Konto s daným e-mailom už existuje.**  
4a1. Systém registráciu zamietne a zobrazí správu: „Účet s týmto e-mailom už existuje.“

### UC-03-001 Vytvorenie a zverejnenie ponuky

#### Účel
Umožniť prevádzke publikovať ponuku prebytočných potravín na rezerváciu.

#### Používateľ
Prevádzka (`SELLER`).

#### Vstupné podmienky
Používateľ je prihlásený a má rolu `SELLER`.

#### Výstup
V systéme pribudla nová ponuka v stave `AVAILABLE`, viditeľná vo vyhľadávaní.

#### Postup
1. Prevádzka otvorí formulár „Nová ponuka“.
2. Prevádzka zadá názov ponuky, popis a položky ponuky.
3. Prevádzka zadá miesto vyzdvihnutia a časové okno vyzdvihnutia.
4. Systém overí povinné údaje a platnosť časového okna.
5. Systém uloží ponuku so stavom `AVAILABLE`.
6. Systém zobrazí ponuku medzi dostupnými ponukami.

#### Alternatívy
**2a. Prevádzka nevyplní povinné údaje.**  
2a1. Systém vypíše validačné chyby a uloženie nepovolí.

**4a. Čas vyzdvihnutia je neplatný.**  
4a1. Systém upozorní na chybný interval a vyžiada opravu.

**4b. Používateľ nemá rolu `SELLER`.**  
4b1. Systém akciu zamietne z dôvodu nedostatočných oprávnení.

### UC-07-001 Rezervácia ponuky

#### Účel
Umožniť používateľovi rezervovať dostupnú ponuku.

#### Používateľ
Používateľ (`USER`).

#### Vstupné podmienky
Používateľ je prihlásený. Ponuka existuje a je v stave `AVAILABLE`.

#### Výstup
V systéme vznikla nová rezervácia v stave `ACTIVE` a ponuka prešla do stavu `RESERVED`.

#### Postup
1. Používateľ otvorí detail ponuky.
2. Systém zobrazí detaily ponuky vrátane miesta a času vyzdvihnutia.
3. Používateľ zvolí možnosť „Rezervovať“.
4. Systém overí, že ponuka je stále dostupná.
5. Systém vytvorí rezerváciu v stave `ACTIVE`.
6. Systém aktualizuje stav ponuky na `RESERVED`.
7. Systém zobrazí potvrdenie o úspešnej rezervácii.
8. Systém odošle notifikáciu používateľovi a prevádzke.

#### Alternatívy

**1a. Používateľ nie je prihlásený.**  
1a1. Systém vyžiada prihlásenie používateľa.

**4a. Ponuka už nie je dostupná.**  
4a1. Systém rezerváciu nevytvorí a zobrazí správu o nedostupnosti ponuky.

### UC-08-001 Zrušenie rezervácie

#### Účel
Umožniť používateľovi zrušiť aktívnu rezerváciu pred koncom časového okna vyzdvihnutia.

#### Používateľ
Používateľ (`USER`).

#### Vstupné podmienky
Používateľ je prihlásený a vlastní rezerváciu v stave `ACTIVE`.

#### Výstup
Rezervácia je v stave `CANCELLED` a ponuka je opäť dostupná, ak to obchodné pravidlá umožňujú.

#### Postup
1. Používateľ otvorí zoznam svojich rezervácií.
2. Používateľ vyberie aktívnu rezerváciu.
3. Používateľ zvolí možnosť „Zrušiť rezerváciu“.
4. Systém overí, či ešte neuplynul koniec času vyzdvihnutia.
5. Systém nastaví stav rezervácie na `CANCELLED`.
6. Systém aktualizuje stav ponuky na `AVAILABLE`, ak ponuka nebola medzičasom ukončená alebo zrušená.
7. Systém odošle notifikáciu prevádzke a používateľovi.

#### Alternatívy

**2a. Používateľ sa pokúsi zrušiť cudziu rezerváciu.**  
2a1. Systém akciu zamietne z dôvodu nedostatočných oprávnení.

**4a. Čas vyzdvihnutia už uplynul.**  
4a1. Systém zrušenie rezervácie nepovolí.

### UC-09-001 Potvrdenie vyzdvihnutia

#### Účel
Umožniť prevádzke potvrdiť odovzdanie rezervovanej ponuky používateľovi.

#### Používateľ
Prevádzka (`SELLER`).

#### Vstupné podmienky
Existuje rezervácia v stave `ACTIVE`, viazaná na ponuku patriacu danej prevádzke.

#### Výstup
Rezervácia prejde do stavu `PICKED_UP`, ponuka prejde do stavu `PICKED_UP` a vznikne záznam o potvrdení vyzdvihnutia.

#### Postup
1. Prevádzka otvorí detail rezervácie alebo detail ponuky.
2. Systém zobrazí údaje o rezervácii a používateľovi.
3. Prevádzka zvolí možnosť „Potvrdiť vyzdvihnutie“.
4. Systém overí, že rezervácia je v stave `ACTIVE`.
5. Systém vytvorí záznam `PickupConfirmation`.
6. Systém nastaví stav rezervácie na `PICKED_UP`.
7. Systém nastaví stav ponuky na `PICKED_UP`.
8. Systém odošle notifikáciu používateľovi.

#### Alternatívy
**1a. Prevádzka sa pokúsi potvrdiť cudziu rezerváciu.**  
1a1. Systém akciu zamietne z dôvodu nedostatočných oprávnení.

**4a. Rezervácia už nie je aktívna.**  
4a1. Systém potvrdenie nepovolí.

### UC-15-001 Zablokovanie konta používateľa

#### Účel
Umožniť administrátorovi zablokovať konto používateľa pri porušení pravidiel.

#### Používateľ
Administrátor (`ADMIN`).

#### Vstupné podmienky
Administrátor je prihlásený. Konto používateľa existuje a nie je už blokované.

#### Výstup
Konto používateľa má stav `BLOCKED`.

#### Postup
1. Administrátor otvorí zoznam používateľov.
2. Administrátor vyberie konkrétne konto.
3. Administrátor zvolí možnosť „Zablokovať konto“.
4. Systém vyžiada dôvod blokácie.
5. Administrátor potvrdí akciu.
6. Systém zmení stav konta na `BLOCKED`.
7. Systém uloží informáciu o dôvode a čase blokácie.
8. Systém môže odoslať používateľovi notifikáciu o blokácii.

#### Alternatívy

**1a. Používateľ nemá administrátorské oprávnenia.**  
1a1. Systém akciu zamietne.

**3a. Konto je už zablokované.**  
3a1. Systém akciu nevykoná a zobrazí informáciu o aktuálnom stave.

## Obchodné pravidlá (Business Rules)

1. Ponuku je možné rezervovať iba v stave `AVAILABLE`.
2. Jedna rezervácia sa viaže práve na jednu ponuku a jedného používateľa.
3. Stav ponuky `RESERVED` znamená, že ponuka už nie je dostupná na ďalšiu rezerváciu.
4. Rezerváciu je možné zrušiť iba vtedy, ak ešte neuplynul koniec `PickupTimeWindow`.
5. Ak používateľ rezervovanú ponuku neprevezme do konca `PickupTimeWindow`, systém môže nastaviť stav rezervácie na `NO_SHOW`.
6. Prevádzka môže upravovať alebo rušiť iba vlastné ponuky.
7. Administrátor môže spravovať všetky kontá a všetky ponuky.
8. Pri potvrdení vyzdvihnutia musí vzniknúť záznam `PickupConfirmation`.
9. `PickupTimeWindow` musí spĺňať podmienku `from < to`.
10. Ponuku so stavom `CANCELLED`, `EXPIRED`, `SOLD_OUT` alebo `PICKED_UP` nie je možné rezervovať.
11. Blokované konto (`BLOCKED`) sa nemôže prihlásiť do systému ani vykonávať nové akcie.
12. Zmena stavu ponuky alebo rezervácie môže vyvolať vytvorenie notifikácie pre dotknutého používateľa.