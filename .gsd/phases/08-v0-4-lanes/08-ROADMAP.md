# M008: v0.4 Lanes

**Vision:** Lanes are the product: user-defined buckets with colour, delivery policy, decay and breakthroughs. New senders are screened once and routed forever, non-Instant lanes are mirrored under Doorman's channels, batches deliver at set times or breakpoints, and everything removed is restorable from a searchable archive.

## Slices

- [ ] **S01: Lanes and Screener** `[sketch]` `risk:medium` `depends:[]`
  > After this: New sender triggers Screener; one tap routes it and re-files its history

- [ ] **S02: Mirroring and policies** `[sketch]` `risk:high` `depends:[S01]`
  > After this: A Quiet-lane notification appears as a Doorman mirror with lane colour, original icon and working open/reply

- [ ] **S03: Batching and delivery** `[sketch]` `risk:high` `depends:[S02]`
  > After this: Batched lane holds notifications and delivers a summary at 12:30 with Open / Deliver all / Keep holding

- [ ] **S04: Archive and memory** `[sketch]` `risk:low` `depends:[S02]`
  > After this: Search the archive, restore a batch, open a keyword page, read the daily digest

## Boundary Map

Not provided.
<!-- gsd:state-version=57:1 -->
