package com.genonbeta.TrebleShot.object;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.FlexibleObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

import java.util.ArrayList;

public class NetworkDevice
		implements FlexibleObject
{
	public String brand;
	public String model;
	public String nickname;
	public String deviceId;
	public String versionName;
	public int versionNumber;
	public long lastUsageTime;
	public boolean isTrusted = false;
	public boolean isRestricted = false;
	public boolean isLocalAddress = false;

	public NetworkDevice()
	{
	}

	public NetworkDevice(String deviceId)
	{
		this.deviceId = deviceId;
	}

	public NetworkDevice(CursorItem item)
	{
		reconstruct(item);
	}

	@Override
	public SQLQuery.Select getWhere()
	{
		return new SQLQuery.Select(AccessDatabase.TABLE_DEVICES)
				.setWhere(AccessDatabase.FIELD_DEVICES_ID + "=?", deviceId);
	}

	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();

		values.put(AccessDatabase.FIELD_DEVICES_ID, deviceId);
		values.put(AccessDatabase.FIELD_DEVICES_USER, nickname);
		values.put(AccessDatabase.FIELD_DEVICES_BRAND, brand);
		values.put(AccessDatabase.FIELD_DEVICES_MODEL, model);
		values.put(AccessDatabase.FIELD_DEVICES_BUILDNAME, versionName);
		values.put(AccessDatabase.FIELD_DEVICES_BUILDNUMBER, versionNumber);
		values.put(AccessDatabase.FIELD_DEVICES_LASTUSAGETIME, lastUsageTime);
		values.put(AccessDatabase.FIELD_DEVICES_ISRESTRICTED, isRestricted ? 1 : 0);
		values.put(AccessDatabase.FIELD_DEVICES_ISTRUSTED, isTrusted ? 1 : 0);
		values.put(AccessDatabase.FIELD_DEVICES_ISLOCALADDRESS, isLocalAddress ? 1 : 0);

		return values;
	}

	@Override
	public void reconstruct(CursorItem item)
	{
		this.deviceId = item.getString(AccessDatabase.FIELD_DEVICES_ID);
		this.nickname = item.getString(AccessDatabase.FIELD_DEVICES_USER);
		this.brand = item.getString(AccessDatabase.FIELD_DEVICES_BRAND);
		this.model = item.getString(AccessDatabase.FIELD_DEVICES_MODEL);
		this.versionName = item.getString(AccessDatabase.FIELD_DEVICES_BUILDNAME);
		this.versionNumber = item.getInt(AccessDatabase.FIELD_DEVICES_BUILDNUMBER);
		this.lastUsageTime = item.getLong(AccessDatabase.FIELD_DEVICES_LASTUSAGETIME);
		this.isTrusted = item.getInt(AccessDatabase.FIELD_DEVICES_ISTRUSTED) == 1;
		this.isRestricted = item.getInt(AccessDatabase.FIELD_DEVICES_ISRESTRICTED) == 1;
		this.isLocalAddress = item.getInt(AccessDatabase.FIELD_DEVICES_ISLOCALADDRESS) == 1;
	}

	@Override
	public void onCreateObject(SQLiteDatabase database)
	{

	}

	@Override
	public void onUpdateObject(SQLiteDatabase database)
	{

	}

	@Override
	public void onRemoveObject(SQLiteDatabase database)
	{
		database.delete(new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION)
				.setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=?", deviceId));

		ArrayList<TransactionObject.Group> groupList = database.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP)
				.setWhere(AccessDatabase.FIELD_TRANSFERGROUP_DEVICEID + "=?", deviceId), TransactionObject.Group.class);

		for (TransactionObject.Group group : groupList)
			database.remove(group);
	}

	public static class Connection implements FlexibleObject
	{
		public String adapterName;
		public String ipAddress;
		public String deviceId;
		public long lastCheckedDate;

		public Connection()
		{
		}

		public Connection(String adapterName, String ipAddress, String deviceId, long lastCheckedDate)
		{
			this.adapterName = adapterName;
			this.ipAddress = ipAddress;
			this.deviceId = deviceId;
			this.lastCheckedDate = lastCheckedDate;
		}

		public Connection(String deviceId, String adapterName)
		{
			this.deviceId = deviceId;
			this.adapterName = adapterName;
		}

		public Connection(String ipAddress)
		{
			this.ipAddress = ipAddress;
		}

		public Connection(CursorItem item)
		{
			reconstruct(item);
		}

		@Override
		public SQLQuery.Select getWhere()
		{
			SQLQuery.Select select = new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION);

			return ipAddress == null
					? select.setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=? AND " + AccessDatabase.FIELD_DEVICECONNECTION_ADAPTERNAME + "=?", deviceId, adapterName)
					: select.setWhere(AccessDatabase.FIELD_DEVICECONNECTION_IPADDRESS + "=?", ipAddress);
		}

		@Override
		public ContentValues getValues()
		{
			ContentValues values = new ContentValues();

			values.put(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID, deviceId);
			values.put(AccessDatabase.FIELD_DEVICECONNECTION_ADAPTERNAME, adapterName);
			values.put(AccessDatabase.FIELD_DEVICECONNECTION_IPADDRESS, ipAddress);
			values.put(AccessDatabase.FIELD_DEVICECONNECTION_LASTCHECKEDDATE, lastCheckedDate);

			return values;
		}

		@Override
		public void reconstruct(CursorItem item)
		{
			this.adapterName = item.getString(AccessDatabase.FIELD_DEVICECONNECTION_ADAPTERNAME);
			this.ipAddress = item.getString(AccessDatabase.FIELD_DEVICECONNECTION_IPADDRESS);
			this.deviceId = item.getString(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID);
			this.lastCheckedDate = item.getLong(AccessDatabase.FIELD_DEVICECONNECTION_LASTCHECKEDDATE);
		}

		@Override
		public void onCreateObject(SQLiteDatabase database)
		{

		}

		@Override
		public void onUpdateObject(SQLiteDatabase database)
		{

		}

		@Override
		public void onRemoveObject(SQLiteDatabase database)
		{

		}
	}
}
