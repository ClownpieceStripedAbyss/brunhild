package org.brunhild.tyck;

import kala.collection.mutable.MutableHashMap;
import org.brunhild.core.Term;
import org.brunhild.generic.LocalVar;
import org.brunhild.generic.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public record Gamma(
  @Nullable Gamma parent,
  @NotNull MutableHashMap<LocalVar, Type<Term>> ctx
) {
  public Gamma() {
    this(null, MutableHashMap.create());
  }

  public void put(@NotNull LocalVar var, @NotNull Type<Term> type) {
    ctx.put(var, type);
  }

  public void put(Term.@NotNull Param param) {
    put(param.ref(), param.type());
  }

  public @Nullable Type<Term> getLocal(@NotNull LocalVar var) {
    return ctx.getOrNull(var);
  }

  public void remove(@NotNull LocalVar var) {
    ctx.remove(var);
  }

  public @NotNull Type<Term> get(@NotNull LocalVar var) {
    var ctx = this;
    while (ctx != null) {
      var res = ctx.getLocal(var);
      if (res != null) return res;
      ctx = ctx.parent();
    }
    throw new IllegalArgumentException(var.name());
  }

  public <T> T with(@NotNull LocalVar var, @NotNull Type<Term> type, @NotNull Supplier<T> action) {
    put(var, type);
    try {
      return action.get();
    } finally {
      remove(var);
    }
  }
}
