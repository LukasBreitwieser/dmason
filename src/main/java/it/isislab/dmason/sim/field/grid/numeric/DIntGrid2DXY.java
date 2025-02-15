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

package it.isislab.dmason.sim.field.grid.numeric;

import it.isislab.dmason.exception.DMasonException;
import it.isislab.dmason.experimentals.util.visualization.globalviewer.VisualizationUpdateMap;
import it.isislab.dmason.experimentals.util.visualization.zoomviewerapp.ZoomArrayList;
import it.isislab.dmason.nonuniform.QuadTree;
import it.isislab.dmason.sim.engine.DistributedMultiSchedule;
import it.isislab.dmason.sim.engine.DistributedState;
import it.isislab.dmason.sim.field.CellType;
import it.isislab.dmason.sim.field.grid.numeric.region.RegionIntegerNumeric;
import it.isislab.dmason.sim.field.support.field2D.DistributedRegionNumeric;
import it.isislab.dmason.sim.field.support.field2D.EntryNum;
import it.isislab.dmason.sim.field.support.field2D.UpdateMap;
import it.isislab.dmason.sim.field.support.field2D.region.RegionNumeric;
import it.isislab.dmason.util.connection.Connection;
import it.isislab.dmason.util.connection.jms.ConnectionJMS;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.logging.Logger;
import sim.engine.SimState;
import sim.util.Bag;
import sim.util.Int2D;


/**
 *  <h3>This Field extends IntGrid2D, to be used in a distributed environment. All the necessary informations for 
 *  the distribution of simulation are wrapped in this class.</h3>
 * <p> This version is for a distribution in a <i>square mode</i>.
 *  It represents the field managed by a single peer.
 *  This is an example for a square mode distribution with 9 peers (only to distinguish the regions):
 *  (for code down)
 *  </p>
 *
 *	<ul>
 *	<li>MYFIELD : Region to be simulated by peer.</li>
 *
 *	<li>LEFT_MINE, EAST_MINE, NORTH_MINE, SOUTH_MINE,CORNER_MINE_LEFT_UP,CORNER_MINE_LEFT_DOWN,
 *		CORNER_MINE_RIGHT_UP,CORNER_MINE_RIGHT_DOWN :Boundaries Regions those must be simulated and sent to neighbors.</li>
 *	
 *	<li>LEFT_OUT, EAST_OUT, NORTH_OUT, SOUTH_OUT, CORNER_OUT_LEFT_UP_DIAG, CORNER_OUT_LEFT_DOWN_DIAG,
 *		CORNER_OUT_RIGHT_UP_DIAG, CORNER_OUT_RIGHT_DOWN_DIAG : Boundaries Regions those must not be simulated and sent to neighbors to be simulated.</li>
 *   <li>
 *	All peers subscribes to the topic of boundary region which want the information and run a asynchronous thread
 *	to receive the updates, then publish a topic for every their border (or neighbor), that can be :
 *	<ul>
 *	<li> MYTOPIC L (LEFT BORDER)</li>
 *	<li> MYTOPIC R (RIGHT BORDER)</li>
 *	<li> MYTOPIC U (UPPER BORDER)</li>
 *	<li> MYTOPIC D (LOWER BORDER)</li>
 *
 *	<li> MYTOPIC CUDL (Corner Up Diagonal Left)</li>
 *	<li> MYTOPIC CUDR (Corner Up Diagonal Right)</li>
 *	<li> MYTOPIC CDDL (Corner Down Diagonal Left)</li>
 *	<li> MYTOPIC CDDR (Corner Down Diagonal Right)</li>
 *</ul>
 *</li>
 *	</ul>
 *
 * <PRE>
 *------------------------------------------------------------------------------------
 * |                        |  |  |                      |  |  |                         |
 * |                        |  |  |                      |  |  |                         |
 * |                        |  |  |                      |  |  |                         |
 * |                        |  |  |                      |  |  |                         |
 * |         00             |  |  |          01          |  |  |            02           |
 * |                        |  |  |                      |  |  |                         |
 * |                        |  |  |                      |  |  |   CORNER DIAG           |
 * |                        |  |  |                      |  |  |  /                      |
 * |                        |  |  |                      |  |  | /                       |
 * |________________________|__|__|______NORTH_OUT_______|__|__|/________________________|
 * |________________________|__|__|______NORTH_MINE______|__|__|_________________________|
 * |________________________|__|__|______________________|__|__|_________________________|
 * |                        |  |  |                     /|  |  |                         |
 * |                        O  O  |                    / |  E  E                         |
 * |                        V  V  |                   /  |  S  S                         |
 * |         10             E  E  |         11   CORNER  |  T  T         12              |
 * |                        S  S  |               MINE   |  |  |                         |
 * |                        T  T  |                      |  M  O                         |
 * |                        O  M  |       MYFIELD        |  |  U                         |
 * |                        U  I  |                      |  N  T                         |
 * |                        T  N  |                      |  E  |                         |
 * |________________________|__|__|______________________|__|__|_________________________|
 * |________________________|__|__|___SOUTH_MINE_________|__|__|_________________________|
 * |________________________|__|__|___SOUTH_OUT__________|__|__|_________________________|
 * |                        |  |  |                      |  |  |                         |
 * |                        |  |  |                      |  |  |                         |
 * |                        |  |  |                      |  |  |                         |
 * |                        |  |  |                      |  |  |                         |
 * |       20               |  |  |          21          |  |  |           22            |
 * |                        |  |  |                      |  |  |                         |
 * |                        |  |  |                      |  |  |                         |
 * |                        |  |  |                      |  |  |                         |
 * |                        |  |  |                      |  |  |                         |
 * ---------------------------------------------------------------------------------------
 * </PRE>
 * @author Michele Carillo
 * @author Ada Mancuso
 * @author Dario Mazzeo
 * @author Francesco Milone
 * @author Francesco Raia
 * @author Flavio Serrapica
 * @author Carmine Spagnuolo
 */

