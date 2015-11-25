package dqcup.repair.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dqcup.repair.ColumnNames;
import dqcup.repair.RepairedCell;
import dqcup.repair.Tuple;

public class DataProfolling {
	//不光包括50个州，还包括DC特区以及GU关岛这样的附属
	private static final List<String> STATES = Arrays.asList(
			"AK", "AL", "AR", "AS", "AZ", "CA", "CO", "CT", "DC", "DE", 
			"FL", "FM", "GA", "HI", "IA", "ID", "IL", "IN", "KS", "KY",
			"LA", "MA", "MD", "ME", "MH", "MI", "MN", "MO", "MP", "MS", 
			"MT", "NC", "ND", "NE", "NH", "NJ", "NM", "NV", "NY", "OH",
			"OK", "OR", "PA", "PR", "PW", "RI", "SC", "SD", "TN", "TX", 
			"UT", "VA", "VI", "VT", "WA", "WI", "WV", "WY", "GU");
	
	//根据各个field的tuples
	private static HashMap<String, HashMap<String, LinkedList<Tuple>>> groupedTuples = new HashMap<String, HashMap<String, LinkedList<Tuple>>>();
	
	//根据groupedTuples,计算truthTuple
	private static HashMap<String, Tuple> truthTuples = new HashMap<String, Tuple>();
	
	
	
	//fields
	public static final String[] FIELDS = new String[]{"CUID", "SSN", "FNAME", "MINIT", "LNAME", "STNUM", "STADD",
			"APMT", "CITY", "STATE", "ZIP"};
	
	//columnNames
	public static ColumnNames COLUMNNAMES;
	
	public static HashMap<String, Tuple> performance(LinkedList<Tuple> tuples){
		groupedTuples.clear();
		truthTuples.clear();
		// 根据FIELDS生成COLUMNNAMES
		generateColumnNames();

		// 初始化groupedTuples
		groupTuples(tuples);

		// 根据most vote原则，计算truthTuples
		generateTruthTuples();

		checkAndRepairTruthTuples();
		
	
		return truthTuples;
	}
	
	private static void checkAndRepairTruthTuples() {
		for (Tuple tuple : truthTuples.values()){
			StringBuffer sb = new StringBuffer();
			for (String field : FIELDS){
				sb.append(checkValue(field, tuple)).append(":");
			}
			sb.deleteCharAt(sb.length() - 1);
			truthTuples.put(tuple.getValue("CUID"), 
					new Tuple(COLUMNNAMES, sb.toString()));
			truthTuples.remove(tuple);
		}
	}

	private static void generateTruthTuples() {
	
		for (Entry entry : groupedTuples.get("CUID").entrySet()){
			String cuid = (String)entry.getKey();
			LinkedList<Tuple> list = (LinkedList<Tuple>)entry.getValue();
			StringBuffer truthValues = new StringBuffer();
			for (String field : FIELDS) {
				String truthValue = voteTruthValue(list, field);
				truthValues.append(truthValue).append(":");				
			}
			truthValues.deleteCharAt(truthValues.length() - 1);
			truthTuples.put(cuid, new Tuple(COLUMNNAMES, truthValues.toString()));
		}
		
	}

	public static String voteTruthValue(LinkedList<Tuple> list, String field) {
		String truthValue = "";
		int maxVote = Integer.MIN_VALUE;
		HashMap<String, Integer> voteBox = new HashMap<String, Integer>();
		for (Tuple tuple : list) {
			String value = tuple.getValue(field);
			if (voteBox.containsKey(value)) {
				voteBox.put(value, voteBox.get(value) + 1);
			} else {
				voteBox.put(value, 1);
			}
		}
		for (Entry voteEntry : voteBox.entrySet()){
			int voteCount = (Integer) voteEntry.getValue();
			String value = (String) voteEntry.getKey();
			if (voteCount > maxVote) {
				truthValue = value;
				maxVote = voteCount;
			}
		}
		return truthValue;
	}

