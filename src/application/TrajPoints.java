package application;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;

public class TrajPoints extends RoboticsAPIApplication{

	@Inject
	private LBR lbr;
    private Tool tracker_tool;
    String fname;
	  
    ArrayList<ArrayList<Double>> joints_poses = new ArrayList<ArrayList<Double>>();
	
	boolean exit; 
	
	//DataRecorder rec;
	
	@Override
	public void initialize() {
		// initialize your application here
		tracker_tool = createFromTemplate("TrackerTool");
		tracker_tool.attachTo(lbr.getFlange());
		
		System.out.println("Tracker tool frame: " + tracker_tool.getFrame("sphere_tcp").toString());
	
		fname = "C:\\Users\\KukaUser\\Desktop\\Metrologia\\registered_poses.txt";
		String str;
		String file = "C:\\Users\\KukaUser\\Desktop\\Metrologia\\poses.txt";
		FileReader f;
      
		Double val;
      
		try 
		{
			f = new FileReader(file);
		
			 BufferedReader br = new BufferedReader(f);
			 ArrayList<Double> j_pose = new ArrayList<Double>();
			 
		     while((str = br.readLine())!=null) 
		     {
		    	 
		    	 String data[] = str.split(",");
		    	 j_pose.clear();
		    	 
			     System.out.println(data.length);

		    	 for(int i=0; i<data.length; i++)
		    	 {
		    		
		    		 val = Double.parseDouble(data[i]);
		    		 j_pose.add(val);

		    	 } 		 	 
			     System.out.println(j_pose.toString());

		    	 joints_poses.add(j_pose);
		    	
			     System.out.println(j_pose.toString());

		     }
		     
		  
		     System.out.println(joints_poses.size());
		     
		     
	    //	 System.out.println(joints_poses.get(0).toString());
	    //	 System.out.println(joints_poses.get(1).toString());


		    /* for(int i =0; i<joints_poses.size(); i++)
		    	 System.out.println(joints_poses.get(i).toString());
	    	 */
		     br.close();
	      } 
	      catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	      } 
	      catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// your application execution starts here

		exit=false;
		
		do {
			/*rec = new DataRecorder();
			rec.setTimeout(2L, TimeUnit.MINUTES);
			
			rec.setFileName(fname);
			rec.addCommandedJointPosition(lbr, DataRecorder.AngleUnit.Radian);
			rec.addCurrentCartesianPositionXYZ(tracker_tool.getFrame("sphere_tcp"), getApplicationData().getFrame("/robot_base"));
		 	rec.enable();
			rec.startRecording();*/
			
			tracker_tool.getFrame("sphere_tcp").move(ptp(getFrame("/robot_base/Home")).setJointVelocityRel(0.25));
			
			switch (getApplicationUI().displayModalDialog(
					ApplicationDialogType.QUESTION,"Start running the application?", 
					"YES", "END DO NOTHING"))
			{

				case 0:
						
					
					FileWriter file = null;
					
					try {
						
				
						file = new FileWriter(fname);
						String str;
						
						for(int i=0; i<joints_poses.size(); i++)
						{
							
							System.out.println("Movement " + i);
							str = "Movement " + String.valueOf(i) + "\n";
							file.write(str);
							
							JointPosition joints = new JointPosition(0,0,0,0,0,0,0);
								
							joints.set(0, joints_poses.get(i).get(0));
							joints.set(1, joints_poses.get(i).get(1));
							joints.set(2, joints_poses.get(i).get(2));
							joints.set(3, joints_poses.get(i).get(3));
							joints.set(4, joints_poses.get(i).get(4));
							joints.set(5, joints_poses.get(i).get(5));
							joints.set(6, joints_poses.get(i).get(6));
							
				    		System.out.println(joints.toString() + "\n");

							lbr.move(ptp(joints).setJointVelocityRel(0.25));
							
							//Save current joint position
							joints = lbr.getCurrentJointPosition();
							str = joints.toString();
							file.write(str);
							
							//Save current robot cartesian pose
							Frame current_pose = lbr.getCurrentCartesianPosition(tracker_tool.getFrame("sphere_tcp"),  getApplicationData().getFrame("/robot_base"));
							str = current_pose.toString();
							file.write(str);
							
							
							try {
								TimeUnit.SECONDS.sleep(5);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}	
						}
						
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
						break;				
								
					case 1:
						getLogger().info("App finished\n"+"***END***");
						exit = true;
						break;
				}
			
		} while (!exit);	
	}
}