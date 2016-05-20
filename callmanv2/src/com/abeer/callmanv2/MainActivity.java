package com.abeer.callmanv2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telecom.*;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	//UI handlers
	TextView userNotification;
	Button call;
	Button reset;
	Button ensureReset;
	Button export;
	Button refresh;
	Button settings;
	Button updateParameters;
	Button receiverMode;
	ProgressBar progress;
	EditText text;
	Button send;
	
	//parameters
	int noOfBytes=50;
	int byteSize=16;
	int callSetupTime=2000;//new encode time(Change ASAP)
	int callDisconnectTime=1500;
	int stateTime=1000;
	String phoneNo="03014543341";//03084762381
	
	//variables
	int encodeTime0=callSetupTime;
	int ensure = 0;
	int closed = 0;
	int callsLeft=noOfBytes*byteSize+1;
	//int data_time[] = {500,1000,1500,2000, 2500, 3000, 3500, 4000, 4500, 5000, 5500, 6000, 6500, 7000, 7500, 8000};
	int data_time[] = {1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 11000, 12000, 13000, 14000, 15000, 16000};
	String freq_char_set="etoainshrdlcumwfgypbvkjxqz111";
	String status="null";
	int call_locked=0;
	int waiting_for_call_connect;
	Method telephonyEndCall;
	Object telephonyObject;
	int receiverState=0;
	
	//utility functions
	public int waitForCall(){
		//Log.e("->>1",""+System.currentTimeMillis()+"\n");
		waiting_for_call_connect=0;
		Visualizer mVisualizer = new Visualizer(0);
		mVisualizer.setEnabled(false);
		int capRate = Visualizer.getMaxCaptureRate();
		int capSize = Visualizer.getCaptureSizeRange()[1];
		mVisualizer.setCaptureSize(capSize);
		Visualizer.OnDataCaptureListener captureListener = new Visualizer.OnDataCaptureListener() {
		  public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
		  int samplingRate) {   
		    for (int i=0;i<bytes.length;i++) {
		        if (bytes[i]!=-128) {
		            //yes detected
		        	waiting_for_call_connect=1;
		        	//Log.e("*********",closed+" break "+waiting_for_call_connect+" "+bytes.length);
		            break;
		        }
		    } 
		    
		    /*if(waiting_for_call_connect==1 && closed!=1){
		    	String s=new String(bytes);
		    	writeToFile("audio",s);
		    	File myDir=makeTimeDir();
		    	makeFile("audio", myDir);
		    	Log.e("**888888**","**888888**");
		    }*/
		    
		  }

		  public void onFftDataCapture(Visualizer visualizer, byte[] bytes,
		    int samplingRate) {
		  }       
		};
	
		int status2 = mVisualizer.setDataCaptureListener(captureListener,
		    capRate, true/*wave*/, false/*no fft needed*/);
		mVisualizer.setEnabled(true);
		//Log.e("->>2",""+System.currentTimeMillis()+"\n");
		while(true){if(waiting_for_call_connect==1){break;}}
		//Log.e("->>3",""+System.currentTimeMillis()+"\n");
		mVisualizer.setEnabled(false);
		mVisualizer.release();
		return 1;

	}
	public void clearFile(String fileName){
		FileOutputStream fos;
		try {
			fos = openFileOutput(fileName, Context.MODE_PRIVATE);//"lastOffHook"
			String str = "";
			byte[] b = str.getBytes();
			fos.write(b);
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public void makeFile(String fileName,File myDir){
		//make dir
		String temp = null;
		
		
		FileInputStream fin;
		try {
			fin = openFileInput(fileName);
			byte[] b = new byte[fin.available()];
			fin.read(b);
			String s = new String(b);
			temp = s;
			fin.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		File file = new File(myDir, fileName+".txt");
		try {
			FileOutputStream out = new FileOutputStream(file);
			out.write(temp.getBytes());
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public File makeTimeDir(){
		String root = Environment.getExternalStorageDirectory().toString();
		long name=System.currentTimeMillis();
		File myDir = new File(root + "/experiemnts/"+name);
		myDir.mkdirs();
		return myDir;	
	}
	public Method disconnectInitializer(){
		
		try {
			String serviceManagerName = "android.os.ServiceManager";
			String serviceManagerNativeName = "android.os.ServiceManagerNative";
			String telephonyName = "com.android.internal.telephony.ITelephony";
			Class<?> telephonyClass;
			Class<?> telephonyStubClass;
			Class<?> serviceManagerClass;
			Class<?> serviceManagerNativeClass;
			
			Object serviceManagerObject;
			telephonyClass = Class.forName(telephonyName);
			telephonyStubClass = telephonyClass.getClasses()[0];
			serviceManagerClass = Class.forName(serviceManagerName);
			serviceManagerNativeClass = Class.forName(serviceManagerNativeName);
			Method getService = // getDefaults[29];
			serviceManagerClass.getMethod("getService", String.class);
			Method tempInterfaceMethod = serviceManagerNativeClass.getMethod(
					"asInterface", IBinder.class);
			Binder tmpBinder = new Binder();
			tmpBinder.attachInterface(null, "fake");
			serviceManagerObject = tempInterfaceMethod.invoke(null, tmpBinder);
			IBinder retbinder = (IBinder) getService.invoke(
					serviceManagerObject, "phone");
			Method serviceMethod = telephonyStubClass.getMethod("asInterface",
					IBinder.class);
			telephonyObject = serviceMethod.invoke(null, retbinder);
			telephonyEndCall = telephonyClass.getMethod("endCall");
			

		} catch (Exception e) {
			e.printStackTrace();

		}
		return telephonyEndCall;
	}
	public void disconnectCall() {
		try {
			telephonyEndCall.invoke(telephonyObject);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void call(View v) {
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.setData(Uri.parse("tel:"+phoneNo));
				status="not done";
				
				//UI
				progress.setProgress(0);

				for (int test = 0; test < noOfBytes; test++) {
					for (int count = 0; count < byteSize; count++) {
						if (closed == 1) {
							break;
						}
						long l=System.currentTimeMillis();
						Log.e("here","1: "+(System.currentTimeMillis()-l));
						startActivity(callIntent);
						Log.e("here","2: "+(System.currentTimeMillis()-l));
						/*Runnable showDialogRun2 = new Runnable() {
				            public void run(){
				                Intent showDialogIntent2 = new Intent(getBaseContext(), temp.class);
				                startActivity(showDialogIntent2);
				            }
				        };
				        Handler h2 = new Handler(Looper.getMainLooper());
				        h2.postDelayed(showDialogRun2, 1000);*/
						Log.e("here","3: "+(System.currentTimeMillis()-l));
				        waitForCall();
				        String str0=readFromFile("connect");
						writeToFile("connect", str0+System.currentTimeMillis()+"\n");
				        Log.e("here","4: "+(System.currentTimeMillis()-l));
				        
						try {
							//Thread.sleep(data_time[count]);
							Thread.sleep(stateTime);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						Log.e("here","5: "+(System.currentTimeMillis()-l));
						
						String str=readFromFile("disconnect");
						writeToFile("disconnect", str+System.currentTimeMillis()+"\n");
						Log.e("here","6: "+(System.currentTimeMillis()-l));
						disconnectCall();
						Log.e("here","7: "+(System.currentTimeMillis()-l));
						progress.setProgress(progress.getProgress()+1);
						callsLeft--;
						Log.e("here","8: "+(System.currentTimeMillis()-l));
						try {
							Thread.sleep(callDisconnectTime);//+data_time[count]
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						//count=count+4;
						Log.e("here","9: "+(System.currentTimeMillis()-l));
						
					}
					
				}
				status="done";
				call_locked=0;
			}
		});
		t.start();
	};
	public void call2(View v) {
		writeToFile("lastState", "r");
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.setData(Uri.parse("tel:"+phoneNo));
				status="not done";
				
				//UI
				progress.setProgress(0);
				//int last_export=0;
				for (int test = 0; test < noOfBytes; test++) {
					for (int count = 0; count < byteSize; count++) {
						if (closed == 1) {
							break;
						}
						long l=System.currentTimeMillis();
						Log.e("here","1: "+(System.currentTimeMillis()-l));
						startActivity(callIntent);
						Log.e("here","2: "+(System.currentTimeMillis()-l));
						/*Runnable showDialogRun2 = new Runnable() {
				            public void run(){
				                Intent showDialogIntent2 = new Intent(getBaseContext(), temp.class);
				                startActivity(showDialogIntent2);
				            }
				        };
				        Handler h2 = new Handler(Looper.getMainLooper());
				        h2.postDelayed(showDialogRun2, 1000);*/
						
						try {
							Thread.sleep(100);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						Log.e("here","3: "+(System.currentTimeMillis()-l));
				        waitForCall();
				        Log.e("here","4: "+(System.currentTimeMillis()-l));
				        String str0=readFromFile("connect");
						writeToFile("connect", str0+System.currentTimeMillis()+"\n");
				        
						try {
							//Thread.sleep(data_time[count]);
							Thread.sleep(data_time[count]);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						Log.e("here","5: "+(System.currentTimeMillis()-l));
						
						String str=readFromFile("disconnect");
						writeToFile("disconnect", str+System.currentTimeMillis()+"\n");
						Log.e("here","6: "+(System.currentTimeMillis()-l));
						disconnectCall();
						Log.e("here","7: "+(System.currentTimeMillis()-l));
						progress.setProgress(progress.getProgress()+1);
						callsLeft--;
						Log.e("here","8: "+(System.currentTimeMillis()-l));
						try {
							Thread.sleep(callDisconnectTime);//+data_time[count]
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						//count=count+4;
						Log.e("here","9: "+(System.currentTimeMillis()-l));
						
					}
					//if(last_export==9){export();last_export=0;}else{last_export++;}
					/*if(last_export==9){
						stateTime=stateTime-100;
						for(int i=0; i<byteSize; i=i+2){
							data_time[i]=encodeTime0;
							data_time[i+1]=stateTime;
						}
						last_export=0;
					}else{last_export++;}*/
					
				}
				status="done";
				call_locked=0;
			}
		});
		t.start();
	};
	public void writeToFile(String fileName,String str){
		FileOutputStream fos;
		try {
			fos=openFileOutput(fileName, Context.MODE_PRIVATE);
			byte[] b=str.getBytes();
			fos.write(b);
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	};
	public String readFromFile(String fileName){
		String temp=null;
		FileInputStream fin;
		try {
			fin = openFileInput(fileName);
			byte[] b = new byte[fin.available()];
			fin.read(b);
			String s = new String(b);
			temp = s;
			fin.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return temp;
	}
	public void saveParameters(){
		writeToFile("parameters", "\nnoOfBytes= "+noOfBytes+"\nbyteSize= "+byteSize+
				"\ncallSetupTime= "+callSetupTime+ "\ncallDisconnectTime= " +
				callDisconnectTime+"\nstateTime= "+stateTime);
		
	}
	public void writeParametersInSeparateFiles(){
		writeToFile("noOfBytes", ""+noOfBytes);
		writeToFile("byteSize", ""+byteSize);
		writeToFile("callSetupTime", ""+callSetupTime);
		writeToFile("callDisconnectTime", ""+callDisconnectTime);
		writeToFile("stateTime", ""+stateTime);
		writeToFile("phoneNo", ""+phoneNo);
	}
	public void myToast(String str){
		Toast.makeText(getApplicationContext(), str,Toast.LENGTH_LONG).show();
	}
	public void export(){
		File myDir=makeTimeDir();
		makeFile("lastOffHook",myDir);
		makeFile("lastIdeal",myDir);
		makeFile("lastRinging",myDir);
		makeFile("parameters",myDir);
		writeToFile(status, "");
		makeFile(status, myDir);
		makeFile("disconnect", myDir);
		makeFile("connect", myDir);
		makeFile("output", myDir);
		
		try {
		    File file = new File(myDir, "_log.txt");
		    Runtime.getRuntime().exec("logcat -d -v time -f " + file.getAbsolutePath());}catch (IOException e){}
		
	}
	public void send_func(View v, final int[] symbols){
		Log.e("here","s");
		writeToFile("lastState", "r");
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.setData(Uri.parse("tel:"+phoneNo));
				status="not done";
				
				//UI
				progress.setProgress(0);
				progress.setMax(symbols.length);
				//int last_export=0;
				for (int test = 0; test < 1; test++) {
					for (int count = 0; count < symbols.length; count++) {
						if (closed == 1) {
							break;
						}
						long l=System.currentTimeMillis();
						//Log.e("here","1: "+(System.currentTimeMillis()-l));
						startActivity(callIntent);
						//Log.e("here","2: "+(System.currentTimeMillis()-l));
						
						try {
							Thread.sleep(100);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						//Log.e("here","3: "+(System.currentTimeMillis()-l));
				        waitForCall();
				        //Log.e("here","4: "+(System.currentTimeMillis()-l));
				        String str0=readFromFile("connect");
						writeToFile("connect", str0+System.currentTimeMillis()+"\n");
				        Log.e("******",""+symbols[count]);
				       
						try {
							//Thread.sleep(data_time[count]);
							Thread.sleep(data_time[symbols[count]]);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						//Log.e("here","5: "+(System.currentTimeMillis()-l));
						
						String str=readFromFile("disconnect");
						writeToFile("disconnect", str+System.currentTimeMillis()+"\n");
						//Log.e("here","6: "+(System.currentTimeMillis()-l));
						disconnectCall();
						//Log.e("here","7: "+(System.currentTimeMillis()-l));
						progress.setProgress(progress.getProgress()+1);
						callsLeft--;
						//Log.e("here","8: "+(System.currentTimeMillis()-l));
						try {
							Thread.sleep(callDisconnectTime);//+data_time[count]
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						//count=count+4;
						//Log.e("here","9: "+(System.currentTimeMillis()-l));
						
					}
					
				}
				status="done";
				call_locked=0;
			}
		});
		t.start();
	}
	public int decode(long num){
		int num2=(int) (num/stateTime);
		num2=(num2-2);
		if(num2<0){num2=0;}
		if(num2>28){num2=28;}
		return num2;	
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		writeParametersInSeparateFiles();
		disconnectInitializer();
		
		call = (Button) findViewById(R.id.bCall);
		reset = (Button) findViewById(R.id.bReset);
		ensureReset = (Button) findViewById(R.id.bResetEnsure);
		export = (Button) findViewById(R.id.bExport);
		userNotification = (TextView) findViewById(R.id.tvUserNotification);
		progress = (ProgressBar) findViewById(R.id.pbCallsleft);
		refresh = (Button) findViewById(R.id.bRefresh);
		settings = (Button) findViewById(R.id.bSettings);
		receiverMode = (Button) findViewById(R.id.bReceiverMode);
		updateParameters =(Button) findViewById(R.id.bUpdateSettings); 
		text = (EditText) findViewById(R.id.et_text1);
		send = (Button) findViewById(R.id.b_send);
		
		call.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(call_locked==0){
					call_locked=1;
					saveParameters();
					progress.setMax(noOfBytes*byteSize);
					call2(v);
				}else{
					myToast("intentionally call blocked");
				}	
			}
		});	

		reset.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (ensure == 0) {
					return;
				}
				clearFile("lastOffHook");
				clearFile("lastIdeal");
				clearFile("lastRinging");
				clearFile("parameters");
				clearFile("connect");
				clearFile("disconnect");
				clearFile("notifications");
				clearFile("output");
				
			}
		});

		ensureReset.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (ensure == 0) {
					ensure = 1;
					ensureReset.setText("yes");
				} else {
					ensure = 0;
					ensureReset.setText("no");
				}

			}
		});

		export.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				export();
				
			}
		});
		
		refresh.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String notifications=readFromFile("notifications");
				userNotification.setText(notifications);
				
			}
		});
		
		receiverMode.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(receiverState==0){receiverState=1;}else{receiverState=0;}
				if(receiverState==0){receiverMode.setText("receiver Mode off");}else{receiverMode.setText("receiver Mode on");}
				if(receiverState==1){
					String output="";
					String str1=readFromFile("lastRinging");
					String[] lines1 = str1.split(System.getProperty("line.separator"));
					long ring[]=new long[lines1.length];
					for(int i=1; i<lines1.length; i++){
						ring[i]=Long.valueOf(lines1[i]).longValue();
						Log.e("rin", "~~"+lines1[i]+"~~"+ring[i]+"~~" );
					}
					String str2=readFromFile("lastIdeal");
					String[] lines2 = str2.split(System.getProperty("line.separator"));
					long ideal[]=new long[lines2.length];
					for(int i=1; i<lines2.length; i++){
						ideal[i]=Long.valueOf(lines2[i]).longValue();
						//Log.e("ide", "~~"+lines2[i]+"~~"+ideal[i]+"~~" );
					}
					if(ideal.length==ring.length){
						long ringtime[]=new long[ideal.length];
						int symbols[]=new int[ideal.length];
						for(int i=1; i<ideal.length; i++){
							ringtime[i]=ideal[i]-ring[i];
							symbols[i]=decode(ringtime[i]);
							//decoding starts here.
							Log.e("String$$",ringtime[i]+"~"+decode(ringtime[i]));
							
						}
						String rec="";
						for(int i=1; i<symbols.length; i++){
							String temp=Integer.toString(symbols[i], 2);
							if(temp.length()==3){temp="0"+temp;}
							if(temp.length()==2){temp="00"+temp;}
							if(temp.length()==1){temp="000"+temp;}
							Log.e("String",temp);
							rec=rec+temp;
							
						}
						Log.e("->->$$",""+rec);
						
						for(int i=0; i<(rec.length()-6); i=i+5){
							String t1=""+rec.charAt(i);
							String t2=""+rec.charAt(i+1);
							String t3=""+rec.charAt(i+2);
							String t4=""+rec.charAt(i+3);
							String t5=""+rec.charAt(i+4);
							String t6=t1+t2+t3+t4+t5;
							int characters=16*Integer.valueOf(""+rec.charAt(i))+8*Integer.valueOf(""+rec.charAt(i+1))+4*Integer.valueOf(""+rec.charAt(i+2))+2*Integer.valueOf(""+rec.charAt(i+3))+Integer.valueOf(""+rec.charAt(i+4));
							Log.e("KKKK",""+t6+"~~"+characters);
							output=output+freq_char_set.charAt(characters);
						}
						//output=output.replaceFirst("1", "");
						userNotification.setText(""+output);
						writeToFile("output", output);
						
					}else{
						output="err no size match";
					}
	
				}
				
			}
		});

		settings.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(call_locked==0){
					Runnable showDialogRun = new Runnable() {
		            	public void run(){
		                	Intent showDialogIntent = new Intent(getBaseContext(), settings.class);
		                	startActivity(showDialogIntent);
		            	}
		        	};
		        	Handler h = new Handler(Looper.getMainLooper());
		        	h.postDelayed(showDialogRun, 100);
		        }else{
		        	myToast("call in progress");
		        	
		        }
		        	
			}
		});
		
		updateParameters.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				noOfBytes=Integer.parseInt(readFromFile("noOfBytes"));
				byteSize=Integer.parseInt(readFromFile("byteSize"));
				callSetupTime=Integer.parseInt(readFromFile("callSetupTime"));
				callDisconnectTime=Integer.parseInt(readFromFile("callDisconnectTime"));
				stateTime=Integer.parseInt(readFromFile("stateTime"));
				phoneNo=readFromFile("phoneNo");
				callsLeft=noOfBytes*byteSize+1;
				for(int i=0; i<byteSize; i=i+2){
					data_time[i]=encodeTime0;
					data_time[i+1]=stateTime;
				};
				
				saveParameters();
				progress.setMax(noOfBytes*byteSize+1);
				
			}
		});
		
		send.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String str=text.getText().toString();
				//str="1"+str+"1";
				/*str="";
				for(int i=0; i<10; i++){
					str=str+"abcdefghijklmnopqrstuvwxyz";
				}*/
				String rec="";
				str="1"+str+"1";
				int index=0;
				for(int i=0; i<str.length(); i++){
					index=0;
					for(int j=0; j<freq_char_set.length(); j++){
						if(str.charAt(i)==freq_char_set.charAt(j)){
							index=j;
							break;
						}
					}
					//Log.e("->->->","~"+freq_char_set.charAt(index)+"~");
					String temp=Integer.toString(index, 2);
					if(temp.length()==4){temp="0"+temp;}
					if(temp.length()==3){temp="00"+temp;}
					if(temp.length()==2){temp="000"+temp;}
					if(temp.length()==1){temp="0000"+temp;}
					Log.e("->->->","~"+temp+"~");
					rec=rec+temp;
					
				}
				
				int symbols[]=new int[rec.length()/4];
				int jk=0;
				String writer="";
				for(int k=0; k<symbols.length; k++){
					int characters=8*Integer.valueOf(""+rec.charAt(jk))+4*Integer.valueOf(""+rec.charAt(jk+1))+2*Integer.valueOf(""+rec.charAt(jk+2))+Integer.valueOf(""+rec.charAt(jk+3));
					symbols[k]=characters;
					jk=jk+4;
					Log.e("String$$",""+symbols[k]);	
					writer=writer+"~"+symbols[k]+"~";
				}
				writeToFile("notifications", writer);
							
				send_func(v,symbols);
			}
		});
		
	
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		closed = 1;
	}
	
	
}
