package com.olc1.visitor.interpreter.value;

public sealed interface ValueWrapper 
    permits IntValue, DecimalValue, VoidValue, BoolValue, StringValue {
    int line();
    int column();
    String getTypeName();
}
