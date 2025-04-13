package ArshiqDS;

import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.*;
import java.nio.charset.StandardCharsets;

//Symbol Table Entry class to store information about identifiers
class SymbolTableEntry {
 String name;
 String type;
 String scope;
 String category;
 int lineNumber;
 Object value;
 boolean isConstant;

 public SymbolTableEntry(String name, String type, String scope, String category, 
                        int lineNumber, Object value, boolean isConstant) {
     this.name = name;
     this.type = type;
     this.scope = scope;
     this.category = category;
     this.lineNumber = lineNumber;
     this.value = value;
     this.isConstant = isConstant;
 }

 

 @Override
 public String toString() {
     return String.format("Name: %s, Type: %s, Scope: %s, Category: %s, Line: %d, Value: %s, Constant: %s",
             name, type, scope, category, lineNumber, value, isConstant);
 }
}


class Token {
    String type;
    String value;

    public Token(String type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return "<" + type + ", " + value + ">";
    }
}



//Symbol Table class to manage program symbols
class SymbolTable {
 private final Map<String, SymbolTableEntry> table;
 private final Set<String> dataTypes;
 private final Set<String> operators;
 private final Set<String> delimiters;  // Add delimiters set
 
 public SymbolTable() {
	 this.table = new HashMap<>();
	    this.dataTypes = new HashSet<>(Arrays.asList("int", "float", "string", "bool", "double", "char"));
	    this.operators = new HashSet<>(Arrays.asList("+", "-", "*", "/", "%", "=", "{", "}", ";"));
	    this.delimiters = new HashSet<>(Arrays.asList(";", "{", "}", "(", ")", ","));
 }


 public void add(String name, String type, String scope, String category, 
                int lineNumber, Object value, boolean isConstant) {
     SymbolTableEntry entry = new SymbolTableEntry(name, type, scope, category, 
                                                  lineNumber, value, isConstant);
     table.put(name, entry);
 }

 public SymbolTableEntry lookup(String name) {
     return table.get(name);
 }

 public boolean isDataType(String type) {
     return dataTypes.contains(type);
 }

 public boolean isOperator(String op) {
     return operators.contains(op);
 }

 public boolean isDelimiter(String delim) {
     return delimiters.contains(delim);
 }
 
 public void printTable() {
     System.out.println("\nSymbol Table Contents:");
     System.out.println("=====================");
     for (SymbolTableEntry entry : table.values()) {
         System.out.println(entry);
     }
 }
}

//Error Handler class to manage compilation errors
class ErrorHandler {
 private final List<String> errors;
 private int currentLine;

 public ErrorHandler() {
     this.errors = new ArrayList<>();
     this.currentLine = 1;
 }

 public void setCurrentLine(int line) {
     this.currentLine = line;
 }

 public void addError(String message) {
     errors.add(String.format("Error at line %d: %s", currentLine, message));
 }

 public boolean hasErrors() {
     return !errors.isEmpty();
 }

 public void printErrors() {
     if (hasErrors()) {
         System.out.println("\nCompilation Errors:");
         System.out.println("==================");
         for (String error : errors) {
             System.out.println(error);
         }
     }
 }
}


// DFA class remains the same
class DFA {
    private final Map<String, Map<Character, String>> transitions;
    private final String startState;
    private final Set<String> acceptingStates;

    public DFA(String startState, Set<String> acceptingStates) {
        this.transitions = new HashMap<>();
        this.startState = startState;
        this.acceptingStates = acceptingStates;
    }

    public void addTransition(String fromState, char input, String toState) {
        transitions.putIfAbsent(fromState, new HashMap<>());
        transitions.get(fromState).put(input, toState);
    }

    public boolean isAcceptingState(String state) {
        return acceptingStates.contains(state);
    }

    public String getNextState(String currentState, char input) {
        return transitions.getOrDefault(currentState, Collections.emptyMap()).get(input);
    }

    public String getStartState() {
        return startState;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DFA: ").append(startState).append(" -> ").append(acceptingStates).append("\n");
        for (String from : transitions.keySet()) {
            Map<Character, String> trans = transitions.get(from);
            for (Map.Entry<Character, String> entry : trans.entrySet()) {
                char input = entry.getKey();
                String to = entry.getValue();
                sb.append(String.format("  %s -- %s --> %s%n", 
                    from, 
                    input == '\'' ? "\\'" : input,  // Escape single quotes
                    to
                ));
            }
        }
        return sb.toString();
    }
}

