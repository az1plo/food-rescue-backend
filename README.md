# Názov témy

Food Rescue Platform

## Stručný popis témy

Food Rescue Platform je systém zameraný na znižovanie plytvania potravinami prostredníctvom evidencie, publikovania a rezervácie prebytočných potravín z reštaurácií a obchodov. Systém umožňuje registráciu a správu používateľov, publikovanie ponúk, ich vyhľadávanie a rezerváciu na vyzdvihnutie. Súčasťou riešenia je aj základná administrácia systému a evidencia stavu ponúk a rezervácií. Cieľom aplikácie je efektívnejšie využitie nepredaných potravín a podpora udržateľnej spotreby.

## Zoznam požiadaviek (Use Cases)

| ID | Požiadavka |
|----|------------|
| RQ01 | Systém umožní registráciu používateľa do systému. |
| RQ02 | Systém umožní prihlásenie používateľa do systému. |
| RQ03 | Systém umožní prevádzke vytvoriť a zverejniť ponuku prebytočných potravín. |
| RQ04 | Systém umožní prevádzke upraviť alebo zrušiť vlastnú ponuku. |
| RQ05 | Systém umožní používateľovi vyhľadávať dostupné ponuky. |
| RQ06 | Systém umožní používateľovi rezervovať dostupnú ponuku. |
| RQ07 | Systém umožní používateľovi zrušiť aktívnu rezerváciu. |
| RQ08 | Systém bude evidovať miesto a čas vyzdvihnutia ku každej ponuke. |
| RQ09 | Systém umožní potvrdiť vyzdvihnutie rezervovanej ponuky. |
| RQ10 | Systém umožní administrátorovi spravovať používateľov a ponuky. |

## Slovník pojmov

| **Pojem** | **Anglický názov** | **Definícia** |
|------------------------|--------------------|-----------------------------------------------------------------------------------------------------------------------|
| **Používateľ** | **User** | Registrovaný a prihlásený používateľ, ktorý môže vyhľadávať a rezervovať ponuky prebytočných potravín. |
| **Prevádzka** | **Seller** | Reštaurácia alebo obchod, ktorý zverejňuje ponuky prebytočných potravín určených na vyzdvihnutie. |
| **Administrátor** | **Administrator** | Používateľ s oprávneniami na správu obsahu platformy a dohľad nad fungovaním systému. |
| **Konto** | **Account** | Identita používateľa v systéme, ktorá obsahuje prihlasovacie a základné profilové údaje. |
| **Rola** | **Role** | Oprávnenie používateľa v systéme. Môže byť napríklad `USER`, `SELLER` alebo `ADMIN`. |
| **Ponuka** | **Offer** | Záznam o dostupných potravinách určených na rezerváciu a následné vyzdvihnutie. |
| **Rezervácia** | **Reservation** | Zablokovanie konkrétnej ponuky pre konkrétneho používateľa. |
| **OfferStatus** | **OfferStatus** | Stav ponuky v systéme. Môže byť napríklad `AVAILABLE`, `RESERVED`, `PICKED_UP`, `EXPIRED` alebo `CANCELLED`. |
| **ReservationStatus** | **ReservationStatus** | Stav rezervácie v systéme. Môže byť napríklad `ACTIVE`, `CANCELLED`, `PICKED_UP` alebo `NO_SHOW`. |
| **Miesto vyzdvihnutia** | **PickupLocation** | Miesto alebo adresa, kde si používateľ rezervovanú ponuku vyzdvihne. |
| **Čas vyzdvihnutia** | **PickupTimeWindow** | Časový interval, počas ktorého je možné rezervovanú ponuku vyzdvihnúť. |
| **Potvrdenie vyzdvihnutia** | **PickupConfirmation** | Záznam o tom, že rezervovaná ponuka bola úspešne prevzatá používateľom. |
