package dqcup.repair.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestDemo {
	public static void main(String args[]){
		String regex = "^[A-Z][a-z|A-Z|,|\\.]*";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher("Edgardo");
		System.out.println(!m.matches());
	}

}
