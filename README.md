# OLC1V1S_Proyecto_202405365

## Descripción General

Este proyecto es un compilador/intérprete para el lenguaje **GoLite**, un lenguaje simplificado inspirado en Go. El proyecto implementa un análisis léxico y sintáctico completo, junto con un intérprete que puede ejecutar programas escritos en GoLite.

### Características Principales

- **Análisis Léxico**: Realizado con JFlex (lexer.flex)
- **Análisis Sintáctico**: Realizado con CUP (parser.cup)
- **Árbol de Sintaxis Abstracta (AST)**: Estructura de nodos para representar el programa
- **Intérprete**: Evaluación y ejecución de programas GoLite
- **Interfaz Gráfica**: Editor integrado con panel de visualización
- **Reportes de Errores**: Recolección y presentación de errores semánticos y sintácticos

## Cómo Ejecutar

### Requisitos Previos

- Java 8 o superior
- Maven 3.6 o superior

### Pasos para Ejecutar

1. **Compilar el proyecto:**
   ```bash
   mvn clean compile
   ```

2. **Ejecutar la aplicación:**
   ```bash
   mvn exec:java -Dexec.mainClass="com.olc1.App"
   ```

   O si la aplicación se empaqueta como JAR:
   ```bash
   mvn package
   java -jar target/golite.jar
   ```

3. **Ejecutar pruebas:**
   ```bash
   mvn test
   ```

### Estructura del Proyecto

- `src/main/jflex/`: Definición del analizador léxico
- `src/main/cup/`: Definición del analizador sintáctico
- `src/main/java/com/olc1/`: Código principal
  - `ast/`: Clases del árbol de sintaxis abstracta
  - `gui/`: Interfaz gráfica de usuario
  - `visitor/`: Patrón visitor para recorrer el AST
  - `reports/`: Manejo de errores y reportes