public class DIntGrid2DXY extends DIntGrid2D {

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(DDoubleGrid2DXY.class.getCanonicalName());

	/**
	 * It's the name of the specific field
	 */
	private String name;


	/** Number of neighbors of this cell, that is also the number of regions to create and of topics to publish/subscribe */ 
	protected int numNeighbors;


	private String topicPrefix = "";

	private ZoomArrayList<EntryNum<Integer, Int2D>> tmp_zoom=new ZoomArrayList<EntryNum<Integer,Int2D>>();

	private int width,height;


	// -----------------------------------------------------------------------
	// GLOBAL PROPERTIES -----------------------------------------------------
	// -----------------------------------------------------------------------
	/** Will contain globals properties */
	public VisualizationUpdateMap<String, Object> globals = new VisualizationUpdateMap<String, Object>();
	
	/**
	 * Start time for
	 */
	private long startTime;
	/**
	 * 
	 */
	private long endTime;

	/**
	 * Constructor of class with paramaters:
	 * /**
	 * 
	 * @author Michele Carillo
	 * @author Carmine Spagnuolo
	 * @author Flavio Serrapica
	 *
	 * 
	 * @param width field's width  
	 * @param height field's height
	 * @param sm The SimState of simulation
	 * @param max_distance maximum shift distance of the agents
	 * @param i i position in the field of the cell
	 * @param j j position in the field of the cell
	 * @param rows number of rows in the division
	 * @param columns number of columns in the division
	 * @param initialGridValue the initial value that we want to set at grid at begin simulation 
	 * @param name ID of a region
	 * @param prefix Prefix for the name of topics used only in Batch mode
	 * @param isToroidal return true if the field is toroidal
	 */
	public DIntGrid2DXY(int width, int height,SimState sm,int max_distance,int i,int j,int rows,int columns, 
			Integer initialGridValue, String name, String prefix, boolean isToroidal) 
	{		
		super(width, height, initialGridValue);
		this.width=width;
		this.height=height;
		this.sm=sm;
		this.AOI=max_distance;
		this.rows = rows;
		this.columns = columns;
		this.cellType = new CellType(i, j);
		this.updates_cache = new ArrayList<RegionNumeric<Integer,EntryNum<Integer,Int2D>>>();
		this.name = name;
		this.topicPrefix=prefix;		

		setToroidal(isToroidal);		
		createRegions();	
		endTime=startTime=0;
	}


	/**
	 * This method first calculates the upper left corner's coordinates, so the regions where the field is divided
	 * @return true if all is ok
	 */

	public boolean createRegions(QuadTree... cell)
	{
		//upper left corner's coordinates
		if(cellType.pos_j<(width%columns))
			own_x=(int)Math.floor(width/columns+1)*cellType.pos_j; 
		else
			own_x=(int)Math.floor(width/columns+1)*((width%columns))+(int)Math.floor(width/columns)*(cellType.pos_j-((width%columns))); 

		if(cellType.pos_i<(height%rows))
			own_y=(int)Math.floor(height/rows+1)*cellType.pos_i; 
		else
			own_y=(int)Math.floor(height/rows+1)*((height%rows))+(int)Math.floor(height/rows)*(cellType.pos_i-((height%rows))); 

		// own width and height
		if(cellType.pos_j<(width%columns))
			my_width=(int) Math.floor(width/columns+1);
		else
			my_width=(int) Math.floor(width/columns);

		if(cellType.pos_i<(height%rows))
			my_height=(int) Math.floor(height/rows+1);
		else
			my_height=(int) Math.floor(height/rows);


		//calculating the neighbors
		for (int k = -1; k <= 1; k++) 
		{
			for (int k2 = -1; k2 <= 1; k2++) 
			{				
				int v1=cellType.pos_i+k;
				int v2=cellType.pos_j+k2;
				if(v1>=0 && v2 >=0 && v1<rows && v2<columns)
					if( v1!=cellType.pos_i || v2!=cellType.pos_j)
					{
						neighborhood.add(v1+""+v2);
					}	
			}
		}


		if(isToroidal()) 
			makeToroidalSections();
		else
			makeNoToroidalSections();

		return true;
	}


