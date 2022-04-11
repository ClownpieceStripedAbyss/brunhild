package org.brunhild.optimize;

import org.brunhild.core.Def;
import org.brunhild.core.Proclaim;
import org.brunhild.core.ops.TermFold;
import org.brunhild.optimize.generic.Optimizer;
import org.brunhild.tyck.Gamma;
import org.jetbrains.annotations.NotNull;

public interface TreeFold extends TermFold, Optimizer.AstRewriter<Gamma.@NotNull ConstGamma> {
  @Override default @NotNull Proclaim traverse(@NotNull Proclaim proclaim, Gamma.@NotNull ConstGamma gamma) {
    return switch (proclaim) {
      case Def.VarDef varDef -> {
        var body = varDef.body = traverse(varDef.body, gamma);
        gamma.put(varDef.ref, body);
        yield varDef;
      }
      default -> AstRewriter.super.traverse(proclaim, gamma);
    };
  }

  class DefaultFold implements TreeFold {}
}
