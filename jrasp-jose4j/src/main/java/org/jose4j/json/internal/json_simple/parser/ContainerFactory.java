package org.jose4j.json.internal.json_simple.parser;

import java.util.List;
import java.util.Map;

/**
 * Container factory for creating containers for JSON object and JSON array.
 * 
 * @see org.jose4j.json.internal.json_simple.parser.JSONParser#parse(java.io.Reader, ContainerFactory)
 * 
 * @author (originally) FangYidong<fangyidong@yahoo.com.cn>
 */
public interface ContainerFactory {
	/**
	 * @return A Map instance to store JSON object, or null if you want to use org.jose4j.json.org.json.json_simple.JSONObject.
	 */
	Map createObjectContainer();
	
	/**
	 * @return A List instance to store JSON array, or null if you want to use org.jose4j.json.org.json.json_simple.JSONArray.
	 */
	List creatArrayContainer();
}