	private void makeNoToroidalSections() {

		myfield=new RegionIntegerNumeric(own_x+AOI,own_y+AOI, own_x+my_width-AOI , own_y+my_height-AOI);

		//corner up left

		rmap.NORTH_WEST_MINE=new RegionIntegerNumeric(own_x, own_y, own_x+AOI, own_y+AOI);

		//corner up right

		rmap.NORTH_EAST_MINE=new RegionIntegerNumeric(own_x+my_width-AOI, own_y, own_x+my_width, own_y+AOI);

		//corner down left

		rmap.SOUTH_WEST_MINE=new RegionIntegerNumeric(own_x, own_y+my_height-AOI,own_x+AOI, own_y+my_height);

		//corner down right

		rmap.SOUTH_EAST_MINE=new RegionIntegerNumeric(own_x+my_width-AOI, own_y+my_height-AOI,own_x+my_width,own_y+my_height);

		rmap.WEST_MINE=new RegionIntegerNumeric(own_x,own_y,own_x + AOI , own_y+my_height);


		rmap.EAST_MINE=new RegionIntegerNumeric(own_x + my_width - AOI,own_y,own_x +my_width , own_y+my_height);


		rmap.NORTH_MINE=new RegionIntegerNumeric(own_x ,own_y,own_x+my_width, own_y + AOI);


		rmap.SOUTH_MINE=new RegionIntegerNumeric(own_x,own_y+my_height-AOI,own_x+my_width, (own_y+my_height));

		//horizontal partitioning
		//horizontal partitioning
		if(rows==1){
			numNeighbors = 2;
			if(cellType.pos_j>0 && cellType.pos_j<columns-1){

				rmap.WEST_OUT=new RegionIntegerNumeric(own_x-AOI,own_y,own_x, own_y+my_height);

				rmap.EAST_OUT=new RegionIntegerNumeric(own_x+my_width,own_y,own_x+my_width,own_y+my_height);
			}

			else if(cellType.pos_j==0){
				numNeighbors = 1;
				rmap.EAST_OUT=new RegionIntegerNumeric(own_x+my_width,own_y,own_x+my_width+AOI,own_y+my_height);
			}	


			else if(cellType.pos_j==columns-1){
				numNeighbors = 1;
				rmap.WEST_OUT=new RegionIntegerNumeric(own_x-AOI,own_y,own_x, own_y+my_height);
			}
		}else 
			if(rows>1 && columns == 1){ // Horizontal partitionig
				numNeighbors =2;
				rmap.NORTH_OUT=new RegionIntegerNumeric(own_x, own_y - AOI,	own_x+ my_width,own_y);
				rmap.SOUTH_OUT=new RegionIntegerNumeric(own_x,own_y+my_height,own_x+my_width, own_y+my_height+AOI);
				if(cellType.pos_i == 0){
					numNeighbors =1;
					rmap.NORTH_OUT = null;
				}
				if(cellType.pos_i == rows-1){
					numNeighbors =1;
					rmap.SOUTH_OUT= null;
				}
			}else{ //sqare partitioning 

				/*
				 * In this case we use a different approach: Firt we make all ghost sections, after that
				 * we remove the useful ghost section
				 * 
				 * */
				numNeighbors = 8;
				//corner up left
				rmap.NORTH_WEST_OUT=new RegionIntegerNumeric(own_x-AOI, own_y-AOI,own_x, own_y);


				//corner up right
				rmap.NORTH_EAST_OUT = new RegionIntegerNumeric(own_x+my_width,own_y-AOI,own_x+my_width+AOI,own_y);


				//corner down left
				rmap.SOUTH_WEST_OUT=new RegionIntegerNumeric(own_x-AOI, own_y+my_height,own_x,own_y+my_height+AOI);

				rmap.NORTH_OUT=new RegionIntegerNumeric(own_x, own_y - AOI,	own_x+ my_width,own_y);

				//corner down right
				rmap.SOUTH_EAST_OUT=new RegionIntegerNumeric(own_x+my_width, own_y+my_height,own_x+my_width+AOI,own_y+my_height+AOI);

				rmap.SOUTH_OUT=new RegionIntegerNumeric(own_x,own_y+my_height,own_x+my_width, own_y+my_height+AOI);

				rmap.WEST_OUT=new RegionIntegerNumeric(own_x-AOI,own_y,own_x, own_y+my_height);


				rmap.EAST_OUT=new RegionIntegerNumeric(own_x+my_width,own_y,own_x+my_width+AOI,own_y+my_height);

				if(cellType.pos_i==0 ){
					numNeighbors = 5;
					rmap.NORTH_OUT = null;
					rmap.NORTH_WEST_OUT = null;
					rmap.NORTH_EAST_OUT = null;
				}

				if(cellType.pos_j == 0){
					numNeighbors = 5;
					rmap.SOUTH_WEST_OUT = null;
					rmap.NORTH_WEST_OUT=null;
					rmap.WEST_OUT = null;
				}

				if(cellType.pos_i == rows -1){
					numNeighbors = 5;
					rmap.SOUTH_WEST_OUT = null;
					rmap.SOUTH_OUT = null;
					rmap.SOUTH_EAST_OUT = null;
				}

				if(cellType.pos_j == columns -1){
					numNeighbors = 5;
					rmap.NORTH_EAST_OUT = null;
					rmap.EAST_OUT = null;
					rmap.SOUTH_EAST_OUT = null;
				}

				if((cellType.pos_i == 0 && cellType.pos_j == 0) || 
						(cellType.pos_i == rows-1 && cellType.pos_j==0) || 
						(cellType.pos_i == 0 && cellType.pos_j == columns -1) || 
						(cellType.pos_i == rows-1 && cellType.pos_j == columns -1))
					numNeighbors = 3;
			}
	}

