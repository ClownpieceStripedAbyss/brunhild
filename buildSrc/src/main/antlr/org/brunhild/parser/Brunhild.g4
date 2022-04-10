grammar Brunhild;

// compilation unit
program: programItem*;
programItem: varDecl | fnDecl;

// decl
varDecl: KW_CONST? primitiveType varDeclItem (',' varDeclItem)* ';';
varDeclItem: ID arrayTypeSuffix* (ASSIGN varInitVal)?;
varInitVal: expr | '{' (varInitVal (',' varInitVal)*)? '}';

fnDecl: returnType ID '(' fnParams? ')' block;
fnParams: fnParam (',' fnParam)*;
fnParam: primitiveType ID arrayParamTypeSuffix?;

block: '{' blockItem* '}';
blockItem: varDecl | stmt;

returnType: KW_VOID | primitiveType;
primitiveType: KW_INT | KW_FLOAT;
arrayParamTypeSuffix: '[' ']' ('[' expr ']')*;
arrayTypeSuffix: '[' expr ']';

// stmt
stmt : lval ASSIGN expr ';'                       # assign
     | expr? ';'                                  # exprStmt
     | block                                      # blockStmt
     | KW_IF '(' cond ')' stmt (KW_ELSE stmt)?    # if
     | KW_WHILE '(' cond ')' stmt                 # while
     | KW_BREAK ';'                               # break
     | KW_CONTINUE ';'                            # continue
     | KW_RETURN expr? ';'                        # return
     ;

// expr
expr: addExpr;
cond: lOrExpr;
lval: ID ('[' expr ']')*;
primaryExpr: '(' expr ')' | lval | number;
number: INT_LITERAL | FLOAT_LITERAL;
unaryExpr: primaryExpr | ID '(' appArg? ')' | (ADD | SUB | LOGICAL_NOT) unaryExpr;
appArg: expr (',' expr)*;
mulExpr: unaryExpr | mulExpr (MUL | DIV | MOD) unaryExpr;
addExpr: mulExpr | addExpr (ADD | SUB) mulExpr;
relExpr: addExpr | relExpr (LT | GT | LE | GE) addExpr;
eqExpr: relExpr | eqExpr (EQ | NE) relExpr;
lAndExpr: eqExpr | lAndExpr LOGICAL_AND eqExpr;
lOrExpr: lAndExpr | lOrExpr LOGICAL_OR lAndExpr;

// keywords
KW_CONST: 'const';
KW_INT: 'int';
KW_FLOAT: 'float';
KW_VOID: 'void';
KW_IF: 'if';
KW_ELSE: 'else';
KW_WHILE: 'while';
KW_RETURN: 'return';
KW_BREAK: 'break';
KW_CONTINUE: 'continue';

// operator tokens
ADD: '+';
SUB: '-';
MUL: '*';
DIV: '/';
MOD: '%';
LT: '<';
GT: '>';
LE: '<=';
GE: '>=';
EQ: '==';
NE: '!=';
ASSIGN: '=';
LOGICAL_AND: '&&';
LOGICAL_OR: '||';
LOGICAL_NOT: '!';

// literals
fragment DEC_INT_LITERAL: '0' | [1-9][0-9]*;
fragment HEX_INT_LITERAL: '0' [xX] [0-9a-fA-F]+;
fragment OCT_INT_LITERAL: '0' [0-7]+;
INT_LITERAL: DEC_INT_LITERAL | HEX_INT_LITERAL | OCT_INT_LITERAL;

fragment DEC_FLOAT_LITERAL: FLOAT_CONST FLOAT_EXP? | [0-9]+ FLOAT_EXP;
fragment HEX_FLOAT_LITERAL: '0' [xX] (HEX_FLOAT_CONST | [0-9a-fA-F]+) BIN_FLOAT_EXP;

fragment FLOAT_CONST: ([0-9]+)? '.' [0-9]+ | [0-9]+ '.';
fragment HEX_FLOAT_CONST: ([0-9a-fA-F]+)? '.' [0-9a-fA-F]+ | [0-9a-fA-F]+ '.';

fragment FLOAT_EXP: [eE] ([+-])? [0-9]+;
fragment BIN_FLOAT_EXP: [pP] ([+-])? [0-9]+;
FLOAT_LITERAL: DEC_FLOAT_LITERAL | HEX_FLOAT_LITERAL;

// identifier
fragment SIMPLE_LETTER : [~!@#$%^&*+=<>?/|[\u005Da-zA-Z_\u2200-\u22FF];
fragment UNICODE : [\u0080-\uFEFE] | [\uFF00-\u{10FFFF}]; // exclude U+FEFF which is a truly invisible char
fragment LETTER : SIMPLE_LETTER | UNICODE;
fragment LETTER_FOLLOW : LETTER | [0-9];
ID : LETTER LETTER_FOLLOW*;

// whitespaces
WS : [ \t\r\n]+ -> channel(HIDDEN);
fragment COMMENT_CONTENT : ~[\r\n]*;
DOC_COMMENT : '///' COMMENT_CONTENT;
LINE_COMMENT : '//' COMMENT_CONTENT -> channel(HIDDEN);
COMMENT : '/*' (COMMENT|.)*? '*/' -> channel(HIDDEN);

// avoid token recognition error in REPL
ERROR_CHAR : .;
