package com.woalk.apps.xposed.ttsb.legacy;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.woalk.apps.xposed.ttsb.Helpers;
import com.woalk.apps.xposed.ttsb.R;
import com.woalk.apps.xposed.ttsb.Settings;

public class SyncActivity extends Activity {
	protected AppSyncListAdapter lA;
	protected TreeMap<String, String> database;
	protected Activity context = this;
	protected ListView lv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sync);

		lA = new AppSyncListAdapter(context, new ArrayList<ApplicationInfo>(),
				new ArrayList<Boolean>(), new ArrayList<Boolean>(),
				new ArrayList<Boolean>(), new ArrayList<String>(),
				new ArrayList<Boolean>());
		lv = (ListView) findViewById(R.id.listView1);
		lv.setAdapter(lA);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				CheckBox chkSync = (CheckBox) view.findViewById(R.id.checkSync);
				chkSync.setChecked(!chkSync.isChecked());
			}
		});

		final Spinner spinner_opt = (Spinner) findViewById(R.id.spinner1);

		findViewById(R.id.button_addsync).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (spinner_opt.getSelectedItemPosition() == 0)
							saveSelected();
						else
							saveSelectedLayout();
						context.finish();
					}
				});

		getSyncables();
	}

	protected void getSyncables() {
		new readDatabaseTask()
				.execute("http://ext.woalk.de/ttsb_database/getdbjson.php");
	}

	private class readDatabaseTask extends
			AsyncTask<String, Integer, AppSyncListAdapter> {
		private AlertDialog progress;

		protected void onPreExecute() {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setMessage(R.string.loadingsync_msg);
			builder.setView(new ProgressBar(context));
			progress = builder.create();
			progress.show();
		}

		@SuppressLint({ "WorldReadableFiles", "SimpleDateFormat" })
		@SuppressWarnings("deprecation")
		protected AppSyncListAdapter doInBackground(String... params) {
			AppSyncListAdapter lA1 = new AppSyncListAdapter(context,
					new ArrayList<ApplicationInfo>(), null, null, null, null,
					null);
			PackageManager pkgMan = context.getPackageManager();
			List<PackageInfo> pkgs = pkgMan.getInstalledPackages(0);
			List<ApplicationInfo> apps = new ArrayList<ApplicationInfo>();
			List<Boolean> is_set = new ArrayList<Boolean>();
			List<Boolean> checked = new ArrayList<Boolean>();
			List<String> timestamps = new ArrayList<String>();
			List<Boolean> timestamps_isnewer = new ArrayList<Boolean>();
			SortedMap<String, Boolean> db_edited = new TreeMap<String, Boolean>();
			SortedMap<String, String> db_timestamps = new TreeMap<String, String>();
			List<Boolean> edited = new ArrayList<Boolean>();
			for (int i = 0; i < pkgs.size(); i++) {
				apps.add(pkgs.get(i).applicationInfo);
			}
			Collections.sort(apps, new ApplicationInfo.DisplayNameComparator(
					pkgMan));
			SharedPreferences sPref = context.getSharedPreferences(
					Helpers.TTSB_PREFERENCES, Context.MODE_WORLD_READABLE);
			TreeMap<String, ?> tree_sPref = new TreeMap<String, Object>(
					sPref.getAll());

			database = new TreeMap<String, String>();
			InputStream is;
			String result = "";
			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost(params[0]);
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
				is = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is, "utf-8"), 8);
				StringBuilder sb = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
				is.close();
				result = sb.toString();
				JSONArray jArray = new JSONArray(result);
				for (int i = 0; i < jArray.length(); i++) {
					JSONObject json_data = jArray.getJSONObject(i);
					String packageName = json_data.getString("package");
					String key = packageName + "/"
							+ json_data.getString("activity");
					database.put(key, json_data.getString("setting"));
					String setting = sPref.getString(key, null);
					String timeStamp = json_data.getString("timestamp");
					db_timestamps.put(packageName, timeStamp);
					if (setting != null) {
						if (setting.equals(database.get(key)))
							continue;
					}
					db_edited.put(packageName, true);
				}
			} catch (UnknownHostException e) {
				publishProgress(404);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

			for (int i = 0; i < apps.size(); i++) {
				if (!Settings.Loader.containsPackage(database,
						apps.get(i).packageName)) {
					apps.remove(i);
					i--;
					continue;
				}
				String packageName = apps.get(i).packageName;
				boolean is_current_set = Settings.Loader.containsPackage(
						tree_sPref, packageName);
				is_set.add(is_current_set);
				boolean is_edited = false;
				if (db_edited.containsKey(packageName))
					is_edited = db_edited.get(packageName);
				boolean is_newer = false;
				/*
				 * try { String timestamp = db_timestamps.get(packageName);
				 * SimpleDateFormat sdf = new SimpleDateFormat(
				 * "yyyy-MM-dd-HH-mm"); Date date; date = sdf.parse(timestamp);
				 * timestamps.add(DateFormat.getInstance().format(date)); String
				 * lastupdate = sPref.getString( Helpers.TTSB_PREF_LASTUPDATE,
				 * getString(R.string.never)); Date datelast; try { datelast =
				 * sdf.parse(lastupdate); } catch (ParseException e) { datelast
				 * = new Date(); } is_newer = date.compareTo(datelast) > 0;
				 * timestamps_isnewer.add(is_newer); } catch (ParseException e)
				 * { timestamps.add("‹?›"); timestamps_isnewer.add(false); }
				 */
				edited.add(is_edited);
				checked.add(!is_current_set || is_newer);
			}
			lA1.apps = apps;
			lA1.is_set = is_set;
			lA1.checked = checked;
			lA1.edited = edited;
			lA1.timestamps = timestamps;
			lA1.timestamps_isnewer = timestamps_isnewer;
			return lA1;
		}

		protected void onProgressUpdate(Integer... progress) {
			if (progress[0] == 404)
				Toast.makeText(SyncActivity.this, R.string.no_connection_e,
						Toast.LENGTH_LONG).show();
		}

		protected void onPostExecute(AppSyncListAdapter result) {
			lA.apps.clear();
			lA.is_set.clear();
			lA.checked.clear();
			lA.edited.clear();
			lA.timestamps.clear();
			lA.timestamps_isnewer.clear();
			if (result != null) {
				lA.apps.addAll(result.apps);
				lA.is_set.addAll(result.is_set);
				lA.checked.addAll(result.checked);
				lA.edited.addAll(result.edited);
				lA.timestamps.addAll(result.timestamps);
				lA.timestamps_isnewer.addAll(result.timestamps_isnewer);
			}
			lA.notifyDataSetChanged();
			progress.dismiss();
		}
	}

	@SuppressLint({ "WorldReadableFiles", "SimpleDateFormat" })
	@SuppressWarnings("deprecation")
	protected void saveSelected() {
		SharedPreferences sPref = context.getSharedPreferences(
				Helpers.TTSB_PREFERENCES, Context.MODE_WORLD_READABLE);
		SharedPreferences.Editor edit = sPref.edit();
		DateFormat f = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		String formattedDate = f.format(new Date());
		// edit.putString(Helpers.TTSB_PREF_LASTUPDATE, formattedDate);
		edit.apply();
		List<String> packageNames = new ArrayList<String>();
		for (int i = 0; i < lA.apps.size(); i++) {
			if (lA.checked.get(i))
				packageNames.add(lA.apps.get(i).packageName);
		}
		if (packageNames.size() == 0)
			return;
		Settings.Saver.deleteEverythingFromPackages(sPref, packageNames);
		for (int i = 0; i < lA.apps.size(); i++) {
			if (lA.checked.get(i)) {
				SortedMap<String, String> appdb = database.subMap(
						lA.apps.get(i).packageName, lA.apps.get(i).packageName
								+ Character.MAX_VALUE);
				for (Entry<String, String> entry : appdb.entrySet()) {
					Settings.Parser parser = new Settings.Parser(
							entry.getValue());
					parser.parseToSettings();
					Settings.Saver.save(sPref, entry.getKey(), parser);
				}
			}
		}
		Toast.makeText(this, R.string.settings_synced_success,
				Toast.LENGTH_SHORT).show();
	}

	@SuppressLint({ "WorldReadableFiles", "SimpleDateFormat" })
	@SuppressWarnings("deprecation")
	protected void saveSelectedLayout() {
		SharedPreferences sPref = context.getSharedPreferences(
				Helpers.TTSB_PREFERENCES, Context.MODE_WORLD_READABLE);
		SharedPreferences.Editor edit = sPref.edit();
		DateFormat f = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		String formattedDate = f.format(new Date());
		// edit.putString(Helpers.TTSB_PREF_LASTUPDATE, formattedDate);
		edit.apply();
		boolean isSyncable = false;
		for (int i = 0; i < lA.apps.size(); i++) {
			if (lA.checked.get(i))
				isSyncable = true;
		}
		if (!isSyncable)
			return;
		for (int i = 0; i < lA.apps.size(); i++) {
			if (lA.checked.get(i)) {
				SortedMap<String, String> appdb = database.subMap(
						lA.apps.get(i).packageName, lA.apps.get(i).packageName
								+ Character.MAX_VALUE);
				for (Entry<String, String> entry : appdb.entrySet()) {
					if (!Settings.Loader.contains(sPref, entry.getKey()))
						continue;
					Settings.Parser parser = Settings.Loader.load(sPref,
							entry.getKey());
					Settings.Parser parser_new = new Settings.Parser(
							entry.getValue());
					parser_new.parseToSettings();
					Settings.Setting setting = parser.getSetting();
					int s_plus_sav = setting.rules.s_plus;
					int n_plus_sav = setting.rules.n_plus;
					setting.rules = parser_new.getSetting().rules;
					setting.rules.s_plus = s_plus_sav;
					setting.rules.n_plus = n_plus_sav;
					parser.setSetting(setting);
					Settings.Saver.save(sPref, entry.getKey(), parser);
				}
			}
		}
		Toast.makeText(this, R.string.settings_synced_success,
				Toast.LENGTH_SHORT).show();
	}
}
