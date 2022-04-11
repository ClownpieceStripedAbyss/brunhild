package org.brunhild.core.ops;

import kala.tuple.Unit;
import org.brunhild.core.Term;
import org.brunhild.tyck.Gamma;
import org.jetbrains.annotations.NotNull;

public record Folder(
  @NotNull Gamma.ConstGamma gamma
) implements TermOps<Unit> {
  @Override public @NotNull Term traverse(@NotNull Term term, Unit unit) {
    return TermOps.super.traverse(term, unit);
  }
}
