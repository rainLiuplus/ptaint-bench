package com.genonbeta.TrebleShot.fragment;

import android.content.Context;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.MusicListAdapter;
import com.genonbeta.TrebleShot.app.ShareableListFragment;
import com.genonbeta.TrebleShot.util.TitleSupport;

public class MusicListFragment
		extends ShareableListFragment<MusicListAdapter.SongHolder, MusicListAdapter>
		implements TitleSupport
{
	@Override
	public MusicListAdapter onAdapter()
	{
		return new MusicListAdapter(getActivity());
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_music);
	}
}
