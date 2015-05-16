package com.woalk.apps.xposed.ttsb.community;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.woalk.apps.xposed.ttsb.Helpers;
import com.woalk.apps.xposed.ttsb.R;
import com.woalk.apps.xposed.ttsb.community.SQL_Operations.CustomQ;
import com.woalk.apps.xposed.ttsb.community.SQL_Operations.Q;

public class OneUserActivity extends Activity {
	public static final String PASS_USERNAME = Helpers.TTSB_PACKAGE_NAME
			+ ".PASS_USERNAME";

	private String username;

	private ListView lv;
	private UserSubmitsAdapter lA;

	private MenuItem prog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_one_user);

		username = getIntent().getStringExtra(PASS_USERNAME);

		lv = (ListView) findViewById(R.id.listView1);
		lA = new UserSubmitsAdapter(this, new ArrayList<String>());
		lA.username = username;
		lv.setAdapter(lA);

		lv.setOnItemClickListener(new ListView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(OneUserActivity.this,
						OneSubmitActivity.class);
				intent.putExtra(OneSubmitActivity.PASS_APP,
						(Parcelable) lA.apps.get(position));
				intent.putExtra(OneSubmitActivity.PASS_DESCR,
						lA.descriptions.get(position));
				Date timestamp;
				try {
					DateFormat d_f = DateFormat.getInstance();
					timestamp = d_f.parse(lA.timestamps.get(position));
				} catch (ParseException e) {
					timestamp = new Date();
					e.printStackTrace();
				}
				intent.putExtra(OneSubmitActivity.PASS_TIMESTAMP, timestamp);
				intent.putExtra(OneSubmitActivity.PASS_ID, lA.ids.get(position));
				intent.putExtra(OneSubmitActivity.PASS_IS_TOPVOTE,
						position == 1);
				intent.putExtra(OneSubmitActivity.PASS_INSTALLED, false);
				intent.putExtra(OneSubmitActivity.PASS_SETTINGS,
						lA.settings.get(position));
				intent.putExtra(OneSubmitActivity.PASS_USER, lA.username);
				intent.putExtra(OneSubmitActivity.PASS_USER_TRUST,
						lA.user_trust);
				intent.putExtra(OneSubmitActivity.PASS_VERSION,
						lA.versions.get(position));
				intent.putExtra(OneSubmitActivity.PASS_VOTES,
						lA.votes.get(position));
				startActivity(intent);
			}
		});

		Q q = new Q(Database.DATABASE_URL);
		q.setPreExecuteListener(new Q.PreExecuteListener() {

			@Override
			public void onPreExecute() {
				if (prog != null)
					prog.setVisible(true);
			}
		});
		final String key_ids = "ids";
		final String key_votes = "votes";
		final String key_apps = "apps";
		final String key_pkgs = "pkgs";
		final String key_descriptions = "descr";
		final String key_versions = "ver";
		final String key_user_trust = "trust";
		final String key_user_votes = "uvotes";
		final String key_timestamps = "time";
		final String key_settings = "set";
		q.setDataLoadedListener(new Q.DataLoadedListener() {

			@SuppressLint("SimpleDateFormat")
			@Override
			public Bundle onDataLoaded(JSONArray data) throws JSONException {
				ArrayList<Integer> ids = new ArrayList<Integer>();
				ArrayList<Integer> votes = new ArrayList<Integer>();
				ArrayList<ApplicationInfo> apps = new ArrayList<ApplicationInfo>();
				ArrayList<String> pkgs = new ArrayList<String>();
				ArrayList<String> descriptions = new ArrayList<String>();
				ArrayList<Integer> versions = new ArrayList<Integer>();
				ArrayList<String> timestamps = new ArrayList<String>();
				ArrayList<String> settings = new ArrayList<String>();
				boolean user_trust;
				int user_votes;

				user_trust = data.getJSONObject(0).getString("user_trust")
						.equals("1");
				user_votes = Integer.valueOf(data.getJSONObject(0).getString(
						"user_votes"));
				if (data.getJSONObject(0).isNull("id")) {
					Bundle bundle = new Bundle();
					bundle.putBoolean(key_user_trust, user_trust);
					bundle.putInt(key_user_votes, user_votes);
					return bundle;
				}
				for (int i = 0; i < data.length(); i++) {
					JSONObject json_data = data.getJSONObject(i);
					Integer id = Integer.valueOf(json_data.getString("id"));
					ids.add(id);
					votes.add(Integer.valueOf(json_data.getString("votes")));
					descriptions.add(json_data.getString("description"));
					versions.add(Integer.valueOf(json_data.getString("version")));
					DateFormat d_f = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
					d_f.setTimeZone(TimeZone.getTimeZone("GMT"));
					try {
						Date timestamp = d_f.parse(json_data
								.getString("timestamp"));
						DateFormat d_f_new = DateFormat.getInstance();
						String date = d_f_new.format(timestamp);
						timestamps.add(date);
					} catch (ParseException e) {
						timestamps.add("‹?›");
						e.printStackTrace();
					}
					settings.add(json_data.getString("settings"));
					String pkg = json_data.getString("package");
					try {
						apps.add(OneUserActivity.this.getPackageManager()
								.getApplicationInfo(pkg, 0));
					} catch (NameNotFoundException e) {
						apps.add(null);
					}
					pkgs.add(pkg);
				}

				Bundle bundle = new Bundle();
				bundle.putIntegerArrayList(key_ids, ids);
				bundle.putIntegerArrayList(key_votes, votes);
				bundle.putParcelableArrayList(key_apps, apps);
				bundle.putStringArrayList(key_pkgs, pkgs);
				bundle.putStringArrayList(key_descriptions, descriptions);
				bundle.putIntegerArrayList(key_versions, versions);
				bundle.putStringArrayList(key_timestamps, timestamps);
				bundle.putStringArrayList(key_settings, settings);
				bundle.putBoolean(key_user_trust, user_trust);
				bundle.putInt(key_user_votes, user_votes);
				return bundle;
			}
		});
		q.setPostExecuteListener(new Q.PostExecuteListener() {

			@Override
			public void onPostExecute(Bundle processed) {
				if (processed == null) {
					if (prog != null)
						prog.setVisible(false);
					Toast.makeText(OneUserActivity.this,
							R.string.user_not_found, Toast.LENGTH_SHORT).show();
					finish();
					return;
				}
				lA.apps.clear();
				lA.descriptions.clear();
				lA.ids.clear();
				lA.pkgs.clear();
				lA.settings.clear();
				lA.timestamps.clear();
				lA.versions.clear();
				lA.votes.clear();

				lA.addBegin();

				if (!processed.containsKey(key_ids)) {
					lA.notifyDataSetChanged();
					if (prog != null)
						prog.setVisible(false);
					return;
				}

				ArrayList<ApplicationInfo> apps = processed
						.getParcelableArrayList(key_apps);
				lA.apps.addAll(apps);
				lA.descriptions.addAll(processed
						.getStringArrayList(key_descriptions));
				lA.ids.addAll(processed.getIntegerArrayList(key_ids));
				lA.pkgs.addAll(processed.getStringArrayList(key_pkgs));
				lA.settings.addAll(processed.getStringArrayList(key_settings));
				lA.timestamps.addAll(processed
						.getStringArrayList(key_timestamps));
				lA.versions.addAll(processed.getIntegerArrayList(key_versions));
				lA.votes.addAll(processed.getIntegerArrayList(key_votes));

				lA.user_trust = processed.getBoolean(key_user_trust);
				lA.user_votes = processed.getInt(key_user_votes);

				lA.notifyDataSetChanged();

				if (prog != null)
					prog.setVisible(false);
			}
		});
		q.addNameValuePair(Database.POST_PIN, Database.COMMUNITY_PIN);
		q.addNameValuePair(Database.POST_FUNCTION,
				Database.FUNCTION_GET_SUBMITS_BY_USER);
		q.addNameValuePair(Database.POST_USERNAME, username);
		q.exec();

		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Submitter.Account acc = Submitter.getSavedAccount(this);
		if (acc != null && !acc.isEmpty() && acc.getUsername().equals(username)) {
			getMenuInflater().inflate(R.menu.one_user, menu);
		} else
			getMenuInflater().inflate(R.menu.onlywaiting, menu);
		prog = menu.findItem(R.id.waiting_apps);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.action_edit_name:
			new Submitter.ChangeNamePwDialog(this).show();
			return true;
		case R.id.action_new_pw:
			new AlertDialog.Builder(this)
					.setTitle(R.string.str_action_new_pw)
					.setMessage(R.string.msg_generate_pw)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									Submitter
											.generateNewPassword(OneUserActivity.this);
								}
							}).setNegativeButton(android.R.string.no, null)
					.show();
			return true;
		case R.id.action_logout:
			Submitter.deleteSavedAccount(this);
			finish();
			return true;
		case R.id.action_delete_profile:
			new AlertDialog.Builder(this)
					.setTitle(R.string.str_action_delete_profile)
					.setMessage(R.string.str_q_delete_profile)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									CustomQ q = new CustomQ(
											Database.DATABASE_URL);
									q.addNameValuePair(Database.POST_PIN,
											Database.COMMUNITY_PIN);
									q.addNameValuePair(Database.POST_FUNCTION,
											Database.FUNCTION_DELETE_PROFILE);
									Submitter.Account delacc = Submitter
											.getSavedAccount(OneUserActivity.this);
									delacc.addToQ(q);
									q.addNameValuePair(
											Database.POST_ACC_DELETION_SURE,
											Database.DELETION_SURE_PIN);

									final AlertDialog progress = new AlertDialog.Builder(
											OneUserActivity.this)
											.setMessage(
													R.string.loadingsync_msg)
											.setView(
													new ProgressBar(
															OneUserActivity.this))
											.create();
									q.setPreExecuteListener(new CustomQ.PreExecuteListener() {
										@Override
										public void onPreExecute() {
											progress.show();
										}
									});
									final String KEY_RESULT = "result";
									q.setHttpResultListener(new CustomQ.HttpResultListener() {
										@Override
										public Bundle onHttpResult(String result) {
											Bundle bundle = new Bundle();
											try {
												bundle.putInt(KEY_RESULT,
														Integer.valueOf(result));
											} catch (Throwable e) {
												e.printStackTrace();
											}
											return bundle;
										}
									});
									q.setPostExecuteListener(new CustomQ.PostExecuteListener() {
										@Override
										public void onPostExecute(
												Bundle processed) {
											if (processed.getInt(KEY_RESULT) != 1)
												Toast.makeText(
														OneUserActivity.this,
														R.string.error_try_again,
														Toast.LENGTH_SHORT)
														.show();
											else {
												Submitter
														.deleteSavedAccount(OneUserActivity.this);
												finish();
											}
											progress.dismiss();
										}
									});
									q.exec();
								}
							}).setNegativeButton(android.R.string.no, null)
					.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
