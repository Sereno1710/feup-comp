grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LRET: '[' ;
RRET: ']' ;
MUL : '*' ;
ADD : '+' ;
DIV: '/' ;
SUB: '-' ;
REM: '%';
LS: '<' ;
GR: '>' ;
LE: '<=';
GE: '>=';
EQ: '==';
NOT: '!';
NEQ: '!=';
AND: '&&';
OR: '||';
INC: '++';
DEC: '--';
CMA: ',';
DOT: '.';
VARGS: '...';
NEW: 'new';

CLASS : 'class' ;
INT : 'int' ;
STRING : 'String' ;
ARRAY : '['[ ]?']';
BOOLEAN : 'boolean' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
IMPORT : 'import' ;
EXTENDS : 'extends' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
FOR: 'for';
THIS: 'this';

INTEGER: [0] | [1-9][0-9]*;
ID: [a-zA-Z_$] [a-zA-Z_$0-9]*;
SINGLE_COMMENT : '//' .*? '\n' -> skip ;
MULTI_COMMENT : '/*' .*? '*/' -> skip ;
WS : [ \t\n\r\f]+ -> skip ;


program
    : importDecl* classDecl EOF
    ;

importDecl
    : IMPORT value+=(ID | 'main')(DOT value+=(ID | 'main'))* SEMI ;


classDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})? CLASS name=(ID | 'main') (EXTENDS sup=(ID | 'main'))?
        LCURLY
        varDecl*
        (methodDecl)*
        RCURLY
    ;

varDecl
    : type name=ID SEMI #VariableDecl
    | type name=('main' | 'length') SEMI #VariableDecl
    ;



type
    : name=INT array=ARRAY
    | name=INT vargs=VARGS
    | name=INT
    | name=STRING (array=ARRAY)?
    | name=BOOLEAN (array=ARRAY)?
    | name=(ID | 'main')
    ;

methodDecl locals[boolean isPublic=false, boolean isStatic=false]
    : (PUBLIC {$isPublic=true;})? ('static' {$isStatic=true;}) type name='main' LPAREN param RPAREN
              LCURLY
              varDecl* stmt*
              RCURLY
    |(PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN params? RPAREN
        LCURLY
        varDecl* stmt*
        returnS?
        RCURLY
    ;
params
    : param (CMA param)*
    ;

param
    : type name=(ID | 'main')
    ;

returnS
    : RETURN expr SEMI #ReturnStmt
    ;

stmt
    : expr SEMI #ExprStmt
    | LCURLY stmt* RCURLY #BracketsStmt
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | FOR LPAREN stmt expr SEMI expr RPAREN stmt #ForStmt
    | name=(ID | 'main' | 'length') EQUALS expr SEMI #AssignStmt //
    | name=(ID | 'main' | 'length') LRET expr RRET EQUALS expr SEMI #AssignStmtArray
    ;


expr
    : className+=(ID | 'this' | 'main') (DOT className+=(ID | 'main'))+ #ClassChainExpr
    | expr (DOT name=(ID | 'main'))* LPAREN (expr (CMA expr)*)? RPAREN #FuncExpr
    | expr LRET expr RRET #AccExpr
    | NOT expr #NotExpr
    | expr op=(MUL  | DIV | REM) expr #BinaryExpr //
    | expr op=(ADD | SUB) expr #BinaryExpr //
    | expr op=(LS | LE | GR | GE) expr #BinaryExpr
    | expr op=(EQ | NEQ) expr #BinaryExpr
    | expr op=AND expr #BinaryExpr
    | expr op=OR expr #BinaryExpr
    | expr (DOT 'length') #LengthExpr
    | NEW type LRET expr RRET #NewArray
    | NEW name=(ID | 'main') LPAREN expr* RPAREN #NewClassExpr
    | LPAREN expr RPAREN #ParenExpr
    | LRET expr (CMA expr)* RRET #ArrayExpr
    | value=INTEGER #IntegerLiteral //
    | value='true' #BooleanLiteral
    | value='false' #BooleanLiteral
    | name=(ID | 'main' | 'length') #VarRefExpr //
    | value='this' #ObjectLiteral
    ;



