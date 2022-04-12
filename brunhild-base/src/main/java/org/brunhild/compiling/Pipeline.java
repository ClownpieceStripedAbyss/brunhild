package org.brunhild.compiling;

import org.brunhild.error.SourceFile;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface Pipeline<I, O> {
  @NotNull O perform(@NotNull I input);

  default <O2> @NotNull Pipeline<I, O2> then(@NotNull Pipeline<O, O2> other) {
    return i -> other.perform(perform(i));
  }

  default <O2, P> @NotNull Pipeline<I, O2> then(@NotNull Pass<O, O2, P> pass, P param) {
    return then(pass.pipelining(param));
  }

  default @NotNull Pipeline<I, O> peek(@NotNull Consumer<O> peek) {
    return then(i -> {
      peek.accept(i);
      return i;
    });
  }

  static <I> @NotNull Pipeline<I, I> identity() {
    return i -> i;
  }

  @NotNull Pipeline<SourceFile, SourceFile> Begin = identity();
}
