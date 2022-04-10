package org.brunhild.concrete.resolve;

import kala.collection.immutable.ImmutableSeq;
import org.brunhild.concrete.Decl;
import org.brunhild.concrete.Stmt;
import org.brunhild.concrete.resolve.context.ModuleContext;
import org.jetbrains.annotations.NotNull;

public interface StmtShallowResolver {
  default void resolveStmts(@NotNull ImmutableSeq<Stmt> stmts, @NotNull ModuleContext context) {
    stmts.forEach(stmt -> resolveStmt(stmt, context));
  }

  default void resolveStmt(@NotNull Stmt stmt, @NotNull ModuleContext context) {
    switch (stmt) {
      case Decl.FnDecl decl -> resolveDecl(decl, context);
      case Decl.VarDecl decl -> resolveDecl(decl, context);
      default -> {
      }
    }
  }

  private void resolveDecl(@NotNull Decl decl, @NotNull ModuleContext context) {
    decl.context = context;
    context.addGlobal(decl.sourcePos, decl.ref().name(), decl.ref());
  }
}
