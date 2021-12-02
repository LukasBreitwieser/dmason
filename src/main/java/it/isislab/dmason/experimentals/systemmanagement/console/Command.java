/**
 * Copyright 2016 Universita' degli Studi di Salerno


   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package it.isislab.dmason.experimentals.systemmanagement.console;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import it.isislab.dmason.experimentals.systemmanagement.Manager;
import it.isislab.dmason.experimentals.systemmanagement.master.MasterServer;

/**
 * 
 * @author Michele Carillo
 * @author Carmine Spagnuolo
 * @author Flavio Serrapica
 *
 */
public enum Command implements Prompt{


	LICENSE(new Action()
	{
		@Override
		public Object exec(Console c, String[] params,String stringPrompt,MasterServer ms)
		{
			c.printf("DMASON is a tool utility powerd by ISISLab, 2011.\n");
			return null;

		}
	}),
	HELP(new Action()
	{
		@Override
		public Object exec(Console c, String[] params,String stringPrompt,MasterServer ms)
		{
			c.printf(""
					+ "██████╗ ███╗   ███╗ █████╗ ███████╗ ██████╗ ███╗   ██╗ \n"
					+ "██╔══██╗████╗ ████║██╔══██╗██╔════╝██╔═══██╗████╗  ██║ \n"
					+ "██║  ██║██╔████╔██║███████║███████╗██║   ██║██╔██╗ ██║ \n"
					+ "██║  ██║██║╚██╔╝██║██╔══██║╚════██║██║   ██║██║╚██╗██║ \n"
					+ "██████╔╝██║ ╚═╝ ██║██║  ██║███████║╚██████╔╝██║ ╚████║ \n"
					+ "╚═════╝ ╚═╝     ╚═╝╚═╝  ╚═╝╚══════╝ ╚═════╝ ╚═╝  ╚═══╝ \n"
					+"\n");

			c.printf("***************************************************************************************************");
			c.printf("*    help                 |print commands list.                                                   *");
			c.printf("*    workers              |available workers list.                                                *");
//			c.printf("*    start                |exec the simulation corresponding to the given id.                     *");
//			c.printf("*    stop                 |stop the simulation corresponding to the given id.                     *");
//			c.printf("*    pause                |stop the simulation corresponding to the given id.                     *");
			c.printf("*    simulationcontroller |(start/stop/pause) a simulation                                        *");
			c.printf("*    createsimulation     |create new simulation execution.                                       *");
			c.printf("*    waitforworkers #workers     |Wait until #workers connected.                                       *");
			c.printf("*    shutdownallworkers   |Shutdown all connected workers.                                       *");
//			c.printf("*    getsimulations       |print all simulations created by the user.                             *");
//			c.printf("*    getsimulation        |print status of the simulation corresponding to the given id.          *");
//			c.printf("*    getlog               |download the results of the simulation corresponding to the given id.  *");
//			c.printf("*    kill                 |kill the simulation corresponding to the given id.                     *");
			c.printf("***************************************************************************************************");
			return null;
		}
	}),
	WORKERS(new Action()
	{
		private String workerPrintFormatter(String prefix, Object s, String postfix, String fillerChar,int len){

			String str = new String(prefix+" "+s);
			int l = str.length();
			if(l<len){
				for(int i=l; i<(len-postfix.length()-1); i++){
					str+=fillerChar;
				}
			}		
			str+=postfix;
			return str;
		}
		@Override
		public Object exec(Console c, String[] params,String stringPrompt, MasterServer ms)
		{

			/**
			 * Simulation s = getSimulationDatabyId(session,  username, simID);
			 */
			if(params != null && params.length > 0 )
			{
				c.printf("Too many arguments!\n");

			}
			String message = "{\"workers\":[";

			int startMessageSize = message.length();

			for(String s : ms.getInfoWorkers().values())
				message+=s+",";

			if(message.length() > startMessageSize)
				message=message.substring(0, message.length()-1)+"]}";
			else
				message="";
			
			if(message.length()==0)
				return null;

			try{
				JSONParser parser = new JSONParser();
				JSONObject j_workers = (JSONObject)parser.parse(message);
				JSONArray worker = (JSONArray)j_workers.get("workers");
				if(worker !=null){

					for(int i=0; i<worker.size(); i++){
						JSONObject o = (JSONObject)worker.get(i);
						//System.out.println(("************************\n").length()); ==25
						c.printf("************************");
						c.printf(workerPrintFormatter("*   ID",o.get("workerID"),"*"," ",25));
						c.printf(workerPrintFormatter("*   CPU",o.get("cpuLoad"),"*"," ",25));
						c.printf(workerPrintFormatter("*   RAM",o.get("maxHeap"),"*"," ",25));
						c.printf(workerPrintFormatter("*     Used",o.get("availableheapmemory"),"*"," ",25));
						c.printf(workerPrintFormatter("*   Slots",o.get("slots"),"*"," ",25));
						c.printf(workerPrintFormatter("*   IP",o.get("ip"),"*"," ",25));
						c.printf("************************");
						//{"workers":[{"slots":4,"cpuLoad":0.0,"availableheapmemory":"110,28","maxHeap":"1765,5","ip":"192.168.122.1","workerID":"-1169498471"f6l5a4v3i2o1

					}
				}else{
					c.printf("No workers available");
				}
			}catch(ParseException e){
				c.printf("Some errors were occurred \n");
				e.printStackTrace();
			}
			return null;
		}
	}),
  WAITFORWORKERS(new Action() {
		@Override
		public Object exec(Console c, String[] params,String stringPrompt,MasterServer ms)
		{
			if(params == null || params.length != 1 ) {
				c.printf("Wrong number of arguments!");
				c.printf("USAGE: ");
				c.printf("waitforworkers <number-of-workers>");
        return null;
			}
      
      int target = Integer.parseInt(params[0]);
      while(ms.getInfoWorkers().size() != target) {
        c.printf("current number of connected workers: " + ms.getInfoWorkers().size());
         try {
            Thread.sleep(1000);
         } catch (Exception e) {
           e.printStackTrace();
         }
      }

			return null;
		}
  }),
  SHUTDOWNALLWORKERS(new Action() {
		@Override
		public Object exec(Console c, String[] params,String stringPrompt,MasterServer ms) {
      if (params != null && params.length > 0) {
        c.printf("This command does not expect any arguments");
        return null;
      }
			String message = "{\"workers\":[";

			int startMessageSize = message.length();

			for(String s : ms.getInfoWorkers().values())
				message+=s+",";

			if(message.length() > startMessageSize)
				message=message.substring(0, message.length()-1)+"]}";
			else
				message="";
			
			if(message.length()==0)
				return null;

			try{
				JSONParser parser = new JSONParser();
				JSONObject j_workers = (JSONObject)parser.parse(message);
				JSONArray worker = (JSONArray)j_workers.get("workers");
				if(worker !=null){
          ArrayList<String> workerIds = new ArrayList<>();
					for(int i=0; i<worker.size(); i++){
						JSONObject o = (JSONObject)worker.get(i);
            workerIds.add((String) o.get("workerID"));
					}
          ms.shutdownAllWorkers(workerIds);
				}else{
					c.printf("No workers available");
				}
			}catch(ParseException e){
				c.printf("Some errors were occurred \n");
				e.printStackTrace();
			}
      return null;
    }
  }),
	CREATESIMULATION(new Action(){

		@Override
		public Object exec(Console c, String[] params, String stringPrompt, MasterServer ms) throws Exception {
			
			if(ms.getInfoWorkers().keySet().size()==0){ c.printf("No worker available"); return null;}

			execCommand(c, PromptMakeSimulation.class, stringPrompt+"/Simulation ",ms);
			
			return null;
		}

	}),
	