class LexicalAnalyzer {
	private final DFA identifierDFA;
    private final Map<String, DFA> keywordDFAs;
    private final DFA operatorDFA;
    private final DFA numberDFA;
    private final DFA decimalDFA;
    private final DFA singleLineCommentDFA;
    private final DFA multiLineCommentDFA;
    private final DFA stringDFA;
    private final DFA delimiterDFA;
    private final DFA charDFA;
    
    private Deque<String> scopeStack = new ArrayDeque<>();
    private String expectedType = null;
    private boolean expectingConst = false;
    
    private final SymbolTable symbolTable;
    private final ErrorHandler errorHandler;
    private String currentScope;
    private int currentLine;

    // getters for all DFAs
    public DFA getIdentifierDFA() { return identifierDFA; }
    public DFA getOperatorDFA() { return operatorDFA; }
    public DFA getNumberDFA() { return numberDFA; }
    public DFA getDecimalDFA() { return decimalDFA; }
    public DFA getSingleLineCommentDFA() { return singleLineCommentDFA; }
    public DFA getMultiLineCommentDFA() { return multiLineCommentDFA; }
    public DFA getStringDFA() { return stringDFA; }
    public DFA getDelimiterDFA() { return delimiterDFA; }
    public DFA getCharDFA() { return charDFA; }
    public Map<String, DFA> getKeywordDFAs() { return keywordDFAs; }
    
    public LexicalAnalyzer() {
        this.identifierDFA = buildIdentifierDFA();
        this.keywordDFAs = buildKeywordDFAs();
        this.operatorDFA = buildOperatorDFA();
        this.numberDFA = buildNumberDFA();
        this.decimalDFA = buildDecimalDFA();
        this.singleLineCommentDFA = buildSingleLineCommentDFA();
        this.multiLineCommentDFA = buildMultiLineCommentDFA();
        this.stringDFA = buildStringDFA();
        this.delimiterDFA = buildDelimiterDFA();
        this.charDFA = buildCharDFA();
        
        scopeStack.push("global");
        
        this.symbolTable = new SymbolTable();
        this.errorHandler = new ErrorHandler();
        this.currentScope = "global";
        this.currentLine = 1;
    }

    private DFA buildDelimiterDFA() {
        DFA dfa = new DFA("q0", Set.of("q1"));
        char[] delimiters = {';', '{', '}', '(', ')', ','};
        for (char delim : delimiters) {
            dfa.addTransition("q0", delim, "q1");
        }
        return dfa;
    }
    
    // Add DFA builder for character literals
    private DFA buildCharDFA() {
        DFA dfa = new DFA("q0", Set.of("q3"));
        dfa.addTransition("q0", '\'', "q1");
        for (char c = 0; c < 127; c++) {
            if (c != '\'') {
                dfa.addTransition("q1", c, "q2");
            }
        }
        dfa.addTransition("q2", '\'', "q3");
        return dfa;
    }
    
    private DFA buildStringDFA() {
        DFA dfa = new DFA("q0", Set.of("q2"));
        dfa.addTransition("q0", '"', "q1");
        for (char c = 0; c < 127; c++) {
            if (c != '"') {
                dfa.addTransition("q1", c, "q1");
            }
        }
        dfa.addTransition("q1", '"', "q2");
        return dfa;
    }


    // Previous DFA builders remain the same
    private DFA buildIdentifierDFA() {
        DFA dfa = new DFA("q0", Set.of("q1"));
        dfa.addTransition("q0", '_', "q1");
        // Allow only lowercase letters
        for (char c = 'a'; c <= 'z'; c++) {
            dfa.addTransition("q0", c, "q1");
            dfa.addTransition("q1", c, "q1");
        }
        // Remove uppercase transitions
        for (char d = '0'; d <= '9'; d++) {
            dfa.addTransition("q1", d, "q1");
        }
        return dfa;
    }

