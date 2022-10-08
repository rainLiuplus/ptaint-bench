package com.genonbeta.android.database;

import android.content.ContentValues;

import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

/**
 * created by: Veli
 * date: 2.11.2017 21:31
 */

public interface FlexibleObject
{
	SQLQuery.Select getWhere();

	ContentValues getValues();

	void reconstruct(CursorItem item);

	void onCreateObject(SQLiteDatabase database);

	void onUpdateObject(SQLiteDatabase database);

	void onRemoveObject(SQLiteDatabase database);
}
