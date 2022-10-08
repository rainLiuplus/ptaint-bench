package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class FileListAdapter extends ShareableListAdapter<FileListAdapter.FileHolder>
{
	private boolean mShowDirectories = true;
	private boolean mShowFiles = true;
	private String mFileMatch;

	private ArrayList<FileHolder> mList = new ArrayList<>();
	private File mDefaultPath;
	private File mPath;

	public FileListAdapter(Context context)
	{
		super(context);
		mDefaultPath = FileUtils.getApplicationDirectory(mContext);
	}

	@Override
	public ArrayList<FileHolder> onLoad()
	{
		ArrayList<FileHolder> list = new ArrayList<>();
		ArrayList<FileHolder> folders = new ArrayList<>();
		ArrayList<FileHolder> files = new ArrayList<>();

		if (mPath != null) {
			File[] fileIndex = mPath.listFiles();

			if (fileIndex != null && fileIndex.length > 0) {
				for (File file : fileIndex) {
					if ((mFileMatch != null && !file.getName().matches(mFileMatch)))
						continue;

					if (file.isDirectory() && mShowDirectories)
						folders.add(new FileHolder(file.getName(), mContext.getString(R.string.text_folder), file, true));
					else if (file.isFile() && mShowFiles)
						files.add(new FileHolder(file.getName(), FileUtils.sizeExpression(file.length(), false), file, false));
				}

				Collections.sort(folders, getDefaultComparator());
				Collections.sort(files, getDefaultComparator());
			}
		} else {
			ArrayList<File> referencedDirectoryList = new ArrayList<>();

			File defaultFolder = FileUtils.getApplicationDirectory(getContext());
			folders.add(new FileHolder(defaultFolder.getName(), getContext().getString(R.string.text_defaultFolder), defaultFolder, true));

			if (Build.VERSION.SDK_INT >= 21)
				referencedDirectoryList.addAll(Arrays.asList(getContext().getExternalMediaDirs()));
			else if (Build.VERSION.SDK_INT >= 19)
				referencedDirectoryList.addAll(Arrays.asList(getContext().getExternalFilesDirs(null)));
			else
				referencedDirectoryList.add(Environment.getExternalStorageDirectory());

			folders.add(new FileHolder(mContext.getString(R.string.text_fileRoot), getContext().getString(R.string.text_mediaDirectory), new File("."), true));

			for (File mediaDir : referencedDirectoryList) {
				FileHolder fileHolder = new FileHolder(mediaDir.getName(), getContext().getString(R.string.text_mediaDirectory), mediaDir, true);
				String[] splitPath = mediaDir.getAbsolutePath().split(File.separator);

				if (splitPath.length >= 2
						&& splitPath[1].equals("storage")
						&& splitPath[splitPath.length - 1].equals(getContext().getPackageName())) {
					if (splitPath.length >= 4 && splitPath[2].equals("emulated")) {
						File file = new File(buildPath(splitPath, 4));

						if (file.canWrite()) {
							fileHolder.file = file;
							fileHolder.friendlyName = getContext().getString(R.string.text_emulatedMediaDirectory, splitPath[3]);
						}
					} else if (splitPath.length >= 3) {
						File file = new File(buildPath(splitPath, 3));

						if (file.canWrite()) {
							fileHolder.file = file;
							fileHolder.friendlyName = splitPath[2];
						}
					}
				}

				folders.add(fileHolder);
			}
		}

		list.addAll(folders);
		list.addAll(files);

		return list;
	}

	@Override
	public void onUpdate(ArrayList<FileHolder> passedItem)
	{
		mList.clear();
		mList.addAll(passedItem);
	}

	public String buildPath(String[] splitPath, int count)
	{
		StringBuilder stringBuilder = new StringBuilder();

		for (int i = 0; (i < count && i < splitPath.length); i++) {
			stringBuilder.append(File.separator);
			stringBuilder.append(splitPath[i]);
		}

		return stringBuilder.toString();
	}

	@Override
	public int getCount()
	{
		return mList.size();
	}

	public File getDefaultPath()
	{
		return mDefaultPath;
	}

	@Override
	public Object getItem(int itemId)
	{
		return mList.get(itemId);
	}

	public ArrayList<FileHolder> getList()
	{
		return mList;
	}

	@Override
	public long getItemId(int position)
	{
		return 0;
	}

	public File getPath()
	{
		return mPath;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if (convertView == null)
			convertView = getInflater().inflate(R.layout.list_file, parent, false);

		final FileHolder holder = (FileHolder) getItem(position);

		final View selector = convertView.findViewById(R.id.selector);
		final View layoutImage = convertView.findViewById(R.id.layout_image);
		ImageView image = convertView.findViewById(R.id.image);
		TextView text1 = convertView.findViewById(R.id.text);
		TextView text2 = convertView.findViewById(R.id.text2);
		TextView text3 = convertView.findViewById(R.id.text3);

		if (getSelectionConnection() != null) {
			selector.setSelected(getSelectionConnection().isSelected(holder));

			layoutImage.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					getSelectionConnection().setSelected(holder);
					selector.setSelected(holder.isSelectableSelected());
				}
			});
		}

		image.setImageResource(holder.isFolder ? R.drawable.ic_folder_white_24dp : R.drawable.ic_whatshot_white_24dp);
		text1.setText(holder.friendlyName);
		text2.setText(holder.fileInfo);
		text3.setText(TimeUtils.getTimeAgo(getContext(), holder.file.lastModified()));

		return convertView;
	}

	public void goPath(File path)
	{
		mPath = path;
	}

	public void setConfiguration(boolean showDirectories, boolean showFiles, String fileMatch)
	{
		mShowDirectories = showDirectories;
		mShowFiles = showFiles;
		mFileMatch = fileMatch;
	}

	public static class FileHolder extends Shareable
	{
		public String fileInfo;
		public File file;
		public boolean isFolder;

		public FileHolder(String friendlyName, String fileInfo, File file, boolean isFolder)
		{
			super(friendlyName, friendlyName, file.lastModified(), file.length(), Uri.fromFile(file));

			this.fileInfo = fileInfo;
			this.file = file;
			this.isFolder = isFolder;
		}
	}
}
