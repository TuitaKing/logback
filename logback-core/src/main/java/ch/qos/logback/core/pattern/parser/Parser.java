package ch.qos.logback.core.pattern.parser;

import java.util.List;
import java.util.Map;

import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.FormatInfo;
import ch.qos.logback.core.spi.ContextAwareBase;


public class Parser extends ContextAwareBase {

  final List tokenList;
  int pointer = 0;
  
  Parser(TokenStream ts) throws ScanException {
    this.tokenList = ts.tokenize();
  }

  public Parser(String pattern) throws ScanException {
    try {
      TokenStream ts = new TokenStream(pattern);
      this.tokenList = ts.tokenize();
    } catch (NullPointerException npe) {
      throw new ScanException("Failed to initialize Parser", npe);
    }
  }

  public Node parse() throws ScanException {
    return E();
  }

  /**
   * When the parsing step is done, the Node list can be transformed into a
   * converter chain.
   * 
   * @param top
   * @param converterMap
   * @return
   * @throws ScanException
   */
  public Converter compile(final Node top, Map converterMap) {
    Compiler compiler = new Compiler(top, converterMap);
    compiler.setContext(context);
    //compiler.setStatusManager(statusManager);
    return compiler.compile();
  }

  Node E() throws ScanException {
    // System.out.println("in E()");
    Node t = T();
    if (t == null) {
      return null;
    }
    Node eOpt = Eopt();
    if (eOpt != null) {
      // System.out.println("setting next node to " + eOpt);
      t.setNext(eOpt);
    }
    return t;
  }

  Node T() throws ScanException {
    // System.out.println("in T()");
    Token t = getCurentToken();
    if (t == null) {
      throw new IllegalStateException("a LITERAL or '%'");
    }

    // System.out.println("Current token is " + t);

    switch (t.getType()) {
    case Token.LITERAL:
      advanceTokenPointer();
      return new Node(Node.LITERAL, t.getValue());
    case Token.PERCENT:
      advanceTokenPointer();
      // System.out.println("% token found");
      FormatInfo fi;
      Token u = getCurentToken();
      FormattingNode c;
      expectNotNull(u, "a FORMAT_MODIFIER, KEYWORD or LEFT_PARENTHESIS");
      if (u.getType() == Token.FORMAT_MODIFIER) {
        fi = FormatInfo.valueOf((String) u.getValue());
        advanceTokenPointer();
        c = C();
        c.setFormatInfo(fi);
      } else {
        c = C();
      }
      return c;

    default:
      return null;

    }

  }

  Node Eopt() throws ScanException {
    // System.out.println("in Eopt()");
    Token next = getCurentToken();
    // System.out.println("Current token is " + next);
    if (next == null) {
      return null;
    } else {
      return E();
    }
  }

  FormattingNode C() throws ScanException {
    Token t = getCurentToken();
    // System.out.println("in C()");
    // System.out.println("Current token is " + t);
    expectNotNull(t, "a LEFT_PARENTHESIS or KEYWORD");
    int type = t.getType();
    switch (type) {
    case Token.KEYWORD:
      return SINGLE();
    case Token.LEFT_PARENTHESIS:
      advanceTokenPointer();
      return COMPOSITE();
    default:
      throw new IllegalStateException("Unexpected token " + t);
    }
  }

  FormattingNode SINGLE() throws ScanException {
    // System.out.println("in SINGLE()");
    Token t = getNextToken();
    // System.out.println("==" + t);
    KeywordNode keywordNode = new KeywordNode(t.getValue());

    Token ot = getCurentToken();
    if (ot != null && ot.getType() == Token.OPTION) {
      List optionList = new OptionTokenizer((String) ot.getValue()).tokenize();
      keywordNode.setOptions(optionList);
      advanceTokenPointer();
    }
    return keywordNode;
  }

  FormattingNode COMPOSITE() throws ScanException {
    CompositeNode compositeNode = new CompositeNode();

    Node childNode = E();
    // System.out.println("Child node: " + childNode);

    compositeNode.setChildNode(childNode);

    Token t = getNextToken();
    // System.out.println("Next token is" + t);

    if (t.getType() != Token.RIGHT_PARENTHESIS) {
      throw new IllegalStateException(
          "Expecting RIGHT_PARENTHESIS token but got " + t);
    } else {
      // System.out.println("got expected ')'");
    }
    return compositeNode;
  }

  Token getNextToken() {
    if (pointer < tokenList.size()) {
      return (Token) tokenList.get(pointer++);
    }
    return null;
  }

  Token getCurentToken() {
    if (pointer < tokenList.size()) {
      return (Token) tokenList.get(pointer);
    }
    return null;
  }

  void advanceTokenPointer() {
    pointer++;
  }

  void expectNotNull(Token t, String expected) {
    if (t == null) {
      throw new IllegalStateException("All tokens consumed but was expecting "
          + expected);
    }
  }

//  public void setStatusManager(StatusManager statusManager) {
//    this.statusManager = statusManager;
//  }
}