# Gramática de GoLite

## Descripción General

Esta es la gramática formal utilizada en el compilador/intérprete de GoLite, basada en CUP (Constructor of Useful Parsers).

---

## Símbolos Terminales (Tokens)

### Palabras Clave
- `if`, `else`, `for`, `break`, `continue`
- `var`, `func`, `nil`
- `true`, `false`

### Tipos de Datos
- `int`, `float64`, `bool`, `string`, `rune`

### Funciones Built-in
- `fmt.println` - Imprime valores
- `strconv.Atoi` - Convierte string a entero
- `strconv.ParseFloat` - Convierte string a flotante
- `reflect.TypeOf` - Obtiene el tipo de una expresión

### Operadores de Asignación
- `=` - Asignación
- `:=` - Declaración corta
- `+=` - Suma y asignación
- `-=` - Resta y asignación

### Operadores de Comparación
- `==` - Igualdad
- `!=` - Desigualdad
- `<` - Menor que
- `<=` - Menor o igual que
- `>` - Mayor que
- `>=` - Mayor o igual que

### Operadores Lógicos
- `&&` - AND lógico
- `||` - OR lógico
- `!` - NOT lógico

### Operadores Aritméticos
- `+` - Suma
- `-` - Resta
- `*` - Multiplicación
- `/` - División
- `%` - Módulo
- `++` - Incremento
- `--` - Decremento

### Delimitadores
- `(` `)` - Paréntesis
- `{` `}` - Llaves
- `;` - Punto y coma
- `,` - Coma

### Literales
- Identificadores (`id`)
- Números enteros (`integer`)
- Números decimales (`decimal`)
- Cadenas de texto (`string`)

---

## Símbolos No Terminales y Producciones

### Punto de Inicio
```
INICIO → INSTRUCCIONES
```

### Instrucciones
```
INSTRUCCIONES → INSTRUCCIONES INSTRUCCION
             | ε (vacío)
```

### Declaración de Variables

#### Declaración sin inicialización
```
INSTRUCCION → var id : int ;?
            | var id : float64 ;?
            | var id : bool ;?
            | var id : string ;?
            | var id : rune ;?
```

#### Declaración con inicialización (Asignación)
```
INSTRUCCION → var id : int = EXPRESION ;?
            | var id : float64 = EXPRESION ;?
            | var id : bool = EXPRESION ;?
            | var id : string = EXPRESION ;?
            | var id : rune = EXPRESION ;?
```

#### Declaración con inicialización (Walrus)
```
INSTRUCCION → var id : int := EXPRESION ;?
            | var id : float64 := EXPRESION ;?
            | var id : bool := EXPRESION ;?
            | var id : string := EXPRESION ;?
            | var id : rune := EXPRESION ;?
```

### Asignaciones
```
INSTRUCCION → id = EXPRESION ;?
            | id := EXPRESION ;?
            | id += EXPRESION ;?
            | id -= EXPRESION ;?
            | id++ ;?
            | id-- ;?
```

### Estructuras de Control

#### Sentencia If
```
INSTRUCCION → if EXPRESION { INSTRUCCIONES } ELSE_IF_ELSE
```

#### Sentencias If-Else
```
ELSE_IF_ELSE → else if EXPRESION { INSTRUCCIONES } ELSE_IF_ELSE
             | else { INSTRUCCIONES }
             | ε (vacío)
```

#### Ciclo For con Inicializador, Condición e Incremento
```
INSTRUCCION → for SIMPLE_STMT ; EXPRESION ; SIMPLE_STMT { INSTRUCCIONES }
```

#### Ciclo While (For con solo condición)
```
INSTRUCCION → for EXPRESION { INSTRUCCIONES }
```

#### Ciclo Infinito (For sin condición)
```
INSTRUCCION → for { INSTRUCCIONES }
```

#### Break y Continue
```
INSTRUCCION → break ;?
            | continue ;?
```

### Instrucciones Simples
```
SIMPLE_STMT → id = EXPRESION
            | id := EXPRESION
            | id += EXPRESION
            | id -= EXPRESION
            | id++
            | id--
            | EXPRESION
```

### Argumentos de Función
```
ARGS → EXPRESION LISTA_ARGS
     | ε (vacío)

LISTA_ARGS → , EXPRESION LISTA_ARGS
           | ε (vacío)
```

---

## Expresiones

### Operaciones Aritméticas
```
EXPRESION → EXPRESION + EXPRESION
          | EXPRESION - EXPRESION
          | EXPRESION * EXPRESION
          | EXPRESION / EXPRESION
          | EXPRESION % EXPRESION
```

### Operaciones de Comparación
```
EXPRESION → EXPRESION == EXPRESION
          | EXPRESION != EXPRESION
          | EXPRESION < EXPRESION
          | EXPRESION <= EXPRESION
          | EXPRESION > EXPRESION
          | EXPRESION >= EXPRESION
```

### Operaciones Lógicas
```
EXPRESION → EXPRESION && EXPRESION
          | EXPRESION || EXPRESION
          | ! EXPRESION
          | - EXPRESION (operador unario)
```

### Operandos Primarios
```
EXPRESION → true
          | false
          | nil
          | id (referencia a variable)
          | integer (literal entero)
          | decimal (literal decimal)
          | string (literal string)
```

### Llamadas a Funciones Built-in
```
EXPRESION → fmt.println ( ARGS )
          | strconv.Atoi ( EXPRESION )
          | strconv.ParseFloat ( EXPRESION )
          | reflect.TypeOf ( EXPRESION )
```

### Expresiones entre Paréntesis
```
EXPRESION → ( EXPRESION )
```

---

## Precedencia de Operadores

(De menor a mayor precedencia)

| Precedencia | Operadores |
|---|---|
| 1 | `\|\|` (OR lógico) |
| 2 | `&&` (AND lógico) |
| 3 | `==`, `!=` (igualdad) |
| 4 | `<`, `<=`, `>`, `>=` (comparación) |
| 5 | `+`, `-` (suma y resta) |
| 6 | `*`, `/`, `%` (multiplicación, división, módulo) |
| 7 | `!`, `-` (NOT, negación unaria) - **Asociatividad: Derecha** |

---

## Reglas de Asociatividad

- **Asociatividad a la Izquierda**: `||`, `&&`, `==`, `!=`, `<`, `<=`, `>`, `>=`, `+`, `-`, `*`, `/`, `%`
- **Asociatividad a la Derecha**: `!`, negación unaria (`-EXPRESION`)

---

## Notas Importantes

1. El punto y coma (`;`) es **opcional** en las instrucciones (OP_SCOL)
2. Las variables deben ser declaradas con `var` antes de usarlas (excepto con `:=`)
3. El operador `:=` es una declaración corta que infiere el tipo
4. Las estruturas de control (`if`, `for`) usan llaves `{}` para delimitar bloques
5. `nil` es el valor nulo en GoLite
6. Los tipos de datos disponibles son: `int`, `float64`, `bool`, `string`, `rune`
