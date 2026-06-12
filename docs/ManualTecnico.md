# Manual Técnico - Compilador/Intérprete GoLite

## Índice

1. [Introducción](#introducción)
2. [Arquitectura General](#arquitectura-general)
3. [Componentes Principales](#componentes-principales)
4. [Flujo de Compilación e Interpretación](#flujo-de-compilación-e-interpretación)
5. [Estructura del Código](#estructura-del-código)
6. [Patrón de Diseño: Visitor](#patrón-de-diseño-visitor)
7. [Manejo de Errores](#manejo-de-errores)
8. [Guía de Extensión](#guía-de-extensión)

---

## Introducción

El compilador/intérprete de GoLite es una aplicación Java que implementa un análisis completo de un lenguaje simplificado inspirado en Go. La aplicación incluye una interfaz gráfica (GUI) que permite escribir y ejecutar código GoLite en tiempo real.

### Tecnologías Utilizadas

- **JFlex**: Generador de analizadores léxicos
- **CUP**: Generador de analizadores sintácticos (Parser)
- **Swing**: Framework para la interfaz gráfica
- **Maven**: Gestor de dependencias y compilación
- **Java 8+**: Lenguaje de programación base

---

## Arquitectura General

El proyecto sigue una arquitectura en capas clásica de compiladores:

```
┌─────────────────────────────────────────────────────┐
│         Interfaz Gráfica (GUI - Swing)              │
├─────────────────────────────────────────────────────┤
│            Analizador Léxico (Lexer)                │
├─────────────────────────────────────────────────────┤
│         Analizador Sintáctico (Parser)              │
├─────────────────────────────────────────────────────┤
│     Árbol de Sintaxis Abstracta (AST)               │
├─────────────────────────────────────────────────────┤
│    Patrón Visitor - Intérprete/Análisis             │
├─────────────────────────────────────────────────────┤
│         Manejo de Errores y Reportes                │
└─────────────────────────────────────────────────────┘
```

---

## Componentes Principales

### 1. Analizador Léxico (Lexer)

**Ubicación**: `src/main/jflex/lexer.flex`

**Responsabilidad**: Convertir el código fuente en una secuencia de tokens (palabras/símbolos reconocibles).

**Características**:
- Define patrones para reconocer palabras clave (`if`, `for`, `var`, etc.)
- Identifica operadores (`+`, `-`, `*`, `/`, etc.)
- Reconoce literales (números, strings, identificadores)
- Maneja espacios en blanco y comentarios
- Genera la clase `Lexer.java` (autogenerada)

**Tokens Principales**:
```
- Palabras clave: if, else, for, break, continue, var, func, nil, true, false
- Tipos: int, float64, bool, string, rune
- Operadores: +, -, *, /, %, ++, --, ==, !=, <, >, <=, >=, &&, ||, !
- Asignación: =, :=, +=, -=
```

**Ejemplo de Flujo**:
```
Entrada: "var x int = 5;"
Salida (Tokens): [var, x, int, =, 5, ;]
```

---

### 2. Analizador Sintáctico (Parser)

**Ubicación**: `src/main/cup/parser.cup`

**Responsabilidad**: Verificar que la secuencia de tokens sigue las reglas de la gramática y construir el Árbol de Sintaxis Abstracta (AST).

**Características**:
- Utiliza CUP (Constructor of Useful Parsers)
- Define la gramática formal de GoLite
- Construye nodos del AST durante el análisis
- Maneja precedencia y asociatividad de operadores
- Genera la clase `parser.java` (autogenerada)

**Producciones Principales**:
```
INSTRUCCION ::= var id:n kwInt = EXPRESION
              | kwIf EXPRESION { INSTRUCCIONES } ELSE_IF_ELSE
              | kwFor EXPRESION { INSTRUCCIONES }
              | ...

EXPRESION ::= EXPRESION + EXPRESION
            | EXPRESION * EXPRESION
            | id
            | integer
            | ...
```

**Ejemplo de Flujo**:
```
Entrada (Tokens): [var, x, int, =, 5, ;]
Parsing: Reconoce la regla "var id int = EXPRESION"
Salida (AST): VarDecl(id="x", type="int", value=Integers(5))
```

---

### 3. Árbol de Sintaxis Abstracta (AST)

**Ubicación**: `src/main/java/com/olc1/ast/`

**Responsabilidad**: Representar la estructura jerárquica del programa en forma de árbol.

**Estructura**:

```
ASTNode (Interfaz)
├── Nodos de Expresiones (ast/exp/)
│   ├── Operaciones Aritméticas: Add, Sub, Mul, Div, Mod, Negate
│   ├── Operaciones Lógicas: And, Or, Not
│   ├── Operaciones de Comparación: Equ, Nequ, Lesser, Greater, LesserEqu, GreaterEqu
│   ├── Literales: Integers, Decimal, StringLiteral, BoolLiteral, NilLiteral
│   ├── Referencias: VarRef
│   └── Llamadas Built-in: FmtPrintln, StrconvAtoi, StrconvParseFloat, ReflectTypeOf
│
└── Nodos de Instrucciones (ast/stm/)
    ├── Declaraciones: VarDecl, ShortDecl
    ├── Asignaciones: Assign, PlusAssign, MinusAssign
    ├── Control de Flujo: IfNode, ForNode, ForWhileNode, BreakNode, ContinueNode
    ├── Partes Condicionales: ElseIfPart, ElsePart
    ├── Contenedores: Statments, ExprStatment
    └── Salida: Imprimir
```

**Característica Principal - Patrón Visitor**:

Cada nodo implementa:
```java
public class Add implements ASTNode {
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
```

---

### 4. Patrón Visitor

**Ubicación**: `src/main/java/com/olc1/visitor/`

**Responsabilidad**: Procesar el AST sin modificar su estructura. Permite separar algoritmos de la estructura de datos.

**Estructura**:

```
Visitor<T> (Interfaz)
├── visit(Integers.Context ctx): T
├── visit(Add.Context ctx): T
├── visit(IfNode.Context ctx): T
├── ... (métodos para todos los nodos)
│
└── InterpreterVisitor implements Visitor<Value>
    ├── visit(Add.Context ctx): Value → suma dos valores
    ├── visit(VarRef.Context ctx): Value → obtiene valor de variable
    ├── visit(IfNode.Context ctx): Value → ejecuta rama condicional
    └── ... (implementaciones del intérprete)
```

**Flujo de Ejecución con Visitor**:

```
1. Cada nodo AST acepta un Visitor
2. El nodo llama al método correspondiente del Visitor
3. El Visitor procesa el nodo y retorna un resultado
4. Los visitantes pueden mantener estado (variables, símbolos, etc.)
```

**Ejemplo**:
```java
// En el AST (Add.java)
Add node = new Add(left, right);
Value result = node.accept(interpreter); // Llama a interpreter.visit(this)

// En InterpreterVisitor
public Value visit(Add.Context ctx) {
    Value left = ctx.left.accept(this);
    Value right = ctx.right.accept(this);
    return new NumValue(left.toNumber() + right.toNumber());
}
```

---

### 5. Intérprete (InterpreterVisitor)

**Ubicación**: `src/main/java/com/olc1/visitor/interpreter/InterpreterVisitor.java`

**Responsabilidad**: Ejecutar el programa GoLite evaluando el AST.

**Características**:
- Mantiene tabla de símbolos (variables)
- Mantiene stack de valores
- Controla flujo (break, continue, return)
- Implementa todas las operaciones

**Componentes Clave**:

```java
public class InterpreterVisitor implements Visitor<Value> {
    // Tabla de símbolos: almacena variables y sus valores
    private Map<String, Value> symbolTable;
    
    // Estado de control de flujo
    private boolean breakFlag = false;
    private boolean continueFlag = false;
    
    // Implementa visit() para cada tipo de nodo
    public Value visit(VarDecl.Context ctx) {
        // Declara una variable
        symbolTable.put(ctx.name, ctx.initialValue);
    }
    
    public Value visit(Add.Context ctx) {
        // Suma dos expresiones
        return left.add(right);
    }
    
    // ... más métodos visit()
}
```

**Clases de Valores**:

```
Value (Interfaz)
├── IntValue: representa números enteros
├── FloatValue: representa números flotantes
├── BoolValue: representa valores booleanos
├── StringValue: representa cadenas de texto
└── NilValue: representa valor nulo
```

---

### 6. Interfaz Gráfica (GUI)

**Ubicación**: `src/main/java/com/olc1/gui/`

**Componentes**:

#### GoliteFrame.java
```
Ventana principal que integra:
- EditorPanel: área de edición de código
- ConsoleTextArea: salida de ejecución
- GoliteMenuBar: menú de opciones
```

#### EditorPanel.java
```
Panel que contiene:
- JTextArea: editor de texto
- Numeración de líneas
- Resaltado de sintaxis (opcional)
```

#### GoliteMenuBar.java
```
Menú con opciones:
- Run: ejecuta el código actual
- Clean: limpia la consola
- New: nuevo archivo
- Exit: salir de la aplicación
```

#### MainPanel.java
```
Panel contenedor que organiza:
- Editor (lado izquierdo)
- Consola (lado derecho)
```

**Flujo de Ejecución en GUI**:

```
Usuario escribe código → Click en "Run" → 
    Lexer analiza → Parser construye AST → 
    Interpreter ejecuta AST → 
    Resultado aparece en consola
```

---

### 7. Manejo de Errores

**Ubicación**: `src/main/java/com/olc1/reports/`

**Componentes**:

#### ErrorCollector.java
```java
public class ErrorCollector {
    // Colecciona todos los errores
    public static void addError(String type, String desc, int line, int col)
    
    // Recupera errores
    public static List<GoLiteError> getErrors()
    
    // Limpia la lista
    public static void clear()
}
```

#### GoLiteError.java
```java
public class GoLiteError {
    String type;      // "léxico", "sintáctico", "semántico"
    String desc;      // Descripción del error
    int line;         // Línea donde ocurrió
    int col;          // Columna donde ocurrió
}
```

#### SymbolEntry.java
```java
// Información de símbolos (variables, funciones, etc.)
public class SymbolEntry {
    String name;
    String type;
    Object value;
}
```

**Tipos de Errores**:

1. **Errores Léxicos**: Token no reconocido por el Lexer
2. **Errores Sintácticos**: Violación de las reglas gramaticales
3. **Errores Semánticos**: Variable no declarada, tipo incompatible, etc.

---

## Flujo de Compilación e Interpretación

### Paso 1: Análisis Léxico

```
Código Fuente
    ↓
┌─────────────┐
│   Lexer     │  (Generado por JFlex)
└─────────────┘
    ↓
Secuencia de Tokens
```

### Paso 2: Análisis Sintáctico

```
Secuencia de Tokens
    ↓
┌─────────────┐
│   Parser    │  (Generado por CUP)
└─────────────┘
    ↓
Árbol de Sintaxis Abstracta (AST)
```

### Paso 3: Interpretación

```
AST
    ↓
┌──────────────────────────┐
│  InterpreterVisitor      │  (Patrón Visitor)
│  - Tabla de símbolos     │
│  - Valores               │
└──────────────────────────┘
    ↓
Ejecución y Resultado
```

### Ejemplo Completo

**Código GoLite**:
```go
var x int = 5;
var y int = 3;
fmt.Println(x + y);
```

**Fase 1 - Lexer**:
```
[var] [x] [int] [=] [5] [;]
[var] [y] [int] [=] [3] [;]
[fmt.println] [(] [x] [+] [y] [)] [;]
```

**Fase 2 - Parser**:
```
Statments
├── VarDecl(x, int, Integers(5))
├── VarDecl(y, int, Integers(3))
└── ExprStatment(FmtPrintln([Add(VarRef(x), VarRef(y))]))
```

**Fase 3 - Interpreter**:
```
1. Declarar x = 5
2. Declarar y = 3
3. Evaluar Add(5, 3) = 8
4. Llamar fmt.Println(8)
5. Resultado: "8" en consola
```

---

## Estructura del Código

```
src/main/java/com/olc1/
├── App.java                          (Punto de entrada)
├── Lexer.java                        (Generado por JFlex)
├── parser.java                       (Generado por CUP)
├── sym.java                          (Símbolos generados por CUP)
│
├── ast/                              (Nodos del Árbol Sintáctico)
│   ├── ASTNode.java                  (Interfaz base)
│   ├── exp/                          (Nodos de expresiones)
│   │   ├── Add.java, Sub.java, ...   (Operaciones binarias)
│   │   ├── Integers.java, StringLiteral.java  (Literales)
│   │   └── VarRef.java               (Referencias a variables)
│   └── stm/                          (Nodos de instrucciones)
│       ├── VarDecl.java              (Declaración de variables)
│       ├── Assign.java               (Asignación)
│       ├── IfNode.java, ForNode.java (Control de flujo)
│       └── Statments.java            (Contenedor de instrucciones)
│
├── visitor/                          (Patrón Visitor)
│   ├── Visitor.java                  (Interfaz Visitor)
│   └── interpreter/
│       ├── InterpreterVisitor.java   (Implementación del intérprete)
│       └── value/
│           ├── Value.java            (Interfaz de valores)
│           ├── IntValue.java         (Valor entero)
│           ├── FloatValue.java       (Valor flotante)
│           ├── BoolValue.java        (Valor booleano)
│           ├── StringValue.java      (Valor string)
│           └── NilValue.java         (Valor nulo)
│
├── gui/                              (Interfaz Gráfica)
│   ├── GoliteFrame.java              (Ventana principal)
│   ├── EditorPanel.java              (Panel editor)
│   ├── GoliteMenuBar.java            (Menú)
│   └── MainPanel.java                (Panel principal)
│
└── reports/                          (Manejo de Errores)
    ├── ErrorCollector.java           (Recolector de errores)
    ├── GoLiteError.java              (Definición de error)
    └── SymbolEntry.java              (Entrada de símbolo)

src/main/cup/
└── parser.cup                        (Definición de gramática CUP)

src/main/jflex/
└── lexer.flex                        (Definición de Lexer JFlex)
```

---

## Patrón de Diseño: Visitor

### ¿Por qué Visitor?

El patrón Visitor permite:
1. **Separación de responsabilidades**: El AST no contiene lógica de interpretación
2. **Extensibilidad**: Se pueden añadir nuevos visitantes sin modificar el AST
3. **Operaciones múltiples**: Diferentes visitantes pueden hacer diferentes análisis

### Implementación

**1. Interfaz Visitor**:
```java
public interface Visitor<T> {
    T visit(Add.Context ctx);
    T visit(VarRef.Context ctx);
    T visit(IfNode.Context ctx);
    // ... más métodos
}
```

**2. Nodos del AST**:
```java
public class Add implements ASTNode {
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
```

**3. Visitante Concreto**:
```java
public class InterpreterVisitor implements Visitor<Value> {
    public Value visit(Add.Context ctx) {
        Value left = ctx.left.accept(this);
        Value right = ctx.right.accept(this);
        return left.add(right);
    }
}
```

**4. Uso**:
```java
ASTNode ast = parser.parse(code);
Value result = ast.accept(new InterpreterVisitor());
```

---

## Manejo de Errores

### Tipos de Errores y Recuperación

#### 1. Errores Léxicos
```
Causa: Carácter no reconocido
Ejemplo: "@variable" (@ no válido)
Localización: Lexer.flex
Acción: Se reporta pero se intenta continuar
```

#### 2. Errores Sintácticos
```
Causa: Violación de gramática
Ejemplo: "if (x) { }" sin expresión booleana
Localización: parser.cup
Acción: Se reporta mediante parser.syntax_error()
```

#### 3. Errores Semánticos
```
Causa: Variable no declarada, tipo incorrecto
Ejemplo: "x + 5" cuando x no está declarada
Localización: InterpreterVisitor
Acción: Se reporta durante la interpretación
```

### Reporte de Errores

```java
// Recolectar error
ErrorCollector.addError("semántico", 
    "Variable 'x' no declarada", 
    line, column);

// Recuperar errores
List<GoLiteError> errors = ErrorCollector.getErrors();

// Mostrar en GUI
for (GoLiteError error : errors) {
    console.append(error.toString() + "\n");
}
```

---

## Guía de Extensión

### Agregar un Nuevo Operador

**1. Lexer (lexer.flex)**:
```
Agregar patrón del operador
```

**2. Parser (parser.cup)**:
```
Agregar terminal en declaraciones
Agregar producción en EXPRESION
```

**3. Nodo AST**:
```java
public class MyOp implements ASTNode {
    private ASTNode left, right;
    
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
```

**4. Interfaz Visitor**:
```java
public interface Visitor<T> {
    T visit(MyOp.Context ctx);
}
```

**5. Implementación en InterpreterVisitor**:
```java
public Value visit(MyOp.Context ctx) {
    Value left = ctx.left.accept(this);
    Value right = ctx.right.accept(this);
    return performMyOperation(left, right);
}
```

### Agregar una Nueva Instrucción

Seguir los mismos pasos pero con nodos de instrucción (`stm/`).

---

## Compilación y Generación

### Archivos Generados

**Por JFlex** (de `lexer.flex`):
- `Lexer.java`

**Por CUP** (de `parser.cup`):
- `parser.java`
- `sym.java`

### Comando de Compilación

```bash
mvn clean compile
```

Esto ejecuta JFlex y CUP de forma automática.

---

## Notas Importantes

1. **Thread Safety**: ErrorCollector usa static, considerare thread-safety si se expande
2. **Memoria**: InterpreterVisitor mantiene tabla de símbolos en memoria
3. **Rendimiento**: No hay optimización de bytecode ni JIT
4. **Alcance**: Las variables son globales (sin alcance de bloques anidados)
5. **Recursión**: No soportada actualmente (sin funciones)

---

## Conclusión

El compilador/intérprete de GoLite utiliza los patrones y herramientas estándar de compiladores:
- **Lexer** para análisis léxico
- **Parser** para análisis sintáctico  
- **AST** para representación intermedia
- **Visitor** para procesamiento
- **GUI** para usabilidad

La arquitectura es extensible y sigue buenas prácticas de ingeniería de software.
