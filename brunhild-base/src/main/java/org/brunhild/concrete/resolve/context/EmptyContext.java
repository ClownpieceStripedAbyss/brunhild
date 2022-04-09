package org.brunhild.concrete.resolve.context;

import org.brunhild.error.Reporter;
import org.brunhild.error.SourceFile;
import org.brunhild.generic.Var;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record EmptyContext(
  @Override @NotNull SourceFile underlyingSourceFile,
  @Override @NotNull Reporter reporter
) implements Context {
  @Override public @Nullable Context parent() {
    return null;
  }

  @Override public @Nullable Var getLocal(@NotNull String name) {
    return null;
  }
}
