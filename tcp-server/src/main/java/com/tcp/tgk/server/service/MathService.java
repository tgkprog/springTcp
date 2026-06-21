package com.tcp.tgk.server.service;

import org.springframework.stereotype.Service;

@Service
public class MathService {

    /**
     * Parses a math expression of the form: <num> <op> <num>
     * Tolerates any amount of whitespace (including none) between tokens.
     * The expression arrives already stripped of the leading "m" and trimmed.
     *
     * Parsing strategy:
     *   1. Skip leading whitespace.
     *   2. Read number token (digits, optional leading '-', optional decimal point).
     *   3. Skip whitespace.
     *   4. Read operator token (single char: + - * / % ^).
     *   5. Skip whitespace.
     *   6. Read rest as second number token.
     *   7. Reject anything remaining after the second number.
     */
    public String calculate(String expression) {
        try {
            if (expression == null || expression.isEmpty()) {
                return "ERROR: Empty expression. Use: m <number> <operator> <number>";
            }

            // --- tokenise ---
            int len = expression.length();
            int pos = 0;

            // 1. skip leading whitespace
            while (pos < len && expression.charAt(pos) == ' ') pos++;

            // 2. read first number token
            int numStart = pos;
            if (pos < len && expression.charAt(pos) == '-') pos++; // optional sign
            while (pos < len && (Character.isDigit(expression.charAt(pos)) || expression.charAt(pos) == '.')) pos++;
            if (pos == numStart || (pos == numStart + 1 && expression.charAt(numStart) == '-')) {
                return "ERROR: Missing first number in expression";
            }
            String tok1 = expression.substring(numStart, pos);

            // 3. skip whitespace
            while (pos < len && expression.charAt(pos) == ' ') pos++;

            // 4. read operator (exactly one char)
            if (pos >= len) {
                return "ERROR: Missing operator in expression";
            }
            char opChar = expression.charAt(pos);
            String operator = String.valueOf(opChar);
            pos++;

            // 5. skip whitespace
            while (pos < len && expression.charAt(pos) == ' ') pos++;

            // 6. read rest as second number token
            if (pos >= len) {
                return "ERROR: Missing second number in expression";
            }
            String tok2 = expression.substring(pos).trim();
            if (tok2.isEmpty()) {
                return "ERROR: Missing second number in expression";
            }
            // make sure tok2 contains no whitespace (extra garbage after number)
            if (tok2.contains(" ")) {
                return "ERROR: Unexpected extra input after second number";
            }

            double num1 = Double.parseDouble(tok1);
            double num2 = Double.parseDouble(tok2);

            double result;
            switch (operator) {
                case "+":
                    result = num1 + num2;
                    break;
                case "-":
                    result = num1 - num2;
                    break;
                case "*":
                    result = num1 * num2;
                    break;
                case "/":
                    if (num2 == 0) return "ERROR: Division by zero";
                    result = num1 / num2;
                    break;
                case "%":
                    if (num2 == 0) return "ERROR: Modulo by zero";
                    result = num1 % num2;
                    break;
                case "^":
                    result = Math.pow(num1, num2);
                    break;
                default:
                    return "ERROR: Unknown operator '" + operator + "'. Supported: +, -, *, /, %, ^";
            }

            if (result == (long) result) {
                return "RESULT: " + (long) result;
            } else {
                return "RESULT: " + result;
            }
        } catch (NumberFormatException e) {
            return "ERROR: Invalid number format";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

}