    private Map<String, DFA> buildKeywordDFAs() {
        Map<String, DFA> keywordDFAs = new HashMap<>();
        String[] keywords = {"int", "float", "double", "char", "string", "bool", 
                "const", "for", "if", "else", "while", "switch", "case"};
        for (String keyword : keywords) {
            DFA dfa = new DFA("q0", Set.of("q" + keyword.length()));
            for (int i = 0; i < keyword.length(); i++) {
                dfa.addTransition("q" + i, keyword.charAt(i), "q" + (i + 1));
            }
            keywordDFAs.put(keyword, dfa);
        }
        return keywordDFAs;
    }

    private DFA buildOperatorDFA() {
        DFA dfa = new DFA("q0", Set.of("q1", "q2"));
        char[] singleCharOperators = {'+', '-', '*', '/', '%', '='};
        for (char op : singleCharOperators) {
            dfa.addTransition("q0", op, "q1");
        }
        // Add exponentiation operator
        dfa.addTransition("q0", '*', "q1");
        dfa.addTransition("q1", '*', "q2");
        return dfa;
    }

    private DFA buildNumberDFA() {
        DFA dfa = new DFA("q0", Set.of("q1"));
        for (char d = '0'; d <= '9'; d++) {
            dfa.addTransition("q0", d, "q1");
            dfa.addTransition("q1", d, "q1");
        }
        return dfa;
    }

    private DFA buildDecimalDFA() {
        DFA dfa = new DFA("q0", Set.of("q3"));
        for (char d = '0'; d <= '9'; d++) {
            dfa.addTransition("q0", d, "q1");
            dfa.addTransition("q1", d, "q1");
            dfa.addTransition("q2", d, "q3");
            dfa.addTransition("q3", d, "q3");
        }
        dfa.addTransition("q1", '.', "q2");
        return dfa;
    }

    private DFA buildSingleLineCommentDFA() {
        DFA dfa = new DFA("q0", Set.of("q2"));
        dfa.addTransition("q0", '/', "q1");
        dfa.addTransition("q1", '/', "q2");
        for (char c = 0; c < 127; c++) {
            if (c != '\n') {
                dfa.addTransition("q2", c, "q2");
            }
        }
        return dfa;
    }

    private DFA buildMultiLineCommentDFA() {
        DFA dfa = new DFA("q0", Set.of("q4"));
        dfa.addTransition("q0", '/', "q1");
        dfa.addTransition("q1", '*', "q2");
        for (char c = 0; c < 127; c++) {
            dfa.addTransition("q2", c, c == '*' ? "q3" : "q2");
            dfa.addTransition("q3", c, c == '/' ? "q4" : c == '*' ? "q3" : "q2");
        }
        return dfa;
    }

    public void analyze(String input) {
        int i = 0;
        currentLine = 1;

        while (i < input.length()) {
            char currentChar = input.charAt(i);

            // Update line counter
            if (currentChar == '\n') {
                currentLine++;
                errorHandler.setCurrentLine(currentLine);
                i++;
                continue;
            }

            // Skip whitespace
            if (Character.isWhitespace(currentChar)) {
                i++;
                continue;
            }
            
            // Handle character literals before strings
            String charResult = simulateDFA(charDFA, input, i);
            if (charResult != null && charResult.length() == 3) {
                handleCharacter(charResult, currentLine);
                i += charResult.length();
                continue;
            }
            
            // Handle string literals
            String stringResult = simulateDFA(stringDFA, input, i);
            if (stringResult != null) {
                handleString(stringResult, currentLine);
                i += stringResult.length();
                continue;
            }

            // Skip comments (previous implementation)
            String multiLineCommentResult = simulateDFA(multiLineCommentDFA, input, i);
            if (multiLineCommentResult != null) {
                // Count newlines in the comment
                int newlines = (int) multiLineCommentResult.chars().filter(c -> c == '\n').count();
                currentLine += newlines;
                errorHandler.setCurrentLine(currentLine);
                i += multiLineCommentResult.length();
                continue;
            }

            String singleLineCommentResult = simulateDFA(singleLineCommentDFA, input, i);
            if (singleLineCommentResult != null) {
                i += singleLineCommentResult.length();
                continue;
            }

            // Process tokens and update symbol table
            Token token = processNextToken(input, i);
            if (token != null) {
                handleToken(token);
                i += token.value.length();
            } else {
            	// Check if it's part of an invalid identifier
                if (Character.isLetter(currentChar)) {
                    String invalidIdentifier = extractInvalidIdentifier(input, i);
                    errorHandler.addError("Invalid identifier (uppercase): " + invalidIdentifier);
                    i += invalidIdentifier.length();
                } else {
                    errorHandler.addError("Invalid character: " + currentChar);
                    i++;
                }
            }
        }

        // Print final results
        symbolTable.printTable();
        errorHandler.printErrors();

    }

    
    // Helper method to extract contiguous letters/digits
    private String extractInvalidIdentifier(String input, int start) {
        int end = start;
        while (end < input.length() && 
               (Character.isLetterOrDigit(input.charAt(end)) || input.charAt(end) == '_')) {
            end++;
        }
        return input.substring(start, end);
    }
    
