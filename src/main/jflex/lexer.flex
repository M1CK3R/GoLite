package com.olc1;

import java.util.ArrayList;
import java.util.List;

// Importaciones necesarias
import java_cup.runtime.Symbol;

import com.olc1.reports.GoLiteError;

%%

// Configuración de JFLEX
%cup //Indicamos que vamos a usar CUP
// Nombre de la clase del lexer
%class Lexer 
%public // Paquete del lexer
%line // conteo de lienas
%column // conteo de columnas
%unicode  // soporte completo Unicode (necesario para caracteres fuera de ASCII)
// %debug // Habilitar modo debug para ver el proceso de tokenización

%{
    // private Symbol symbol(int type) {
    //     return new Symbol(type, yyline, yycolumn);
    // }

    // private Symbol symbol(int type, Object value) {
    //     return new Symbol(type, yyline, yycolumn, value);
    // }

    public final List<GoLiteError> errors = new ArrayList<>();

%}

%init{
    yyline = 1;
    yycolumn = 1;
%init}

%eofval{
    return new Symbol(sym.EOF, yyline, yycolumn, yytext());
%eofval}

// Definición de patrones léxicos
digit = [0-9]
letter = [a-zA-Z_]
whitespace = [\ \r\t\f\n]+
escape_char = \\ [\"\\nrt]
normal_char = [^\"\\\n\r]
str_lex = ({normal_char} | {escape_char})*
rune_lex = '([^'\\\n\r]|\\.)'
newline = \n

%%

// COmentarios
"//" [^\n]*          { /* Ignorar comentarios de una línea */ }
"/*" [^*]* "*"+ ([^*/] [^*]* "*"+)* "/" { /* Ignorar comentarios multilínea */ }

// Numeros
{digit}+\.{digit}+  { return new Symbol(sym.decimal, yyline, yycolumn, yytext()); }
{digit}+            { return new Symbol(sym.integer, yyline, yycolumn, yytext()); }

// Simbolos
"("     { return new Symbol(sym.lparen, yyline, yycolumn, yytext()); }
")"     { return new Symbol(sym.rparen, yyline, yycolumn, yytext()); }
"{"     { return new Symbol(sym.lbrace, yyline, yycolumn, yytext()); }
"}"     { return new Symbol(sym.rbrace, yyline, yycolumn, yytext()); }
";"     { return new Symbol(sym.scol, yyline, yycolumn, yytext()); }
":"     { return new Symbol(sym.colon, yyline, yycolumn, yytext()); }
","     { return new Symbol(sym.comma, yyline, yycolumn, yytext()); }
"++"    { return new Symbol(sym.plusplus, yyline, yycolumn, yytext()); }
"--"    { return new Symbol(sym.minusminus, yyline, yycolumn, yytext()); }
"+"     { return new Symbol(sym.plus, yyline, yycolumn, yytext()); }
"-"     { return new Symbol(sym.minus, yyline, yycolumn, yytext()); }
"*"     { return new Symbol(sym.times, yyline, yycolumn, yytext()); }
"/"     { return new Symbol(sym.slash, yyline, yycolumn, yytext()); }
"%"     { return new Symbol(sym.mod, yyline, yycolumn, yytext()); }
"["     { return new Symbol(sym.lbracket, yyline, yycolumn, yytext()); }
"]"     { return new Symbol(sym.rbracket, yyline, yycolumn, yytext()); }
"."     { return new Symbol(sym.dot, yyline, yycolumn, yytext()); }

// Para los de asignacion
"="     { return new Symbol(sym.assign, yyline, yycolumn, yytext()); }
":="    { return new Symbol(sym.walrus_assign, yyline, yycolumn, yytext()); }
"+="    { return new Symbol(sym.plus_assign, yyline, yycolumn, yytext()); }
"-="    { return new Symbol(sym.minus_assign, yyline, yycolumn, yytext()); }