	private void makeToroidalSections() {

		numNeighbors = 8;
		myfield=new RegionIntegerNumeric(own_x+AOI,own_y+AOI, own_x+my_width-AOI , own_y+my_height-AOI);


		//corner up left
		rmap.NORTH_WEST_OUT=new RegionIntegerNumeric((own_x-AOI + width)%width, (own_y-AOI+height)%height, 
				(own_x+width)%width==0?width:(own_x+width)%width, (own_y+height)%height==0?height:(own_y+height)%height);
		rmap.NORTH_WEST_MINE=new RegionIntegerNumeric(own_x, own_y, own_x+AOI, own_y+AOI);

		//corner up right
		rmap.NORTH_EAST_OUT = new RegionIntegerNumeric((own_x+my_width+width)%width, (own_y-AOI+height)%height,
				(own_x+my_width+AOI+width)%width==0?width:(own_x+my_width+AOI+width)%width, (own_y+height)%height==0?height:(own_y+height)%height);
		rmap.NORTH_EAST_MINE=new RegionIntegerNumeric(own_x+my_width-AOI, own_y, own_x+my_width, own_y+AOI);

		//corner down left
		rmap.SOUTH_WEST_OUT=new RegionIntegerNumeric((own_x-AOI+width)%width, (own_y+my_height+height)%height,
				(own_x+width)%width==0?width:(own_x+width)%width,(own_y+my_height+AOI+height)%height==0?height:(own_y+my_height+AOI+height)%height);
		rmap.SOUTH_WEST_MINE=new RegionIntegerNumeric(own_x, own_y+my_height-AOI,own_x+AOI, own_y+my_height);

		//corner down right
		rmap.SOUTH_EAST_OUT=new RegionIntegerNumeric((own_x+my_width+width)%width, (own_y+my_height+height)%height, 
				(own_x+my_width+AOI+width)%width==0?width:(own_x+my_width+AOI+width)%width,(own_y+my_height+AOI+height)%height==0?height:(own_y+my_height+AOI+height)%height);
		rmap.SOUTH_EAST_MINE=new RegionIntegerNumeric(own_x+my_width-AOI, own_y+my_height-AOI,own_x+my_width,own_y+my_height);

		rmap.WEST_OUT=new RegionIntegerNumeric((own_x-AOI+width)%width,(own_y+height)%height,
				(own_x+width)%width==0?width:(own_x+width)%width, ((own_y+my_height)+height)%height==0?height:((own_y+my_height)+height)%height);
		rmap.WEST_MINE=new RegionIntegerNumeric(own_x,own_y,own_x + AOI , own_y+my_height);

		rmap.EAST_OUT=new RegionIntegerNumeric((own_x+my_width+width)%width,(own_y+height)%height,
				(own_x+my_width+AOI+width)%width==0?width:(own_x+my_width+AOI+width)%width, (own_y+my_height+height)%height==0?height:(own_y+my_height+height)%height);
		rmap.EAST_MINE=new RegionIntegerNumeric(own_x + my_width - AOI,own_y,own_x +my_width , own_y+my_height);


		rmap.NORTH_MINE=new RegionIntegerNumeric(own_x ,own_y,own_x+my_width, own_y + AOI);


		rmap.SOUTH_MINE=new RegionIntegerNumeric(own_x,own_y+my_height-AOI,own_x+my_width, (own_y+my_height));

		rmap.NORTH_OUT=new RegionIntegerNumeric((own_x+width)%width, (own_y - AOI+height)%height,
				(own_x+ my_width +width)%width==0?width:(own_x+ my_width +width)%width,(own_y+height)%height==0?height:(own_y+height)%height);

		rmap.SOUTH_OUT=new RegionIntegerNumeric((own_x+width)%width,(own_y+my_height+height)%height,
				(own_x+my_width+width)%width==0?width:(own_x+my_width+width)%width, (own_y+my_height+AOI+height)%height==0?height:(own_y+my_height+AOI+height)%height);

		//if square partitioning
		if(rows==1 && columns >1){
			numNeighbors = 6;
			rmap.NORTH_OUT = null;
			rmap.SOUTH_OUT = null;
		}
		else if(rows > 1 && columns == 1){
			numNeighbors = 6;
			rmap.EAST_OUT = null;
			rmap.WEST_OUT = null;
		}
	}

