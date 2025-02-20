package ArshiqDS;

import java.util.*;

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
}

class LexicalAnalyzer {
    private final DFA identifierDFA;
    private final Map<String, DFA> keywordDFAs;
    private final DFA operatorDFA;
    private final DFA numberDFA;
    private final DFA decimalDFA;
    private final DFA singleLineCommentDFA;
    private final DFA multiLineCommentDFA;

    public LexicalAnalyzer() {
        this.identifierDFA = buildIdentifierDFA();
        this.keywordDFAs = buildKeywordDFAs();
        this.operatorDFA = buildOperatorDFA();
        this.numberDFA = buildNumberDFA();
        this.decimalDFA = buildDecimalDFA();
        this.singleLineCommentDFA = buildSingleLineCommentDFA();
        this.multiLineCommentDFA = buildMultiLineCommentDFA();
    }

    // Previous DFA builders remain the same
    private DFA buildIdentifierDFA() {
        DFA dfa = new DFA("q0", Set.of("q1"));
        dfa.addTransition("q0", '_', "q1");
        for (char c = 'a'; c <= 'z'; c++) {
            dfa.addTransition("q0", c, "q1");
            dfa.addTransition("q1", c, "q1");
        }
        for (char d = '0'; d <= '9'; d++) {
            dfa.addTransition("q1", d, "q1");
        }
        return dfa;
    }

    private Map<String, DFA> buildKeywordDFAs() {
        Map<String, DFA> keywordDFAs = new HashMap<>();
        String[] keywords = {"int", "float", "char", "bool", "for", "if", "else", "while", ";"};
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
        // Add exponentiation operator ""
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
        while (i < input.length()) {
            char currentChar = input.charAt(i);

            // Skip whitespace
            if (Character.isWhitespace(currentChar)) {
                i++;
                continue;
            }

            // Skip comments
            String multiLineCommentResult = simulateDFA(multiLineCommentDFA, input, i);
            if (multiLineCommentResult != null) {
                i += multiLineCommentResult.length();
                continue;
            }

            String singleLineCommentResult = simulateDFA(singleLineCommentDFA, input, i);
            if (singleLineCommentResult != null) {
                i += singleLineCommentResult.length();
                continue;
            }

            // Process actual tokens
            boolean keywordMatched = false;
            for (Map.Entry<String, DFA> entry : keywordDFAs.entrySet()) {
                String keyword = entry.getKey();
                DFA keywordDFA = entry.getValue();
                String keywordResult = simulateDFA(keywordDFA, input, i);
                if (keywordResult != null && keywordResult.equals(keyword)) {
                    System.out.println("<KEYWORD, " + (keyword.equals(";") ? "TERMINATOR" : keyword) + ">");
                    i += keyword.length();
                    keywordMatched = true;
                    break;
                }
            }
            if (keywordMatched) continue;

            String identifierResult = simulateDFA(identifierDFA, input, i);
            if (identifierResult != null) {
                System.out.println("<IDENTIFIER, " + identifierResult + ">");
                i += identifierResult.length();
                continue;
            }

            String decimalResult = simulateDFA(decimalDFA, input, i);
            if (decimalResult != null) {
                System.out.println("<DECIMAL, " + String.format("%.5f", Double.parseDouble(decimalResult)) + ">");
                i += decimalResult.length();
                continue;
            }

            String numberResult = simulateDFA(numberDFA, input, i);
            if (numberResult != null) {
                System.out.println("<NUMBER, " + numberResult + ">");
                i += numberResult.length();
                continue;
            }

            String operatorResult = simulateDFA(operatorDFA, input, i);
            if (operatorResult != null) {
                String operatorType = operatorResult.equals("") ? "EXPONENT" : operatorResult;
                System.out.println("<OPERATOR, " + operatorType + ">");
                i += operatorResult.length();
                continue;
            }

            System.out.println("<INVALID, " + currentChar + ">");
            i++;
        }
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
}

public class CompilerAssignment {
    public static void main(String[] args) {
        LexicalAnalyzer analyzer = new LexicalAnalyzer();
        String input = """
            int x = 10;
            // This is a single line comment
            float y = 2.5 ** 2; /* This is a
            multi-line comment */
            """;
        analyzer.analyze(input);
    }
}