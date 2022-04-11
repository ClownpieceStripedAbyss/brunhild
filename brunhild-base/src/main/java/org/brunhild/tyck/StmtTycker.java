package org.brunhild.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Option;
import org.brunhild.concrete.Decl;
import org.brunhild.concrete.Expr;
import org.brunhild.concrete.Stmt;
import org.brunhild.core.Def;
import org.brunhild.core.Proclaim;
import org.brunhild.core.Term;
import org.brunhild.error.Reporter;
import org.brunhild.generic.Type;
import org.brunhild.tyck.problem.AssigningNonLvalue;
import org.brunhild.tyck.problem.MissingReturnValue;
import org.brunhild.tyck.problem.NotInLoop;
import org.jetbrains.annotations.NotNull;

public record StmtTycker(
  @NotNull Reporter reporter
) {
  public @NotNull ImmutableSeq<Proclaim> tyckStmts(@NotNull ImmutableSeq<Stmt> resolved, @NotNull ExprTycker tycker) {
    return resolved.map(stmt -> tyckTopLevel(stmt, tycker));
  }

  public @NotNull Def tyckTopLevel(@NotNull Stmt stmt, @NotNull ExprTycker tycker) {
    return switch (stmt) {
      case Decl.FnDecl decl -> {
        var tele = checkTele(tycker, decl.telescope);
        var result = tycker.infer(decl.result).wellTyped();
        decl.signature = new Def.Signature(tele, result);
        var body = check(decl.body, result, false, tycker);
        yield new Def.FnDef(decl.ref, tele, body, result);
      }
      case Decl.VarDecl decl -> {
        var result = tycker.infer(decl.result).wellTyped();
        decl.signature = new Def.Signature(ImmutableSeq.empty(), result);
        var body = tycker.check(decl.body, result).wellTyped();
        if (decl.isConst()) tycker.constGamma().put(decl.ref, body);
        yield new Def.VarDef(decl.ref, body, result);
      }
      default -> throw new IllegalStateException("Top level cannot have statements" + stmt);
    };
  }

  private @NotNull Proclaim check(@NotNull Stmt stmt, @NotNull Type<Term> returnType, boolean insideLoop, @NotNull ExprTycker tycker) {
    return switch (stmt) {
      case Stmt.AssignStmt assignStmt -> {
        var lvalue = tycker.infer(assignStmt.lvalue());
        var rvalue = tycker.check(assignStmt.rvalue(), lvalue.type());
        if (lvalue.wellTyped() instanceof Term.RefTerm ref) {
          yield new Proclaim.VarAssignProclaim(ref.var(), rvalue.wellTyped());
        } else if (lvalue.wellTyped() instanceof Term.IndexTerm indexTerm) {
          yield new Proclaim.IndexAssignProclaim(indexTerm.term(), indexTerm.index(), rvalue.wellTyped());
        } else {
          yield tycker.fail(new AssigningNonLvalue(assignStmt.sourcePos()));
        }
      }
      case Stmt.IfStmt ifStmt -> {
        var cond = tycker.check(ifStmt.cond(), new Type.Int<>()).wellTyped();
        var then = check(ifStmt.thenBranch(), returnType, insideLoop, tycker);
        var elseBranch = ifStmt.elseBranch().map(s -> check(s, returnType, insideLoop, tycker));
        yield new Proclaim.IfProclaim(cond, then, elseBranch);
      }
      case Stmt.ExprStmt exprStmt -> {
        var term = tycker.infer(exprStmt.expr()).wellTyped();
        yield new Proclaim.TermProclaim(term);
      }
      case Stmt.BlockStmt blockStmt -> {
        var bodyTycker = tycker.derive();
        var proclaim = blockStmt.block().map(s -> check(s, returnType, insideLoop, bodyTycker));
        yield new Proclaim.BlockProclaim(proclaim);
      }
      case Stmt.WhileStmt whileStmt -> {
        var cond = tycker.check(whileStmt.cond(), new Type.Int<>()).wellTyped();
        var body = check(whileStmt.body(), returnType, true, tycker);
        yield new Proclaim.WhileProclaim(cond, body);
      }
      case Stmt.ReturnStmt returnStmt -> {
        var expr = returnStmt.expr();
        if (expr.isEmpty()) {
          if (!(returnType instanceof Type.Void<Term>))
            yield tycker.fail(new MissingReturnValue(returnStmt.sourcePos()));
          yield new Proclaim.ReturnProclaim(Option.none());
        } else {
          var term = tycker.check(expr.get(), returnType).wellTyped();
          yield new Proclaim.ReturnProclaim(Option.some(term));
        }
      }
      case Stmt.BreakStmt ignored -> {
        if (!insideLoop) yield tycker.fail(new NotInLoop(stmt.sourcePos(), "break"));
        yield new Proclaim.BreakProclaim();
      }
      case Stmt.ContinueStmt ignored -> {
        if (!insideLoop) yield tycker.fail(new NotInLoop(stmt.sourcePos(), "continue"));
        yield new Proclaim.ContinueProclaim();
      }
      case Decl.VarDecl varDecl -> tyckTopLevel(varDecl, tycker);
      case Decl.FnDecl ignored ->
        throw new IllegalStateException("Function declarations are only allowed in top level");
    };
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
