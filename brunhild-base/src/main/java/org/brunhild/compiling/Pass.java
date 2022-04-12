package org.brunhild.compiling;

import kala.collection.immutable.ImmutableSeq;
import org.brunhild.concrete.Stmt;
import org.brunhild.concrete.parse.GenericBrunhildParser;
import org.brunhild.concrete.resolve.context.ModuleContext;
import org.brunhild.core.Proclaim;
import org.brunhild.core.ops.ProclaimOps;
import org.brunhild.error.Reporter;
import org.brunhild.error.SourceFile;
import org.jetbrains.annotations.NotNull;

public interface Pass<I, O, P> {
  @NotNull O perform(@NotNull I input, P param);

  interface AstRewriter<P> extends Pass<ImmutableSeq<Proclaim>, ImmutableSeq<Proclaim>, P>, ProclaimOps<P> {
    @Override default @NotNull ImmutableSeq<Proclaim> perform(@NotNull ImmutableSeq<Proclaim> input, P param) {
      return input.map(s -> traverse(s, param));
    }
  }

  default @NotNull Pipeline<I, O> pipelining(@NotNull P param) {
    return i -> perform(i, param);
  }

  @NotNull Pass<SourceFile, ImmutableSeq<Stmt>, GenericBrunhildParser> Parsing = (i, parse) -> parse.program(i);
  @NotNull Pass<ImmutableSeq<Stmt>, ImmutableSeq<Stmt>, ModuleContext> Resolving = Stmt::resolve;
  @NotNull Pass<ImmutableSeq<Stmt>, ImmutableSeq<Proclaim>, Reporter> Tycking = Stmt::tyck;
}
