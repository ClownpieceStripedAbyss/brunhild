grammar Brunhild;

program: ID;

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
