package org.brunhild.core.ops;

import org.brunhild.core.Def;
import org.brunhild.core.Proclaim;
import org.jetbrains.annotations.NotNull;

public interface ProclaimOps<P> extends TermOps<P> {
  default @NotNull Proclaim traverse(@NotNull Proclaim proclaim, @NotNull P param) {
    return switch (proclaim) {
      case Proclaim.VarAssignProclaim ass -> {
        var rvalue = traverse(ass.rvalue(), param);
        if (rvalue == ass.rvalue()) yield ass;
        yield new Proclaim.VarAssignProclaim(ass.var(), rvalue);
      }
      case Proclaim.IndexAssignProclaim ass -> {
        var index = traverse(ass.index(), param);
        var rvalue = traverse(ass.rvalue(), param);
        if (index == ass.index() && rvalue == ass.rvalue()) yield ass;
        yield new Proclaim.IndexAssignProclaim(ass.term(), index, rvalue);
      }
      case Proclaim.TermProclaim termProclaim -> {
        var term = traverse(termProclaim.term(), param);
        if (term == termProclaim.term()) yield termProclaim;
        yield new Proclaim.TermProclaim(term);
      }
      case Proclaim.BlockProclaim blockProclaim -> {
        var block = blockProclaim.block().map(b -> traverse(b, param));
        if (block.sameElements(blockProclaim.block())) yield blockProclaim;
        yield new Proclaim.BlockProclaim(block);
      }
      case Proclaim.ReturnProclaim returnProclaim -> {
        var term = returnProclaim.term().map(t -> traverse(t, param));
        if (term.sameElements(returnProclaim.term())) yield returnProclaim;
        yield new Proclaim.ReturnProclaim(term);
      }
      case Proclaim.IfProclaim ifProclaim -> {
        var cond = traverse(ifProclaim.cond(), param);
        var then = traverse(ifProclaim.thenBranch(), param);
        var else_ = ifProclaim.elseBranch().map(b -> traverse(b, param));
        if (cond == ifProclaim.cond() && then == ifProclaim.thenBranch() && else_.sameElements(ifProclaim.elseBranch()))
          yield ifProclaim;
        yield new Proclaim.IfProclaim(cond, then, else_);
      }
      case Proclaim.WhileProclaim whileProclaim -> {
        var cond = traverse(whileProclaim.cond(), param);
        var body = traverse(whileProclaim.body(), param);
        if (cond == whileProclaim.cond() && body == whileProclaim.body()) yield whileProclaim;
        yield new Proclaim.WhileProclaim(cond, body);
      }
      case Def.VarDef varDef -> {
        varDef.body = traverse(varDef.body, param);
        yield varDef;
      }
      case Def.FnDef fnDef -> {
        fnDef.body = traverse(fnDef.body, param);
        yield fnDef;
      }
      case Def.PrimDef primDef -> primDef;
      case Proclaim.ContinueProclaim cont -> cont;
      case Proclaim.BreakProclaim brk -> brk;
    };
  }
}
