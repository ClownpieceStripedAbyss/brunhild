package org.brunhild.tyck;

import kala.collection.mutable.MutableLinkedHashMap;
import kala.collection.mutable.MutableMap;
import kala.control.Option;
import org.brunhild.core.Term;
import org.brunhild.generic.LocalVar;
import org.brunhild.generic.Type;
import org.brunhild.generic.Var;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface Gamma<K, V> {
  @Nullable Gamma<K, V> parent();
  @NotNull MutableMap<K, V> ctx();

  default void put(@NotNull K var, @NotNull V v) {
    ctx().put(var, v);
  }

  default @Nullable V getLocal(@NotNull K var) {
    return ctx().getOrNull(var);
  }

  default void remove(@NotNull K var) {
    ctx().remove(var);
  }

  default @NotNull V get(@NotNull K var) {
    return getOption(var).getOrThrow(() -> new IllegalArgumentException(var.toString()));
  }

  default @NotNull Option<V> getOption(@NotNull K var) {
    var ctx = this;
    while (ctx != null) {
      var res = ctx.getLocal(var);
      if (res != null) return Option.some(res);
      ctx = ctx.parent();
    }
    return Option.none();
  }

  default <T> T with(@NotNull K var, @NotNull V v, @NotNull Supplier<T> action) {
    put(var, v);
    try {
      return action.get();
    } finally {
      remove(var);
    }
  }

  record TypeGamma(
    @Override @Nullable TypeGamma parent,
    @Override @NotNull MutableMap<LocalVar, Type<Term>> ctx
  ) implements Gamma<LocalVar, Type<Term>> {
    public TypeGamma() {
      this(null, MutableLinkedHashMap.of());
    }

    public void put(Term.@NotNull Param param) {
      put(param.ref(), param.type());
    }
  }

  record ConstGamma(
    @Override @Nullable ConstGamma parent,
    @Override @NotNull MutableMap<Var, Term> ctx
  ) implements Gamma<Var, Term> {
    public ConstGamma() {
      this(null, MutableLinkedHashMap.of());
    }

    public @NotNull ConstGamma derive() {
      return new ConstGamma(this, MutableLinkedHashMap.of());
    }
  }
}
