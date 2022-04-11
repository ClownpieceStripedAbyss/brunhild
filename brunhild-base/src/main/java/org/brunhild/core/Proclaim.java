package org.brunhild.core;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Option;
import org.jetbrains.annotations.NotNull;

public sealed interface Proclaim permits Def, Proclaim.AssignProclaim, Proclaim.BlockProclaim, Proclaim.BreakProclaim,
  Proclaim.ContinueProclaim, Proclaim.IfProclaim, Proclaim.ReturnProclaim, Proclaim.TermProclaim, Proclaim.WhileProclaim {
  record AssignProclaim(
    @NotNull Term.LValueTerm lvalue,
    @NotNull Term rvalue
  ) implements Proclaim {
    @Override public @NotNull String toString() {
      return String.format("%s = %s;", lvalue, rvalue);
    }
  }

  record TermProclaim(
    @NotNull Term term
  ) implements Proclaim {
    @Override public @NotNull String toString() {
      return term + ";";
    }
  }

  record BlockProclaim(
    @NotNull ImmutableSeq<Proclaim> block
  ) implements Proclaim {
    @Override public @NotNull String toString() {
      return "{\n" + block.joinToString("\n  ", "  ", "") + "\n}";
    }
  }

  record IfProclaim(
    @NotNull Term condition,
    @NotNull Proclaim thenBranch,
    @NotNull Option<Proclaim> elseBranch
  ) implements Proclaim {
    @Override public @NotNull String toString() {
      if (elseBranch.isEmpty()) return String.format("if (%s) %s", condition, thenBranch);
      else return String.format("if (%s) %s else %s", condition, thenBranch, elseBranch.get());
    }
  }

  record WhileProclaim(
    @NotNull Term cond,
    @NotNull Proclaim body
  ) implements Proclaim {
    @Override public @NotNull String toString() {
      return String.format("while (%s) %s", cond, body);
    }
  }

  record ReturnProclaim(
    @NotNull Option<Term> term
  ) implements Proclaim {
    @Override public @NotNull String toString() {
      return term.map(t -> "return " + t + ";").getOrDefault("return;");
    }
  }

  record BreakProclaim() implements Proclaim {
    @Override public @NotNull String toString() {
      return "break;";
    }
  }
  record ContinueProclaim() implements Proclaim {
    @Override public @NotNull String toString() {
      return "continue;";
    }
  }
}
