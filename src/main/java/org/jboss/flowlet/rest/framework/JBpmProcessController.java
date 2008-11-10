package org.jboss.flowlet.rest.framework;

import java.io.Serializable;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

public class JBpmProcessController implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public Long createProcessInstance(String processDefintion, String user){
	JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
	try {
		ProcessInstance instance = ctx.newProcessInstance(processDefintion);
		instance.getContextInstance().setVariable("user", user);
		Token t = instance.getRootToken();
		t.signal();
		
	    ctx.save(instance);

		return instance.getId();

	} finally {
		ctx.close();
	}
	}
	
	public Long createProcessInstanceFromDataBase(String processDefintion,Long version, String user){
		JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
		try {
			ProcessInstance instance = ctx.newProcessInstance(processDefintion);
			instance.getContextInstance().setVariable("user", user);
			Token t = instance.getRootToken();
			t.signal();
			
		    ctx.save(instance);

			return instance.getId();

		} finally {
			ctx.close();
		}
		}	

}
