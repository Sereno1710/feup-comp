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
MUL : '*' ;
ADD : '+' ;

CLASS : 'class' ;
INT : 'int' ;
STRING : 'String' ;
ARRAY : '[]' ;
BOOLEAN : 'boolean' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
IMPORT : 'import' ;
EXTENDS : 'extends' ;

INTEGER : [0-9] ;
ID : [a-zA-Z]+ ;

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
    : expr op= MUL expr #BinaryExpr //
    | expr op= ADD expr #BinaryExpr //
    | value=INTEGER #IntegerLiteral //
    | name=ID #VarRefExpr //
    ;



