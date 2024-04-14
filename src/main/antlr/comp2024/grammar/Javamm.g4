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
    : IMPORT value+=ID(DOT value+=ID)* SEMI ;


classDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})? CLASS name=ID (EXTENDS sup=ID)?
        LCURLY
        varDecl*
        (methodDecl | mainMethod)*
        RCURLY
    ;

varDecl
    : type name=ID SEMI #VariableDecl
    | type name='main' SEMI #VariableDecl
    ;

type
    : name=INT array=ARRAY
    | name=INT VARGS
    | name=INT
    | name=STRING (array=ARRAY | VARGS)?
    | name=BOOLEAN (array=ARRAY | VARGS)?
    | name=ID (array=ARRAY | VARGS)?
    ;


mainMethod
    : (PUBLIC)? 'static' ret='void' name='main' LPAREN param RPAREN
        LCURLY
        varDecl* stmt*
        RCURLY
    ;


methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN params? RPAREN
        LCURLY
        varDecl* stmt*
        RETURN expr SEMI
        RCURLY

    ;

params
    : param (CMA param)*
    ;

param
    : type name=ID
    ;

stmt
    : expr SEMI #ExprStmt
    | LCURLY stmt* RCURLY #BracketsStmt
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | FOR LPAREN stmt expr SEMI expr RPAREN stmt #ForStmt
    | name=ID EQUALS expr SEMI #AssignStmt //
    | name=ID LRET expr RRET EQUALS expr SEMI #AssignStmt
    | type name=ID EQUALS expr SEMI #AssignStmt
    ;


expr
    : name=ID op=(INC | DEC) #IncDecExpr
    | NOT expr #NotExpr
    | expr op=(MUL  | DIV | REM) expr #BinaryExpr //
    | expr op=(ADD | SUB) expr #BinaryExpr //
    | expr op=(LS | LE | GR | GE) expr #BooleanExpr
    | expr op=(EQ | NEQ) expr #BooleanExpr
    | expr op=AND expr #BooleanExpr
    | expr op=OR expr #BooleanExpr
    | expr LRET expr RRET #AccExpr
    | expr (DOT 'length') #LengthExpr
    | expr (DOT name=ID)* LPAREN (expr (CMA expr)*)? RPAREN #FuncExpr
    | NEW type LRET expr RRET #NewArray
    | NEW name=ID LPAREN expr* RPAREN #NewClassExpr
    | LPAREN expr RPAREN #ParenExpr
    | LRET (expr (CMA expr)*)? RRET #ArrayExpr
    | value=INTEGER #IntegerLiteral //
    | value='true' #BooleanLiteral
    | value='false' #BooleanLiteral
    | name=ID #VarName //
    | value= 'this' #ObjectLiteral
    ;