// Para los de comparacion
"=="    { return new Symbol(sym.eq, yyline, yycolumn, yytext());  }
"!="    { return new Symbol(sym.neq, yyline, yycolumn, yytext()); }
"<"     { return new Symbol(sym.lt, yyline, yycolumn, yytext());  }
"<="    { return new Symbol(sym.leq, yyline, yycolumn, yytext()); }
">"     { return new Symbol(sym.gt, yyline, yycolumn, yytext());  }
">="    { return new Symbol(sym.geq, yyline, yycolumn, yytext()); }

// Logicos
"&&"    { return new Symbol(sym.and, yyline, yycolumn, yytext()); }
"||"    { return new Symbol(sym.or, yyline, yycolumn, yytext()); }
"!"     { return new Symbol(sym.not, yyline, yycolumn, yytext()); }

// Key Words
"true"      { return new Symbol(sym.kwTrue,    yyline, yycolumn, yytext()); }
"false"     { return new Symbol(sym.kwFalse,   yyline, yycolumn, yytext()); }
"if"        { return new Symbol(sym.kwIf,      yyline, yycolumn, yytext()); }
"else"      { return new Symbol(sym.kwElse,    yyline, yycolumn, yytext()); }
"var"       { return new Symbol(sym.kwVar,     yyline, yycolumn, yytext()); }
"func"      { return new Symbol(sym.kwFunc,    yyline, yycolumn, yytext()); }
"nil"       { return new Symbol(sym.kwNil,     yyline, yycolumn, yytext()); }
"for"       { return new Symbol(sym.kwFor,      yyline, yycolumn, yytext()); }
"break"     { return new Symbol(sym.kwBreak,    yyline, yycolumn, yytext()); }
"continue"  { return new Symbol(sym.kwContinue, yyline, yycolumn, yytext()); }
"switch"    { return new Symbol(sym.kwSwitch,   yyline, yycolumn, yytext()); }
"case"      { return new Symbol(sym.kwCase,     yyline, yycolumn, yytext()); }
"default"   { return new Symbol(sym.kwDefault,  yyline, yycolumn, yytext()); }
"type"      { return new Symbol(sym.kwType,     yyline, yycolumn, yytext()); }
"struct"    { return new Symbol(sym.kwStruct,   yyline, yycolumn, yytext()); }
"return"    { return new Symbol(sym.kwReturn,   yyline, yycolumn, yytext()); }
"append"    { return new Symbol(sym.kwAppend,   yyline, yycolumn, yytext()); }

// Tipos de datos
"int"       { return new Symbol(sym.kwInt,     yyline, yycolumn, yytext()); }
"float64"   { return new Symbol(sym.kwFloat,   yyline, yycolumn, yytext()); }
"bool"      { return new Symbol(sym.kwBool,    yyline, yycolumn, yytext()); }
"string"    { return new Symbol(sym.kwString,  yyline, yycolumn, yytext()); }
"rune"      { return new Symbol(sym.kwRune,    yyline, yycolumn, yytext()); }

// Funciones
"fmt.Println"               { return new Symbol(sym.fmt_println, yyline, yycolumn, yytext()); }
"strconv.Atoi"              { return new Symbol(sym.strconv_atoi, yyline, yycolumn, yytext()); }
"strconv.ParseFloat"        { return new Symbol(sym.strconv_parsefloat, yyline, yycolumn, yytext()); }
"reflect.TypeOf"            { return new Symbol(sym.reflect_typeof, yyline, yycolumn, yytext()); }
"slices.Index"              { return new Symbol(sym.kwSlicesIndex, yyline, yycolumn, yytext()); }
"strings.Join"              { return new Symbol(sym.kwStringsJoin, yyline, yycolumn, yytext()); }
"len"                       { return new Symbol(sym.kwLen, yyline, yycolumn, yytext()); }

// ID - String
{letter}({letter}|{digit})* { return new Symbol(sym.id, yyline, yycolumn, yytext()); }
\"{str_lex}\"               { return new Symbol(sym.string, yyline, yycolumn, yytext().substring(1, yytext().length() - 1)); }
{rune_lex}                  { return new Symbol(sym.rune, yyline, yycolumn, yytext()); }


// Ignorar
{whitespace}    {/* pass */}

// Errores lexicos
.               { errors.add(new GoLiteError("léxico", "Caracter no reconocido: " + yytext(), yyline, yycolumn)); }