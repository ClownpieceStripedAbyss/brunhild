package org.brunhild.concrete.resolve.context;

import org.brunhild.error.Reporter;
import org.brunhild.error.SourceFile;
import org.brunhild.generic.Var;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record BindContext(
  @Override @NotNull Context parent,
  @NotNull String name,
  @NotNull Var ref
) implements Context {
  @Override public @NotNull Reporter reporter() {
    return parent.reporter();
  }

  @Override public @NotNull SourceFile underlyingSourceFile() {
    return parent.underlyingSourceFile();
  }

  @Override public @Nullable Var getLocal(@NotNull String name) {
    return name.equals(this.name) ? ref : null;
  }
}
