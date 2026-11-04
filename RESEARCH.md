# Vanilla brewing and target API checks

Verified on 2026-11-04 for Minecraft/Paper **1.21.11**. This note records
primary-source checks, not a production-server smoke test.

## Sources and method

- [Mojang's exact 1.21.11 version metadata](https://piston-meta.mojang.com/v1/packages/35f8a9bc4874e2c95e51b3ad21e043a33776f7c2/1.21.11.json)
  identifies the official server and mappings downloads.
- [Official server jar](https://piston-data.mojang.com/v1/objects/64bb6d763bed0a9f1d632ec347938594144943ed/server.jar),
  SHA-1 `64bb6d763bed0a9f1d632ec347938594144943ed`.
- [Official server mappings](https://piston-data.mojang.com/v1/objects/5621e9253f05fd57872bbe7f8ddf5f9a7d525955/server.txt),
  SHA-1 `5621e9253f05fd57872bbe7f8ddf5f9a7d525955`.
- [Paper 1.21.11 PotionType API](https://jd.papermc.io/paper/1.21.11/org/bukkit/potion/PotionType.html).
- [Paper 1.21.11 EntityScheduler API](https://jd.papermc.io/paper/1.21.11/io/papermc/paper/threadedregions/scheduler/EntityScheduler.html).

The Mojang downloads were fetched into a task-specific temporary directory,
their hashes verified, and the inner `server-1.21.11.jar` inspected with
`javap -c -p`. Official mappings identify `PotionBrewing` as `dnq`,
`PotionBrewing.Builder` as `dnq$a`, `BrewingStandBlockEntity` as `elf`,
`Items` as `dlx`, and `Potions` as `dns`. The server was never launched.

## Effect recipes and names

`PotionBrewing.addVanillaMixes` (mapped source lines 138–223) contains the
following effect paths. All start with water plus nether wart, then the listed
ingredient, **except weakness**, which is directly water plus fermented spider
eye. Corrupted effects add fermented spider eye after the precursor effect.
Source: the official server/mappings above, `dnq.a(dnq$a)`.

| Effect | Main path after awkward | Extension | Enhancement |
| --- | --- | --- | --- |
| Night vision | Golden carrot | Redstone | None |
| Invisibility | Golden carrot, fermented spider eye | Redstone | None |
| Leaping | Rabbit foot | Redstone | Glowstone dust |
| Fire resistance | Magma cream | Redstone | None |
| Swiftness | Sugar | Redstone | Glowstone dust |
| Slowness | Sugar, fermented spider eye | Redstone | Glowstone dust |
| Turtle master | Turtle helmet | Redstone | Glowstone dust |
| Water breathing | Pufferfish | Redstone | None |
| Healing | Glistering melon slice | None | Glowstone dust |
| Harming | Glistering melon slice, fermented spider eye | None | Glowstone dust |
| Poison | Spider eye | Redstone | Glowstone dust |
| Regeneration | Ghast tear | Redstone | Glowstone dust |
| Strength | Blaze powder | Redstone | Glowstone dust |
| Weakness | **No awkward:** fermented spider eye directly into water | Redstone | None |
| Slow falling | Phantom membrane | Redstone | None |
| Wind charged | Breeze rod | None | None |
| Weaving | Cobweb | None | None |
| Oozing | Slime block | None | None |
| Infested | Stone | None | None |

`Builder.addStartMix` registers the awkward effect path plus an unrelated
water-to-mundane recipe; the latter must not be mistaken for brewing the effect
without nether wart. Source: official `dnq$a.a(Item, Holder)`, mapped lines
267–271.

Specific edge cases, confirmed in the same registration bytecode:

- `SLOWNESS + GLOWSTONE_DUST -> STRONG_SLOWNESS` exists. Brewing sugar,
  fermented spider eye, then glowstone after awkward is valid; do not invent
  a `SLOWNESS_IV` enum.
- `HARMING + GLOWSTONE_DUST -> STRONG_HARMING` exists. Enhancing healing first
  and then corrupting it is also valid, as are poison-based harming paths.
- `INVISIBILITY + REDSTONE -> LONG_INVISIBILITY` and
  `SLOWNESS + REDSTONE -> LONG_SLOWNESS` exist, so extending after corruption
  is valid.
- Wind charged uses `BREEZE_ROD`, **not** `WIND_CHARGE`; oozing uses
  `SLIME_BLOCK`, **not** `SLIME_BALL`; infested uses `STONE`, not cobblestone.
- `TURTLE_HELMET` and `GLISTERING_MELON_SLICE` are the actual ingredient names.

Paper's target enum contains 46 values. Removing the four non-effect bases
`WATER`, `AWKWARD`, `MUNDANE`, `THICK`, and non-survival-brewable `LUCK` leaves
**41 effect variants across 19 base effects**. The recipes above account for
all 41: 19 base, 13 long, 9 strong. The valid enhancement constants are
`STRONG_LEAPING`, `STRONG_SWIFTNESS`, `STRONG_SLOWNESS`,
`STRONG_TURTLE_MASTER`, `STRONG_HEALING`, `STRONG_HARMING`,
`STRONG_POISON`, `STRONG_REGENERATION`, and `STRONG_STRENGTH`.
Sources: Paper's target enum and Mojang's recipe registration above.

## Batch economics, fuel, and remainder

In `BrewingStandBlockEntity.doBrew` (mapped lines 188–208), the implementation
loops bottle slots **0, 1, 2**, applies the mix to each, then shrinks the
ingredient stack by **one**. Therefore each ingredient operation costs one
ingredient for the selected batch of one, two, or three bottles, not one
ingredient per bottle. Source: official `elf.a(Level, BlockPos, NonNullList)`.

`PotionBrewing.addVanillaMixes` explicitly registers:

- Normal potion + **one gunpowder operation** -> splash potion.
- Splash potion + **one dragon's breath operation** -> lingering potion.

Thus lingering requires both modifiers, once each per selected batch.
Source: official `dnq.a(dnq$a)`.

`BrewingStandBlockEntity.serverTick` (mapped lines 111–151) consumes one fuel
item when empty, sets fuel to **20**, and subtracts one fuel use when each
brewing operation starts. The jar's
`data/minecraft/tags/item/brewing_fuel.json` contains only
`minecraft:blaze_powder`. Recipe ingredients and splash/lingering modifiers
each represent one such operation. Strength's ingredient blaze powder is
additional to fuel powder. Sources: official `elf.a(Level, BlockPos,
BlockState, BrewingStandBlockEntity)` and bundled vanilla item tag.

Preserving unused fuel as per-player persistent credits is a GUI design
decision, not vanilla stand behavior. It preserves the same 20-operations-per-
powder material economics across GUI batches while avoiding unnecessary
per-batch overcharging. Credits and inventory must be updated within the same
entity-scheduler transaction, and neither changes on a failed transaction.

`Items.DRAGON_BREATH` explicitly uses
`Item.Properties.craftRemainder(Items.GLASS_BOTTLE)`.
`doBrew` obtains that remainder once after consuming the batch's ingredient.
Therefore **one lingering batch returns exactly one empty glass bottle**,
regardless of selected output count. Sources: official `dlx` static initializer
at the `dragon_breath` registration, `Item.Properties.craftRemainder`
(`dlp$a.b(Item)`), and `elf.doBrew`.

## Folia/API implications

Paper's target `EntityScheduler` is explicitly thread-safe to schedule from
foreign threads and executes work on the region owning the entity, including
when it moves. `execute` returns `false` if already retired; in that case
neither callback runs. For accepted work, exactly one of the run/retired
callbacks runs. Retired callbacks execute in critical code and must not
perform broad world/entity operations. Source: the linked target
`EntityScheduler` Javadoc.

Consequences for this plugin:

- Form callbacks may safely enqueue work through `player.getScheduler()`;
  inventory, persistent player data, and other player/world accesses belong
  inside that entity-owned work.
- Handle both scheduler refusal and retirement when clearing per-player form
  or transaction guards; otherwise a disconnect can strand a session.
- An entity scheduler is not authority to mutate an arbitrary remote brewing
  stand's block state. This GUI can transact against the player's inventory
  without sharing vanilla stand inventory/fuel.
- A detached inventory placement plan can account for slots freed by consumed
  water/ingredients and for the returned glass bottle before committing. If
  the entire result cannot fit, rejecting with no consumption avoids reliance
  on world item drops or cancellable item-spawn events.

These implementation consequences are deductions from the cited API contract
and verified vanilla costs, not claims that a live Bedrock/Folia integration
test has been performed.
