package com.diamond.SmartVoice;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity
{
	@SuppressWarnings("deprecation")
	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.pref_general);
		
		bindPreferenceSummaryToValue(findPreference("server_ip"));
		bindPreferenceSummaryToValue(findPreference("server_login"));
		bindPreferenceSummaryToValue(findPreference("server_password"));
	}

	@Override
	public boolean onIsMultiPane()
	{
		return false;
	}

	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener(){
		@Override
		public boolean onPreferenceChange(Preference preference, Object value)
		{
			String stringValue = value.toString();
			preference.setSummary(stringValue);
			return true;
		}
	};

	private static void bindPreferenceSummaryToValue(Preference preference)
	{
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
		sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
	}
}
