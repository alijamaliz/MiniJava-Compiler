package parser;


import Log.Log;
import codeGenerator.CodeGenerator;
import errorHandler.ErrorHandler;
import guru.nidi.graphviz.attribute.RankDir;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import scanner.lexicalAnalyzer;
import scanner.token.Token;
import scanner.type.Type;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static guru.nidi.graphviz.model.Factory.*;


public class Parser {
    private ArrayList<Rule> rules;
    private Stack<Integer> parsStack;
    private ParseTable parseTable;
    private lexicalAnalyzer lexicalAnalyzer;
    private CodeGenerator cg;

    public Parser() {
        parsStack = new Stack<Integer>();
        parsStack.push(0);
        try {
            parseTable = new ParseTable(Files.readAllLines(Paths.get("outputs/parseTable")).get(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
        rules = new ArrayList<Rule>();
        try {
            for (String stringRule : Files.readAllLines(Paths.get("outputs/Rules"))) {
                rules.add(new Rule(stringRule));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        cg = new CodeGenerator();
    }

    public void startParse(java.util.Scanner sc) {
        MutableGraph g = mutGraph("AST").setDirected(true);
        lexicalAnalyzer = new lexicalAnalyzer(sc);
        Token lookAhead = lexicalAnalyzer.getNextToken();
        System.out.println(lookAhead);
        boolean finish = false;
        Action currentAction;
        int indent = 0;
        int counter = 0;
        Stack<String> astRoots = new Stack<String>();
        while (!finish) {
            try {
                Log.print(/*"lookahead : "+*/ lookAhead.toString() + "\t" + parsStack.peek());
//        System.out.println(lookAhead);
//                Log.print("state : "+ parsStack.peek());
                currentAction = parseTable.getActionTable(parsStack.peek(), lookAhead);
                Log.print(currentAction.toString());
                //Log.print("");

                switch (currentAction.action) {
                    case shift:
                        parsStack.push(currentAction.number);
                        lookAhead = lexicalAnalyzer.getNextToken();
                        for (int i = 0; i < indent; i++)
                            System.out.print('\t');
                        System.out.println(lookAhead);

                        String currentNodeName = lookAhead.toString() + " / " + String.valueOf(counter);
                        String parentNodeName = "";
                        if (astRoots.size() != 0)
                            parentNodeName = astRoots.peek();

                        counter++;


                        if (lookAhead.type == Type.OPENINGCB || lookAhead.type == Type.OPENINGP) {
                            indent++;
                            astRoots.push(currentNodeName);
                        }
                        if (lookAhead.type == Type.CLOSINGCB || lookAhead.type == Type.CLOSINGP) {
                            indent--;
                            astRoots.pop();
                        }


                        if (parentNodeName.equals(""))
                            g.add(mutNode(currentNodeName));
                        else {
                            g.add(mutNode(currentNodeName).addLink(mutNode(parentNodeName)));

                        }


                        break;
                    case reduce:
                        Rule rule = rules.get(currentAction.number);
                        for (int i = 0; i < rule.RHS.size(); i++) {
                            parsStack.pop();
                        }

                        Log.print(/*"state : " +*/ parsStack.peek() + "\t" + rule.LHS);
//                        Log.print("LHS : "+rule.LHS);
                        parsStack.push(parseTable.getGotoTable(parsStack.peek(), rule.LHS));
                        Log.print(/*"new State : " + */parsStack.peek() + "");
//                        Log.print("");
                        try {
                            cg.semanticFunction(rule.semanticAction, lookAhead);
                        } catch (Exception e) {
                            Log.print("Code Genetator Error");
                        }
                        break;
                    case accept:
                        finish = true;
                        break;
                }
                Log.print("");

            } catch (Exception ignored) {

                ignored.printStackTrace();
//                boolean find = false;
//                for (NonTerminal t : NonTerminal.values()) {
//                    if (parseTable.getGotoTable(parsStack.peek(), t) != -1) {
//                        find = true;
//                        parsStack.push(parseTable.getGotoTable(parsStack.peek(), t));
//                        StringBuilder tokenFollow = new StringBuilder();
//                        tokenFollow.append(String.format("|(?<%s>%s)", t.name(), t.pattern));
//                        Matcher matcher = Pattern.compile(tokenFollow.substring(1)).matcher(lookAhead.toString());
//                        while (!matcher.find()) {
//                            lookAhead = lexicalAnalyzer.getNextToken();
//                        }
//                    }
//                }
//                if (!find)
//                    parsStack.pop();
            }

        }
        try {
            Graphviz.fromGraph(g)
                    .width(30000)
                    .render(Format.PNG)
                    .toFile(new File("outputs/ast.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!ErrorHandler.hasError)
            cg.printMemory();

    }
}
