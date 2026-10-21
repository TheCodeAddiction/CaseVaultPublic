# CaseVault — Hvem kan gjøre hva

- Hver bruker har **én rolle** (`OFFICER` / `SUPERVISOR` / `ADMIN`) og **ett distrikt** (`NORTH` / `EAST` / `SOUTH` / `WEST`).
- ✅ tillatt · ⛔ ikke tillatt
- **Eget distrikt + delt** = saker i eget distrikt, saker delt med ditt distrikt, saker delt med deg personlig.
- Officers og supervisors har tilgang til de samme sakene — supervisor gir kun flere handlinger.
- **Uten historikk** = kontoen har ingen tildelte saker, dokumenter, delinger, meldinger eller oppføringer i aktivitetsloggen.

## Før innlogging

| Handling | Alle |
|---|---|
| Registrere konto | ✅ blir alltid en `OFFICER`, i distriktet som velges ved registrering |
| Tilbakestille glemt passord | ✅ kode sendes til kontoens e-postadresse |

## Saker

| Handling | OFFICER | SUPERVISOR | ADMIN |
|---|---|---|---|
| Liste / søke / filtrere / eksportere saker | Eget distrikt + delt | Eget distrikt + delt | Alle distrikter |
| Statistikk på dashbordet | Eget distrikt + delt | Eget distrikt + delt | Alle distrikter |
| Åpne en sak + aktivitetsloggen | Eget distrikt + delt | Eget distrikt + delt | Alle distrikter |
| Opprette sak | ✅ eget distrikt, tildeles oppretteren | ✅ samme | ✅ samme |
| Endre saksstatus (lukke / gjenåpne) | Eget distrikt + delt | Eget distrikt + delt | Alle distrikter |
| Tildele en sak til en `OFFICER` i sakens distrikt | ⛔ | Eget distrikt + delt | Alle distrikter |
| Dele sak / trekke tilbake deling | ⛔ | Kun saker i eget distrikt | ✅ alle saker |
| Se hvem en sak er delt med | Eget distrikt + delt | Eget distrikt + delt | Alle distrikter |
| Admin-saksliste (alle saker, alle distrikter) | ⛔ | ⛔ | ✅ |
| **Slette en sak** | ⛔ | ⛔ | ⛔ |

## Saksdokumenter (kommentarer, rapporter, filer)

| Handling | OFFICER | SUPERVISOR | ADMIN |
|---|---|---|---|
| Lese dokumentloggen | Eget distrikt + delt | Eget distrikt + delt | Alle distrikter |
| Legge til kommentar / rapport / fil | Eget distrikt + delt | Eget distrikt + delt | Alle distrikter |
| Laste ned vedlagt fil | Eget distrikt + delt | Eget distrikt + delt | Alle distrikter |
| Slette et dokument | Kun egne dokumenter | Egne dokumenter + alle på saker i eget distrikt | Alle dokumenter |

## Rapportering

| Handling | OFFICER | SUPERVISOR | ADMIN |
|---|---|---|---|
| Distriktsrapport: statustelling + arbeidsmengde per officer | ⛔ | Kun eget distrikt | Alle distrikter |

## Kontoer

| Handling | OFFICER | SUPERVISOR | ADMIN |
|---|---|---|---|
| Endre eget navn / e-post / passord / profilbilde | ✅ | ✅ | ✅ |
| Endre egen rolle eller eget distrikt fra profilsiden | ⛔ | ⛔ | ⛔ |
| Slette egen konto | ✅ passord + uten historikk | ✅ samme | ✅ samme |
| Se profilbildet til en annen bruker | ✅ | ✅ | ✅ |
| Liste brukere | ⛔ | Officers i eget distrikt | Alle |
| Opprette bruker med valgt rolle / distrikt | ⛔ | ⛔ | ✅ |
| Endre rolle / distrikt på en annen bruker | ⛔ | ⛔ | ✅ alle brukere, også seg selv |
| Deaktivere / reaktivere konto | ⛔ | ⛔ | ✅ alle brukere, også seg selv |
| Slette kontoen til en annen bruker | ⛔ | ⛔ | ✅ kun uten historikk |
| Se aktivitetsloggen til en bruker | ⛔ | ⛔ | ✅ |
| Logge inn med deaktivert konto | ⛔ | ⛔ | ⛔ |

## Meldinger

| Handling | Alle roller |
|---|---|
| Søke etter brukere, starte samtale, sende melding | ✅ enhver annen bruker, alle distrikter, alle roller (søket viser kun aktive brukere) |
| Lese samtale, svare, markere som lest, laste ned vedlegg | Kun deltakere (avsender + mottaker) |
| Koble en melding til en sak | Kun en sak **avsenderen** selv har tilgang til — mottakeren ser saksnummer + tittel, men får ikke tilgang til saken |
