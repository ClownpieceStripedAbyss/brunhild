package org.brunhild.generic;

import org.brunhild.concrete.Decl;
import org.brunhild.core.Def;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public final class DefVar<Core extends Def, Concrete extends Decl> implements Var {
  private final @NotNull String name;
  /** Initialized in parsing, so it might be null for deserialized user definitions. */
  public @UnknownNullability Concrete concrete;
  /** Initialized in type checking or core deserialization, so it might be null for unchecked user definitions. */
  public @UnknownNullability Core core;

  @Contract(pure = true) public @NotNull String name() {
    return name;
  }

  private DefVar(Concrete concrete, Core core, @NotNull String name) {
    this.concrete = concrete;
    this.core = core;
    this.name = name;
  }

  public static <Core extends Def, Concrete extends Decl>
  @NotNull DefVar<Core, Concrete> concrete(@NotNull Concrete concrete, @NotNull String name) {
    return new DefVar<>(concrete, null, name);
  }

  public static <Core extends Def, Concrete extends Decl>
  @NotNull DefVar<Core, Concrete> core(@NotNull Core core, @NotNull String name) {
    return new DefVar<>(null, core, name);
  }

  public boolean isForeign() {
    return this.concrete == null && this.core != null;
  }

  @Override public boolean equals(Object o) {
    return this == o;
  }
}
