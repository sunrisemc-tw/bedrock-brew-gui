package tw.sunrisemc.bedrockbrew.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record BrewPlan(
    boolean success,
    Map<String, Integer> remove,
    Map<String, Integer> missing,
    int outputs,
    int fuelRemaining) {
  public BrewPlan {
    remove = Collections.unmodifiableMap(new LinkedHashMap<>(remove));
    missing = Collections.unmodifiableMap(new LinkedHashMap<>(missing));
  }
}
