package org.jose4j.json.internal.json_simple;

/**
 * Beans that support customized output of JSON text shall implement this interface.  
 * @author (originally) FangYidong<fangyidong@yahoo.com.cn>
 */
public interface JSONAware {
	/**
	 * @return JSON text
	 */
	String toJSONString();
}
