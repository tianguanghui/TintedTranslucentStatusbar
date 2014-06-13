package com.woalk.apps.xposed.ttsb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private PackageManager pkgMan;
	public List<PackageInfo> installedPkg;
	protected SharedPreferences sPref;
	protected List<ActivityInfo> loadedActivities = new ArrayList<ActivityInfo>();
	
	String[] pkgNames;
	String[] appNames;
	
	private ListView listView1;
	private ArrayAdapter<String> arrAdapter;
	private ArrayList<String> arr_activities = new ArrayList<String>();

	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		pkgMan = getPackageManager();
		sPref = getApplicationContext().getSharedPreferences(Helpers.TTSB_PREFERENCES, Context.MODE_WORLD_READABLE);
		
		listView1 = (ListView) findViewById(R.id.listView1);
		
		listView1.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				launchDetails(loadedActivities.get(position));
			}
		});
		
		arrAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arr_activities);
		listView1.setAdapter(arrAdapter);
		
		// Load list of names and sort
		// TODO: Inefficient! Use Asynchronous Task!
		installedPkg = pkgMan.getInstalledPackages(PackageManager.GET_ACTIVITIES);
		pkgNames = new String[installedPkg.size()];
		ArrayList<String> appNamesList = new ArrayList<String>();
		for (int i = 0; i < installedPkg.size(); i++) {
			appNamesList.add(pkgMan.getApplicationLabel(installedPkg.get(i).applicationInfo).toString() + "::" + String.valueOf(i));
			pkgNames[i] = installedPkg.get(i).packageName;
			List<ActivityInfo> listActvt;
			if (installedPkg.get(i).activities != null) { 
				listActvt = new ArrayList<ActivityInfo>(Arrays.asList(installedPkg.get(i).activities));
			}
			else {
				listActvt = new ArrayList<ActivityInfo>();
			}
			ActivityInfo inf = new ActivityInfo();
			inf.name = pkgNames[i] + ".[ALL]";
			inf.packageName = pkgNames[i];
			listActvt.add(0, inf);
			installedPkg.get(i).activities = listActvt.toArray(new ActivityInfo[listActvt.size()]);
		}
		
		Collections.sort(appNamesList);
		
		appNames = appNamesList.toArray(new String[appNamesList.size()]);
	}
	
	@Override
	protected void onResume() {
		reload();
		super.onResume();
	}
	
	protected void reload() {
		loadedActivities.clear();
		arr_activities.clear();
		Map<String, ?> sPrefAll = sPref.getAll();
		if (sPrefAll != null) {
			for (Map.Entry<String, ?> entry : sPrefAll.entrySet()) {
				if (!entry.getKey().endsWith("+s")) {
					String pkgName = sPref.getString(entry.getKey() + "+p", null);
					if (pkgName != null) {
						String activityClass = entry.getKey();
						if (!activityClass.endsWith(".[ALL]")) {
							ComponentName cName = new ComponentName(pkgName, activityClass);
							ActivityInfo entryInfo;
							try {
								entryInfo = pkgMan.getActivityInfo(cName, 0);
							} catch (NameNotFoundException e) {
								e.printStackTrace();
								return;
							}
							loadedActivities.add(entryInfo);
							arr_activities.add(activityClass);
						} else {
							ActivityInfo inf = new ActivityInfo();
							inf.name = activityClass;
							inf.packageName = pkgName;
							loadedActivities.add(inf);
							arr_activities.add(activityClass);
						}
					}
				}
			}
			arrAdapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_add:
	            addItem();
	            return true;
	        case R.id.action_settings:
	        	launchSettings();
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	protected void addItem() {
		
		Dialogs.SelectPkgDialog dialog1 = new Dialogs.SelectPkgDialog();
		dialog1.setPkgNames(pkgNames, appNames);
		dialog1.setOnClick(ocl1);
		dialog1.show(getFragmentManager(), "SelectPkg");
	}
	
	private DialogInterface.OnClickListener ocl1 = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			addItem_part2(which);
		}
	};
	
	ActivityInfo[] activities = null;
	String[] activityAlphabeticalNames = null;
	
	private void addItem_part2(int result1) {
		int index = Integer.valueOf(appNames[result1].substring(appNames[result1].lastIndexOf("::") + 2));
		activities = installedPkg.get(index).activities;
		if (activities.length > 1) {
			List<String> activityNames = new ArrayList<String>();
			for (int i = 0; i < activities.length; i++) {
				activityNames.add(activities[i].name + "::" + String.valueOf(i));
			}
			
			Collections.sort(activityNames, String.CASE_INSENSITIVE_ORDER);
			activityAlphabeticalNames = activityNames.toArray(new String[activityNames.size()]);
		
			Dialogs.SelectActivityDialog dialog2 = new Dialogs.SelectActivityDialog();
			dialog2.setPkgName(pkgMan.getApplicationLabel(installedPkg.get(index).applicationInfo).toString());
			dialog2.setActivityNames(activityAlphabeticalNames);
			dialog2.setOnClick(ocl2);
			dialog2.show(getFragmentManager(), "SelectActivity");
		} else {
			Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_activities_info), Toast.LENGTH_LONG);
			toast.show();
		}
	}
	
	private DialogInterface.OnClickListener ocl2 = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			addItem_part3(which);
		}
	};
	
	private void addItem_part3(int result2) {
		int index = Integer.valueOf(activityAlphabeticalNames[result2].substring(activityAlphabeticalNames[result2].lastIndexOf("::") + 2));
		launchDetails(activities[index]);
		
		activities = null;
		activityAlphabeticalNames = null;
	}
	
	private void launchDetails(ActivityInfo activity) {
		Intent add_Intent = new Intent(this, DetailsActivity.class);
		if (activity.name.endsWith(".[ALL]")) {
			add_Intent.putExtra(DetailsActivity.SEL_PACKAGE_ALL_PKGNAME, activity.name);
		}
		else {
			add_Intent.putExtra(DetailsActivity.SEL_PACKAGE_ACTIVITY_INFO, activity);
		}
		startActivity(add_Intent);
	}
	
	private void launchSettings() {
		Intent sett_Intent = new Intent(this, SettingsActivity.class);
		startActivity(sett_Intent);
	}
}