	SIMULATIONCONTROLLER(new Action(){
		@Override
		public Object exec(Console c, String[] params, String stringPrompt, MasterServer ms) throws Exception {
			if(ms.getSimulationsList().size()!=0)
				execCommand(c, PromptSimulationController.class, stringPrompt+"/SimController ",ms);
			else
				c.printf("No simulation available.");
			return null;
		}
	})

	//	,
	//
	//	START(new Action()
	//	{
	//		@Override
	//		public Object exec(Console c, String[] params,String stringPrompt)
	//		{
	//			/**
	//			 * Simulation s = getSimulationDatabyId(session,  username, simID);
	//			 */
	//			if(params == null || params.length < 1 )
	//			{
	//				c.printf("usage of start command : start ID [ID:intger ID of simulation execution environment]\n");
	//				return null;
	//			}else{
	//				int simID = Integer.parseInt(params[0])-1;
	//				Simulations sims = SofManager.getSimulationsData(SOFShellClient.session);
	//				if(sims == null){
	//					c.printf("No such simulation");
	//					return null;
	//				}
	//				Simulation sim = sims.getSimulations().get(simID);
	//				//sim = ScudManager.getSimulationDatabyId(SCUDShellClient.session,  SCUDShellClient.session.getUsername(), simID);
	//				SofManager.runAsynchronousSimulation(SOFShellClient.session,sim);
	//				//ScudManager.runSimulation(SCUDShellClient.session, SCUDShellClient.session.getUsername(), simID, s.getLoop());	
	//				return null;
	//			}
	//		}
	//	}),
	//
	//	CREATESIMULATION(new Action()
	//	{
	//		@Override
	//		public Object exec(Console c, String[] params,String stringPrompt)
	//		{
	//
	//			String parsedParams[] = SofManager.parseParamenters(params,4);
	//
	//			if(parsedParams.length == 6)
	//			{
	//				
	//				try {
	//					SofManager.checkParamMakeSimulationFolder(parsedParams);
	//					
	//				} catch (ParameterException e1) {
	//					// TODO Auto-generated catch block
	//					c.printf(e1.getMessage());
	//					return null;
	//				}
	//				try {
	//
	//
	//					SofManager.makeSimulationFolder(
	//							
	//							SOFShellClient.session,
	//							parsedParams[0],//MODEL TYPE MASON - NETLOGO -GENERIC
	//							parsedParams[1],//SIM NAME
	//							parsedParams[2],//INPUT.XML PATH 
	//							parsedParams[3],//OUTPUT.XML PATH 
	//							parsedParams[4],//DESCRIPTION SIM
	//							parsedParams[5],//SIMULATION EXEC PATH
	//							""); //interpretergenericpath
	//					return null;
	//				} catch (Exception e) {
	//					e.printStackTrace();
	//					c.printf("Error in making execution environment!\n");
	//					return null;
	//				}
	//			}
	//			else if(parsedParams.length == 7){
	//				try {
	//					
	//					SofManager.checkParamMakeSimulationFolder(parsedParams);
	//				} catch (ParameterException e1) {
	//					// TODO Auto-generated catch block
	//					c.printf(e1.getMessage());
	//					return null;
	//				}
	//				try {
	//
	//
	//					SofManager.makeSimulationFolder(
	//							SOFShellClient.session,
	//							parsedParams[0],//MODEL TYPE MASON - NETLOGO -GENERIC
	//							parsedParams[1],//SIM NAME
	//							parsedParams[2],//INPUT.XML PATH 
	//							parsedParams[3],//OUTPUT.XML PATH 
	//							parsedParams[4],//DESCRIPTION SIM
	//							parsedParams[5],//SIMULATION EXEC PATH
	//							parsedParams[6]); //intepretergenericpath
	//					return null;
	//				} catch (Exception e) {
	//					e.printStackTrace();
	//					c.printf("Error in making execution environment!\n");
	//					return null;
	//				}
	//			}
	//			
	//			else{
	//				c.printf("Error "+(parsedParams.length<6?"few":"much more")+" parameters.:\n");
	//				c.printf("usage: MODEL[MASON-NETLOGO-GENERIC] SIM-NAME[String]INPUT.xml[String absolutely]"
	//						+ " Output.xml[String absolutely path] DESCRIPTION-SIM[String] SIMULATION-EXECUTABLE-MODEL[String absolutely path]\n");
	//				return null;
	//			}
	//		}
	//	}),
	//	GETSIMULATIONS(new Action()
	//	{
	//		@Override
	//		public Object exec(Console c, String[] params,String stringPrompt)
	//		{	
	//			try {
	//
	//				Simulations listSim  = SofManager.getSimulationsData(SOFShellClient.session);
	//				if(listSim == null){
	//					c.printf("No such simulation");
	//					return null;
	//				}
	//
	//				for(Simulation s: listSim.getSimulations())
	//					c.printf(s+"\n");
	//				return null;
	//			} catch (NumberFormatException e) {
	//				// TODO Auto-generated catch block
	//				e.printStackTrace();
	//				return null;
	//			}
	//		}
	//	}),
	//	GETSIMULATION(new Action()
	//	{
	//		@Override
	//		public Object exec(Console c, String[] params,String stringPrompt)
	//		{
	//			if(params == null || params.length < 1 )
	//			{
	//				c.printf("few parameters!\n Usage: getsimuation #IDSIM");
	//				return null;
	//			}else{
	//				int simID = Integer.parseInt(params[0])-1;
	//				Simulations listSim  = SofManager.getSimulationsData(SOFShellClient.session);
	//				if(listSim == null){
	//					c.printf("No such simulation");
	//					return null;
	//				}
	//				Simulation sim = listSim.getSimulations().get(simID);
	//				try {
	//					if(!HadoopFileSystemManager.ifExists(SOFShellClient.session,SofManager.fs.getHdfsUserPathSimulationByID(sim.getId())))
	//						c.printf("Simulation SIM"+simID+" not exists\n");
	//
	//					//sim = ScudManager.getSimulationDatabyId(SCUDShellClient.session,SCUDShellClient.session.getUsername(),simID);
	//					c.printf(sim+"\n");
	//					return null;
	//				} catch (NumberFormatException e) {
	//					// TODO Auto-generated catch block
	//					e.printStackTrace();
	//					return null;
	//				} catch (JSchException e) {
	//					c.printf("Error during resource creating\n"+e.getMessage());
	//					e.printStackTrace();
	//					return null;
	//				} catch (IOException e) {
	//					c.printf("Error during resource creating\n"+e.getMessage());
	//					e.printStackTrace();
	//					return null;
	//				}
	//
	//			}
	//		}
	//	}),
	//
	//	GETRESULT(new Action()
	//	{
	//		@Override
	//		public Object exec(Console c, String[] params,String stringPrompt)
	//		{
	//			if(params == null)
	//			{
	//				c.printf("Error few parameters!\n Usage: getresult simID [destinationDirPath]");
	//				return null;
	//			}else{
	//				Simulations sims = SofManager.getSimulationsData(SOFShellClient.session);
	//				if(sims == null){
	//					c.printf("No such simulation");
	//					return null;
	//				}
	//				int simID = Integer.parseInt(params[0])-1;
	//				Simulation sim = null;
	//				try{
	//					sim = sims.getSimulations().get(simID);
	//				}catch(IndexOutOfBoundsException e){
	//					c.printf("No such simulation");
	//					return null;
	//				}
	//				//if no path is specified, saves in current directory
	//				String path = (params.length < 2)? System.getProperty("user.dir"):params[1];
	//				c.printf("Simulation will download in: "+path);
	//				SofManager.downloadSimulation(SOFShellClient.session,sim.getId(),path);
	//				return null;
	//			}
	//		}
	//	}),
	//	LIST(new Action()
	//	{
	//		@Override
	//		public Object exec(Console c, String[] params,String stringPrompt)
	//		{
	//			Simulations sims = SofManager.getSimulationsData(SOFShellClient.session);
	//			if(sims == null){
	//				c.printf("No such simulation");
	//				return null;
	//			}
	//			for(int i=1; i<=sims.getSimulations().size(); i++){
	//				int simID= i-1;
	//				Simulation s = sims.getSimulations().get(simID);
	//				c.printf("*****************************************************************************************************************\n");
	//				c.printf("sim-id: "+i+" name: "+s.getName()+" state: "+s.getState()+" time: "+s.getCreationTime()+" hdfsId: "+s.getId()+" *\n");
	//				c.printf("*****************************************************************************************************************\n");
	//			}
	//			return null;
	//		}
	//	}),
	//	KILL(new Action()
	//	{
	//		@Override
	//		public Object exec(Console c, String[] params,String stringPrompt)
	//		{
	//			if(params == null || params.length < 1 )
	//			{
	//				c.printf("Error few parameters!\n Usage: kill simID");
	//				return null;
	//			}else{
	//				int simID = Integer.parseInt(params[0])-1;
	//
	//				Simulations listSim  = SofManager.getSimulationsData(SOFShellClient.session);
	//				if(listSim == null){
	//					c.printf("No such simulation");
	//					return null;
	//				}
	//				if(listSim.getSimulations().size()-1<simID){
	//					c.printf("Simulation not exists");
	//					return null;
	//				}
	//
	//				Simulation sim = listSim.getSimulations().get(simID);
	//				String cmd="pkill -f \""+sim.getProcessName()+"\"";
	//
	//				String bash = "if "+cmd+" ; then echo 0; else echo -1; fi";
	//
	//				try {
	//					HadoopFileSystemManager.exec(SOFShellClient.session,bash);
	//				} catch (Exception e ) {
	//					// TODO Auto-generated catch block
	//					e.printStackTrace();
	//					return null;
	//				}
	//				
	//				c.printf("Process killed");
	//				SofManager.setSimulationStatus(SOFShellClient.session,sim,Simulation.KILLED);
	//				return null;
	//			}
	//		}
	//	}),
	//	STOP(new Action()
	//	{
	//		@Override
	//		public Object exec(Console c, String[] params,String stringPrompt)
	//		{
	//			if(params == null || params.length < 1 )
	//			{
	//				c.printf("Error few parameters!\n Usage: stop simID");
	//				return null;
	//			}else{
	//				int simID = Integer.parseInt(params[0])-1;
	//
	//				Simulations listSim  = SofManager.getSimulationsData(SOFShellClient.session);
	//				if(listSim == null){
	//					c.printf("No such simulation");
	//					return null;
	//				}
	//				if(listSim.getSimulations().size()-1<simID){
	//					c.printf("Simulation not exists");
	//					return null;
	//				}
	//
	//				Simulation sim = listSim.getSimulations().get(simID);
	//				if(sim.getState().equals(Simulation.STOPPED)){
	//					c.printf("Already stopped!\n");
	//					return null;
	//				}
	//				sim = listSim.getSimulations().get(simID);
	//				if(sim.getState().equals(Simulation.RUNNING)){
	//				   Message stop = new Message();
	//				   stop.setId(SofManager.getMexID());
	//				   stop.setMessage(Message.STOP_MESSAGE);
	//				   SofManager.sendMessage(SOFShellClient.session, sim, stop);
	//				   return null;
	//				}
	//				return null;
	//			}
	//		}
	//	})

