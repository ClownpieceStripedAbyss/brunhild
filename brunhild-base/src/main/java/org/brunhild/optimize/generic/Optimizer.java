package org.brunhild.optimize.generic;

import kala.collection.immutable.ImmutableSeq;
import org.brunhild.core.Proclaim;
import org.brunhild.core.ops.ProclaimOps;
import org.jetbrains.annotations.NotNull;

public interface Optimizer<I, O, P> {
  @NotNull O perform(@NotNull I input, P param);

  interface AstRewriter<P> extends Optimizer<ImmutableSeq<Proclaim>, ImmutableSeq<Proclaim>, P>,
    ProclaimOps<P> {
    @Override default @NotNull ImmutableSeq<Proclaim> perform(@NotNull ImmutableSeq<Proclaim> input, P param) {
      return input.map(s -> traverse(s, param));
    }
  }
}
