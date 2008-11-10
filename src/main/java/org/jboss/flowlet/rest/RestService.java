package org.jboss.flowlet.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jboss.flowlet.rest.framework.FileCopy;
import org.jboss.flowlet.rest.framework.TextFileUtil;
import org.jboss.flowlet.rest.framework.xml.XStreamSerializerHelper;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.db.GraphSession;
import org.jbpm.file.def.FileDefinition;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.persistence.db.DbPersistenceService;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.taskmgmt.exe.TaskInstance;


import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * This is the RestService controller to handle the process execution
 * @author Edgar Silva
 * */

@Provider
@Path("/")
public class RestService implements Serializable { 
	private static final long serialVersionUID = -550878933446485830L;
	protected ProcessDefinition processDefinition;

	
	@GET
	@Path("/")
	@ProduceMime("text/plain")
	public String home(){
		
		List<java.lang.reflect.Method> methods = Arrays.asList(this.getClass().getDeclaredMethods());
		
		StringBuilder b = new StringBuilder();
		
        for (java.lang.reflect.Method m : methods) {
        	
        	if (null != m.getAnnotation(Path.class)) {
        		
        		Path p = m.getAnnotation(Path.class);
        		
        		b.append(  String.format("Method %s with annotation: %s \n", m.getName(), p.value() ) );
        		
        	}
        	
        	
			
		}
		
		return b.toString();
	}
	
