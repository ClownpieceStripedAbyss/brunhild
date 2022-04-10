package org.brunhild.core;

import org.brunhild.generic.LocalVar;
import org.brunhild.generic.Type;
import org.jetbrains.annotations.NotNull;

public interface Term {
  record Param(
    @NotNull LocalVar ref,
    @NotNull Type<Term> type
  ) {
    public Param(@NotNull Term.Param param, @NotNull Type<Term> type) {
      this(param.ref, type);
    }
  }
}
