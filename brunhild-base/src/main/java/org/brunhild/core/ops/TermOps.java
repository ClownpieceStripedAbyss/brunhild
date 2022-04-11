package org.brunhild.core.ops;

import org.brunhild.core.Term;
import org.jetbrains.annotations.NotNull;

public interface TermOps<P> {
  default @NotNull Term traverse(@NotNull Term term, P param) {
    return switch (term) {
      case Term.LitTerm lit -> lit;
      case Term.CoerceTerm coerceTerm -> {
        var t = traverse(coerceTerm.term(), param);
        if (t == coerceTerm.term()) yield coerceTerm;
        yield new Term.CoerceTerm(t, coerceTerm.fromType(), coerceTerm.toType());
      }
      case Term.RefTerm refTerm -> refTerm;
      case Term.UnaryTerm unaryTerm -> {
        var t = traverse(unaryTerm.term(), param);
        if (t == unaryTerm.term()) yield unaryTerm;
        yield new Term.UnaryTerm(unaryTerm.op(), t);
      }
      case Term.BinaryTerm binaryTerm -> {
        var lhs = traverse(binaryTerm.lhs(), param);
        var rhs = traverse(binaryTerm.rhs(), param);
        if (lhs == binaryTerm.lhs() && rhs == binaryTerm.rhs()) yield binaryTerm;
        yield new Term.BinaryTerm(binaryTerm.op(), lhs, rhs);
      }
      case Term.IndexTerm indexTerm -> {
        var t = traverse(indexTerm.term(), param);
        var i = traverse(indexTerm.index(), param);
        if (t == indexTerm.term() && i == indexTerm.index()) yield indexTerm;
        yield new Term.IndexTerm(t, i);
      }
      case Term.FnCall fnCall -> {
        var args = fnCall.args().map(arg -> traverse(arg, param));
        if (args.sameElements(fnCall.args())) yield fnCall;
        yield new Term.FnCall(fnCall.fn(), args);
      }
      case Term.InitializedArray initializedArray -> {
        var values = initializedArray.values().map(elem -> traverse(elem, param));
        if (values.sameElements(initializedArray.values())) yield initializedArray;
        yield new Term.InitializedArray(initializedArray.type(), values);
      }
      case Term.UninitializedArray uninitializedArray -> uninitializedArray;
      case Term.PrimCall primCall -> {
        var args = primCall.args().map(arg -> traverse(arg, param));
        if (args.sameElements(primCall.args())) yield primCall;
        yield new Term.PrimCall(primCall.prim(), args);
      }
    };
  }
}
