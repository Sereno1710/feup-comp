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

CLASS : 'class' ;
INT : 'int' ;
STRING : 'String' ;
ARRAY : '[]' ;
BOOLEAN : 'boolean' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
IMPORT : 'import' ;
EXTENDS : 'extends' ;

INTEGER: '0' | [1-9][0-9]*;
ID: [a-zA-Z_$] [a-zA-Z_0-9$]*;

WS : [ \t\n\r\f]+ -> skip ;


program
    : importDecl*
        classDecl EOF
    ;

importDecl
    : IMPORT ID('.'ID)* SEMI ;


classDecl
    : CLASS name=ID (EXTENDS name=ID)?
        LCURLY
        mainMethod?
        (varDecl | methodDecl)*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    | type ID SEMI
    ;

type
    : name=INT(ARRAY)?
    | name=STRING(ARRAY)?
    | name=BOOLEAN
    | name=ID
    ;

mainMethod
    : 'static' 'void' 'main' LPAREN type ID RPAREN
        LCURLY
        varDecl*
        RCURLY
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : type name=ID
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt //
    | RETURN expr SEMI #ReturnStmt
    ;

expr
    : LPAREN expr RPAREN #Paren
    | name=ID op=(INC | DEC) #IncDec
    | value = NOT expr #Unary
    | expr op=(MUL  | DIV) expr #BinaryExpr //
    | expr op=(ADD | SUB) expr #BinaryExpr //
    | value=INTEGER #IntegerLiteral //
    | name=ID #VarRefExpr //
    ;



