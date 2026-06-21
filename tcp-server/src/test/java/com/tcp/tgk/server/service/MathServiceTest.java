package com.tcp.tgk.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MathService.
 *
 * The command protocol delivers the expression to calculate() already stripped
 * of the leading "m" token. So calculate() receives strings like:
 *   "5 + 10"          (normal spacing)
 *   "5+10"            (no spaces at all)
 *   "5  +  10"        (multiple spaces)
 *   "  5   +   10  "  (leading/trailing spaces – trimmed by CommandHandler before reaching here)
 */
class MathServiceTest {

    private MathService mathService;

    @BeforeEach
    void setUp() {
        mathService = new MathService();
    }

    // -------------------------------------------------------------------------
    // Spacing variants
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Normal single-space spacing: '5 + 10'")
    void normalSpacing() {
        assertEquals("RESULT: 15", mathService.calculate("5 + 10"));
    }

    @Test
    @DisplayName("No spaces between tokens: '5+10'")
    void noSpaces() {
        assertEquals("RESULT: 15", mathService.calculate("5+10"));
    }

    @Test
    @DisplayName("Multiple spaces between tokens: '5   +   10'")
    void multipleSpaces() {
        assertEquals("RESULT: 15", mathService.calculate("5   +   10"));
    }

    @Test
    @DisplayName("Leading whitespace before first number: '  5 + 10'")
    void leadingWhitespace() {
        assertEquals("RESULT: 15", mathService.calculate("  5 + 10"));
    }

    @Test
    @DisplayName("Trailing whitespace after second number: '5 + 10  '")
    void trailingWhitespace() {
        assertEquals("RESULT: 15", mathService.calculate("5 + 10  "));
    }

    @Test
    @DisplayName("Spaces only around operator: '5+ 10'")
    void spaceAfterOp() {
        assertEquals("RESULT: 15", mathService.calculate("5+ 10"));
    }

    @Test
    @DisplayName("Space before operator only: '5 +10'")
    void spaceBeforeOp() {
        assertEquals("RESULT: 15", mathService.calculate("5 +10"));
    }

    // -------------------------------------------------------------------------
    // All operators
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Subtraction: '20 - 7'")
    void subtraction() {
        assertEquals("RESULT: 13", mathService.calculate("20 - 7"));
    }

    @Test
    @DisplayName("Multiplication: '6 * 7'")
    void multiplication() {
        assertEquals("RESULT: 42", mathService.calculate("6 * 7"));
    }

    @Test
    @DisplayName("Division exact: '10 / 2'")
    void divisionExact() {
        assertEquals("RESULT: 5", mathService.calculate("10 / 2"));
    }

    @Test
    @DisplayName("Division with decimal result: '10 / 3'")
    void divisionDecimal() {
        String result = mathService.calculate("10 / 3");
        assertTrue(result.startsWith("RESULT:"), "Should produce a RESULT, got: " + result);
        double val = Double.parseDouble(result.replace("RESULT:", "").trim());
        assertEquals(10.0 / 3.0, val, 1e-9);
    }

    @Test
    @DisplayName("Modulo: '10 % 3'")
    void modulo() {
        assertEquals("RESULT: 1", mathService.calculate("10 % 3"));
    }

    @Test
    @DisplayName("Power: '2 ^ 8'")
    void power() {
        assertEquals("RESULT: 256", mathService.calculate("2 ^ 8"));
    }

    // -------------------------------------------------------------------------
    // Decimal numbers
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Decimal operands: '1.5 + 2.5'")
    void decimalOperands() {
        assertEquals("RESULT: 4", mathService.calculate("1.5 + 2.5"));
    }

    @Test
    @DisplayName("Decimal result preserved: '1.1 + 2.2'")
    void decimalResult() {
        String result = mathService.calculate("1.1 + 2.2");
        assertTrue(result.startsWith("RESULT:"), "Expected RESULT, got: " + result);
    }

    // -------------------------------------------------------------------------
    // Error cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Division by zero")
    void divisionByZero() {
        assertEquals("ERROR: Division by zero", mathService.calculate("5 / 0"));
    }

    @Test
    @DisplayName("Modulo by zero")
    void moduloByZero() {
        assertEquals("ERROR: Modulo by zero", mathService.calculate("5 % 0"));
    }

    @Test
    @DisplayName("Unknown operator '&'")
    void unknownOperator() {
        String result = mathService.calculate("5 & 10");
        assertTrue(result.startsWith("ERROR: Unknown operator"), "Expected unknown operator error, got: " + result);
    }

    @Test
    @DisplayName("Non-numeric first operand")
    void invalidFirstNumber() {
        String result = mathService.calculate("abc + 10");
        assertTrue(result.startsWith("ERROR:"), "Expected error, got: " + result);
    }

    @Test
    @DisplayName("Non-numeric second operand")
    void invalidSecondNumber() {
        String result = mathService.calculate("10 + xyz");
        assertTrue(result.startsWith("ERROR:"), "Expected error, got: " + result);
    }

    @Test
    @DisplayName("Empty expression")
    void emptyExpression() {
        String result = mathService.calculate("");
        assertTrue(result.startsWith("ERROR:"), "Expected error, got: " + result);
    }

    @Test
    @DisplayName("Null expression")
    void nullExpression() {
        String result = mathService.calculate(null);
        assertTrue(result.startsWith("ERROR:"), "Expected error, got: " + result);
    }

    @Test
    @DisplayName("Missing operator (only one token)")
    void missingOperator() {
        String result = mathService.calculate("42");
        assertTrue(result.startsWith("ERROR:"), "Expected error, got: " + result);
    }

    @Test
    @DisplayName("Missing second number")
    void missingSecondNumber() {
        String result = mathService.calculate("42 +");
        assertTrue(result.startsWith("ERROR:"), "Expected error, got: " + result);
    }

    @Test
    @DisplayName("Extra tokens after second number")
    void extraTokens() {
        String result = mathService.calculate("5 + 10 garbage");
        assertTrue(result.startsWith("ERROR:"), "Expected error for extra tokens, got: " + result);
    }

    // -------------------------------------------------------------------------
    // Result formatting
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Integer result formatted without decimal: '10 / 2' → 'RESULT: 5' not 'RESULT: 5.0'")
    void integerResultNoDecimalPoint() {
        String result = mathService.calculate("10 / 2");
        assertEquals("RESULT: 5", result, "Integer result should not have .0 suffix");
    }
}
