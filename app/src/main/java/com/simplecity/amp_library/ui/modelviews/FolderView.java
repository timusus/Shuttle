package com.simplecity.amp_library.ui.modelviews;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.aesthetic.Aesthetic;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.interfaces.FileType;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.FileObject;
import com.simplecity.amp_library.model.FolderObject;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.ui.views.CircleImageView;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import java.lang.ref.WeakReference;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FolderView extends BaseSelectableViewModel<FolderView.ViewHolder, BaseFileObject> {

    public interface ClickListener {

        void onFileObjectClick(int position, FolderView folderView);

        void onFileObjectOverflowClick(View v, FolderView folderView);

        void onFileObjectCheckboxClick(View v, FolderView folderView);
    }

    @NonNull
    public BaseFileObject baseFileObject;

    @Nullable
    private ClickListener listener;

    private boolean showCheckboxes;

    public FolderView(@NonNull BaseFileObject baseFileObject) {
        this.baseFileObject = baseFileObject;
    }

    public void setClickListener(@Nullable ClickListener listener) {
        this.listener = listener;
    }

    public void setShowCheckboxes(boolean show) {
        showCheckboxes = show;
    }

    @Override
    public BaseFileObject getItem() {
        return baseFileObject;
    }

    private void onClick(int position) {
        if (listener != null) {
            listener.onFileObjectClick(position, this);
        }
    }

    private void onOverflowClick(View v) {
        if (listener != null) {
            listener.onFileObjectOverflowClick(v, this);
        }
    }

    private void onCheckboxClick(CheckBox checkBox) {
        setSelected(checkBox.isChecked());
        if (listener != null) {
            listener.onFileObjectCheckboxClick(checkBox, this);
        }
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
        super.bindView(holder);

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
                holder.imageView.setImageDrawable(holder.parentFolderDrawable);
                holder.lineTwo.setText(holder.itemView.getContext().getString(R.string.parent_folder));
                holder.overflow.setVisibility(View.GONE);
                holder.lineThree.setVisibility(View.GONE);
                holder.lineOne.setText(baseFileObject.name);
                break;
            case FileType.FOLDER:
                holder.overflow.setVisibility(View.VISIBLE);
                holder.imageView.setImageDrawable(holder.folderDrawable);
                holder.lineTwo.setText(StringUtils.makeSubfoldersLabel(holder.itemView.getContext(), ((FolderObject) baseFileObject).folderCount, ((FolderObject) baseFileObject).fileCount));
                holder.lineThree.setVisibility(View.GONE);
                holder.lineOne.setText(baseFileObject.name);
                break;
            case FileType.FILE:
                holder.overflow.setVisibility(View.VISIBLE);
                holder.imageView.setImageDrawable(holder.fileDrawable);
                holder.lineThree.setVisibility(View.VISIBLE);
                holder.lineOne.setText(((FileObject) baseFileObject).tagInfo.trackName);
                holder.lineTwo.setText(String.format("%s - %s", ((FileObject) baseFileObject).tagInfo.artistName, ((FileObject) baseFileObject).tagInfo.albumName));
                DurationTask durationTask = new DurationTask(holder.lineThree, (FileObject) baseFileObject);
                durationTask.execute();
                break;
        }

        if (showCheckboxes && baseFileObject.fileType == FileType.FOLDER) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.imageView.setVisibility(View.GONE);
        } else {
            holder.checkBox.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.VISIBLE);
        }

        holder.checkBox.setChecked(isSelected());
        holder.itemView.setActivated(false);
    }

    @Override
    public void bindView(ViewHolder holder, int position, List payloads) {
        super.bindView(holder, position, payloads);

        if (baseFileObject instanceof FileObject && SettingsManager.getInstance().getFolderBrowserShowFileNames()) {
            holder.lineFour.setText(String.format("%s.%s", ((FileObject) baseFileObject).name, ((FileObject) baseFileObject).extension));
            holder.lineFour.setVisibility(View.VISIBLE);
            holder.textContainer.setVisibility(View.GONE);
        } else {
            holder.lineFour.setVisibility(View.GONE);
            holder.textContainer.setVisibility(View.VISIBLE);
        }

        if (showCheckboxes && baseFileObject.fileType == FileType.FOLDER) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.imageView.setVisibility(View.GONE);
        } else {
            holder.checkBox.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.VISIBLE);
        }

        holder.checkBox.setChecked(isSelected());
        holder.itemView.setActivated(false);
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder<FolderView> {

        @BindView(R.id.line_one)
        public TextView lineOne;

        @BindView(R.id.line_two)
        public TextView lineTwo;

        @BindView(R.id.line_three)
        public TextView lineThree;

        @BindView(R.id.line_four)
        public TextView lineFour;

        @BindView(R.id.textContainer)
        public View textContainer;

        @BindView(R.id.image)
        public CircleImageView imageView;

        @BindView(R.id.btn_overflow)
        public ImageButton overflow;

        @BindView(R.id.checkbox)
        public CheckBox checkBox;

        Drawable folderDrawable;
        Drawable parentFolderDrawable;
        Drawable fileDrawable;

        public ViewHolder(final View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(v -> viewModel.onClick(getAdapterPosition()));
            overflow.setOnClickListener(v -> viewModel.onOverflowClick(v));
            checkBox.setOnClickListener(v -> viewModel.onCheckboxClick((CheckBox) v));

            int colorPrimary = Aesthetic.get(itemView.getContext()).colorPrimary().blockingFirst();

            folderDrawable = itemView.getContext().getResources().getDrawable(R.drawable.ic_folder_24dp);
            parentFolderDrawable = itemView.getContext().getResources().getDrawable(R.drawable.ic_folder_open_24dp);
            fileDrawable = itemView.getContext().getResources().getDrawable(R.drawable.ic_headphones_white);

            imageView.setColorFilter(colorPrimary);
        }

        @Override
        public String toString() {
            return "FolderView.ViewHolder";
        }
    }

    private static class DurationTask extends AsyncTask<Void, Void, String> {

        private TextView textView;

        private FileObject fileObject;

        DurationTask(TextView textView, FileObject fileObject) {
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
        if (!super.equals(o)) return false;

        FolderView that = (FolderView) o;

        return baseFileObject.equals(that.baseFileObject);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + baseFileObject.hashCode();
        return result;
    }

    @Override
    public boolean areContentsEqual(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        if (!super.areContentsEqual(other)) return false;

        if (!baseFileObject.equals(((FolderView) other).baseFileObject)) return false;
        if (isSelected() != ((FolderView) other).isSelected()) return false;
        return true;
    }
}