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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import it.isislab.dmason.experimentals.systemmanagement.master.MasterServer;
import it.isislab.dmason.experimentals.systemmanagement.utils.Simulation;

/**
 * Prompt Controller
 * 
 * @author Michele Carillo
 * @author Carmine Spagnuolo
 * @author Flavio Serrapica
 *
 */
public enum PromptSimulationController implements Prompt {
	HELP(new Action(){

		@Override
		public Object exec(Console c, String[] params, String stringPrompt, MasterServer ms) throws Exception {
			c.printf("********************************************************************************************************");
			c.printf("*    help                 |print commands list.                                                        *");
			c.printf("*    start                |exec the simulation corresponding to the given id.                          *");
			c.printf("*    stop                 |stop the simulation corresponding to the given id.                          *");
			c.printf("*    pause                |pause the simulation corresponding to the given id.                         *");
			c.printf("*    list                 |list the existing simulations                                               *");
			c.printf("*    logs                 |show the path where find the simulation log corresponding to the given id.  *");
			c.printf("*    waitall              |wait for all started simulations to finish execution.  *");
			c.printf("********************************************************************************************************");
			return null;
		}

	}),
	START(new Action(){
		
		@Override
		public Object exec(Console c, String[] params, String stringPrompt, MasterServer ms) throws Exception {

			if(params!=null && params.length>0 && params.length<2){
				int simID;
				try{
				 simID= Integer.parseInt(params[0]);
				}catch(NumberFormatException e){
					c.printf("An Error was occurred! \n"
							+"   Invalid simID\n");
					c.printf("Operation aborted!");
					return null;
				}
				if(ms.getSimulationsList().containsKey(simID)){
					ms.start(simID);
					c.printf("Simulation started");
				}
				else{
					c.printf("No simulation found");
				}
				
				return null;
			}else{
				if(params==null) c.printf("Too few arguments");
				else c.printf("Too many arguments");
				c.printf("Usage: \n"
						+"      start <simID>\n");
				return null;
			}
		}

	}),STOP(new Action(){

		@Override
		public Object exec(Console c, String[] params, String stringPrompt, MasterServer ms) throws Exception {

			if(params!=null && params.length>0 && params.length<2){
				int simID;
				try{
				 simID= Integer.parseInt(params[0]);
				}catch(NumberFormatException e){
					c.printf("An Error was occurred! \n"
							+"   Invalid simID\n");
					c.printf("Operation aborted!");
					return null;
				}
				if(ms.getSimulationsList().containsKey(simID)){
					ms.start(simID);
					c.printf("Simulation started");
				}
				else{
					c.printf("No simulation found");
				}
				
				return null;
			}else{
				if(params==null) c.printf("Too few arguments");
				else c.printf("Too many arguments");
				c.printf("Usage: \n"
						+"      stop <simID>\n");
				return null;
			}
		}

	}),PAUSE(new Action(){

		@Override
		public Object exec(Console c, String[] params, String stringPrompt, MasterServer ms) throws Exception {

			if(params!=null && params.length>0 && params.length<2){
				int simID;
				try{
				 simID= Integer.parseInt(params[0]);
				}catch(NumberFormatException e){
					c.printf("An Error was occurred! \n"
							+"   Invalid simID\n");
					c.printf("Operation aborted!");
					return null;
				}
				if(ms.getSimulationsList().containsKey(simID)){
					ms.start(simID);
					c.printf("Simulation started");
				}
				else{
					c.printf("No simulation found");
				}
				
				return null;
			}else{
				if(params==null) c.printf("Too few arguments");
				else c.printf("Too many arguments");
				c.printf("Usage: \n"
						+"      pause <simID>\n");
				return null;
			}
		}

	}),LIST(new Action(){

		@Override
		public Object exec(Console c, String[] params, String stringPrompt, MasterServer ms) throws Exception {
			Iterator i = ms.getSimulationsList().values().iterator();
			Simulation s1=null,s2=null; 
			String toPrint="";
			while(i.hasNext()){
				s1= (Simulation)i.next();
				if(i.hasNext()){
					s2 = (Simulation)i.next();

					toPrint+=String.format("*************************       *************************\n"+
							"*  ID %-18d*       *  ID %-18d*\n"+
							"*                       *       *                       *\n"+
							"*  Status %-14s*       *  Status %-14s*\n"+
							"*                       *       *                       *\n"+
							"*  Step(s)  %-12d*       *  Step(s)  %-12d*\n"+
							"*                       *       *                       *\n"+
							"*  Step(s)  %-12d*       *  Step(s)  %-12d*\n"+
							"*                       *       *                       *\n"+
							"*                       *       *                       *\n"+
							"*************************       *************************\n",
							s1.getSimID(),s2.getSimID(),s1.getStatus(),s2.getStatus(),s1.getStep(),s2.getStep());
				}else
					toPrint+=String.format("*************************\n"+
							"*  ID %d                *\n"+
							"*                       *\n"+
							"*  Status %s            *\n"+
							"*                       *\n"+
							"*  Step(s)  %d          *\n"+
							"*                       *\n"+
							"*                       *\n"+
							"*************************\n",
							s1.getSimID(),s1.getStatus(),s1.getStep());
			}
			c.printf(toPrint);
			return null;
		}

	}),LOGS(new Action(){
		
		private String getSimLog(MasterServer ms, int simID){
			String hitory_pathName = ms.getMasterHistory();
			File f_history = new File(hitory_pathName);
			if(!f_history.exists() || !f_history.isDirectory()){
				return null;
			}
			File[] cur_dir_files = null;
			Properties prop = null;
			InputStream in = null;
			for(File f: f_history.listFiles()){
				if(!f.isDirectory()) continue;
				cur_dir_files = f.listFiles(new FileFilter() {
					
					@Override
					public boolean accept(File pathname) {
						
						return pathname.getName().endsWith(".history");
					}
				});
				prop = new Properties();
				try {
					in = new FileInputStream(cur_dir_files[0]);
					prop.load(in);
					String id=prop.getProperty("simID");
					String path=null;
					if(simID == Integer.parseInt(id))
						path=prop.getProperty("simLogZipFile");
					in.close();
					if(path!=null) return path;
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
			}
			return null;
		}

		@Override
		public Object exec(Console c, String[] params, String stringPrompt, MasterServer ms) throws Exception {

			if(params!=null && params.length>0 && params.length<2){
				int simID;
				try{
				 simID= Integer.parseInt(params[0]);
				}catch(NumberFormatException e){
					c.printf("An Error was occurred! \n"
							+"   Invalid simID\n");
					c.printf("Operation aborted!");
					return null;
				}
				if(ms.getSimulationsList().containsKey(simID)){
					String logsPathName = ms.logRequestForSimulationByID(simID,"logreq");
					c.printf("Simulation log folder:\n"
							+"     "+ logsPathName);
				}
				else{
					c.printf("No running simulation found!\n Checking in History.....");
					String result = getSimLog(ms,simID);
					if(result==null)
						c.printf("No simulation found!");
					else
						c.printf("Simulation log folder:\n"
								+"     "+ result);
				}
				
				return null;
			}else{
				if(params==null) c.printf("Too few arguments");
				else c.printf("Too many arguments");
				c.printf("Usage: \n"
						+"      logs <simID>\n");
				return null;
			}
		}
	
  }),WAITALL(new Action(){

    boolean emptyOrNotStarted(MasterServer ms) {
			Collection<Simulation> values = ms.getSimulationsList().values();
      if (values.size() == 0) {
        return true;
      } 
      for (Simulation sim : values) {
        System.out.println(sim.getStatus());
        if (sim.getStatus().equals(Simulation.STARTED)) {
          return false;
        }
      }
      return true;
    }

		@Override
		public Object exec(Console c, String[] params, String stringPrompt, MasterServer ms) throws Exception {
      do {
         try {
            Thread.sleep(10000);
         } catch (Exception e) {
           e.printStackTrace();
         }
      } while(!emptyOrNotStarted(ms));
      return null;
		}

	});


	private Action action;
	private PromptSimulationController(Action a){ action =a;}

	@Override
	public Object exec(Console c, String[] params, String stringPrompt, MasterServer ms, PromptListener l) {
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

}
