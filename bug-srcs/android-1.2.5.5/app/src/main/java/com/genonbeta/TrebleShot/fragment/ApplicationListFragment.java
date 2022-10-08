package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ApplicationListAdapter;
import com.genonbeta.TrebleShot.app.ShareableListFragment;
import com.genonbeta.TrebleShot.util.TitleSupport;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;

public class ApplicationListFragment
		extends ShareableListFragment<ApplicationListAdapter.PackageHolder, ApplicationListAdapter>
		implements TitleSupport
{
	private SharedPreferences mPreferences;


	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
	}

	@Override
	public ApplicationListAdapter onAdapter()
	{
		return new ApplicationListAdapter(getActivity(), PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("show_system_apps", false));
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.actions_application, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.show_system_apps) {
			mPreferences.edit().putBoolean("show_system_apps", !mPreferences.getBoolean("show_system_apps", false)).apply();
			getAdapter().showSystemApps(mPreferences.getBoolean("show_system_apps", false));

			refreshList();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		MenuItem menuSystemApps = menu.findItem(R.id.show_system_apps);
		menuSystemApps.setChecked(mPreferences.getBoolean("show_system_apps", false));
	}

	@Override
	public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
	{
		boolean result = super.onCreateActionMenu(context, actionMode, menu);

		MenuItem shareOthers = menu.findItem(R.id.action_mode_share_all_apps);

		if (shareOthers != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			shareOthers.setVisible(false);

		return result;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		if (isSelectionActivated())
			super.onListItemClick(l, v, position, id);
		else {
			final ApplicationListAdapter.PackageHolder appInfo = (ApplicationListAdapter.PackageHolder) getAdapter().getItem(position);
			final Intent launchIntent = getActivity().getPackageManager().getLaunchIntentForPackage(appInfo.packageName);

			if (launchIntent != null) {
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());

				dialogBuilder.setMessage(R.string.ques_launchApplication);
				dialogBuilder.setNegativeButton(R.string.butn_cancel, null);
				dialogBuilder.setPositiveButton(R.string.butn_appLaunch, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						startActivity(launchIntent);
					}
				});

				dialogBuilder.show();
			} else
				Toast.makeText(getActivity(), R.string.mesg_launchApplicationError, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_application);
	}
}
