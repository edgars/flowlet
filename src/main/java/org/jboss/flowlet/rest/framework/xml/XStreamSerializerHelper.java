package org.jboss.flowlet.rest.framework.xml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

public class XStreamSerializerHelper {
	
	protected static XStream xstreamJSON = new XStream(new JettisonMappedXmlDriver());
	protected static XStream xstreamXML = new XStream();
	
	protected static XStreamSerializerHelper me;
	
	private XStreamSerializerHelper(){
		
		xstreamXML.alias("process-definition", org.jbpm.graph.def.ProcessDefinition.class);
		
		xstreamXML.alias("process-instance", org.jbpm.graph.exe.ProcessInstance.class);
	
		
	}
	
	public static synchronized XStreamSerializerHelper getInstance(){
		
		if (null==me){
			
			me =  new XStreamSerializerHelper();
		}
		return me;
	}
	
	public String toXML(Object obj){
		
		return xstreamXML.toXML(obj);
	}
	
	public String toJSON(Object obj){
		
		return xstreamJSON.toXML(obj);
	}
	
		

}
