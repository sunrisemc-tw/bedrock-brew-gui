# CODEX_DONE

Completed end-to-end on 2026-11-04 (Asia/Taipei).

Changed files:

- `pom.xml`
- `RESEARCH.md`
- `VERIFY.md`
- `src/main/resources/plugin.yml`
- `src/main/java/tw/sunrisemc/bedrockbrew/BedrockBrewPlugin.java`
- `src/main/java/tw/sunrisemc/bedrockbrew/BrewInventory.java`
- `src/main/java/tw/sunrisemc/bedrockbrew/BrewMenu.java`
- `src/main/java/tw/sunrisemc/bedrockbrew/BrewText.java`
- `src/main/java/tw/sunrisemc/bedrockbrew/BrewingStandListener.java`
- `src/main/java/tw/sunrisemc/bedrockbrew/FormSessions.java`
- `src/main/java/tw/sunrisemc/bedrockbrew/core/BottleKind.java`
- `src/main/java/tw/sunrisemc/bedrockbrew/core/BrewCatalog.java`
- `src/main/java/tw/sunrisemc/bedrockbrew/core/BrewCategory.java`
- `src/main/java/tw/sunrisemc/bedrockbrew/core/BrewPlan.java`
- `src/main/java/tw/sunrisemc/bedrockbrew/core/BrewRecipe.java`
- `src/main/java/tw/sunrisemc/bedrockbrew/core/BrewTransaction.java`
- `src/test/java/tw/sunrisemc/bedrockbrew/BrewInventoryTest.java`
- `src/test/java/tw/sunrisemc/bedrockbrew/BrewMenuTest.java`
- `src/test/java/tw/sunrisemc/bedrockbrew/BrewTextTest.java`
- `src/test/java/tw/sunrisemc/bedrockbrew/BrewingStandListenerTest.java`
- `src/test/java/tw/sunrisemc/bedrockbrew/FormSessionsTest.java`
- `src/test/java/tw/sunrisemc/bedrockbrew/core/BrewCatalogTest.java`
- `src/test/java/tw/sunrisemc/bedrockbrew/core/BrewTransactionTest.java`

Final verification:

- `mvn -B test` — BUILD SUCCESS; 41 tests passed, 0 failures/errors/skips.
- `mvn -B clean package` — BUILD SUCCESS; 41 tests passed, 0 failures/errors/skips
  (19:33:03 +08:00).
- Jar inspection passed: `plugin.yml` present; test stubs and
  Floodgate/Cumulus classes not shaded.
- Final `mvn -B test` — BUILD SUCCESS; 41 tests passed, 0 failures/errors/skips.
- Final static audit and `git diff --check` passed.

No commit, push, deployment, server launch, or restart was performed.