    private String simulateDFA(DFA dfa, String input, int startIndex) {
        String currentState = dfa.getStartState();
        StringBuilder lexeme = new StringBuilder();
        int i = startIndex;
        String lastAcceptedLexeme = null;
        
        while (i < input.length()) {
            char currentChar = input.charAt(i);
            String nextState = dfa.getNextState(currentState, currentChar);
            if (nextState == null) break;
            
            currentState = nextState;
            lexeme.append(currentChar);
            i++;
            
            if (dfa.isAcceptingState(currentState)) {
                lastAcceptedLexeme = lexeme.toString();
            }
        }
        
        return lastAcceptedLexeme;
    }
    
    // Add character literal handler
    private void handleCharacter(String value, int line) {
        char charValue = value.charAt(1); // Get 'A' from 'A'
        symbolTable.add("char_" + line, "char", scopeStack.peek(), 
                       "literal", line, charValue, false);
        System.out.println("<CHAR_LITERAL, " + charValue + ">");
    }
    
    private void handleString(String value, int line) {
        // Remove quotes for storage
        String strValue = value.substring(1, value.length() - 1);
        symbolTable.add("str_" + line, "string", currentScope, "literal", 
                       line, strValue, false);
        System.out.println("<STRING, " + strValue + ">");
    }

    
    private Token processNextToken(String input, int startIndex) {
    	// Check delimiters first
        String delimiterResult = simulateDFA(delimiterDFA, input, startIndex);
        if (delimiterResult != null) {
            return new Token("DELIMITER", delimiterResult);
        }
    	
        // Check Operators
    	String operatorResult = simulateDFA(operatorDFA, input, startIndex);
        if (operatorResult != null) {
            return new Token("OPERATOR", operatorResult);
        }
    	
        // Check character literals
        String charResult = simulateDFA(charDFA, input, startIndex);
        if (charResult != null && charResult.length() == 3) {
            return new Token("CHAR_LITERAL", charResult);
        }
        
    	// Check keywords
        for (Map.Entry<String, DFA> entry : keywordDFAs.entrySet()) {
            String keyword = entry.getKey();
            String result = simulateDFA(entry.getValue(), input, startIndex);
            if (result != null && result.equals(keyword)) {
                return new Token("KEYWORD", keyword);
            }
        }
        
        // Check other token types
        String identifierResult = simulateDFA(identifierDFA, input, startIndex);
        if (identifierResult != null) {
        	if (identifierResult.equals("true") || identifierResult.equals("false")) {
                return new Token("BOOL_LITERAL", identifierResult);
            }
            return new Token("IDENTIFIER", identifierResult);
        }

        String decimalResult = simulateDFA(decimalDFA, input, startIndex);
        if (decimalResult != null) {
            return new Token("DECIMAL", decimalResult);
        }

        String numberResult = simulateDFA(numberDFA, input, startIndex);
        if (numberResult != null) {
            return new Token("NUMBER", numberResult);
        }


        return null;
    }

