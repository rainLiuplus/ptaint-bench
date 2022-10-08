package com.genonbeta.TrebleShot.object;

import android.net.Uri;

import com.genonbeta.TrebleShot.util.TextUtils;

/**
 * created by: Veli
 * date: 19.11.2017 16:50
 */

public class Shareable implements Editable
{
	public String friendlyName;
	public String fileName;
	public Uri uri;
	public long date;
	public long size;

	private boolean isSelected = false;

	public Shareable()
	{
	}

	public Shareable(String friendlyName, String fileName, long date, long size, Uri uri)
	{
		this.friendlyName = friendlyName;
		this.fileName = fileName;
		this.date = date;
		this.size = size;
		this.uri = uri;
	}

	@Override
	public boolean isSelectableSelected()
	{
		return isSelected;
	}

	@Override
	public String getComparableName()
	{
		return getSelectableFriendlyName();
	}

	@Override
	public long getComparableDate()
	{
		return this.date;
	}

	@Override
	public long getComparableSize()
	{
		return this.size;
	}

	@Override
	public String getSelectableFriendlyName()
	{
		return this.friendlyName;
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof Shareable ? ((Shareable) obj).uri.equals(uri) : super.equals(obj);
	}

	public boolean searchMatches(String searchWord)
	{
		return TextUtils.searchWord(this.friendlyName, searchWord);
	}

	@Override
	public void setSelectableSelected(boolean selected)
	{
		isSelected = selected;
	}
}