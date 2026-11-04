# TASK: Finish and harden BedrockBrewGUI

You are finishing an in-progress Java Minecraft plugin at this repository. Read every source/test/resource file first.

## Product intent
Bedrock players on a Canvas/Folia 1.21.11 server cannot reliably use the vanilla brewing-stand protocol. Provide an intuitive native Bedrock Forms GUI that bypasses that container protocol while preserving vanilla survival costs.

## Required UX
- Bedrock player right-clicking a brewing stand opens the GUI instead of the vanilla container. `/brew` is an alternate entry.
- Java players remain untouched and get the vanilla brewing stand.
- Mobile-first native Forms: short pages, large buttons, no inventory-GUI layout. Flow: category -> base effect -> variant -> bottle type + 1..3 count -> confirmation -> success or exact missing-material page. Back navigation works.
- Traditional Chinese, concise labels. Never present a page with dozens of potion buttons.
- Supported output kinds: normal, splash, lingering.

## Correctness requirements
- All currently survival-brewable vanilla potion effects/variants in target API, no invented PotionType constants. Verify every PotionType name against the actual dependency/API by compiling.
- Preserve vanilla batch economics: 1 ingredient processes up to three water bottles in one brewing operation; splash/lingering modifiers likewise cost one per selected batch, not one per bottle.
- Weakness follows its real path without nether wart. Other recipes follow their actual paths. Audit special 1.21+ recipes and ingredient material names.
- Atomic transaction on the player's entity scheduler: count and re-check materials, remove only if all exist, then add exactly the outputs. Never consume on failure. Handle full inventory without item loss; safely drop overflow at player location if needed.
- Only count plain water potions as water bottles; never consume other potions.
- Prevent duplicate form callbacks/double clicks per player.
- Form callbacks may run on foreign threads: all Bukkit inventory/world/player mutations must hop to `player.getScheduler()`.
- Folia compatible; no legacy Bukkit scheduler.
- Plugin depends on floodgate API already installed server-side.
- Do not restart or deploy to production. Do not commit or push.

## Existing state
The repository is partially implemented. Current tests were written first. One category-count test expectation may be wrong; correct the test only when the product taxonomy proves it. Current production code may have compile errors, invalid Cumulus generics/API calls, compressed formatting, incorrect space checks, and incomplete material translation. Treat all existing code as untrusted and review it fully rather than patching only the first compiler error.

## Tests and verification
1. Keep/add focused unit tests for recipe catalog, costs, missing material and transaction planning.
2. Run `mvn -B test` until genuine BUILD SUCCESS. Do not pipe in a way that masks exit status.
3. Run `mvn -B clean package` and inspect the jar: plugin.yml present, test stubs absent, floodgate/cumulus classes not shaded.
4. Inspect all Java for Folia affinity, duplicate callback races, item loss/duplication, recipe correctness, and API compatibility.
5. Do at least three review/fix/test passes. Record each pass in `VERIFY.md` with actual command result summaries and remaining limitations.
6. Normal readable Java formatting; no one-line compressed methods.
7. Write `CODEX_DONE.md` only after all checks pass, listing changed files and exact final test/build result. If blocked, state the blocker honestly instead of writing DONE.
