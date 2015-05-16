package com.woalk.apps.xposed.ttsb;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.woalk.apps.xposed.ttsb.community.Database;

public class EasySettingsActivity extends Activity {

	public static String ACTIVITY_INFO_SELECTED = "com.woalk.apps.xposed.ttsb.ACTIVITY_INFO_SELECTED";

	protected ActivityInfo act_inf;
	protected Settings.Parser mSettings;
	protected Settings.Setting setting;

	protected CheckBox check_status;
	protected CheckBox check_nav;
	protected CheckBox check_s_plus;
	protected EditText edit_s_color;
	protected EditText edit_n_color;
	protected Spinner spinner_layoutopt;
	protected Button button_advanced;

	private final Context context = this;
	private Menu menu;

	private boolean resume1 = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_easy_settings);

		act_inf = getIntent().getParcelableExtra(ACTIVITY_INFO_SELECTED);
		if (act_inf.name.equals("All"))
			setTitle(getString(R.string.title_activity_easy_settings) + " "
					+ act_inf.packageName + ", "
					+ getString(R.string.all_activities));
		else
			setTitle(getString(R.string.title_activity_easy_settings) + " "
					+ act_inf.name);

		if (act_inf.name.equals("All"))
			act_inf.name = "";

		mSettings = Settings.Loader.load(this, act_inf.packageName,
				act_inf.name);
		setting = mSettings.getSetting();

		resume1 = true;

		check_status = (CheckBox) findViewById(R.id.checkBox_status);
		check_nav = (CheckBox) findViewById(R.id.checkBox_nav);
		check_s_plus = (CheckBox) findViewById(R.id.checkBox_s_plus);
		edit_s_color = (EditText) findViewById(R.id.editText_s_color);
		edit_n_color = (EditText) findViewById(R.id.editText_n_color);
		spinner_layoutopt = (Spinner) findViewById(R.id.spinner_layoutopt);
		button_advanced = (Button) findViewById(R.id.button_advanced);

		settingsUpdated();

		check_status.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				setting.status = isChecked;
				edit_s_color.setEnabled(isChecked);
				check_s_plus.setEnabled(isChecked);
			}
		});
		check_nav.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				setting.nav = isChecked;
				edit_n_color.setEnabled(isChecked);
			}
		});

		spinner_layoutopt
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						if (arg2 == 4)
							button_advanced.setEnabled(true);
						else
							button_advanced.setEnabled(false);
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						button_advanced.setEnabled(false);
					}
				});

		button_advanced.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (setting.status || setting.nav)
					save();
				Intent intent = new Intent(context, RulesActivity.class);
				intent.putExtra(RulesActivity.RULES_ACTIVITY_INFO_SELECTED,
						(android.os.Parcelable) act_inf);
				((Activity) context).startActivity(intent);
			}
		});
	}

	protected void settingsUpdated() {
		mSettings.parseToSettings();
		setting = mSettings.getSetting();
		check_status.setChecked(setting.status);
		edit_s_color.setEnabled(setting.status);
		check_s_plus.setEnabled(setting.status);
		check_nav.setChecked(setting.nav);
		edit_n_color.setEnabled(setting.nav);
		check_s_plus.setChecked(setting.rules.s_plus > 0);
		edit_s_color.setText(Helpers.getColorHexString(setting.s_color));
		edit_n_color.setText(Helpers.getColorHexString(setting.n_color));
		if (setting.rules.content == null && setting.rules.decview == null
				&& setting.rules.cview != null
				&& setting.rules.cview.setFSW == true
				&& setting.rules.cview.setFSW_value == true
				&& setting.rules.cview.setCTP == true
				&& setting.rules.cview.setCTP_value == false
				&& setting.rules.cview.padding == null
				&& setting.rules.cview.land == null
				&& setting.rules.view == null)
			spinner_layoutopt.setSelection(0);
		else if (setting.rules.content == null
				&& setting.rules.decview == null
				&& setting.rules.cview == null
				&& setting.rules.view != null
				&& setting.rules.view.size() > 0
				&& setting.rules.view.get(0).childindexes.length == 1
				&& setting.rules.view.get(0).childindexes[0] == 0
				&& setting.rules.view.get(0).from == Settings.Setting.ViewSettingsPack.FROM_CVIEW
				&& setting.rules.view.get(0).levels == 1
				&& setting.rules.view.get(0).settings != null
				&& setting.rules.view.get(0).settings.setFSW == true
				&& setting.rules.view.get(0).settings.setFSW_value == true
				&& setting.rules.view.get(0).settings.setCTP == true
				&& setting.rules.view.get(0).settings.setCTP_value == false
				&& setting.rules.view.get(0).settings.padding == null
				&& setting.rules.view.get(0).settings.land == null)
			spinner_layoutopt.setSelection(1);
		else if (setting.rules.content == null && setting.rules.decview == null
				&& setting.rules.cview != null
				&& setting.rules.cview.setFSW == false
				&& setting.rules.cview.setCTP == false
				&& setting.rules.cview.padding != null
				&& setting.rules.cview.padding.plus_nav_h
				&& setting.rules.cview.padding.plus_status_h
				&& setting.rules.cview.land == null
				&& setting.rules.view == null)
			spinner_layoutopt.setSelection(2);
		else if (setting.rules.content == null && setting.rules.decview == null
				&& setting.rules.cview == null && setting.rules.view == null)
			spinner_layoutopt.setSelection(3);
		else {
			spinner_layoutopt.setSelection(4);
			button_advanced.setEnabled(true);
		}
	}

	protected void updateSettings() {
		setting.s_color = Helpers.getColor(edit_s_color.getText().toString());
		setting.n_color = Helpers.getColor(edit_n_color.getText().toString());
		if (check_s_plus.isChecked()) {
			if (setting.rules.s_plus < 1)
				setting.rules.s_plus = 1;
		} else
			setting.rules.s_plus = 0;
		switch (spinner_layoutopt.getSelectedItemPosition()) {
		case 0:
			setting.rules.content = null;
			setting.rules.decview = null;
			setting.rules.cview = new Settings.Setting.ViewSettings();
			setting.rules.cview.setFSW = true;
			setting.rules.cview.setFSW_value = true;
			setting.rules.cview.setCTP = true;
			setting.rules.cview.setCTP_value = false;
			setting.rules.view = null;
			break;
		case 1:
			setting.rules.content = null;
			setting.rules.decview = null;
			setting.rules.cview = null;
			setting.rules.view = new ArrayList<Settings.Setting.ViewSettingsPack>();
			Settings.Setting.ViewSettingsPack vsetpk = new Settings.Setting.ViewSettingsPack();
			vsetpk.childindexes = new int[] { 0 };
			vsetpk.from = Settings.Setting.ViewSettingsPack.FROM_CVIEW;
			vsetpk.levels = 1;
			vsetpk.settings = new Settings.Setting.ViewSettings();
			vsetpk.settings.setFSW = true;
			vsetpk.settings.setFSW_value = true;
			vsetpk.settings.setCTP = true;
			vsetpk.settings.setCTP_value = false;
			setting.rules.view.add(vsetpk);
			break;
		case 2:
			setting.rules.content = null;
			setting.rules.decview = null;
			setting.rules.cview = new Settings.Setting.ViewSettings();
			setting.rules.cview.setFSW = false;
			setting.rules.cview.setCTP = false;
			setting.rules.cview.padding = new Settings.Setting.ViewSettings.IntOptPadding();
			setting.rules.cview.padding.plus_nav_h = true;
			setting.rules.cview.padding.plus_status_h = true;
			setting.rules.view = null;
			break;
		case 3:
			setting.rules.content = null;
			setting.rules.decview = null;
			setting.rules.cview = null;
			setting.rules.view = null;
			break;
		}
		mSettings.setSetting(setting);
		if (setting != null)
			mSettings.parseToString();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.easy_settings, menu);
		if (Helpers.clipboard_sav instanceof Settings.Parser)
			menu.getItem(4).setEnabled(true);
		this.menu = menu;
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		updateSettings();
		switch (item.getItemId()) {
		case R.id.action_save_easy_settings:
			Toast.makeText(getApplicationContext(), R.string.str_saved,
					Toast.LENGTH_SHORT).show();
			save();
			finish();
			return true;
		case R.id.action_delete:
			delete();
			return true;
		case R.id.action_use_settingscode:
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(R.string.settingscode_title);
			alert.setMessage(R.string.settingscode_msg);
			// Set an EditText view to get user input
			final EditText input = new EditText(this);
			alert.setView(input);
			input.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			input.setText(mSettings.getLine());
			input.setTypeface(Typeface.MONOSPACE);
			alert.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							String value = input.getText().toString();
							mSettings.setLine(value);
							mSettings.parseToSettings();
							settingsUpdated();
						}
					});
			alert.setNegativeButton(android.R.string.cancel, null);
			alert.show();
			return true;
		case R.id.action_copy_easy_settings:
			Helpers.clipboard_sav = mSettings;
			menu.getItem(4).setEnabled(true);
			return true;
		case R.id.action_paste_easy_settings:
			if (!(Helpers.clipboard_sav instanceof Settings.Parser))
				return false;
			mSettings = (Settings.Parser) Helpers.clipboard_sav;
			settingsUpdated();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void save() {
		updateSettings();
		Settings.Saver.save(this, act_inf.packageName, act_inf.name, mSettings);
		SharedPreferences sPref_c = context.getSharedPreferences(
				Database.Preferences.COMMUNITY_PREF_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = sPref_c.edit();
		edit.remove(Database.Preferences.PREF_PREFIX_IS_TOPVOTED_USED
				+ act_inf.packageName);
		edit.remove(Database.Preferences.PREF_PREFIX_USED_SUBMIT_ID
				+ act_inf.packageName);
		edit.apply();
	}

	public void delete() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.delete_title);
		alert.setMessage(R.string.delete_msg);
		alert.setNegativeButton(android.R.string.no, null);
		alert.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Settings.Saver.delete(context, act_inf.packageName,
								act_inf.name);
						if (!Settings.Loader.containsPackage(context,
								act_inf.packageName)) {
							SharedPreferences sPref_c = context
									.getSharedPreferences(
											Database.Preferences.COMMUNITY_PREF_NAME,
											Context.MODE_PRIVATE);
							SharedPreferences.Editor edit = sPref_c.edit();
							edit.remove(Database.Preferences.PREF_PREFIX_IS_TOPVOTED_USED
									+ act_inf.packageName);
							edit.remove(Database.Preferences.PREF_PREFIX_USED_SUBMIT_ID
									+ act_inf.packageName);
							edit.apply();
						}
						((Activity) context).finish();
					}
				});
		alert.show();
	}

	@Override
	protected void onResume() {
		if (resume1) {
			resume1 = false;
			super.onResume();
			return;
		}
		mSettings = Settings.Loader.load(this, act_inf.packageName,
				act_inf.name);
		setting = mSettings.getSetting();
		settingsUpdated();
		super.onResume();
	}
}
