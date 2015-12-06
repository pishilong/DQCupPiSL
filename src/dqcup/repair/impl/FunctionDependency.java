package dqcup.repair.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import dqcup.repair.Tuple;

public class FunctionDependency {
	private static HashMap<String, HashMap<String, LinkedList<Tuple>>> groupedTruthTuples = new HashMap<String, HashMap<String, LinkedList<Tuple>>>();
	
	private static void groupTruthTuples(Collection<Tuple> truthTuples, String field) {
		groupedTruthTuples.remove(field);
		HashMap<String, LinkedList<Tuple>> hashMap = new HashMap<String, LinkedList<Tuple>>();
		for (Tuple tuple : truthTuples) {
			String value = tuple.getValue(field);
			if (hashMap.containsKey(value)) {
				hashMap.get(value).add(tuple);
			} else {
				LinkedList<Tuple> list = new LinkedList<Tuple>();
				list.add(tuple);
				hashMap.put(value, list);
			}
		}
		groupedTruthTuples.put(field, hashMap);
	}

	public static HashMap<String, Tuple> performance(HashMap<String, Tuple> truthTuples) {
		groupedTruthTuples.clear();
		groupTruthTuples(truthTuples.values(), "CITY");
		groupTruthTuples(truthTuples.values(), "FNAME");
		
		truthTuples = checkAndRepairField(truthTuples, "ZIP");
		truthTuples = checkAndRepairField(truthTuples, "APMT");
		
		groupTruthTuples(truthTuples.values(), "APMT");
		truthTuples = checkAndRepairField(truthTuples, "STATE");
		
		
		
		return truthTuples;
	}
	
	private static HashMap<String, Tuple> checkAndRepairField(HashMap<String, Tuple> truthTuples, String field) {
		for (Tuple tuple : truthTuples.values()){
			StringBuffer sb = new StringBuffer();
			for (String _field : DataProfolling.FIELDS){
				if (_field.equals(field)){
					sb.append(checkValue(field, tuple)).append(":");
				} else {
					sb.append(tuple.getValue(_field)).append(":");
				}
			}
			sb.deleteCharAt(sb.length() - 1);
			truthTuples.put(tuple.getValue("CUID"), 
					new Tuple(DataProfolling.COLUMNNAMES, sb.toString()));
			truthTuples.remove(tuple);
		}
		return truthTuples;	
	}

	/*
	 * 修复truthValue
	 */
	public static String checkValue(String field, Tuple tuple) {
		String result = tuple.getValue(field);
		switch (field){
			case "CUID":
				break;
			case "SSN":
				break;
			case "FNAME":
				break;
			case "MINIT":
				break;
			case "LNAME":
				break;
			case "STNUM":
				break;
			case "STADD":
				break;
			case "APMT":
				result = checkAPMT(tuple);
				break;
			case "CITY":
				break;
			case "STATE":
				result = checkSTATE(tuple);
				break;
			case "ZIP":
				result = checkZIP(tuple);
				break;	
		}
		return result;
	}

	private static String checkAPMT(Tuple tuple) {
		// FNAME, STNUM -> APMT
		String stnum = tuple.getValue("STNUM");
		LinkedList<Tuple> sameFNAMETuples = groupedTruthTuples.get("FNAME").get(tuple.getValue("FNAME"));
		LinkedList<Tuple> sameSTNUMTuples = new LinkedList<Tuple>();
		for (Tuple _tuple : sameFNAMETuples){
			String _stnum = _tuple.getValue("STNUM");
			if (_stnum.equals(stnum)){
				sameSTNUMTuples.add(_tuple);
			}
		}
		if (sameSTNUMTuples.size() > 2){
			return DataProfolling.voteTruthValue(sameSTNUMTuples, "APMT");
		}else{
			return tuple.getValue("APMT");
		}
	}

	private static String checkSTATE(Tuple tuple) {
		// APMT, ZIP -> STATE
		String zip = tuple.getValue("ZIP");
		LinkedList<Tuple> sameAPMTTuples = groupedTruthTuples.get("APMT").get(tuple.getValue("APMT"));
		LinkedList<Tuple> sameZIPTuples = new LinkedList<Tuple>();
		for (Tuple sameAPMTTuple : sameAPMTTuples){
			String _zip = sameAPMTTuple.getValue("ZIP");
			if (zip != null && zip.equals(_zip)){
				sameZIPTuples.add(sameAPMTTuple);
			}
		}
		String truthValue = DataProfolling.voteTruthValue(sameZIPTuples, "STATE");
		return truthValue;
	}

	private static String checkZIP(Tuple tuple) {
		// CITY, STADD -> ZIP
		String value = tuple.getValue("ZIP");
		String stadd = tuple.getValue("STADD");
		String cuid = tuple.getValue("CUID");
		
		LinkedList<Tuple> sameFNAMETuples = groupedTruthTuples.get("FNAME").get(tuple.getValue("FNAME"));
		LinkedList<Tuple> sameNameTuples = new LinkedList<Tuple>();
		for (Tuple _tuple : sameFNAMETuples){
			String _zip = _tuple.getValue("ZIP");
			if (tuple.getValue("MINIT").equals(_tuple.getValue("MINIT")) &&
					tuple.getValue("LNAME").equals(_tuple.getValue("LNAME")) && 
					DataProfolling.checkZIPFormat(_zip)){
				sameNameTuples.add(_tuple);
			}
		}
		String truthValue = "";
		if (sameNameTuples.size() > 1) {
			truthValue = DataProfolling.voteTruthValue(sameNameTuples, "ZIP");
		} else if(sameNameTuples.size() == 1 &&
				!sameNameTuples.get(0).getValue("CUID").equals(cuid)){
			truthValue = DataProfolling.voteTruthValue(sameNameTuples, "ZIP");
		}else{
			LinkedList<Tuple> sameCITYTuples = groupedTruthTuples.get("CITY").get(tuple.getValue("CITY"));
			LinkedList<Tuple> sameSTADDTules = new LinkedList<Tuple>();
			for (Tuple sameCITYTuple : sameCITYTuples){
				String _stadd = sameCITYTuple.getValue("STADD");
				String _cuid = sameCITYTuple.getValue("CUID");
				String _zip = sameCITYTuple.getValue("ZIP");
				if (stadd.equals(_stadd) && DataProfolling.checkZIPFormat(_zip)){
					sameSTADDTules.add(sameCITYTuple);
				}	
			}
			if (sameSTADDTules.size() == 2 && DataProfolling.checkZIPFormat(value)){
				truthValue = value;
			} else {
				truthValue = DataProfolling.voteTruthValue(sameSTADDTules, "ZIP");
			}
		}
		return truthValue;		
	}
	
}