	private static void groupTuples(LinkedList<Tuple> tuples) {
		
		String[] fields = new String[]{"CUID"};
		for (String field : fields) {
			HashMap<String, LinkedList<Tuple>> hashMap = new HashMap<String, LinkedList<Tuple>>();
			for (Tuple tuple : tuples) {
				String value = tuple.getValue(field);
				if (hashMap.containsKey(value)) {
					hashMap.get(value).add(tuple);
				} else {
					LinkedList<Tuple> list = new LinkedList<Tuple>();
					list.add(tuple);
					hashMap.put(value, list);
				}
			}
			groupedTuples.put(field, hashMap);
		}
	}

	private static void generateColumnNames() {
		StringBuffer sb = new StringBuffer();
		for (String field : FIELDS) {
			sb.append(field).append(":");
		}
		sb.deleteCharAt(sb.length() - 1);
		COLUMNNAMES = new ColumnNames(sb.toString());	
	}

	/*
	 * 修复truthValue
	 */
	private static String checkValue(String field, Tuple tuple) {
		String result = tuple.getValue(field);
		switch (field){
			case "CUID":
				break;
			case "SSN":
				result = checkSingleSSN(tuple);
				break;
			case "FNAME":
				result = checkSingleFNAME(tuple);
				break;
			case "MINIT":
				result = checkSingleMINIT(tuple);
				break;
			case "LNAME":
				result = checkSingleLNAME(tuple);
				break;
			case "STNUM":
				result = checkSingleSTNUM(tuple);
				break;
			case "STADD":
				result = checkSingleSTADD(tuple);
				break;
			case "APMT":
				result = checkSingleAPMT(tuple);
				break;
			case "CITY":
				result = checkSingleCITY(tuple);
				break;
			case "STATE":
				result = checkSingleSTATE(tuple);
				break;
			case "ZIP":
				result = checkSingleZIP(tuple);
				break;	
		}
		return result;
	}

	/*
	 * 邮政编码,五位纯数字
	 */
	public static boolean checkZIPFormat(String value){
		String regex = "^\\d{5}$";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(value);
		return m.matches();
	}
	private static String checkSingleZIP(Tuple tuple) {
		String value = tuple.getValue("ZIP");
		if (checkZIPFormat(value)){
			return value;
		} else {
			//TODO	
			return value + "-NeedRepair";
		}

	}

	/*
	 * 为美国的50个州的简称
	 * 这里的Metadata是错的！不光有50个州，还有DC(特区)，GU(关岛)等附属
	 */
	private static String checkSingleSTATE(Tuple tuple) {
		String value = tuple.getValue("STATE");
		if (STATES.contains(value)) {
			return value;
		} else {
			String regex = "[a-z]+";
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(value);
			if (m.matches()){
				return value.toUpperCase();
			} else {
				//TODO
				return value + "-NeedRepair";
			}
		}
	}
	

	/*
	 * 可能包含字母+五种标点符号'-/. (空格也算一种)
	 */
	private static String checkSingleCITY(Tuple tuple) {
		String value = tuple.getValue("CITY");
		String regex = "[a-zA-Z'-/\\. ]*";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(value);
		if (m.matches()) {
			return value;
		} else {
			//TODO
			return value + "-NeedRepair";		
		}
	}

	/*
	 * 公寓房号,3位,第1位和第3位为数字,第2位为小写字母.
	 * STADD属性为“PO Box xxxx”时该属性为空
	 */
	private static String checkSingleAPMT(Tuple tuple) {
		String apmt = tuple.getValue("APMT");
		if ( checkSTADDforSTNUMandAPMT(tuple) ) {
			if (apmt.isEmpty()) {
				return apmt;
			} else {
				//TODO
				return apmt + "-NeedRepair";
			}
		} else {
			String regex = "^\\d[a-z]\\d$";
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(apmt);
			if (m.matches()){
				return apmt;
			} else {
				//如果是w27这样的，修正为2w7
				regex = "^[a-z]\\d{2}$";
				p = Pattern.compile(regex);
				m = p.matcher(apmt);
				if (m.matches()){
					return apmt.substring(1, 2) + apmt.substring(0, 1) + apmt.substring(2);
				}else{
					p = Pattern.compile("^\\d{2}[a-z]");
					m = p.matcher(apmt);
					if (m.matches()){
						return apmt.substring(0, 1) + apmt.substring(2) + apmt.substring(1, 2);
					} else{
						p = Pattern.compile("\\d[A-Z]\\d");
						m = p.matcher(apmt);
						if (m.matches()){
							return apmt.toLowerCase();
						} else {
							return apmt + "-NeedRepair";
						}
					}		
				}
				
			}
		}	
	}

