/*
 * Copyright (c) 2012-2017 The ANTLR Project. All rights reserved.
 * Use of this file is governed by the BSD 3-clause license that
 * can be found in the LICENSE.txt file in the project root.
 */

package org.antlr.v4.codegen.target;

import org.antlr.v4.codegen.CodeGenerator;
import org.antlr.v4.codegen.Target;
import org.antlr.v4.codegen.UnicodeEscapes;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.tool.ErrorType;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.ast.GrammarAST;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.StringRenderer;
import org.stringtemplate.v4.misc.STMessage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class RustTarget extends Target {

	protected static final String[] rustKeywords = {
			"_", "abstract", "alignof", "as", "become ",
			"box", "break", "const", "continue", "crate",
			"do", "else", "enum", "extern", "false",
			"final", "fn", "for", "if", "impl",
			"in", "let", "loop", "macro", "match",
			"mod", "move", "mut", "offsetof", "override",
			"priv", "proc", "pub", "pure", "ref",
			"return", "Self", "self", "sizeof", "static",
			"struct", "super", "trait", "true", "type",
			"typeof", "unsafe", "unsized", "use", "virtual",
			"where", "while", "yield"
	};

	/**
	 * Avoid grammar symbols in this set to prevent conflicts in gen'd code.
	 */
	protected final Set<String> badWords = new HashSet<String>();

	public RustTarget(CodeGenerator gen) {
		super(gen, "Rust");
	}

	public String getVersion() {
		return "4.8";
	}

	public Set<String> getBadWords() {
		if (badWords.isEmpty()) {
			addBadWords();
		}

		return badWords;
	}

	protected void addBadWords() {
		badWords.addAll(Arrays.asList(rustKeywords));
		badWords.add("rule");
		badWords.add("parserRule");
	}

	@Override
	public String encodeIntAsCharEscape(int v) {

		if (v < Character.MIN_VALUE || v > Character.MAX_VALUE) {
			throw new IllegalArgumentException(String.format("Cannot encode the specified value: %d", v));
		}

//		if (v >= 0 && v < targetCharValueEscape.length && targetCharValueEscape[v] != null) {
//			return targetCharValueEscape[v];
//		}

//		if (v >= 0x20 && v < 127 && (!Character.isDigit(v) || v == '8' || v == '9')) {
//			return String.valueOf((char)v);
//		}

		if (v >= 0 && v <= 127) {
			String oct = Integer.toHexString(v | 0x100).substring(1, 3);
			return "\\x" + oct;
		}

		//encode surrogates
		if (v >= 0xD800 && v <= 0xDFFF) {
			v += 0x3000;
		}

		String hex = Integer.toHexString(v);
		return "\\u{" + hex + "}";
	}

	@Override
	public String getRecognizerFileName(boolean header) {
		Grammar g = getCodeGenerator().g;
		assert g != null;
		String name;
		switch (g.getType()) {
			case ANTLRParser.PARSER:
				name = g.name.endsWith("Parser") ? g.name.substring(0, g.name.length() - 6) : g.name;
				name = name.toLowerCase() + "parser";
				break;
			case ANTLRParser.LEXER:
				name = g.name.endsWith("Lexer") ? g.name.substring(0, g.name.length() - 5) : g.name; // trim off "lexer"
				name = name.toLowerCase() + "lexer";
				break;
			case ANTLRParser.COMBINED:
				name = g.name.toLowerCase() + "parser";
				break;
			default:
				return "INVALID_FILE_NAME";
		}
		ST extST = getTemplates().getInstanceOf("codeFileExtension");
		return name + extST.render();
	}

	@Override
	public String getListenerFileName(boolean header) {
		assert gen.g.name != null;
		ST extST = getTemplates().getInstanceOf("codeFileExtension");
		String listenerName = gen.g.name.toLowerCase() + "listener";
		return listenerName + extST.render();
	}

	@Override
	public String getVisitorFileName(boolean header) {
		assert gen.g.name != null;
		ST extST = getTemplates().getInstanceOf("codeFileExtension");
		String listenerName = gen.g.name.toLowerCase() + "visitor";
		return listenerName + extST.render();
	}

	@Override
	public String getBaseListenerFileName(boolean header) {
		assert gen.g.name != null;
		ST extST = getTemplates().getInstanceOf("codeFileExtension");
		String listenerName = gen.g.name + "_baseListener";
		return listenerName + extST.render();
	}

	@Override
	public String getBaseVisitorFileName(boolean header) {
		assert gen.g.name != null;
		ST extST = getTemplates().getInstanceOf("codeFileExtension");
		String listenerName = gen.g.name + "_baseVisitor";
		return listenerName + extST.render();
	}

	@Override
	protected boolean visibleGrammarSymbolCausesIssueInGeneratedCode(GrammarAST idNode) {
		return getBadWords().contains(idNode.getText());
	}

	@Override
	protected STGroup loadTemplates() {
		STGroup result = super.loadTemplates();
//		result.registerRenderer(Integer.class, new NumberRenderer());
		result.registerRenderer(String.class, new RustStringRenderer(), true);
		result.setListener(new STErrorListener() {
			@Override
			public void compileTimeError(STMessage msg) {
				reportError(msg);
			}

			@Override
			public void runTimeError(STMessage msg) {
				reportError(msg);
			}

			@Override
			public void IOError(STMessage msg) {
				reportError(msg);
			}

			@Override
			public void internalError(STMessage msg) {
				reportError(msg);
			}

			private void reportError(STMessage msg) {
				getCodeGenerator().tool.errMgr.toolError(ErrorType.STRING_TEMPLATE_WARNING, msg.cause, msg.toString());
			}
		});

		return result;
	}

	protected static class RustStringRenderer extends StringRenderer {

		@Override
		public String toString(Object o, String formatString, Locale locale) {
			if ("java-escape".equals(formatString)) {
				// 5C is the hex code for the \ itself
				return ((String) o).replace("\\u", "\\u{005C}u");
			}
			if ("low".equals(formatString)) {
				return ((String) o).toLowerCase(locale);
			}

			return super.toString(o, formatString, locale);
		}

	}

	@Override
	public String processActionText(String text) {
		// in rust `'` is not escapable so we don't care about inside string
		return text.replaceAll("\\\\'", "'");
	}

	@Override
	public boolean wantsBaseListener() {
		return false;
	}

	@Override
	public boolean wantsBaseVisitor() {
		return false;
	}

	@Override
	protected void appendUnicodeEscapedCodePoint(int codePoint, StringBuilder sb) {
		// C99 and Python share the same escaping style.
		UnicodeEscapes.appendSwiftStyleEscapedCodePoint(codePoint, sb);
	}
}
