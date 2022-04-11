package org.brunhild.core;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Option;
import org.jetbrains.annotations.NotNull;

public sealed interface Proclaim permits Def, Proclaim.AssignProclaim, Proclaim.BlockProclaim, Proclaim.BreakProclaim,
  Proclaim.ContinueProclaim, Proclaim.IfProclaim, Proclaim.ReturnProclaim, Proclaim.TermProclaim, Proclaim.WhileProclaim {
  record AssignProclaim(
    @NotNull Term.LValueTerm lvalue,
    @NotNull Term rvalue
  ) implements Proclaim {}

  record TermProclaim(
    @NotNull Term term
  ) implements Proclaim {}

  record BlockProclaim(
    @NotNull ImmutableSeq<Proclaim> block
  ) implements Proclaim {}

  record IfProclaim(
    @NotNull Term condition,
    @NotNull Proclaim thenBranch,
    @NotNull Option<Proclaim> elseBranch
  ) implements Proclaim {}

  record WhileProclaim(
    @NotNull Term cond,
    @NotNull Proclaim body
  ) implements Proclaim {}

  record ReturnProclaim(
    @NotNull Option<Term> term
  ) implements Proclaim {}

  record BreakProclaim() implements Proclaim {}
  record ContinueProclaim() implements Proclaim {}
}
