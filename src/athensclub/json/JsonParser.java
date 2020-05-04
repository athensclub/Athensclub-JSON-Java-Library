package athensclub.json;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import athensclub.compiler.provider.DecimalableNumberProvider;
import athensclub.compiler.provider.KeywordProvider;
import athensclub.compiler.provider.StringProvider;
import athensclub.compiler.provider.WhitespaceProvider;
import athensclub.compiler.syntax.Block;
import athensclub.compiler.syntax.BlockProvider;
import athensclub.compiler.syntax.Parser;
import athensclub.compiler.syntax.Statement;
import athensclub.compiler.syntax.StatementProvider;
import athensclub.compiler.token.StreamTokenizer;
import athensclub.compiler.token.StringTokenizer;
import athensclub.compiler.token.Token;
import athensclub.compiler.token.TokenCreator;
import athensclub.compiler.token.Tokenizer;

/**
 * A parser for JSON file format.A value from JSON are converted to Java Values.
 * <p>
 * The parsed JSON value will return a list of value that are separated by
 * comma, mapped to java value by this parser's mapping rule.
 * </p>
 * <p>
 * <code>
 * The parser will convert following json value (left) to java value (right) </br>
 * integer -> Integer</br>
 * decimal -> Double</br>
 * boolean -> Boolean</br>
 * String -> String</br>
 * null -> null</br>
 * Array -> List{@literal <Object>} (Objects type are mapped to java value by this mapping)</br>
 * JSON Object -> Map{@literal <String,Object>} (Objects type are mapped to java value by this mapping)</br>
 * </code>
 * </p>
 * 
 * @author Athensclub
 *
 */
public class JsonParser {

    private static final String BRACKETS = "[]{}";

    private Tokenizer tokenizer;

    private Parser parser;

    public JsonParser(Reader stream) {
	tokenizer = new StreamTokenizer(stream);
	parser = new Parser(tokenizer);
	setupTokenizer(tokenizer);
	setupParser(parser);
    }

    public JsonParser(String str) {
	tokenizer = new StringTokenizer(str);
	parser = new Parser(tokenizer);
	setupTokenizer(tokenizer);
	setupParser(parser);
    }

    /**
     * Parse everything from the stream and return as list of json values mapped to java
     * values.
     * 
     * @return
     * @throws Exception
     */
    public List<Object> parseObjects() throws Exception {
	Block block = parser.parse();
	ArrayList<Object> result = new ArrayList<>();
	boolean expectComma = false;
	boolean hasComma = false;
	for (Statement stm : block.getSubStatement()) {
	    if (expectComma) {
		if (stm.getStatement().getFirst().getString().equals(",")) {
		    expectComma = false;
		    hasComma = true;
		} else {
		    throw new IllegalArgumentException("Expected comma,found: " + stm);
		}
	    } else {
		result.add(parse(stm));
		expectComma = true;
		hasComma = false;
	    }
	}
	if(hasComma) {
	    throw new IllegalArgumentException("Unexpected comma (at the end)");
	}
	return Collections.unmodifiableList(result);
    }

    private Object parse(Statement stm) {
	if (stm instanceof JsonBlock) {
	    return parse((JsonBlock) stm);
	}
	Token t = stm.getStatement().getFirst();
	if (t instanceof StringProvider.Token) {
	    return ((StringProvider.Token) t).getLiteralValue();
	}
	if(t instanceof DecimalableNumberProvider.Token) {
	    DecimalableNumberProvider.Token token = (DecimalableNumberProvider.Token)t;
	    if(token.getFractional() == null) {
		return Integer.parseInt(token.getIntegeral().getString());
	    }else {
		return Double.parseDouble(token.getString());
	    }
	}
	switch (t.getString()) {
	case "true":
	    return true;
	case "false":
	    return false;
	case "null":
	    return null;
	}
	throw new IllegalArgumentException("Unknown token: " + t);
    }

