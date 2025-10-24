package uk.co.bithatch.fatexplorer.util;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Util {
	public static <T> Stream<T> stream(Iterable<T> it){
	    return StreamSupport.stream(it.spliterator(), false);
	} 
}
