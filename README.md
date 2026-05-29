# Názov témy

**Savr Platform**

## Stručný popis témy

Savr Platform je informačný systém zameraný na znižovanie plytvania potravinami prostredníctvom evidencie, publikovania, vyhľadávania a objednávania prebytočných potravín z reštaurácií, kaviarní a obchodov.

Používateľ vie:

- registrovať sa a prihlásiť do systému
- vyhľadávať dostupné ponuky v marketplace
- zobraziť detail ponuky a vytvoriť objednávku
- sledovať svoje objednávky, pickup pass a históriu vyzdvihnutí
- po úspešnom vyzdvihnutí ohodnotiť prevádzku
- založiť a spravovať jednu alebo viac prevádzok
- vytvárať a spravovať ponuky vrátane auto-repeat scenára
- komunikovať so support chat asistentom
- pripraviť draft ponuky a ilustračný cover obrázok pomocou AI

Administrátorský scope v aktuálne implementovanom produkte pokrýva najmä schvaľovanie čakajúcich prevádzok.

## Aktuálny implementovaný scope

- public marketplace browsing s filtrami, mapou a detailom ponuky
- customer flow pre cart, vytvorenie objednávky, pickup pass, potvrdenie pickup-u a review
- owner workspace pre správu prevádzok, ponúk, objednávok, analytiky a nastavení
- admin workflow pre zobrazenie pending prevádzok a ich schválenie
- support chat
- AI-assisted offer drafting a generovanie ilustračného cover obrázka

Nie je súčasťou aktuálne exponovaného UI/API flow:

- broad admin user management
- blokovanie používateľov ako samostatný admin use case
- reject/block business akcie vystavené vo frontend workflow

## Architektúra backendu

Backend je navrhnutý ako hexagonálna aplikácia s API-first workflow.

Java moduly sú rozdelené pod `application/`:

- `domain` - business pravidlá, doménový model, facade/service vrstva a repository porty
- `api-spec` - OpenAPI kontrakt a generované DTO/API rozhrania
- `inbound-controller-rest` - REST adaptér, security a mapovanie DTO
- `outbound-repository-jpa` - JPA persistence adaptéry
- `outbound-geocoding-nominatim` - geocoding adaptér
- `outbound-identity-keycloak` - identity provider adaptér
- `outbound-offer-media-filesystem` - storage adaptér pre obrázky
- `outbound-support-openai` a `outbound-support-stub` - AI support adaptéry
- `springboot` - runtime skladanie aplikácie, konfigurácia a architektonické testy

Kľúčové zásady:

- OpenAPI kontrakt v `application/api-spec/src/main/resources/openapi/food-rescue.yaml` je source of truth pre HTTP vrstvu
- controllery sú thin a delegujú na facade/service vrstvu
- business logika patrí do doménových modelov a doménových service tried
- repository porty žijú v `domain`, implementácie v outbound adaptéroch
- doména neobsahuje Spring, REST ani JPA závislosti

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
| RQ16 | Systém umožní prevádzke potvrdiť vyzdvihnutie objednávky pomocou pickup tokenu. |
| RQ17 | Systém umožní používateľovi po vyzdvihnutí ohodnotiť prevádzku. |
| RQ18 | Systém upozorní používateľa na dôležité zmeny prostredníctvom notifikácií a umožní ich čítať a mazať. |
| RQ19 | Systém umožní prevádzke zobraziť analytiku prevádzky a ponúk. |
| RQ20 | Systém umožní používateľovi komunikovať so support chat asistentom. |
| RQ21 | Systém umožní prevádzke vytvoriť draft ponuky z obrázka a vygenerovať ilustračný cover obrázok pomocou AI. |

## Slovník pojmov

