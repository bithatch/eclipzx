package uk.co.bithatch.zxbasic.preprocessor;

import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public record IncludeContext<CONTEXT>(
		CONTEXT context, 
		String uri, 
		Iterator<String> stream, 
		AtomicInteger lineNumber, 
		Stack<Condition> conditions, 
		AtomicInteger originalOffset, 
		AtomicInteger originalLength,
		AtomicInteger originalLine, 
		AtomicInteger originalLines,  
		AtomicInteger preprocessedLength,   
		AtomicInteger preprocessedLines,
		AtomicBoolean runtimeModule,
		AtomicInteger lastPrintedLine) {
	IncludeContext(CONTEXT context, String uri, Iterator<String> stream) {
		this(context, uri, stream, new AtomicBoolean());
	}
	
	IncludeContext(CONTEXT context, String uri, Iterator<String> stream, AtomicBoolean runtimeModule) {
		this(
				context, 
				uri, 
				stream, 
				new AtomicInteger(0), 
				new Stack<>(), 
				new AtomicInteger(), 
				new AtomicInteger(), 
				new AtomicInteger(),  
				new AtomicInteger(), 
				new AtomicInteger(), 
				new AtomicInteger(),
				runtimeModule,
				new AtomicInteger());
	}
}