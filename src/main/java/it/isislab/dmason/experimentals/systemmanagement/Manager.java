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
package it.isislab.dmason.experimentals.systemmanagement;

import it.isislab.dmason.exception.DMasonException;
import it.isislab.dmason.experimentals.systemmanagement.console.Command;
import it.isislab.dmason.experimentals.systemmanagement.console.Console;
import it.isislab.dmason.experimentals.systemmanagement.console.Prompt;
import it.isislab.dmason.experimentals.systemmanagement.console.PromptListener;
import it.isislab.dmason.experimentals.systemmanagement.master.MasterServer;
import it.isislab.dmason.experimentals.systemmanagement.master.MasterServerMain;
import it.isislab.dmason.experimentals.systemmanagement.worker.Worker;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import sun.misc.Signal;
import sun.misc.SignalHandler;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Class to launch master/workers of 
 *  DMason System Management
 *  
 * @author Michele Carillo
 * @author Carmine Spagnuolo
 * @author Flavio Serrapica
 *
*/
public class Manager {
	private static final String dmason="DMASON-3.1.jar";
	@Option(name="-m", aliases = { "--mode" },usage="master or worker")
	private String mode = "worker";

	@Option(name="-ip", aliases = { "--ipjms" },usage="ip address of JMS broker (default is localhost)")
	private String ip = "127.0.0.1";

	@Option(name="-p", aliases = { "--portjms" },usage="port of the JMS broker (default is 61616)")
	private String port = "61616";

	@Option(name="-ns", aliases = { "--numberofslots" },usage="number of simulation slot for this worker (default is 1)")
	private int ns = 1;

	@Option(name="-h", aliases = { "--hostslist" },usage="start worker on list of hosts (-h host1 host2 host3)")
	private boolean hosts = false;
	
	@Option(name="-ui", aliases = { "--webUI" },usage="start worker with web UI (-ui")
	private boolean webUI = false;

	@Argument
	private List<String> arguments = new ArrayList<String>();
	
	
	
	public static final String NO_CONSOLE = "Error: Console unavailable";
	public static final String TIME_FORMAT = "%1$tH:%1$tM:%1$tS";
	public static final String PROMPT = "dmason$ "+TIME_FORMAT + " >>>";
	public static final String UNKNOWN_COMMAND = "Unknown command [%1$s]\n";
	public static final String COMMAND_ERROR = "Command error [%1$s]: [%2$s]\n";


	public static void main(String[] args) {
		System.setProperty("java.library.path","./resources/sigar");
		System.setProperty("org.apache.activemq.SERIALIZABLE_PACKAGES","*");
		try {

			new Manager().doMain(args);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DMasonException e) {
			// TODO Auto-generated catch block
			System.err.println("Problem in this installation of DMASON.");
		}
	}

	class WorkerThread extends Thread{

