package com.intel.fangpei.process;

import java.util.HashMap;
import java.util.Map;

import com.intel.fangpei.task.JvmTask;
import com.intel.fangpei.task.TaskTracker;
import com.intel.fangpei.util.ClientUtil;

public class ChildStrategy extends Thread{
	private StartegyRunner runner = null;
	Map<String,String[]> child = new HashMap<String,String[]>();
	//clean env:close the jvm 
	private String lastwork="break";
	public ChildStrategy(){
	}
	public synchronized void addLoads(Map<String,String[]> map){
//		System.out.println("***"+map.get(map.keySet().iterator().next())[0]);
		child.putAll(map);
	}
	public Map<String,String[]> getLoads(){
		return child;
	}
	public String getLastWork(){
		return lastwork;
	}
	public  boolean canDoNextWork(){	
		return true;
	}
	public void startStrategyRunner(TaskTracker boss, JvmTask taskManager){
		runner = new StartegyRunner(this,boss,taskManager);
		//runner.setDaemon(true);
		runner.start();
	}
	public class StartegyRunner extends Thread{
		private TaskTracker boss = null;
		private JvmTask taskManager = null;
		private ChildStrategy childStrate = null;
		public StartegyRunner(ChildStrategy childStrate,TaskTracker boss, JvmTask taskManager){
			this.childStrate = childStrate;
			this.boss = boss;
			this.taskManager = taskManager;
		}
		public void run(){
			System.out.println("[ChildStartegy]boss is running:"+boss.isRunning());
			System.out.println("[ChildStartegy]has no work to assign?:"+taskManager.noTaskAssign());
			while(boss.isRunning()&&!taskManager.noTaskAssign()){	
				if(childStrate.canDoNextWork()){
					if(taskManager.nextWork()){
					System.out.println("[ChildStartegy]assign new task");
					}
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
