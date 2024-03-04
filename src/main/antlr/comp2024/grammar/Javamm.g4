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
ARRAY : '[]' ;
BOOLEAN : 'boolean' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
IMPORT : 'import' ;
EXTENDS : 'extends' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;

INTEGER: '0' | [1-9][0-9]*;
ID: [a-zA-Z] [a-zA-Z_0-9]*;

WS : [ \t\n\r\f]+ -> skip ;


program
    : importDecl*
        classDecl EOF
    ;

importDecl
    : IMPORT value+=ID(DOT value+=ID)* SEMI ;


classDecl
    : CLASS name=ID (EXTENDS sup=ID)?
        LCURLY
        (varDecl | methodDecl)*
        mainMethod?
        (varDecl | methodDecl)*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : typ=INT(ARRAY | VARGS)?
    | typ=STRING(ARRAY | VARGS)?
    | typ=BOOLEAN(ARRAY | VARGS)?
    | typ=ID(ARRAY | VARGS)?
    ;

mainMethod
    : ('public')? 'static' 'void' 'main' LPAREN type ID RPAREN
        LCURLY
        varDecl*
        RCURLY
    ;


methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (CMA param)*)? RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : type name=ID
    ;

stmt
    : expr SEMI #EmptyStmt
    | LCURLY (stmt)* RCURLY #BracketsStmt
    | IF LPAREN expr RPAREN LCURLY stmt* RCURLY ELSE LCURLY stmt* RCURLY #IfStmtCurly
    | IF LPAREN expr RPAREN stmt? ELSE stmt? #IfStmt
    | WHILE LPAREN expr RPAREN LCURLY stmt* RCURLY #WhileStmtCurly
    | WHILE LPAREN expr RPAREN stmt? #WhileStmt
    | expr EQUALS expr SEMI #AssignStmt //
    | RETURN expr SEMI #ReturnStmt
    ;

expr
    : LPAREN name=ID RPAREN (LRET expr RRET)? #ParentsArrayExpr
    | LPAREN expr RPAREN #ParenExpr
    | name=ID op=(INC | DEC) #IncDecExpr
    | value = NOT expr #UnaryExpr
    | expr op=(MUL  | DIV) expr #BinaryExpr //
    | expr op=(ADD | SUB) expr #BinaryExpr //
    | expr op=(LS | LE | GR | GE) expr #BooleanExpr
    | expr op=(EQ | NEQ) expr #BooleanExpr
    | expr op=AND expr #BooleanExpr
    | expr op=OR expr #BooleanExpr
    | name=ID DOT name=ID LPAREN (expr (CMA expr)*)? RPAREN (DOT name=ID LPAREN (expr (CMA expr)*)? RPAREN)* #FuncExpr
    | name=ID DOT name=ID LPAREN (expr (CMA expr)*)? RPAREN (DOT name=ID LPAREN (expr (CMA expr)*)? RPAREN)* ((LRET expr RRET)* (DOT 'length')*) #FuncExpr
    | value=INTEGER #IntegerLiteral //
    | name=ID (LRET expr RRET)* #VarRefExpr //
    | name=ID (DOT 'length')* #LengthExpr
    | NOT expr #NotExpr
    | NEW type LRET expr RRET #ArrayExpr
    | LRET expr (CMA expr)* RRET #ArrayExpr
    | NEW name=ID LPAREN expr* RPAREN #NewClassExpr
    ;


