package com.simplecity.amp_library.ui.modelviews;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.interfaces.FileType;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.FileObject;
import com.simplecity.amp_library.model.FolderObject;
import com.simplecity.amp_library.ui.views.CircleImageView;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.StringUtils;

import java.lang.ref.WeakReference;

public class FolderView extends BaseAdaptableItem<BaseFileObject, FolderView.ViewHolder> {

    public BaseFileObject baseFileObject;

    private boolean mShowCheckboxes;

    private boolean mIsChecked;

    public FolderView(BaseFileObject baseFileObject) {
        this.baseFileObject = baseFileObject;
    }

    public void setShowCheckboxes(boolean show) {
        mShowCheckboxes = show;
    }

    public void setChecked(boolean checked) {
        mIsChecked = checked;
    }

    @Override
    public int getViewType() {
        return ViewType.FOLDER;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_folder;
    }

    @Override
    public void bindView(ViewHolder holder) {

        if (baseFileObject instanceof FileObject && SettingsManager.getInstance().getFolderBrowserShowFileNames()) {
            holder.lineFour.setText(String.format("%s.%s", ((FileObject) baseFileObject).name, ((FileObject) baseFileObject).extension));
            holder.lineFour.setVisibility(View.VISIBLE);
            holder.textContainer.setVisibility(View.GONE);
        } else {
            holder.lineFour.setVisibility(View.GONE);
            holder.textContainer.setVisibility(View.VISIBLE);
        }

        holder.lineThree.setText(null);

        switch (baseFileObject.fileType) {
            case FileType.PARENT:
                holder.imageView.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.ic_folder_open_white));
                holder.lineTwo.setText(holder.itemView.getContext().getString(R.string.parent_folder));
                holder.overflow.setVisibility(View.GONE);
                holder.lineThree.setVisibility(View.GONE);
                holder.lineOne.setText(baseFileObject.name);
                break;
            case FileType.FOLDER:
                holder.overflow.setVisibility(View.VISIBLE);
                holder.imageView.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.ic_folder_closed_white));
                holder.lineTwo.setText(StringUtils.makeSubfoldersLabel(holder.itemView.getContext(), ((FolderObject) baseFileObject).folderCount, ((FolderObject) baseFileObject).fileCount));
                holder.lineThree.setVisibility(View.GONE);
                holder.lineOne.setText(baseFileObject.name);
                break;
            case FileType.FILE:
                holder.overflow.setVisibility(View.VISIBLE);
                holder.imageView.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.ic_headphones_white));
                holder.lineThree.setVisibility(View.VISIBLE);
                holder.lineOne.setText(((FileObject) baseFileObject).tagInfo.trackName);
                holder.lineTwo.setText(String.format("%s - %s", ((FileObject) baseFileObject).tagInfo.artistName, ((FileObject) baseFileObject).tagInfo.albumName));

                DurationTask durationTask = new DurationTask(holder.lineThree, (FileObject) baseFileObject);
                durationTask.execute();
                break;
        }

        if (ColorUtils.isPrimaryColorLowContrast(holder.itemView.getContext())) {
            holder.imageView.setColorFilter(ColorUtils.getAccentColor());
        } else {
            holder.imageView.setColorFilter(ColorUtils.getPrimaryColor());
        }

        if (mShowCheckboxes && baseFileObject.fileType == FileType.FOLDER) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.imageView.setVisibility(View.GONE);
        } else {
            holder.checkBox.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.VISIBLE);
        }

        holder.checkBox.setChecked(mIsChecked);
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    @Override
    public BaseFileObject getItem() {
        return baseFileObject;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public View itemView;
        public TextView lineOne;
        public TextView lineTwo;
        public TextView lineThree;
        public TextView lineFour;
        public View textContainer;
        public CircleImageView imageView;
        public ImageButton overflow;
        public CheckBox checkBox;

        public ViewHolder(final View itemView) {
            super(itemView);

            this.itemView = itemView;
            lineOne = (TextView) itemView.findViewById(R.id.line_one);
            lineTwo = (TextView) itemView.findViewById(R.id.line_two);
            lineThree = (TextView) itemView.findViewById(R.id.line_three);
            lineFour = (TextView) itemView.findViewById(R.id.line_four);
            textContainer = itemView.findViewById(R.id.textContainer);
            imageView = (CircleImageView) itemView.findViewById(R.id.image);
            overflow = (ImageButton) itemView.findViewById(R.id.btn_overflow);
            overflow.setImageDrawable(DrawableUtils.getColoredStateListDrawable(itemView.getContext(), R.drawable.ic_overflow_white));
            checkBox = (CheckBox) itemView.findViewById(R.id.checkbox);
        }

        @Override
        public String toString() {
            return "FolderView.ViewHolder";
        }
    }

    private static class DurationTask extends AsyncTask<Void, Void, String> {

        private TextView textView;

        private FileObject fileObject;

        public DurationTask(TextView textView, FileObject fileObject) {
            this.textView = textView;
            this.fileObject = fileObject;
            textView.setTag(new WeakReference<>(DurationTask.this));
        }

        @Override
        protected String doInBackground(Void... params) {
            return fileObject.getTimeString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (textView != null) {
                if (((WeakReference<DurationTask>) textView.getTag()).get() == DurationTask.this) {
                    textView.setText(s);
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FolderView that = (FolderView) o;

        return baseFileObject != null ? baseFileObject.equals(that.baseFileObject) : that.baseFileObject == null;
    }

    @Override
    public int hashCode() {
        return baseFileObject != null ? baseFileObject.hashCode() : 0;
    }
}