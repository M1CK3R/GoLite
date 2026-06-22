package com.olc1;

import java.util.ArrayList;
import java.util.List;

// Importaciones necesarias
import java_cup.runtime.Symbol;

import com.olc1.reports.GoLiteError;
import com.olc1.reports.TokenEntry;

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
    public final List<TokenEntry> tokens = new ArrayList<>();

    private Symbol track(int type, int line, int col, String text) {
        String typeName = (type >= 0 && type < sym.terminalNames.length)
            ? sym.terminalNames[type] : "UNKNOWN";
        tokens.add(new TokenEntry(typeName, text, line, col));
        return new Symbol(type, line, col, text);
    }

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
{digit}+\.{digit}+  { return track(sym.decimal, yyline, yycolumn, yytext()); }
{digit}+            { return track(sym.integer, yyline, yycolumn, yytext()); }

// Simbolos
"("     { return track(sym.lparen, yyline, yycolumn, yytext()); }
")"     { return track(sym.rparen, yyline, yycolumn, yytext()); }
"{"     { return track(sym.lbrace, yyline, yycolumn, yytext()); }
"}"     { return track(sym.rbrace, yyline, yycolumn, yytext()); }
";"     { return track(sym.scol, yyline, yycolumn, yytext()); }
":"     { return track(sym.colon, yyline, yycolumn, yytext()); }
","     { return track(sym.comma, yyline, yycolumn, yytext()); }
"++"    { return track(sym.plusplus, yyline, yycolumn, yytext()); }
"--"    { return track(sym.minusminus, yyline, yycolumn, yytext()); }
"+"     { return track(sym.plus, yyline, yycolumn, yytext()); }
"-"     { return track(sym.minus, yyline, yycolumn, yytext()); }
"*"     { return track(sym.times, yyline, yycolumn, yytext()); }
"/"     { return track(sym.slash, yyline, yycolumn, yytext()); }
"%"     { return track(sym.mod, yyline, yycolumn, yytext()); }
"["     { return track(sym.lbracket, yyline, yycolumn, yytext()); }
"]"     { return track(sym.rbracket, yyline, yycolumn, yytext()); }
"."     { return track(sym.dot, yyline, yycolumn, yytext()); }

// Para los de asignacion
"="     { return track(sym.assign, yyline, yycolumn, yytext()); }
":="    { return track(sym.walrus_assign, yyline, yycolumn, yytext()); }
"+="    { return track(sym.plus_assign, yyline, yycolumn, yytext()); }
"-="    { return track(sym.minus_assign, yyline, yycolumn, yytext()); }

// Para los de comparacion
"=="    { return track(sym.eq, yyline, yycolumn, yytext());  }
"!="    { return track(sym.neq, yyline, yycolumn, yytext()); }
"<"     { return track(sym.lt, yyline, yycolumn, yytext());  }
"<="    { return track(sym.leq, yyline, yycolumn, yytext()); }
">"     { return track(sym.gt, yyline, yycolumn, yytext());  }
">="    { return track(sym.geq, yyline, yycolumn, yytext()); }

// Logicos
"&&"    { return track(sym.and, yyline, yycolumn, yytext()); }
"||"    { return track(sym.or, yyline, yycolumn, yytext()); }
"!"     { return track(sym.not, yyline, yycolumn, yytext()); }

// Key Words
"true"      { return track(sym.kwTrue,    yyline, yycolumn, yytext()); }
"false"     { return track(sym.kwFalse,   yyline, yycolumn, yytext()); }
"if"        { return track(sym.kwIf,      yyline, yycolumn, yytext()); }
"else"      { return track(sym.kwElse,    yyline, yycolumn, yytext()); }
"var"       { return track(sym.kwVar,     yyline, yycolumn, yytext()); }
"func"      { return track(sym.kwFunc,    yyline, yycolumn, yytext()); }
"nil"       { return track(sym.kwNil,     yyline, yycolumn, yytext()); }
"for"       { return track(sym.kwFor,      yyline, yycolumn, yytext()); }
"range"     { return track(sym.kwRange,    yyline, yycolumn, yytext()); }
"break"     { return track(sym.kwBreak,    yyline, yycolumn, yytext()); }
"continue"  { return track(sym.kwContinue, yyline, yycolumn, yytext()); }
"switch"    { return track(sym.kwSwitch,   yyline, yycolumn, yytext()); }
"case"      { return track(sym.kwCase,     yyline, yycolumn, yytext()); }
"default"   { return track(sym.kwDefault,  yyline, yycolumn, yytext()); }
"type"      { return track(sym.kwType,     yyline, yycolumn, yytext()); }
"struct"    { return track(sym.kwStruct,   yyline, yycolumn, yytext()); }
"return"    { return track(sym.kwReturn,   yyline, yycolumn, yytext()); }
"append"    { return track(sym.kwAppend,   yyline, yycolumn, yytext()); }

// Tipos de datos
"int"       { return track(sym.kwInt,     yyline, yycolumn, yytext()); }
"float64"   { return track(sym.kwFloat,   yyline, yycolumn, yytext()); }
"bool"      { return track(sym.kwBool,    yyline, yycolumn, yytext()); }
"string"    { return track(sym.kwString,  yyline, yycolumn, yytext()); }
"rune"      { return track(sym.kwRune,    yyline, yycolumn, yytext()); }

// Funciones
"fmt.Println"               { return track(sym.fmt_println, yyline, yycolumn, yytext()); }
"strconv.Atoi"              { return track(sym.strconv_atoi, yyline, yycolumn, yytext()); }
"strconv.ParseFloat"        { return track(sym.strconv_parsefloat, yyline, yycolumn, yytext()); }
"reflect.TypeOf"            { return track(sym.reflect_typeof, yyline, yycolumn, yytext()); }
"slices.Index"              { return track(sym.kwSlicesIndex, yyline, yycolumn, yytext()); }
"strings.Join"              { return track(sym.kwStringsJoin, yyline, yycolumn, yytext()); }
"len"                       { return track(sym.kwLen, yyline, yycolumn, yytext()); }

// ID - String
{letter}({letter}|{digit})* { return track(sym.id, yyline, yycolumn, yytext()); }
\"{str_lex}\"               { String raw = yytext(); tokens.add(new TokenEntry("string", raw.substring(1, raw.length() - 1), yyline, yycolumn)); return new Symbol(sym.string, yyline, yycolumn, raw.substring(1, raw.length() - 1)); }
{rune_lex}                  { return track(sym.rune, yyline, yycolumn, yytext()); }


// Ignorar
{whitespace}    {/* pass */}

// Errores lexicos
.               { errors.add(new GoLiteError("léxico", "Caracter no reconocido: " + yytext(), yyline, yycolumn)); }