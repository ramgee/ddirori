package org.beatific.ddirori.attribute;

import java.lang.reflect.Method;
import java.util.regex.Matcher;

import org.beatific.ddirori.attribute.caster.AttributeTypeCaster;
import org.beatific.ddirori.attribute.caster.DefinitionTypeCaster;
import org.beatific.ddirori.bean.BeanContainer;

public abstract class AttributeExtractor {

	private final static String REGEX_DEFINITION = "\\$\\[(\\w*)\\]";
	private final static String REGEX_TYPE = "\\$(\\w+)\\[(\\w*)\\]";
	private final static String REGEX_METHOD = "(\\w+)\\.(\\w+)\\(([\\w\\,\\s\\'\\-\\.\\$\\[\\]]*)\\)";
	private final static String REGEX_OBJECT = "\\$\\{(\\w*)\\}";
	private final static String REGEX_STRING = "\\$S\\{([^\\{S\\}]*)\\}";
	private AttributeTypeCaster typeCaster = null;
	private DefinitionTypeCaster definitionCaster = null;
	private boolean isUseDDiroriExpression = false;
	
	public AttributeExtractor(String[] basePackage) {
		this(basePackage, false);
	}
	
	public AttributeExtractor(String[] basePackage, boolean isUseDDiroriExpression) {
		this.typeCaster = new AttributeTypeCaster(basePackage);
		this.typeCaster.init();
		this.definitionCaster = new DefinitionTypeCaster(basePackage);
		this.definitionCaster.init();
		this.isUseDDiroriExpression = isUseDDiroriExpression;
	}
	
	protected Object processReservedWord(String word) {
		return definitionCaster.cast(word);
	}
	
	protected Object processType(BeanContainer container,String type, String attribute) {
		Object object = extractAttribute(container, attribute);
		return this.typeCaster.cast(type, object);
	}
	
	public Object extract(BeanContainer container, String attribute) {
		return extractAttribute(container, attribute);
	}
	
    protected Object extractAttribute(BeanContainer container, String attribute) {
		
    	if(this.isUseDDiroriExpression == false)  return attribute;
    	Matcher objectMatcher = Parser.getMatcher(attribute, REGEX_OBJECT);
    	Matcher stringMatcher = Parser.getMatcher(attribute, REGEX_STRING);
		Matcher definitionMatcher = Parser.getMatcher(attribute, REGEX_DEFINITION);
		Matcher typeMatcher = Parser.getMatcher(attribute, REGEX_TYPE);
		
		StringBuffer buffer = new StringBuffer();
		
		if(definitionMatcher.find()) {
			
			definitionMatcher.appendReplacement(buffer, (String)processReservedWord(definitionMatcher.group(1)));
			definitionMatcher.appendTail(buffer);
			
			return extractAttribute(container, buffer.toString());
		}
		
		if(typeMatcher.find()) {
			return processType(container, typeMatcher.group(1), typeMatcher.group(2));
		}
		
		if(objectMatcher.find()) {
			return processMethod(container, objectMatcher.group(1));
		}
		
	    while (stringMatcher.find()) {
	      Object resultCandidator = processMethod(container, stringMatcher.group(1));
	      
	      if(resultCandidator == null)return null;
	      stringMatcher.appendReplacement(buffer, String.valueOf(resultCandidator));
	    }
	    stringMatcher.appendTail(buffer);
	    
	    return buffer.toString();
	}
    
    protected abstract Object getObject(BeanContainer container, String objectName);
    
    protected Object processMethod(BeanContainer container, String attribute) {
    	
	    Object result = null;
	      
    	Matcher methodMatcher = Parser.getMatcher(attribute, REGEX_METHOD);
	      if (methodMatcher.find()) {
	    	  String objectName = methodMatcher.group(1);
	    	  String methodName = methodMatcher.group(2);
	    	  String[] args = methodMatcher.group(3).split(",");
	    	  Object object = getObject(container, objectName);
	    	  result = invoke(object, methodName, TransferObjectType(container, args));
	      } else {
	          result = getObject(container, attribute);
	      }
	      return result;
    }
	
	protected Object invoke(Object object, String methodName, Object[] args) {
		if(object == null)return null;
		Class<?> clazz = object.getClass();
		Method method = null;
		Object result = null;
		
		try {
			method = clazz.getMethod(methodName, getObjectTypeList(args));
			result = method.invoke(object, args);
		} catch (Exception e) {}
		
		return result;
	}
	
	private Class<?>[] getObjectTypeList(Object[] args) {
		Class<?>[] classes = new Class<?>[args.length];
		int index = 0;
		for(Object arg : args) {
			classes[index++] = arg.getClass();
		}
		return classes;
	}
	
	private Object[] TransferObjectType(BeanContainer container, String[] args) {
		
		Object[] objects = new Object[args.length];
		int index = 0;
		
		for(String arg : args) {
			
			arg = arg.trim();
			
			objects[index++] = extractAttribute(container, arg);
		}
		return objects;
	}
	
}
