package com.genonbeta.TrebleShot.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;

import static com.genonbeta.TrebleShot.activity.HomeActivity.REQUEST_PERMISSION_ALL;

/**
 * created by: Veli
 * date: 18.11.2017 20:16
 */

public class RationalePermissionRequest extends AlertDialog.Builder
{
	public PermissionRequest mPermissionQueue;

	public RationalePermissionRequest(final Activity activity, @NonNull PermissionRequest permission)
	{
		super(activity);

		mPermissionQueue = permission;

		setCancelable(false);
		setTitle(permission.title);
		setMessage(permission.message);

		if (ActivityCompat.shouldShowRequestPermissionRationale(activity, mPermissionQueue.permission))
			setPositiveButton(R.string.butn_settings, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialogInterface, int i)
				{
					Intent intent = new Intent()
							.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
							.setData(Uri.fromParts("package", activity.getPackageName(), null));

					activity.startActivity(intent);
				}
			});
		else
			setPositiveButton(R.string.butn_ask, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialogInterface, int i)
				{
					ActivityCompat.requestPermissions(activity, new String[]{mPermissionQueue.permission}, REQUEST_PERMISSION_ALL);
				}
			});

		setNegativeButton(R.string.butn_reject, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialogInterface, int i)
			{
				activity.finish();
			}
		});
	}

	public static AlertDialog requestIfNecessary(Activity activity, PermissionRequest permissionQueue)
	{
		return ActivityCompat.checkSelfPermission(activity, permissionQueue.permission) == PackageManager.PERMISSION_GRANTED
				? null
				: new RationalePermissionRequest(activity, permissionQueue).show();
	}

	public static class PermissionRequest
	{
		public String permission;
		public String title;
		public String message;

		public PermissionRequest(String permission, String title, String message)
		{
			this.permission = permission;
			this.title = title;
			this.message = message;
		}

		public PermissionRequest(Context context, String permission, int titleRes, int messageRes)
		{
			this(permission, context.getString(titleRes), context.getString(messageRes));
		}
	}
}