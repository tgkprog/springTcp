package com.tcp.tgk.server.service;

import org.springframework.stereotype.Service;

@Service
public class MathService {

    public String calculate(String expression) {
        try {
            String[] parts = expression.split("\\s+");
            if (parts.length != 3) {
                return "ERROR: Invalid expression format. Use: m: <number> <operator> <number>";
            }

            double num1 = Double.parseDouble(parts[0]);
            String operator = parts[1];
            double num2 = Double.parseDouble(parts[2]);

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
                    if (num2 == 0) {
                        return "ERROR: Division by zero";
                    }
                    result = num1 / num2;
                    break;
                case "%":
                    if (num2 == 0) {
                        return "ERROR: Modulo by zero";
                    }
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
