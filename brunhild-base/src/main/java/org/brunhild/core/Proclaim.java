package org.brunhild.core;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Option;
import org.brunhild.generic.Var;
import org.jetbrains.annotations.NotNull;

public sealed interface Proclaim permits Def, Proclaim.BlockProclaim, Proclaim.BreakProclaim, Proclaim.ContinueProclaim, Proclaim.IfProclaim, Proclaim.IndexAssignProclaim, Proclaim.ReturnProclaim, Proclaim.TermProclaim, Proclaim.VarAssignProclaim, Proclaim.WhileProclaim {
  record VarAssignProclaim(
    @NotNull Var var,
    @NotNull Term rvalue
  ) implements Proclaim {
    @Override public @NotNull String toString() {
      return String.format("%s = %s;", var.name(), rvalue);
    }
  }

  record IndexAssignProclaim(
    @NotNull Term term,
    @NotNull Term index,
    @NotNull Term rvalue
  ) implements Proclaim {
    @Override public @NotNull String toString() {
      return String.format("%s[%s] = %s;", term, index, rvalue);
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
    @NotNull Term cond,
    @NotNull Proclaim thenBranch,
    @NotNull Option<Proclaim> elseBranch
  ) implements Proclaim {
    @Override public @NotNull String toString() {
      if (elseBranch.isEmpty()) return String.format("if (%s) %s", cond, thenBranch);
      else return String.format("if (%s) %s else %s", cond, thenBranch, elseBranch.get());
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
