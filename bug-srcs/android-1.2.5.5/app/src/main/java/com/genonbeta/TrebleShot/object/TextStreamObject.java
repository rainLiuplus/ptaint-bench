package com.genonbeta.TrebleShot.object;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.FlexibleObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

/**
 * created by: Veli
 * date: 30.12.2017 13:19
 */

public class TextStreamObject
		implements FlexibleObject, Editable
{
	public int id;
	public long time;
	public String text;

	private boolean isSelected = false;

	public TextStreamObject()
	{
	}

	public TextStreamObject(int id)
	{
		this.id = id;
	}

	public TextStreamObject(int id, String index)
	{
		this.id = id;
		this.time = System.currentTimeMillis();
		this.text = index;
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof TextStreamObject && ((TextStreamObject) obj).id == id;
	}

	@Override
	public boolean isSelectableSelected()
	{
		return isSelected;
	}

	@Override
	public String getComparableName()
	{
		return text;
	}

	@Override
	public long getComparableDate()
	{
		return time;
	}

	@Override
	public long getComparableSize()
	{
		return text.length();
	}

	@Override
	public String getSelectableFriendlyName()
	{
		return text;
	}

	@Override
	public SQLQuery.Select getWhere()
	{
		return new SQLQuery.Select(AccessDatabase.TABLE_CLIPBOARD)
				.setWhere(AccessDatabase.FIELD_CLIPBOARD_ID + "=?", String.valueOf(id));
	}

	@Override
	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();

		values.put(AccessDatabase.FIELD_CLIPBOARD_ID, id);
		values.put(AccessDatabase.FIELD_CLIPBOARD_TIME, time);
		values.put(AccessDatabase.FIELD_CLIPBOARD_TEXT, text);

		return values;
	}

	@Override
	public void reconstruct(CursorItem item)
	{
		this.id = item.getInt(AccessDatabase.FIELD_CLIPBOARD_ID);
		this.text = item.getString(AccessDatabase.FIELD_CLIPBOARD_TEXT);
		this.time = item.getLong(AccessDatabase.FIELD_CLIPBOARD_TIME);
	}

	@Override
	public void setSelectableSelected(boolean selected)
	{
		isSelected = selected;
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