    private Object parse(JsonBlock block) {
	if (block.isArray) {
	    boolean endWithComma = false;
	    boolean expectComma = false;
	    ArrayList<Object> result = new ArrayList<>();
	    for (Statement stm : block.getSubStatement()) {
		if (expectComma) {
		    if (stm.getStatement().getFirst().getString().equals(",")) {
			endWithComma = true;
			expectComma = false;
		    } else {
			throw new IllegalArgumentException("Expected comma,found: " + stm);
		    }
		} else {
		    result.add(parse(stm));
		    endWithComma = false;
		    expectComma = true;
		}
	    }
	    if (endWithComma) {
		throw new IllegalArgumentException("Unexpected comma");
	    }
	    return Collections.unmodifiableList(result);
	} else {
	    Iterator<Statement> it = block.getSubStatement().iterator();
	    HashMap<String, Object> result = new HashMap<>();
	    boolean expectComma = false;
	    boolean endWithComma = false;
	    while (it.hasNext()) {
		Statement stm = it.next();
		if (expectComma) {
		    if (stm.getStatement().getFirst().getString().equals(",")) {
			endWithComma = true;
			expectComma = false;
		    } else {
			throw new IllegalArgumentException("Expected comma,found: " + stm);
		    }
		} else {
		    Token begin = stm.getStatement().getFirst();
		    if (begin instanceof StringProvider.Token) {
			if (it.hasNext()) {
			    Token colon = it.next().getStatement().getFirst();
			    if (colon.getString().equals(":")) {
				if (it.hasNext()) {
				    Statement value = it.next();
				    String key = ((StringProvider.Token) begin).getLiteralValue();
				    if (!result.containsKey(key)) {
					result.put(key, parse(value));
					expectComma = true;
					endWithComma = false;
				    } else {
					throw new IllegalArgumentException("Duplicate Object key: \"" + key + "\"");
				    }
				} else {
				    throw new IllegalArgumentException("Expected value after object key: " + begin);
				}
			    } else {
				throw new IllegalArgumentException("Expected ':',found: " + colon);
			    }
			} else {
			    throw new IllegalArgumentException("Unexpected String: " + begin);
			}
		    } else {
			throw new IllegalArgumentException("Expected String in object key, found: " + begin);
		    }
		}
	    }
	    if (endWithComma) {
		throw new IllegalArgumentException("Unexpected comma");
	    }
	    return Collections.unmodifiableMap(result);
	}
    }

    private void setupParser(Parser parser) {
	parser.addBlockProvider(new BlockProvider() {

	    @Override
	    public boolean matchEndBlock(Parser parser, LinkedList<Token> statement) {
		if (statement.isEmpty()) {
		    return false;
		}
		Token t = statement.getFirst();
		if (t instanceof BracketToken) {
		    BracketToken b = (BracketToken) t;
		    return b.isArray && !b.isOpen;
		}
		return false;
	    }

	    @Override
	    public Block createBlock(LinkedList<Token> blockBegin) {
		return new JsonBlock(true);
	    }

	    @Override
	    public boolean matchBeginBlock(Parser parser, LinkedList<Token> statement) {
		if (statement.isEmpty()) {
		    return false;
		}
		Token t = statement.getFirst();
		if (t instanceof BracketToken) {
		    BracketToken b = (BracketToken) t;
		    return b.isArray && b.isOpen;
		}
		return false;
	    }
	});
	parser.addBlockProvider(new BlockProvider() {

	    @Override
	    public boolean matchEndBlock(Parser parser, LinkedList<Token> statement) {
		if (statement.isEmpty()) {
		    return false;
		}
		Token t = statement.getFirst();
		if (t instanceof BracketToken) {
		    BracketToken b = (BracketToken) t;
		    return !b.isArray && !b.isOpen;
		}
		return false;
	    }

	    @Override
	    public Block createBlock(LinkedList<Token> blockBegin) {
		return new JsonBlock(false);
	    }

	    @Override
	    public boolean matchBeginBlock(Parser parser, LinkedList<Token> statement) {
		if (statement.isEmpty()) {
		    return false;
		}
		Token t = statement.getFirst();
		if (t instanceof BracketToken) {
		    BracketToken b = (BracketToken) t;
		    return !b.isArray && b.isOpen;
		}
		return false;
	    }
	});
	parser.addStatementProvider(new StatementProvider() {

	    @Override
	    public boolean matchStatement(LinkedList<Token> tokens) {
		return !(tokens.getFirst() instanceof BracketToken);
	    }
	});
    }

    private void setupTokenizer(Tokenizer tokenizer) {
	KeywordProvider provider = new KeywordProvider();
	provider.addKeywords(":", "{", "}", "[", "]", ",");
	provider.setTokenCreator(new TokenCreator() {
	    @Override
	    public Token createToken(StringBuilder string) {
		if (BRACKETS.contains(string)) {
		    return new BracketToken(string.charAt(0));
		}
		return TokenCreator.super.createToken(string);
	    }
	});
	tokenizer.addProviders(new WhitespaceProvider(), StringProvider.INSTANCE, DecimalableNumberProvider.INSTANCE,
		provider);
    }

    private static class JsonBlock extends Block {

	private boolean isArray;

	public JsonBlock(boolean arr) {
	    isArray = arr;
	}
    }

    /**
     * A token of bracket.
     * 
     * @author Athensclub
     *
     */
    private static class BracketToken extends Token {

	private boolean isArray, isOpen;

	public BracketToken(char c) {
	    super();
	    if (c == '[' || c == ']') {
		isArray = true;
	    }
	    if (c == '[' || c == '{') {
		isOpen = true;
	    }
	}

    }

}