| Pojem | Anglický názov | Definícia |
|-------|----------------|-----------|
| Používateľ | User | Registrovaný používateľ systému. Vie objednávať ponuky a zároveň môže vlastniť prevádzky. |
| Administrátor | Administrator | Používateľ s oprávnením schvaľovať čakajúce prevádzky. |
| Prevádzka | Business | Reštaurácia, kaviareň alebo obchod patriaci konkrétnemu používateľovi. |
| Stav prevádzky | BusinessStatus | Stav prevádzky (`PENDING`, `ACTIVE`, `BLOCKED`, `REJECTED`). |
| Ponuka | Offer | Záznam o dostupných potravinách na objednanie. |
| Stav ponuky | OfferStatus | Stav ponuky (`DRAFT`, `AVAILABLE`, `RESERVED`, `PICKED_UP`, `SOLD_OUT`, `EXPIRED`, `CANCELLED`). |
| Marketplace | Marketplace | Verejná časť systému určená na vyhľadávanie a prehliadanie ponúk. |
| Položka ponuky | OfferItem | Jednotlivá položka obsiahnutá v ponuke. |
| Miesto vyzdvihnutia | PickupLocation | Miesto, kde si používateľ prevezme objednávku. |
| Čas vyzdvihnutia | PickupTimeWindow | Časový interval, v ktorom je možné objednávku vyzdvihnúť. |
| Objednávka | Order | Záznam o kúpe ponuky používateľom. |
| Stav objednávky | OrderStatus | Stav objednávky (`ACTIVE`, `CANCELLED`, `PICKED_UP`, `NO_SHOW`). |
| Pickup pass | OrderPickupPass | Potvrdenie objednávky s pickup tokenom. |
| Platba objednávky | OrderPayment | Simulovaná platobná časť objednávky vrátane pickup tokenu a payout transferu. |
| Potvrdenie vyzdvihnutia | OrderPickupConfirmation | Záznam o potvrdení úspešného vyzdvihnutia objednávky. |
| Hodnotenie | Review | Spätná väzba používateľa na prevádzku po vyzdvihnutí objednávky. |
| Notifikácia | Notification | Správa doručená používateľovi pri dôležitých zmenách v systéme. |
| Support chat | Support Chat | Konverzačný asistent poskytujúci pomoc používateľovi. |
| AI asistent ponuky | Offer Assistant | Nástroj, ktorý pripraví draft ponuky a cover obrázok z obrázkového vstupu. |

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
3. Používateľ zadá názov, popis a adresu prevádzky.
4. Používateľ zvolí uloženie prevádzky.
5. Systém overí povinné údaje a unikátnosť názvu pre daného ownera.
6. Systém vytvorí novú prevádzku, priradí ju prihlásenému používateľovi a nastaví stav `PENDING`.
7. Systém zobrazí potvrdenie o úspešnom vytvorení prevádzky.

### UC-04-001 Schválenie prevádzky

#### Účel

Schváliť čakajúcu prevádzku tak, aby mohla publikovať ponuky v marketplace.

#### Používateľ

Administrátor.

#### Vstupné podmienky

Administrátor je prihlásený do systému. Prevádzka existuje a je v stave `PENDING`.

#### Výstup

Prevádzka zmenila stav na `ACTIVE` a môže publikovať ponuky.

#### Postup

1. Administrátor otvorí workspace pending prevádzok.
2. Systém zobrazí zoznam prevádzok v stave `PENDING`.
3. Administrátor vyberie konkrétnu prevádzku.
4. Systém zobrazí detail prevádzky.
5. Administrátor zvolí schválenie prevádzky.
6. Systém overí oprávnenie administrátora.
7. Systém zmení stav prevádzky na `ACTIVE`.
8. Systém uloží zmenu a zobrazí potvrdenie.

### UC-06-001 Vytvorenie a zverejnenie ponuky

#### Účel

Vytvoriť a zverejniť novú ponuku prebytočných potravín patriacu ku konkrétnej prevádzke.

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
5. Používateľ zadá názov, popis, cenu, množstvo, alergény, pickup location a pickup time window.
6. Používateľ zvolí uloženie ponuky.
7. Systém overí povinné údaje a platnosť časového okna.
8. Systém vytvorí novú ponuku a po splnení podmienok ju publikuje v stave `AVAILABLE`.
9. Systém zaradí ponuku medzi verejne dostupné ponuky v marketplace.

### UC-09-001 Vytvorenie objednávky

#### Účel

Vytvoriť objednávku dostupnej ponuky určenú na vyzdvihnutie.

#### Používateľ

Používateľ.

#### Vstupné podmienky

Používateľ je prihlásený do systému. Vybraná ponuka existuje, patrí aktívnej prevádzke a je v stave `AVAILABLE`.

