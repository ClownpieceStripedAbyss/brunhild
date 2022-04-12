package org.brunhild.compiling.generate;

import org.brunhild.compiling.Pass;
import org.brunhild.compiling.Pipeline;
import org.jetbrains.annotations.NotNull;

public interface Generator<I, O, P> extends Pass<I, O, P> {
  static @NotNull <I> Pipeline<I, I> justForFun() {
    return Pipeline.identity();
  }
}