    private void handleToken(Token token) {
        System.out.println(token);

        switch (token.type) {
	        case "KEYWORD":
	            // Remove type declaration tracking
	            if (token.value.equals("const")) {
	                expectingConst = true;
	            } else if (symbolTable.isDataType(token.value)) {
	                expectedType = token.value;
	            }
	            break;
	
	        case "BOOL_LITERAL":
	            symbolTable.add(token.value, "bool", scopeStack.peek(), "literal",
	                          currentLine, Boolean.parseBoolean(token.value), true);
	            break;

            case "IDENTIFIER":
            	// Validate lowercase-only format
            	if (!token.value.matches("[a-z_][a-z0-9_]*")) {
                    errorHandler.addError("Invalid identifier (uppercase): " + token.value);
                    expectedType = null;
                    expectingConst = false;
                    break;
                }
                
                if (expectedType != null) {
                    symbolTable.add(token.value, expectedType, scopeStack.peek(), 
                                   "variable", currentLine, null, expectingConst);
                    expectingConst = false;
                } else if (symbolTable.lookup(token.value) == null) {
                    symbolTable.add(token.value, "unknown", scopeStack.peek(), 
                                   "identifier", currentLine, null, false);
                }
                expectedType = null;  // Clear after declaration
                break;

            case "NUMBER":
            case "DECIMAL":
                BigDecimal bd = new BigDecimal(token.value).setScale(5, RoundingMode.HALF_UP);
                double roundedValue = bd.doubleValue();
                symbolTable.add("const_" + currentLine, "double", scopeStack.peek(), 
                              "constant", currentLine, roundedValue, true);
                break;

                
            case "DELIMITER":
                handleDelimiter(token.value);
                break;
                
            case "OPERATOR":
                // Handle scope changes but don't add to symbol table
                if (token.value.equals(";")) {
                    // Reset declaration tracking
                    expectedType = null;
                    expectingConst = false;
                }
                if (token.value.equals("{")) {
                    scopeStack.push("block_" + scopeStack.size());
                } else if (token.value.equals("}")) {
                    if (scopeStack.size() > 1) {
                        scopeStack.pop();
                    } else {
                        errorHandler.addError("Unmatched '}' at line " + currentLine);
                    }
                }
                // Validate operator
                if (!symbolTable.isOperator(token.value)) {
                    errorHandler.addError("Invalid operator: " + token.value);
                }
                break;
        }
    }
    
    // Add delimiter handling method
    private void handleDelimiter(String delimiter) {
        // Handle scope changes
        if (delimiter.equals("{")) {
            scopeStack.push("block_" + scopeStack.size());
        } else if (delimiter.equals("}")) {
            if (scopeStack.size() > 1) {
                scopeStack.pop();
            } else {
                errorHandler.addError("Unmatched '}' at line " + currentLine);
            }
        }
        
        // Handle statement termination
        if (delimiter.equals(";")) {
            expectedType = null;
            expectingConst = false;
        }
    }

    
}

public class 22i1023-22i1285-E {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Show DFAs before analysis? (yes/no): ");
        String choice = scanner.nextLine().trim().toLowerCase();
        
        LexicalAnalyzer analyzer = new LexicalAnalyzer();
        
        // Print DFAs if requested
        if (choice.equals("yes") || choice.equals("y")) {
            printAllDFAs(analyzer);
        }
        
        // Rest of the main method remains the same
        try (InputStream inputStream = CompilerAssignment.class.getResourceAsStream("input.cpp")) {
            if (inputStream == null) {
                System.err.println("Error: input.cpp file not found in the package");
                return;
            }
            
            String input = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            analyzer.analyze(input);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printAllDFAs(LexicalAnalyzer analyzer) {
        System.out.println("\n=== Identifier DFA ===");
        System.out.println(analyzer.getIdentifierDFA());

        System.out.println("\n=== Operator DFA ===");
        System.out.println(analyzer.getOperatorDFA());

        System.out.println("\n=== Number DFA ===");
        System.out.println(analyzer.getNumberDFA());

        System.out.println("\n=== Decimal DFA ===");
        System.out.println(analyzer.getDecimalDFA());

        System.out.println("\n=== Single-line Comment DFA ===");
        System.out.println(analyzer.getSingleLineCommentDFA());

        System.out.println("\n=== Multi-line Comment DFA ===");
        System.out.println(analyzer.getMultiLineCommentDFA());

        System.out.println("\n=== String DFA ===");
        System.out.println(analyzer.getStringDFA());

        System.out.println("\n=== Delimiter DFA ===");
        System.out.println(analyzer.getDelimiterDFA());

        System.out.println("\n=== Char Literal DFA ===");
        System.out.println(analyzer.getCharDFA());

        System.out.println("\n=== Keyword DFAs ===");
        analyzer.getKeywordDFAs().forEach((kw, dfa) -> {
            System.out.println("Keyword: " + kw);
            System.out.println(dfa);
        });
    }
}