#### Výstup

V systéme pribudla nová objednávka v stave `ACTIVE`, vznikol pickup pass a boli vytvorené notifikácie pre customer aj ownera.

#### Postup

1. Používateľ vyhľadá dostupné ponuky.
2. Systém zobrazí zoznam dostupných ponúk.
3. Používateľ otvorí detail konkrétnej ponuky.
4. Systém zobrazí cenu, položky, pickup location a pickup time window.
5. Používateľ zvolí vytvorenie objednávky.
6. Systém overí, že ponuka je stále dostupná a používateľ nekupuje vlastnú ponuku.
7. Systém vytvorí objednávku.
8. Systém vytvorí `OrderPayment` a pickup token.
9. Systém upraví zostávajúce množstvo ponuky.
10. Systém odošle notifikácie customerovi a ownerovi prevádzky.

## UML Diagram

V repozitári sú aktuálne dva UML podklady:

- `food-rescue-platform.puml` - čitateľný doménový overview
- `food-rescue-platform-detail.puml` - detailnejší technický pohľad vrátane service/facade vrstvy a portov

Odporúčané použitie:

- overview diagram na rýchle vysvetlenie domény
- detail diagram ako appendix pri otázkach ku kódu a architektúre

`diagram.png` nie je source of truth. Aktuálne sa udržiavajú `.puml` zdroje.

## Obchodné pravidlá

1. Používateľ sa musí prihlásiť, aby mohol vytvárať prevádzky, ponuky a objednávky.
2. Používateľ môže vlastniť nula alebo viac prevádzok.
3. Každá prevádzka patrí presne jednému ownerovi.
4. Novovytvorená prevádzka má stav `PENDING`.
5. Len administrátor môže schváliť čakajúcu prevádzku.
6. Ponuku je možné publikovať iba pre prevádzku v stave `ACTIVE`.
7. Verejný marketplace zobrazuje iba ponuky aktívnych prevádzok.
8. Offer detail pre public caller je dostupný iba pre verejne viditeľné stavy ponuky.
9. Ponuka musí obsahovať korektné obchodné údaje, pickup location a pickup time window.
10. Pickup time window musí spĺňať `from < to`.
11. Objednávku je možné vytvoriť iba z dostupnej ponuky aktívnej prevádzky.
12. Používateľ nemôže vytvoriť objednávku na vlastnú ponuku.
13. Pri vytvorení objednávky vzniká pickup token a simulated payment záznam.
14. Vyzdvihnutie objednávky je viazané na správny pickup token a vhodný stav objednávky.
15. Review je možné vytvoriť až po úspešnom vyzdvihnutí objednávky a iba raz pre jednu objednávku.
16. Dôležité zmeny stavu vytvárajú notifikácie pre relevantných používateľov.
17. Neaktívny alebo blokovaný používateľ nesmie používať protected business/order/notification flow.
18. Support chat a AI offer assistant využívajú externé porty, ale doménový flow ostáva provider-agnostic.
19. Obrázky prevádzok a ponúk sú ukladané mimo jadra domény cez storage adaptéry.
20. Auto-repeat ponuky generuje ďalší výskyt z existujúcej recurring šablóny.

## Spustenie

Databáza:

```bash
docker compose up -d db
```

Voliteľne Keycloak prostredie:

```bash
docker compose up -d my-keycloak
bash .scripts/keycloak/bootstrap-fsa-realm.sh
```

Backend aplikácia:

```bash
./mvnw -pl application/springboot spring-boot:run
```

alebo s Keycloak profilom:

```bash
./mvnw -pl application/springboot spring-boot:run -Dspring-boot.run.profiles=keycloak
```

## Testy

Doménové a modulové testy:

```bash
./mvnw test
```

Celý lokálny workflow:

```bash
./.scripts/test-all.sh
```

## Poznámka k scope

Tento backend je navrhnutý ako kvalitný end-to-end product slice pre akadémiu, nie ako maximalisticky široký informačný systém so všetkými možnými admin flow naraz.

Zámer projektu je ukázať:

- hexagonálnu architektúru
- OpenAPI-first workflow
- business logiku v doméne
- prepojenie backendu, frontendu a cloud nasadenia
