package com.genonbeta.TrebleShot.util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest;
import com.genonbeta.TrebleShot.object.NetworkDevice;

import java.util.ArrayList;

public class AppUtils
{
	public static final String TAG = AppUtils.class.getSimpleName();

	private static int mUniqueNumber = 0;

	public static void applyAdapterName(NetworkDevice.Connection connection)
	{
		if (connection.ipAddress == null) {
			Log.e(AppUtils.class.getSimpleName(), "Connection should be provided with IP address");
			return;
		}

		ArrayList<AddressedInterface> interfaceList = NetworkUtils.getInterfaces(true, AppConfig.DEFAULT_DISABLED_INTERFACES);

		for (AddressedInterface addressedInterface : interfaceList) {
			if (NetworkUtils.getAddressPrefix(addressedInterface.getAssociatedAddress())
					.equals(NetworkUtils.getAddressPrefix(connection.ipAddress))) {
				connection.adapterName = addressedInterface.getNetworkInterface().getDisplayName();
				return;
			}
		}

		connection.adapterName = Keyword.Local.NETWORK_INTERFACE_UNKNOWN;
	}

	public static boolean checkRunningConditions(Context context)
	{
		for (RationalePermissionRequest.PermissionRequest request : getRequiredPermissions(context))
			if (ActivityCompat.checkSelfPermission(context, request.permission) != PackageManager.PERMISSION_GRANTED)
				return false;

		return true;
	}

	public static String getHotspotName(Context context)
	{
		return AppConfig.ACCESS_POINT_PREFIX + AppUtils.getLocalDeviceName(context)
				.replace(" ", "_");
	}

	public static String getLocalDeviceName(Context context)
	{
		String deviceName = PreferenceManager.getDefaultSharedPreferences(context)
				.getString("device_name", null);

		return deviceName == null || deviceName.length() == 0
				? Build.MODEL.toUpperCase()
				: deviceName;
	}

	public static ArrayList<RationalePermissionRequest.PermissionRequest> getRequiredPermissions(Context context)
	{
		ArrayList<RationalePermissionRequest.PermissionRequest> permissionRequests = new ArrayList<>();

		if (Build.VERSION.SDK_INT >= 16) {
			permissionRequests.add(new RationalePermissionRequest.PermissionRequest(context,
					Manifest.permission.WRITE_EXTERNAL_STORAGE,
					R.string.text_requestPermissionStorage,
					R.string.text_requestPermissionStorageSummary));
		}

		if (Build.VERSION.SDK_INT >= 26) {
			permissionRequests.add(new RationalePermissionRequest.PermissionRequest(context,
					Manifest.permission.READ_PHONE_STATE,
					R.string.text_requestPermissionReadPhoneState,
					R.string.text_requestPermissionReadPhoneStateSummary));
		}

		return permissionRequests;
	}

	public static int getUniqueNumber()
	{
		return (int) System.currentTimeMillis() + (++mUniqueNumber);
	}

	public static NetworkDevice getLocalDevice(Context context)
	{
		String serial = Build.VERSION.SDK_INT < 26
				? Build.SERIAL
				: (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ? Build.getSerial() : null);

		NetworkDevice device = new NetworkDevice(serial);

		device.brand = Build.BRAND;
		device.model = Build.MODEL;
		device.nickname = AppUtils.getLocalDeviceName(context);
		device.isRestricted = false;
		device.isLocalAddress = true;

		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getApplicationInfo().packageName, 0);

			device.versionNumber = packageInfo.versionCode;
			device.versionName = packageInfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}

		return device;
	}

	public static void startForegroundService(Context context, Intent intent)
	{
		if (Build.VERSION.SDK_INT >= 26)
			context.startForegroundService(intent);
		else
			context.startService(intent);
	}
}