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
	
	//根据CUID归类的tuples
	private static HashMap<String, LinkedList<Tuple>> groupedTuples = new HashMap<String, LinkedList<Tuple>>();
	
	//根据groupedTuples,计算truthTuple
	private static HashMap<String, Tuple> truthTuples = new HashMap<String, Tuple>();
	
	//fields
	private static final String[] FIELDS = new String[]{"CUID", "SSN", "FNAME", "MINIT", "LNAME", "STNUM", "STADD",
			"APMT", "CITY", "STATE", "ZIP"};
	
	//columnNames
	private static ColumnNames COLUMNNAMES;
	
	public static HashSet<RepairedCell> performance(HashSet<RepairedCell> result, LinkedList<Tuple> tuples){
		/*
		 * 初始化辅助变量
		 * groupedTuples
		 * truthTuples
		 */
		
		initAux(tuples);
		/*
		 * 遍历一次tuples
		 * 进行单行单列处理（对比metadata进行检查和修复）
		 */
		for (Tuple tuple: tuples){
			Tuple truthTuple = truthTuples.get(tuple.getValue("CUID"));
			if (!checkSingleSSN(tuple)){
				result.add(new RepairedCell(Integer.parseInt(tuple.getValue("RUID")), 
						"SSN", 
						truthTuple.getValue("SSN")));
			}
			if (!checkSingleFNAME(tuple)){
				result.add(new RepairedCell(Integer.parseInt(tuple.getValue("RUID")), 
						"FNAME", 
						truthTuple.getValue("FNAME")));
			}
			if (!checkSingleMINIT(tuple)){
				result.add(new RepairedCell(Integer.parseInt(tuple.getValue("RUID")), 
						"MINIT", 
						truthTuple.getValue("MINIT")));
			}
			if (!checkSingleLNAME(tuple)){
				result.add(new RepairedCell(Integer.parseInt(tuple.getValue("RUID")), 
						"LNAME", 
						truthTuple.getValue("LNAME")));
			}
			if (!checkSingleSTNUM(tuple)){
				result.add(new RepairedCell(Integer.parseInt(tuple.getValue("RUID")), 
						"STNUM", 
						truthTuple.getValue("STNUM")));
			}
			if (!checkSingleSTADD(tuple)){
				result.add(new RepairedCell(Integer.parseInt(tuple.getValue("RUID")), 
						"STADD", 
						truthTuple.getValue("STADD")));
			}
			if (!checkSingleAPMT(tuple)){
				result.add(new RepairedCell(Integer.parseInt(tuple.getValue("RUID")), 
						"APMT", 
						truthTuple.getValue("APMT")));
			}
			if (!checkSingleCITY(tuple)){
				result.add(new RepairedCell(Integer.parseInt(tuple.getValue("RUID")), 
						"CITY",
						truthTuple.getValue("CITY")));
			}
			if (!checkSingleSTATE(tuple)){
				result.add(new RepairedCell(Integer.parseInt(tuple.getValue("RUID")), 
						"STATE", 
						truthTuple.getValue("STATE")));
			}
			if (!checkSingleZIP(tuple)){
				result.add(new RepairedCell(Integer.parseInt(tuple.getValue("RUID")), 
						"ZIP", 
						truthTuple.getValue("ZIP")));
			}
			//一致性检查
			//checkCUIDConsistence(tuple, result);
		}
	
		return result;
	}
	
	/*
	 * 一致性检查
	 */
	private static void checkCUIDConsistence(Tuple tuple, HashSet<RepairedCell> result) {
		Tuple truthTuple = truthTuples.get(tuple.getValue("CUID"));
		for (String field : FIELDS) {
			String value = tuple.getValue(field);
			String truthValue = truthTuple.getValue(field);
			if (value != truthValue){
				result.add(new RepairedCell(
						Integer.parseInt(tuple.getValue("RUID")), 
						field, 
						truthValue));
			}
		}
	}

	/*
	 * 初始化辅助数据
	 * groupedTuples
	 * truthTruples
	 */
	private static void initAux(LinkedList<Tuple> tuples) {
		//根据FIELDS生成COLUMNNAMES
		StringBuffer sb = new StringBuffer();
		for (String field : FIELDS) {
			sb.append(field).append(":");
		}
		sb.deleteCharAt(sb.length() - 1);
		COLUMNNAMES = new ColumnNames(sb.toString());
		//初始化groupedTuples
		for (Tuple tuple : tuples){
			String cuid = tuple.getValue("CUID");
			if (groupedTuples.containsKey(cuid)) {
				groupedTuples.get(cuid).add(tuple);
			} else {
				LinkedList<Tuple> list = new LinkedList<Tuple>();
				list.add(tuple);
				groupedTuples.put(cuid, list);
			}
		}
		
		//根据most vote原则，计算truthTuples
		for (Entry entry : groupedTuples.entrySet()){
			String cuid = (String)entry.getKey();
			LinkedList<Tuple> list = (LinkedList<Tuple>)entry.getValue();
			StringBuffer truthValues = new StringBuffer();
			for (String field : FIELDS) {
				HashMap<String, Integer> voteBox = new HashMap<String, Integer>();
				String truthValue = "";
				int maxVote = Integer.MIN_VALUE;
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
				truthValues.append(truthValue).append(":");				
			}
			truthValues.deleteCharAt(truthValues.length() - 1);
			truthTuples.put(cuid, new Tuple(COLUMNNAMES, truthValues.toString()));
		}
		
	}

	/*
	 * 邮政编码,五位纯数字
	 */
	private static boolean checkSingleZIP(Tuple tuple) {
		String regex = "^\\d{5}$";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(tuple.getValue("ZIP"));
		return m.matches();

	}

	/*
	 * 为美国的50个州的简称
	 * 这里的Metadata是错的！不光有50个州，还有DC(特区)，GU(关岛)等附属
	 */
	private static boolean checkSingleSTATE(Tuple tuple) {
		return STATES.contains(tuple.getValue("STATE"));
	}

	/*
	 * 可能包含字母+五种标点符号'-/. (空格也算一种)
	 */
	private static boolean checkSingleCITY(Tuple tuple) {
		String regex = "[a-zA-Z'-/\\. ]*";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(tuple.getValue("CITY"));
		return m.matches();
	}

	/*
	 * 公寓房号,3位,第1位和第3位为数字,第2位为小写字母.
	 * STADD属性为“PO Box xxxx”时该属性为空
	 */
	private static boolean checkSingleAPMT(Tuple tuple) {
		String apmt = tuple.getValue("APMT");
		if ( checkSTADDforSTNUMandAPMT(tuple) ) {
			return apmt.isEmpty();
		} else {
			String regex = "^\\d[a-z]\\d$";
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(apmt);
			return m.matches();
		}	
	}

	/*
	 * 能包含字母,空格,逗号及句号,或者为“PO Box xxxx”,其中"xxxx"为1-4位纯数字.
	 * 若为“PO Box xxxx”,则STNUM和APMT属性皆为空
	 */
	private static boolean checkSingleSTADD(Tuple tuple) {
		String stadd = tuple.getValue("STADD");
		String regex = "[a-zA-Z, \\.]+";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(stadd);
		if ( m.matches() ){
			return true;
		}else{
			regex = "PO Box \\d{1,4}";
			p = Pattern.compile(regex);
			m = p.matcher(stadd);
			if (m.matches()){
				return tuple.getValue("STNUM").isEmpty() && 
						tuple.getValue("APMT").isEmpty();
			} else {
				return false;
			}
		}
	}

	/*
	 * 1-4位纯数字, STADD属性为“PO Box xxxx”时该属性为空
	 */
	private static boolean checkSingleSTNUM(Tuple tuple) {
		String stunum = tuple.getValue("STNUM");
		if ( checkSTADDforSTNUMandAPMT(tuple) ) {
			return stunum.isEmpty();
		} else {
			String regex = "\\d{1,4}";
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(stunum);
			return m.matches();
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
	private static boolean checkSingleLNAME(Tuple tuple) {
		return checkNAME(tuple.getValue("LNAME"));
	}

	/*
	 * 可为空,或为1位大写字母
	 */
	private static boolean checkSingleMINIT(Tuple tuple) {
		String minit = tuple.getValue("MINIT");
		if (minit.isEmpty()){
			return true;
		} else {
			String regex = "[A-Z]{1}";
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(minit);
			return m.matches();
		}
	}

	/*
	 * 可能包含字母,逗号及句号,首字母大写
	 */
	private static boolean checkSingleFNAME(Tuple tuple) {
		return checkNAME(tuple.getValue("FNAME"));
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
	private static boolean checkSingleSSN(Tuple tuple) {
		String regex = "\\d{9}";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(tuple.getValue("SSN"));
		return m.matches();
	}

}