	@Override
	public synchronized boolean synchro() {

		this.startTime = System.currentTimeMillis();

		ConnectionJMS conn = (ConnectionJMS)((DistributedState<?>)sm).getCommunicationVisualizationConnection();
		Connection connWorker = (Connection)((DistributedState<?>)sm).getCommunicationWorkerConnection();

		if(((DistributedMultiSchedule)sm.schedule).isEnableZoomView)
		{
			tmp_zoom=new ZoomArrayList<EntryNum<Integer, Int2D>>();
			tmp_zoom.STEP=sm.schedule.getSteps()-1;
		}

		if(this.getState().schedule.getSteps() !=0){

			clear_ghost_regions();
			memorizeRegionOut();


			//every value in the myfield region is setted
			for(EntryNum<Integer, Int2D> e: myfield.values())
			{			
				Int2D loc=e.l;
				int i = e.r;
				this.field[loc.getX()][loc.getY()]=i;	
				if(((DistributedMultiSchedule)sm.schedule).monitor.ZOOM)
					tmp_zoom.add(new EntryNum<Integer,Int2D>(i, loc));
			}     
			if(conn!=null &&
					((DistributedMultiSchedule)sm.schedule).monitor.ZOOM)
			{
				try {
					tmp_zoom.STEP=((DistributedMultiSchedule)sm.schedule).getSteps()-1;
					conn.publishToTopic(tmp_zoom,topicPrefix+"GRAPHICS"+cellType,name);
					tmp_zoom=new ZoomArrayList<EntryNum<Integer,Int2D>>();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

		}


		//--> publishing the regions to correspondent topics for the neighbors	
		publishRegions(connWorker);

		processUpdates();

		this.endTime = System.currentTimeMillis();

		return true;
	}


	private void processUpdates() {
		PriorityQueue<Object> q;
		try 
		{
			q = updates.getUpdates(sm.schedule.getSteps()-1, numNeighbors);
			while(!q.isEmpty())
			{
				DistributedRegionNumeric<Integer, EntryNum<Integer,Int2D>> region =
						(DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>>)q.poll();
				verifyUpdates(region);	
			}			

		}catch (InterruptedException e1) {e1.printStackTrace(); } catch (DMasonException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		for(RegionNumeric<Integer,EntryNum<Integer,Int2D>> region : updates_cache){
			for(EntryNum<Integer,Int2D> e_m: region.values())
			{
				Int2D i=new Int2D(e_m.l.getX(), e_m.l.getY());
				field[i.getX()][i.getY()]=e_m.r;	
			}
		}	

		this.reset();
	}


	private void publishRegions(Connection connWorker) {
		//--> publishing the regions to correspondent topics for the neighbors
		if(rmap.WEST_OUT!=null)
		{
			DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>> dr =
					new DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>>
			(rmap.WEST_MINE,rmap.WEST_OUT, (sm.schedule.getSteps()-1),
					cellType,DistributedRegionNumeric.WEST);
			try 
			{				
				connWorker.publishToTopic(dr,topicPrefix+cellType+"W", name);

			} catch (Exception e1) { e1.printStackTrace();}
		}

		if(rmap.EAST_OUT!=null)
		{
			DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>> dr = 
					new DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>>
			(rmap.EAST_MINE,rmap.EAST_OUT,(sm.schedule.getSteps()-1),
					cellType,DistributedRegionNumeric.EAST);	
			try 
			{				
				connWorker.publishToTopic(dr,topicPrefix+cellType.toString()+"E", name);

			} catch (Exception e1) {e1.printStackTrace(); }
		}
		if(rmap.NORTH_OUT!=null )
		{
			DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>> dr = 
					new  DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>>
			(rmap.NORTH_MINE,rmap.NORTH_OUT,(sm.schedule.getSteps()-1),
					cellType,DistributedRegionNumeric.NORTH);
			try 
			{
				connWorker.publishToTopic(dr,topicPrefix+cellType.toString()+"N", name);

			} catch (Exception e1) {e1.printStackTrace();}
		}

		if(rmap.SOUTH_OUT!=null )
		{
			DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>> dr =
					new DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>>
			(rmap.SOUTH_MINE,rmap.SOUTH_OUT,(sm.schedule.getSteps()-1),
					cellType,DistributedRegionNumeric.SOUTH);

			try 
			{				
				connWorker.publishToTopic(dr,topicPrefix+cellType.toString()+"S", name);

			} catch (Exception e1) { e1.printStackTrace(); }
		}

		if(rmap.NORTH_WEST_OUT!=null)
		{
			DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>> dr = 
					new DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>>
			(rmap.NORTH_WEST_MINE,rmap.NORTH_WEST_OUT,
					(sm.schedule.getSteps()-1),cellType,DistributedRegionNumeric.NORTH_WEST);

			try 
			{
				connWorker.publishToTopic(dr,topicPrefix+cellType.toString()+"NW", name);

			} catch (Exception e1) { e1.printStackTrace();}
		}
		if(rmap.NORTH_EAST_OUT!=null)
		{
			DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>> dr = 
					new DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>>	
			(rmap.NORTH_EAST_MINE,rmap.NORTH_EAST_OUT,
					(sm.schedule.getSteps()-1),cellType,DistributedRegionNumeric.NORTH_EAST);
			try 
			{
				connWorker.publishToTopic(dr,topicPrefix+cellType.toString()+"NE", name);

			} catch (Exception e1) {e1.printStackTrace();}
		}
		if( rmap.SOUTH_WEST_OUT!=null)
		{
			DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>> dr = 
					new DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>>
			(rmap.SOUTH_WEST_MINE, rmap.SOUTH_WEST_OUT,
					(sm.schedule.getSteps()-1),cellType,DistributedRegionNumeric.SOUTH_WEST);
			try 
			{
				connWorker.publishToTopic(dr,topicPrefix+cellType.toString()+"SW", name);

			} catch (Exception e1) {e1.printStackTrace();}
		}
		if(rmap.SOUTH_EAST_OUT!=null)
		{
			DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>> dr = 
					new DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>>
			(rmap.SOUTH_EAST_MINE,rmap.SOUTH_EAST_OUT,
					(sm.schedule.getSteps()-1),cellType,DistributedRegionNumeric.SOUTH_EAST);
			try 
			{
				connWorker.publishToTopic(dr,topicPrefix+cellType.toString()+"SE", name);

			} catch (Exception e1) { e1.printStackTrace(); }
		}	
		//<--
	}


	private void clear_ghost_regions() {
		updateFields(); //update fields with java reflect
		updates_cache= new ArrayList<RegionNumeric<Integer,EntryNum<Integer,Int2D>>>();
	}



	/**
	 * Provide the int value shift logic among the peers
	 * @param remoteValue the value 
	 * @param l the location
	 * @param sm the simstate
	 * @return true if it is correct 
	 */
	public boolean setDistributedObjectLocation( Int2D l, Object remoteValue ,SimState sm) throws DMasonException{

		if(!(remoteValue instanceof Integer))
			throw new DMasonException("Cast Exception setDistributedObjectLocation, second parameter must be a int");

		int d = (Integer) remoteValue;

		if(setValue(d, l)) return true;
		else{
			String errorMessage = String.format("Unable to set value on position (%d, %d): out of boundaries on cell %s. (ex OH MY GOD!)",
					l.x, l.y, cellType);

			logger.severe( errorMessage ); // it should never happen (don't tell it to anyone shhhhhhhh! ;P) // it should never happen (don't tell it to anyone shhhhhhhh! ;P)
		}
		return false;
	}



	@Override
	public DistributedState getState() {

		return (DistributedState)sm;
	}

	/**
	 * This method, written with Java Reflect, provides to add the value
	 * in the right Region.
	 * @param value The value to add
	 * @param l The new location of the value
	 * @return true if the value is added in right way
	 */
	private boolean setValue(int value, Int2D l){

		if(rmap.NORTH_WEST_MINE!=null && rmap.NORTH_WEST_MINE.isMine(l.x,l.y))
		{
			if(((DistributedMultiSchedule)sm.schedule).monitor.ZOOM)
				tmp_zoom.add(new EntryNum<Integer, Int2D>(value, l));
			rmap.NORTH_WEST_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
			rmap.WEST_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
			myfield.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
			return rmap.NORTH_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
		}
		else
			if(rmap.NORTH_EAST_MINE!=null && rmap.NORTH_EAST_MINE.isMine(l.x,l.y))
			{
				if(((DistributedMultiSchedule)sm.schedule).monitor.ZOOM)
					tmp_zoom.add(new EntryNum<Integer, Int2D>(value, l));
				rmap.NORTH_EAST_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
				rmap.EAST_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
				myfield.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
				return rmap.NORTH_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
			}
			else
				if(rmap.SOUTH_WEST_MINE!=null && rmap.SOUTH_WEST_MINE.isMine(l.x,l.y))
				{
					if(((DistributedMultiSchedule)sm.schedule).monitor.ZOOM)
						tmp_zoom.add(new EntryNum<Integer, Int2D>(value, l));
					rmap.SOUTH_WEST_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
					rmap.WEST_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
					myfield.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
					return rmap.SOUTH_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
				}
				else
					if(rmap.SOUTH_EAST_MINE!=null && rmap.SOUTH_EAST_MINE.isMine(l.x,l.y))
					{
						if(((DistributedMultiSchedule)sm.schedule).monitor.ZOOM)
							tmp_zoom.add(new EntryNum<Integer, Int2D>(value, l));
						rmap.SOUTH_EAST_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
						rmap.EAST_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
						myfield.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
						return rmap.SOUTH_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
					}
					else
						if(rmap.WEST_MINE != null && rmap.WEST_MINE.isMine(l.x,l.y))
						{
							if(((DistributedMultiSchedule)sm.schedule).monitor.ZOOM)
								tmp_zoom.add(new EntryNum<Integer, Int2D>(value, l));
							myfield.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
							return rmap.WEST_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
						}
						else
							if(rmap.EAST_MINE != null && rmap.EAST_MINE.isMine(l.x,l.y))
							{
								if(((DistributedMultiSchedule)sm.schedule).monitor.ZOOM)
									tmp_zoom.add(new EntryNum<Integer, Int2D>(value, l));
								myfield.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
								return rmap.EAST_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
							}
							else
								if(rmap.NORTH_MINE != null && rmap.NORTH_MINE.isMine(l.x,l.y))
								{
									if(((DistributedMultiSchedule)sm.schedule).monitor.ZOOM)
										tmp_zoom.add(new EntryNum<Integer, Int2D>(value, l));
									myfield.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
									return rmap.NORTH_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
								}
								else
									if(rmap.SOUTH_MINE != null && rmap.SOUTH_MINE.isMine(l.x,l.y))
									{
										if(((DistributedMultiSchedule)sm.schedule).monitor.ZOOM)
											tmp_zoom.add(new EntryNum<Integer, Int2D>(value, l));
										myfield.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
										return rmap.SOUTH_MINE.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
									}
									else
										if(myfield.isMine(l.x,l.y))
										{
											if(((DistributedMultiSchedule)sm.schedule).monitor.ZOOM)
												tmp_zoom.add(new EntryNum<Integer, Int2D>(value, l));

											return myfield.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
										}
										else
											if(rmap.WEST_OUT!=null && rmap.WEST_OUT.isMine(l.x,l.y)) 
												return rmap.WEST_OUT.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
											else
												if(rmap.EAST_OUT!=null && rmap.EAST_OUT.isMine(l.x,l.y)) 
													return rmap.EAST_OUT.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
												else
													if(rmap.NORTH_OUT!=null && rmap.NORTH_OUT.isMine(l.x,l.y))
														return rmap.NORTH_OUT.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
													else
														if(rmap.SOUTH_OUT!=null && rmap.SOUTH_OUT.isMine(l.x,l.y))
															return rmap.SOUTH_OUT.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
														else
															if(rmap.NORTH_WEST_OUT!=null && rmap.NORTH_WEST_OUT.isMine(l.x,l.y)) 
																return rmap.NORTH_WEST_OUT.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
															else 
																if(rmap.SOUTH_WEST_OUT!=null && rmap.SOUTH_WEST_OUT.isMine(l.x,l.y)) 
																	return rmap.SOUTH_WEST_OUT.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
																else
																	if(rmap.NORTH_EAST_OUT!=null && rmap.NORTH_EAST_OUT.isMine(l.x,l.y)) 
																		return rmap.NORTH_EAST_OUT.addEntryNum(new EntryNum<Integer,Int2D>(value, l));
																	else
																		if(rmap.SOUTH_EAST_OUT!=null && rmap.SOUTH_EAST_OUT.isMine(l.x,l.y))
																			return rmap.SOUTH_EAST_OUT.addEntryNum(new EntryNum<Integer,Int2D>(value, l));


		return false;	       			       			
	}


	/**
	 * This method, written with Java Reflect, follows two logical ways for all the regions:
	 * - if a region is an out one, the value's location is updated and it's insert a new Entry 
	 * 		in the updates_cache (cause the agent is moving out and it's important to maintain the information
	 * 		for the next step)
	 * - if a region is a mine one, the value's location is updated and the value is setted.
	 */
	public void updateFields()
	{
		Class o=rmap.getClass();

		Field[] fields = o.getDeclaredFields();
		for (int z = 0; z < fields.length; z++)
		{
			fields[z].setAccessible(true);
			try
			{
				String name=fields[z].getName();
				Method method = o.getMethod("get"+name, null);
				Object returnValue = method.invoke(rmap, null);
				if(returnValue!=null)
				{
					RegionNumeric<Integer,EntryNum<Integer,Int2D>> region=((RegionNumeric<Integer,EntryNum<Integer,Int2D>>)returnValue);

					if(name.contains("OUT"))
					{
						for(EntryNum<Integer,Int2D> e : region.values()){

							Int2D pos = new Int2D(e.l.getX(), e.l.getY());
							int i = e.r;
							this.field[pos.getX()][pos.getY()]=i;
						}
					}
				}
			}
			catch (IllegalArgumentException e){e.printStackTrace();} 
			catch (IllegalAccessException e) {e.printStackTrace();} 
			catch (SecurityException e) {e.printStackTrace();} 
			catch (NoSuchMethodException e) {e.printStackTrace();} 
			catch (InvocationTargetException e) {e.printStackTrace();}
		}	 
	}


	public void memorizeRegionOut()
	{
		Class o=rmap.getClass();

		Field[] fields = o.getDeclaredFields();
		for (int z = 0; z < fields.length; z++)
		{
			fields[z].setAccessible(true);
			try
			{
				String name=fields[z].getName();
				Method method = o.getMethod("get"+name, null);
				Object returnValue = method.invoke(rmap, null);
				if(returnValue!=null)
				{
					RegionNumeric<Integer,EntryNum<Integer,Int2D>> region=((RegionNumeric<Integer,EntryNum<Integer,Int2D>>)returnValue);
					if(name.contains("OUT"))
					{

						updates_cache.add(region.clone());
					}
				}
			}
			catch (IllegalArgumentException e){e.printStackTrace();} 
			catch (IllegalAccessException e) {e.printStackTrace();} 
			catch (SecurityException e) {e.printStackTrace();} 
			catch (NoSuchMethodException e) {e.printStackTrace();} 
			catch (InvocationTargetException e) {e.printStackTrace();}
		}	     
	}

	/**
	 * This method takes updates from box and set every value in the regions out.
	 * Every value in the regions mine is compared with every value in the updates_cache:
	 * if they are not equals, that in box mine is added.
	 * 
	 * @param box A Distributed Region that contains the updates
	 */
	public void verifyUpdates(DistributedRegionNumeric<Integer,EntryNum<Integer,Int2D>> box)
	{
		RegionNumeric<Integer,EntryNum<Integer,Int2D>> r_mine=box.out;
		RegionNumeric<Integer,EntryNum<Integer,Int2D>> r_out=box.mine;

		for(EntryNum<Integer,Int2D> e_m: r_mine.values())
		{
			Int2D i=new Int2D(e_m.l.getX(),e_m.l.getY());

			field[i.getX()][i.getY()]=e_m.r;		  		
		}		
		updates_cache.add(r_out);
	}

	/**
	 * Clear all Regions.
	 * @return true if the clearing is successful, false if exception is generated
	 */
	public  boolean reset()
	{
		myfield.clear();

		Class o=rmap.getClass();

		Field[] fields = o.getDeclaredFields();
		for (int z = 0; z < fields.length; z++)
		{
			fields[z].setAccessible(true);
			try
			{
				String name=fields[z].getName();
				Method method = o.getMethod("get"+name, null);
				Object returnValue = method.invoke(rmap, null);

				if(returnValue!=null)
				{
					RegionNumeric<Integer,EntryNum<Integer, Int2D>> region=((RegionNumeric<Integer,EntryNum<Integer, Int2D>>)returnValue);
					region.clear();    
				}
			}
			catch (IllegalArgumentException e){e.printStackTrace(); return false;} 
			catch (IllegalAccessException e) {e.printStackTrace();return false;} 
			catch (SecurityException e) {e.printStackTrace();return false;} 
			catch (NoSuchMethodException e) {e.printStackTrace();return false;} 
			catch (InvocationTargetException e) {e.printStackTrace();return false;}
		}
		return true;
	}

	@Override
	public Int2D getAvailableRandomLocation() {

		double shiftx=((DistributedState)sm).random.nextDouble();
		double shifty=((DistributedState)sm).random.nextDouble();
		int x= (int)(own_x+my_width*shiftx);	
		int y= (int)(own_y+my_height*shifty);

		return (new Int2D(x, y));
	}



	@Override
	public void setTable(HashMap table) {
		ConnectionJMS conn = (ConnectionJMS) ((DistributedState<?>)sm).getCommunicationManagementConnection();
		if(conn!=null)
			conn.setTable(table);

	}




	@Override
	public UpdateMap getUpdates() {
		return updates;
	}





	@Override
	public VisualizationUpdateMap<String, Object> getGlobals()
	{
		return globals;
	}
	@Override
	public boolean verifyPosition(Int2D pos) {

		return (rmap.NORTH_WEST_MINE!=null && rmap.NORTH_WEST_MINE.isMine(pos.x,pos.y))||

				(rmap.NORTH_EAST_MINE!=null && rmap.NORTH_EAST_MINE.isMine(pos.x,pos.y))
				||
				(rmap.SOUTH_WEST_MINE!=null && rmap.SOUTH_WEST_MINE.isMine(pos.x,pos.y))
				||(rmap.SOUTH_EAST_MINE!=null && rmap.SOUTH_EAST_MINE.isMine(pos.x,pos.y))
				||(rmap.WEST_MINE != null && rmap.WEST_MINE.isMine(pos.x,pos.y))
				||(rmap.EAST_MINE != null && rmap.EAST_MINE.isMine(pos.x,pos.y))
				||(rmap.NORTH_MINE != null && rmap.NORTH_MINE.isMine(pos.x,pos.y))
				||(rmap.SOUTH_MINE != null && rmap.SOUTH_MINE.isMine(pos.x,pos.y))
				||(myfield.isMine(pos.x,pos.y));

	}


	@Override
	public String getDistributedFieldID() {

		return name;
	}


	@Override
	public Bag clear() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public long getCommunicationTime() {
		// TODO Auto-generated method stub
		return this.endTime - this.startTime;
	}
}