	;


	private Action action;

	private Command(Action a)
	{
		this.action = a;
	}

	public Object exec(final Console c, final String[] params,String stringPrompt, MasterServer ms, final PromptListener l)
	{
		try
		{
			return action.exec(c, params,stringPrompt,ms);
		}
		catch (Exception e)
		{
			l.exception(e);
			return null;
		}
	}


	private static Object execCommand(final Console console, Class enumClass, String stringPrompt,MasterServer ms) throws IOException{
		String commandLine = null;
		Scanner scanner = null;
		Object toReturn = null;
		final Enum helpmsg = Enum.valueOf(enumClass, "help".toUpperCase());
		((Prompt)helpmsg).exec(console,null,stringPrompt,ms, new PromptListener()
		{
			@Override
			public void exception(Exception e)
			{
				console.printf(Manager.COMMAND_ERROR, helpmsg, e.getMessage());
			}
		});


		while (true)
		{
			commandLine = console.readLine(stringPrompt+" "+Manager.TIME_FORMAT+" >>>", new Date());
			scanner = new Scanner(commandLine);

			if (scanner.hasNext())
			{
				final String commandName = scanner.next().toUpperCase();
				if(commandName.equalsIgnoreCase("exit")){
					return toReturn;
				}
				try
				{
					final Enum cmd = Enum.valueOf(enumClass, commandName);
					String param= scanner.hasNext() ? scanner.nextLine() : null;
					if(param !=null && param.charAt(0)== ' ')
						param=param.substring(1,param.length());
					String[] params = param!=null?param.split(" "):null;

					toReturn = ((Prompt)cmd).exec(console,params, stringPrompt, ms ,new PromptListener()
					{
						@Override
						public void exception(Exception e)
						{
							console.printf(Manager.COMMAND_ERROR, cmd, e.getMessage());
						}
					});
				}
				catch (IllegalArgumentException e)
				{
					console.printf(Manager.UNKNOWN_COMMAND, commandName);
				}
			}

			scanner.close();
		}
	}

}