	@GET
	@Path("/test")
	@ProduceMime("text/html")
	public String testRS(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException, ServletException{
		
		List<java.lang.reflect.Method> methods = Arrays.asList(this.getClass().getDeclaredMethods());
		
		StringBuilder b = new StringBuilder();
		
		
		
		b.append("<html><title>JBoss RestEasy - WebMethods </title><body><font face='verdana'><h2> Your Service contains the following available methods</h2>");
		b.append(String.format("<p>Application Context:%s </p><hr size=1>",request.getContextPath()));
		b.append("<ul>");
		
        for (java.lang.reflect.Method m : methods) {
        	
        	if (null != m.getAnnotation(Path.class)) {
        		
        		Path p = m.getAnnotation(Path.class);
        		
        		b.append(  String.format("<li>Method <bold>%s</bold> : <a href='%s%s'>%s</a> \n</li>", m.getName(), request.getContextPath(),p.value(),p.value() ) );
        		
        	}
        	
        	
			
		}
		b.append("</ul></font></body></html>");
		
		 
		return b.toString();
		
	
	}

	
	
	@GET
	@Path("/start/process/{processdefinition}/{user}")
	@ProduceMime("text/plain")
	public String startProcesInstance(
			@PathParam("processdefinition")String processDefintion, 
			@PathParam("user")String user) {
		
		JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
		
		try {
			ProcessInstance instance = ctx.newProcessInstance(processDefintion);
			instance.getContextInstance().setVariable("user", user);
			Token t = instance.getRootToken();
			t.signal();
		    ctx.save(instance);

			return new Long(instance.getId()).toString();
 		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			ctx.close();
		}
		return "ERROR";

	}
	
	
	@GET
	@Path("/signal/process/{id}/{user}")
	@ProduceMime("text/plain")
	public String signalProcess(
			@PathParam("id")String id, 
			@PathParam("user")String user) {
	      JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
	 
	        try{
	        GraphSession graphSession = ctx.getGraphSession();
	        ProcessInstance processInstance = graphSession.loadProcessInstance((new Long(id)).longValue());
             
	        processInstance.signal();
	        
	        ctx.save(processInstance);
	       
	        
	        return processInstance.getRootToken().getNode().getName(); 
	        
	        } catch (Exception e ) {
	        	e.printStackTrace();
	        	return "ERROR";
	        }
	        finally {
	        	ctx.close();
	        }
	}
	
	
	@SuppressWarnings("unchecked")
	@GET
	@Path("/process/{id}/add/var")
	@ProduceMime("text/plain")
	public String addVariables(
			@PathParam("id")String id,
			@Context HttpServletRequest request) {
	      JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
	 
	        try{
	        GraphSession graphSession = ctx.getGraphSession();
	        ProcessInstance processInstance = 
	        	graphSession.loadProcessInstance((new Long(id)).longValue());
             
	        Enumeration  params = request.getParameterNames();
	        String param;
	        StringBuilder b = new StringBuilder();
	        while (params.hasMoreElements()) {
	        	
	        	param = (String) params.nextElement();
	        	processInstance.getContextInstance().setVariable(param, request.getParameter(param));
	        	b.append(String.format("Param:%s=%s is Stored in BPM Context\n",param,request.getParameter(param)));
	        }
	        
	        
	        ctx.save(processInstance);
	       
	        
	        return b.toString(); 
	        
	        } catch (Exception e ) {
	        	e.printStackTrace();
	        	return "ERROR";
	        }
	        finally {
	        	ctx.close();
	        }
	}
	
	@GET
	@Path("/invoke/message/esb/{message}")
	@ProduceMime("text/xml")
	public String publishESBMessage(
			@PathParam("message")String message) {
		
		return XStreamSerializerHelper.getInstance().toXML(message);
		
	}

	@GET
	@Path("/invoke/message/esb/{message}/json")
	@ProduceMime("text/plain")
	public String publishESBMessageAsJSON(
			@PathParam("message")String message) {

		return XStreamSerializerHelper.getInstance().toJSON(message);
		
	}
	
	
	
	@SuppressWarnings("unchecked")
	@GET
	@Path("/process/definitions")
	@ProduceMime("text/xml")
	public String getAllProcessDefinitions() {
		JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
 		XStream xstream = new XStream();
 		xstream.alias("process-definition", org.jbpm.graph.def.ProcessDefinition.class);
		List list;  
 		
		try{
		list = ctx.getGraphSession().findAllProcessDefinitions();
 	 		} finally {
 			ctx.close();
 		}
 
		return xstream.toXML(list);

	}
	@SuppressWarnings("unchecked")
	@GET
	@Path("/process/definitions/json")
	@ProduceMime("text/plain")
	public String getAllProcessDefinitionsAsJSON() {
		JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
 		xstream.alias("process-definition", org.jbpm.graph.def.ProcessDefinition.class);
		List list;  
 		
		try{
		list = ctx.getGraphSession().findAllProcessDefinitions();
 	 		} finally {
 			ctx.close();
 		}
		return xstream.toXML(list);

	}
	
	@SuppressWarnings({ "unchecked", "static-access" })
	@GET
	@Path("/process/instances/")
	@ProduceMime("text/plain")
	public String getAllProcessInstances() {
		JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
		DbPersistenceService dbService = new DbPersistenceService((DbPersistenceServiceFactory) ctx.getServices().getServiceFactory(ctx.getServices().SERVICENAME_PERSISTENCE));
		Session session = dbService.getSession();

		 List result = null;
		    try {
		      Query query = session.getNamedQuery("Flowlet.ProcessInstances");
		      result = query.list();
		    } catch (Exception e) {
		    	e.printStackTrace();
		    }finally {
		    	
		    	session.close();
		    } 
		
	    return XStreamSerializerHelper.getInstance().toXML(result);
	}

	@POST
	@Path("/deploy/process/jar")
	@ProduceMime("text/plain")
	public String deployProcessJAR(@QueryParam("f")String fileName) {
		String jbossRunningDir =   Thread.currentThread().getContextClassLoader().getResource("/web/index.jsp").getPath();
		jbossRunningDir =   jbossRunningDir.substring(0, jbossRunningDir.lastIndexOf("/tmp/"));
		try {
			FileCopy.copy(fileName, jbossRunningDir+"/deploy");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "OK";
		

	}
	
	@GET
	@Path("/study")
	@ProduceMime("text/plain")
	public String study(@Context HttpServletRequest req) {
		
		HttpServletRequest request = (HttpServletRequest)req;
		
		return request.getParameter("name");
		

	}
	
	@SuppressWarnings("unchecked")
	@POST
	@Path("/upload/process")
	@ProduceMime("text/plain")
	public String uploadProcess(@Context HttpServletRequest req) throws Exception {

		String jbossRunningDir =  
		Thread.currentThread().getContextClassLoader().getResource("/web/index.jsp").getPath();
		
		jbossRunningDir =   jbossRunningDir.substring(0, jbossRunningDir.lastIndexOf("/tmp/"));
		
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
		List  items = upload.parseRequest(req);
		Iterator iter = items.iterator();
		while (iter.hasNext()) {
		    FileItem item = (FileItem) iter.next();

		    if (item.isFormField()) {
		        System.out.println("FORM FIELD");
		    } else {
		    	if (!item.isFormField()) {
		    	    String fileName = item.getName();
		    	    System.out.println("File Name:" + fileName);
		    		File fullFile  = new File(item.getName());  
		    		File savedFile = new File(jbossRunningDir+"/deploy", fullFile.getName());
		    		item.write(savedFile);
		    	}
		    }
		}
		return "OK";
	}

	
	
	@SuppressWarnings({ "static-access", "unchecked" })
	@GET
	@Path("/process/instances/json")
	@ProduceMime("text/plain")
	public String getAllProcessInstancesAsJSON() {
		
		JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
		DbPersistenceService dbService = new DbPersistenceService((DbPersistenceServiceFactory) ctx.getServices().getServiceFactory(ctx.getServices().SERVICENAME_PERSISTENCE));
		Session session = dbService.getSession();

		 List result = null;
		    try {
		      Query query = session.getNamedQuery("Flowlet.ProcessInstances");
		      result = query.list();
		    } catch (Exception e) {
		    	e.printStackTrace();
		    }finally {
		    	
		    	session.close();
		    } 
		
	    return XStreamSerializerHelper.getInstance().toJSON(result);
	}
	
	@GET
	@Path("/ui/{page}")
	@ProduceMime("text/html")
	public String getPageName(@PathParam("page")String page) {
        
		String pageToGo = "/opt/java/eclipse/workspace/flowlet/src/main/web/index.jsp";
		
		try {
			String pageBuilt = TextFileUtil.readTextFile(pageToGo);
			
			return pageBuilt.toString();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "ERROR";

	}
	

	@GET
	@Path("/ui/path/{page}")
	@ProduceMime("text/html")
	public String getPageNameFromClassPath(@PathParam("page")String page) {

		String pageToGo = Thread.currentThread().getContextClassLoader().getResource
		("/web/"+ page+".jsp").getPath();
		
		System.out.println("Page to go: " + pageToGo);
		
		try {
			String pageBuilt = TextFileUtil.readTextFile(pageToGo);
			
			return pageBuilt.toString();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "ERROR";

	}
	
	@GET
	@Path("/licencia/{ano}/{placa}/{rg}")
	@ProduceMime("text/plain")
	public String licenciar(@PathParam("ano")String ano, 
			@PathParam("placa")String placa, 
			@PathParam("rg") String rg) {

		JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
		try {
			ProcessInstance instance = ctx.newProcessInstance("licenciamento");
			instance.getContextInstance().setVariable("ano", ano);
			instance.getContextInstance().setVariable("placa", placa);
			instance.getContextInstance().setVariable("rg", rg);
			Token t = instance.getRootToken();
			t.signal();
			
		    ctx.save(instance);

			return new Long(instance.getId()).toString();

		} finally {
			ctx.close();
		}

	}

	@GET
	@Path("/status/{id}")
	@ProduceMime("text/plain")
	public String licenciar(@PathParam("id")
	String id) {

		JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
		try {
			GraphSession graphSession = ctx.getGraphSession();
			ProcessInstance processInstance = graphSession
					.loadProcessInstance(new Long(id));

			Token t = processInstance.getRootToken();
					
			return processInstance.getContextInstance().getVariables()
					.toString()
					+ t.getNode().getName() ;
		} finally {
			ctx.close();
		}

	}
	
    @SuppressWarnings("unchecked")
	@GET
	@Path("/realize/pagementosefa/{id}/{rg}")
	@ProduceMime("text/plain")
    public String doPagamentoSefa(@PathParam("id")String id,@PathParam("rg") String rg)
    {
        JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
        String s;
        try{
        GraphSession graphSession = ctx.getGraphSession();
        ProcessInstance processInstance = graphSession.loadProcessInstance((new Long(id)).longValue());
        Collection taskList = null;
        taskList = ctx.getTaskList(rg);
        processInstance.getContextInstance().setVariable("idsefa", Long.valueOf(System.currentTimeMillis() + 1L));
        StringBuilder b = new StringBuilder();
        for(Iterator iterator = taskList.iterator(); iterator.hasNext();)
        {
            TaskInstance taskInstance = (TaskInstance)iterator.next();

            if(taskInstance.getStart()!= null)
                b.append(String.format("User %s, the Task: %s [%s] is already started. This is a task from the Process Id: %s \n", new Object[] {
                    taskInstance.getActorId(), taskInstance.getName(), Long.valueOf(taskInstance.getId()), Long.valueOf(taskInstance.getProcessInstance().getId())
                }));
            else
            if(taskInstance.hasEnded())
                b.append(String.format("User %s, the Task: %s [%s] is ended. This is a task from the Process Id: %s \n", new Object[] {
                    taskInstance.getActorId(), taskInstance.getName(), Long.valueOf(taskInstance.getId()), Long.valueOf(taskInstance.getProcessInstance().getId())
                }));
            else
                b.append(String.format("User %s, the Task: %s [%s] is waiting for Starting. This is a task from the Process Id: %s \n", new Object[] {
                    taskInstance.getActorId(), taskInstance.getName(), Long.valueOf(taskInstance.getId()), Long.valueOf(taskInstance.getProcessInstance().getId())
                }));
        }

        b.append((new StringBuilder("Payment Id: ")).append(processInstance.getContextInstance().getVariable("idsefa").toString()).toString());
        s = b.toString();
       
        return s; 
        
        }
        finally {
        	ctx.close();
        }

    }


	@SuppressWarnings("unchecked")
	@GET
	@Path("/status/{id}/tarefas")
	@ProduceMime("text/plain")
	public String statusTarefas(@PathParam("id")
	String id) {

		JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
		try {
			GraphSession graphSession = ctx.getGraphSession();
			ProcessInstance processInstance = graphSession
					.loadProcessInstance(new Long(id));
			
		
		
			Collection<TaskInstance> taskList = null;

			taskList = (Collection<TaskInstance>) processInstance.getTaskMgmtInstance().getTaskInstances();
			
		
			
			StringBuilder b = new StringBuilder();
			
			for (TaskInstance taskInstance : taskList) {
				
				b.append( String.format("I am waiting for Task %s created in %s ", taskInstance.getName(), taskInstance.getCreate())  );
			}
				
			return b.toString();

		} finally {
			ctx.close();
		}

	}	
	
	
	

	@GET
	@Path("/execute/tarefa/{idtask}/{action}")
	@ProduceMime("text/plain")
	public String executeTarefa(@PathParam("idtask")Long taskId,  @PathParam("action")String action) {

		JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
		try {
			
			TaskInstance task = ctx.loadTaskInstance(new Long(taskId));
			
			if ("start".equalsIgnoreCase(action)) {
				
				task.start();
			}
			
           if ("end".equalsIgnoreCase(action)) {
				
				task.end();
				
			}
		
				
			return String.format("task %s has %sed",task.getId(),action);

		} finally {
			ctx.close();
		}

	}	
	

	
	@SuppressWarnings("unchecked")
	@GET
	@Path("/realize/vistoria/{id}/{idsefa}")
	@ProduceMime("text/plain")
	public String doVistoria(@PathParam("id")
	String id, @PathParam("idsefa") String idSefa) {

		JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
		try {
			GraphSession graphSession = ctx.getGraphSession();
			ProcessInstance processInstance = graphSession
					.loadProcessInstance(new Long(id));
		
			if (processInstance.getContextInstance().getVariable("idsefa")==null) {
				
				return "POR FAVOR FACA A PAGAMENTO NA SEFA";
			}

			Collection<TaskInstance> taskList = null;

			taskList = (Collection<TaskInstance>) processInstance.getTaskMgmtInstance().getTaskInstances();
			
			taskList.iterator().next();
			
			((TaskInstance)taskList.iterator().next()).end();
			
			processInstance.getContextInstance().setVariable("idvistoria",  System.currentTimeMillis()+1L );
				
			
			return processInstance.getContextInstance().getVariable("idvistoria").toString() ;

		} finally {
			ctx.close();
		}

	}	
	
	
	@SuppressWarnings("unchecked")
	@GET
	@Path("/realize/entrega/documento/{id}")
	@ProduceMime("text/plain")
	public String doEntregaDocumento(@PathParam("id")
	String id) {

		JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
		try {
			GraphSession graphSession = ctx.getGraphSession();
			ProcessInstance processInstance = graphSession
					.loadProcessInstance(new Long(id));
		
			if (processInstance.getContextInstance().getVariable("idvistoria")==null) {
				
				return "POR FAVOR ENTREGUE O ID DA VISTORIA";
			}

			Collection<TaskInstance> taskList = null;

			taskList = (Collection<TaskInstance>) processInstance.getTaskMgmtInstance().getTaskInstances();
			
			taskList.iterator().next();
			taskList.iterator().next();

			
			


			((TaskInstance)taskList.iterator().next()).end();
			
			processInstance.getContextInstance().setVariable("identrega",  System.currentTimeMillis()+1L );
				
			
			return processInstance.getContextInstance().getVariable("identrega").toString() ;

		} finally {
			ctx.close();
		}

	}	
	@GET
	@Path("/status/image/{id}")
	@ProduceMime("image/jpg")
	public byte[] statusImage(@PathParam("id")
	String id) throws Exception {

		JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
		try {
			GraphSession graphSession = ctx.getGraphSession();
			ProcessInstance processInstance = graphSession
					.loadProcessInstance(new Long(id));

			ProcessDefinition processDefinition = ctx.getGraphSession()
					.loadProcessDefinition(
							processInstance.getProcessDefinition().getId());

			FileDefinition fileDefinition = processDefinition
					.getFileDefinition();
			if (fileDefinition == null) {
				fileDefinition = new FileDefinition();
				processInstance.getProcessDefinition().addDefinition(
						fileDefinition);
			}
			fileDefinition
					.addFile(
							"processimage.jpg",
							new FileInputStream(
									"/opt/java/eclipse/workspace/carlic/src/main/jpdl/licenciamento/processimage.jpg"));
			fileDefinition
					.addFile(
							"processdefinition.xml",
							new FileInputStream(
									"/opt/java/eclipse/workspace/carlic/src/main/jpdl/licenciamento/processdefinition.xml"));
			fileDefinition
					.addFile(
							"gpd.xml",
							new FileInputStream(
									"/opt/java/eclipse/workspace/carlic/src/main/jpdl/licenciamento/gpd.xml"));

			ctx.getGraphSession().saveProcessDefinition(processDefinition);

			byte[] bytes = fileDefinition.getBytes("processimage.jpg");

			return bytes;

		} finally {
			ctx.close();
		}

	}

	@GET
	@Path("/updateprocess/{processname}")
	@ProduceMime("text/plain")
	public String updateProcess(@PathParam("processname") String processName) {
		JbpmContext ctx = JbpmConfiguration.getInstance().createJbpmContext();
		try {
			processDefinition = ProcessDefinition
					.parseXmlResource(String.format("%s/processdefinition.xml",processName));
			ctx.deployProcessDefinition(processDefinition);
			return processName+" has been deployed/updated!".toUpperCase();

		} finally {
			ctx.close();
		}

	}
	
	
	


}