	/*
	 * 能包含字母,空格,逗号及句号,或者为“PO Box xxxx”,其中"xxxx"为1-4位纯数字.
	 * 若为“PO Box xxxx”,则STNUM和APMT属性皆为空
	 */
	private static String checkSingleSTADD(Tuple tuple) {
		String stadd = tuple.getValue("STADD");
		String regex = "[a-zA-Z, \\.]+";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(stadd);
		if ( m.matches() ){
			return stadd;
		}else{
			regex = "PO Box \\d{1,4}";
			p = Pattern.compile(regex);
			m = p.matcher(stadd);
			if (m.matches()){
				if (tuple.getValue("STNUM").isEmpty() && 
						tuple.getValue("APMT").isEmpty()) {
					return stadd;
				} else {
					//TODO
					return stadd + "NeedRepair";
				}
			} else {
				//TODO
				return stadd + "NeedRepair";
			}
		}
	}

	/*
	 * 1-4位纯数字, STADD属性为“PO Box xxxx”时该属性为空
	 */
	private static String checkSingleSTNUM(Tuple tuple) {
		String stunum = tuple.getValue("STNUM");
		if ( checkSTADDforSTNUMandAPMT(tuple) ) {
			if (stunum.isEmpty()) {
				return stunum;
			} else {
				//TODO
				return stunum + "NeedRepair";
			}
		} else {
			String regex = "\\d{1,4}";
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(stunum);
			if (m.matches()) {
				return stunum;
			} else {
				//TODO
				return stunum + "-NeedRepair";
			}
		}	
	}

	/*
	 * STADD属性为“PO Box xxxx”时STUNUM属性为空
	 */
	private static boolean checkSTADDforSTNUMandAPMT(Tuple tuple) {
		String regex = "PO Box \\d{1,4}";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(tuple.getValue("STADD"));
		return m.matches();
	}

	/*
	 * 可能包含字母,逗号及句号,首字母大写
	 */
	private static String checkSingleLNAME(Tuple tuple) {
		String lname = tuple.getValue("LNAME");
		if (checkNAME(lname)){
			return lname;
		} else {
			//TODO
			return lname + "-NeedRepair";
		}
	}

	/*
	 * 可为空,或为1位大写字母
	 */
	private static String checkSingleMINIT(Tuple tuple) {
		String minit = tuple.getValue("MINIT");
		if (minit.isEmpty()){
			return minit;
		} else {
			String regex = "[A-Z]{1}";
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(minit);
			if (m.matches()) {
				return minit;
			} else {
				//TODO
				return minit + "-NeedRepair";
			}
		}
	}

	/*
	 * 可能包含字母,逗号及句号,首字母大写
	 */
	private static String checkSingleFNAME(Tuple tuple) {
		String fname = tuple.getValue("FNAME");
		if (checkNAME(fname)){
			return fname;
		} else {
			//TODO
			return fname + "-NeedRepair";
		}
	}
	
	private static boolean checkNAME(String name){
		String regex = "^[A-Z][a-zA-Z,\\.]*";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(name);
		return m.matches();
	}

	/*
	 * SSN 9 位纯数字
	 * */
	private static String checkSingleSSN(Tuple tuple) {
		String ssn = tuple.getValue("SSN");
		String regex = "\\d{9}";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(ssn);
		if (m.matches()){
			return ssn;
		} else {
			//TODO
			return ssn + "-NeedRepair";
		}
	}

}