		ChannelExec channel;
		Session session;
		BufferedReader in;
		boolean running = true;
		String host;
		public WorkerThread(String host,int nslot) throws IOException, JSchException {
			this.host=host;
			JSch jsch=new JSch();
			session=jsch.getSession(System.getProperty("user.name"), host, 22);
			jsch.addIdentity(System.getProperty("user.home")+File.separator+".ssh"+File.separator+"id_rsa");
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			channel=(ChannelExec) session.openChannel("exec");
		    in=new BufferedReader(new InputStreamReader(channel.getInputStream()));
		    //System.out.println("./"+dmason+" -m worker -ip "+ip+" -port "+port+" -ns "+ns+";");
		 
		    System.out.println("java -jar "+dmason+" -m worker -ip "+ip+" -p "+port+" -ns "+nslot+";");
			channel.setCommand("java -jar "+dmason+" -m worker -ip "+ip+" -p "+port+" -ns "+nslot+";");
			
		}
		@Override
		public void run() {
			
			try {
				channel.connect();
				System.out.println("Connected to remote machine.");
				String msg=null;
				while(running && (msg=in.readLine())!=null){
				  System.out.println(msg);
				}

				channel.disconnect();
				session.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		public void stopThread() throws Exception{
			running=false;
			System.out.println("Stopping worker "+host+".");
			
			ChannelExec channel2=(ChannelExec) session.openChannel("exec");
			channel2.setCommand("killall java");
			channel2.connect();
			channel2.disconnect();
			
		}
	}
	private List<WorkerThread> workers=new ArrayList<WorkerThread>();
	private boolean waitThread=true;
	final Lock lock = new ReentrantLock();
	final Condition workersWork  = lock.newCondition();
	
	private void signalWorkers()
	{
		lock.lock();
		try {
			waitThread=false;
			for(WorkerThread w: workers) w.stopThread();
			
			workersWork.signalAll();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}

	@SuppressWarnings("restriction")
	public void doMain(String[] args) throws IOException, DMasonException {
		CmdLineParser parser = new CmdLineParser(this);
		try {

			parser.parseArgument(args);
			
			if(mode.equals("master"))
			{
				System.out.println("Starting DMASON in Master mode ...");
				startMaster();
			}
			else if(mode.equals("worker"))
			{	
				if(hosts)
				{
					System.out.println("Starting workers...");
					if(arguments.isEmpty())
						throw new CmdLineException("Please specify the list of hosts, where start the workes.");
					
					WorkerThread w;

					//for(String host : arguments)
					for (int i = 0; i < arguments.size(); i++)	
					{
						//System.out.println("compaaaa "+arguments.get(i));
						String host=arguments.get(i);
						//int ns=Integer.parseInt(arguments.get(i+1));
						System.out.println("Start worker on "+host+" with nslots "+ns);
						w=new WorkerThread(host,ns);
						workers.add(w);
						w.start();
					}

					try {

						Signal.handle(new Signal("TERM"), new SignalHandler() {

							@Override
							public void handle(Signal arg0) {
								// Signal handler method for CTRL-C and simple kill command.
								System.out.println("Kill jobs ...");
								signalWorkers();

							}
						});
						Signal.handle(new Signal("INT"), new SignalHandler() {

							@Override
							public void handle(Signal arg0) {
								// Signal handler method for CTRL-C and simple kill command.
								System.out.println("Kill jobs ...");
								signalWorkers();

							}
						});
						Signal.handle(new Signal("HUP"), new SignalHandler() {

							@Override
							public void handle(Signal arg0) {
								// Signal handler method for CTRL-C and simple kill command.
								System.out.println("Kill jobs ...");
								signalWorkers();

							}
						});
					}
					catch (final IllegalArgumentException e) {
						e.printStackTrace();
					}

					lock.lock();
					try {
						while(waitThread)
							workersWork.await();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						lock.unlock();
					}

				}else{
					System.out.println("Starting DMASON in Worker mode ...");
					System.out.println("\tJMS broker IP: "+ip);
					System.out.println("\tJMS broker PORT: "+port);
					System.out.println("\tNumber of simulations slots: "+ns);
					startWorker();
				}

			}else
			{
				throw new CmdLineException("No correct mode given, master or worker is allowed.");
			}


		} catch( CmdLineException e ) {
			// if there's a problem in the command line,
			// you'll get this exception. this will report
			// an error message.
			System.err.println(e.getMessage());
			System.err.println("java -jar dmason.jar [options...]");
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();

			// print option sample. This is useful some time
			System.err.println("  Example: java -jar dmason.jar --mode master");

			return;
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	public void startWorker(){
		Worker worker=new Worker(ip, port,ns);

	}
	
	public void startMaster() throws DMasonException
	{

		File dataresources=new File("resources");
		if(!dataresources.exists() || !dataresources.isDirectory())
			throw new DMasonException("Problems in resources check your data.");

    MasterServerMain msm = new MasterServerMain(webUI);
    msm.start();

		MasterServer ms = msm.getMasterServer();

    // use console mode if webUI is false
		if(!webUI){
			while(ms==null){}
			Console console = new Console();
			if (console != null)
				try {
					execCommandLoop(console,ms);
          System.exit(0);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else
				throw new RuntimeException(NO_CONSOLE);
		}
	}
	
	
	protected static void execCommandLoop(final Console console, MasterServer ms) throws IOException
	{
		String rootPrompt = "dmason";
		final Enum helpmsg = Enum.valueOf(Command.class, "help".toUpperCase());
		((Prompt)helpmsg).exec(console,null,rootPrompt,ms, new PromptListener()
		{
			@Override
			public void exception(Exception e)
			{
				console.printf(Manager.COMMAND_ERROR, helpmsg, e.getMessage());
			}
		});
		
		while (true)
		{
			String commandLine = console.readLine(PROMPT, new Date());
      // reached end of input
      if (commandLine == null) {
        break;
      }
			Scanner scanner = new Scanner(commandLine);

			if (scanner.hasNext())
			{
				final String commandName = scanner.next().toUpperCase();

				try
				{
					final Command cmd = Enum.valueOf(Command.class, commandName);
					String param= scanner.hasNext() ? scanner.nextLine() : null;
					if(param !=null && param.charAt(0)== ' ')
						param=param.substring(1,param.length());
					String[] params = param!=null?param.split(" "):null;
					
					cmd.exec(console,params,rootPrompt, ms,new PromptListener() {
						@Override
						public void exception(Exception e)
						{
							console.printf(COMMAND_ERROR, cmd, e.getMessage());
							e.printStackTrace(System.out);
						}
					});
				}
				catch (IllegalArgumentException e)
				{
					console.printf(UNKNOWN_COMMAND, commandName);
				}
			}

			scanner.close();
		}
	}

}
