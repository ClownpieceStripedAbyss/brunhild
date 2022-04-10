package org.brunhild.tyck;

import kala.collection.immutable.ImmutableSeq;
import org.brunhild.concrete.Decl;
import org.brunhild.concrete.Expr;
import org.brunhild.concrete.Stmt;
import org.brunhild.core.Def;
import org.brunhild.core.Proclaim;
import org.brunhild.core.Term;
import org.brunhild.error.Reporter;
import org.brunhild.generic.Type;
import org.jetbrains.annotations.NotNull;

public record StmtTycker(
  @NotNull Reporter reporter
) {
  public @NotNull ExprTycker exprTycker() {
    return new ExprTycker(reporter, new Gamma());
  }

  public @NotNull Def tyckTopLevel(@NotNull Stmt stmt, @NotNull ExprTycker tycker) {
    return switch (stmt) {
      case Decl.FnDecl decl -> {
        var tele = checkTele(tycker, decl.telescope);
        var result = tycker.infer(decl.result).wellTyped();
        decl.signature = new Def.Signature(tele, result);
        var body = check(decl.body, result, tycker);
        yield new Def.FnDef(decl.ref, tele, body, result);
      }
      case Decl.VarDecl decl -> {
        var result = tycker.infer(decl.result).wellTyped();
        decl.signature = new Def.Signature(ImmutableSeq.empty(), result);
        var body = tycker.check(decl.body, result).wellTyped();
        yield new Def.VarDef(decl.ref, body, result);
      }
      default -> throw new IllegalStateException("Top level cannot have statements" + stmt);
    };
  }

  private @NotNull Proclaim check(@NotNull Stmt stmt, @NotNull Type<Term> type, @NotNull ExprTycker tycker) {
    throw new UnsupportedOperationException("TODO");
  }

  private @NotNull ImmutableSeq<Term.Param> checkTele(@NotNull ExprTycker tycker, @NotNull ImmutableSeq<Expr.Param> tele) {
    return tele.map(param -> {
      var paramTyped = tycker.infer(param.type()).wellTyped();
      var newParam = new Term.Param(param, paramTyped);
      tycker.gamma().put(newParam);
      return newParam;
    });
  }
}
