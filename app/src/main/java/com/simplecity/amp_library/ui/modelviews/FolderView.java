package com.simplecity.amp_library.ui.modelviews;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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
import com.simplecity.amp_library.ui.views.CircleImageView;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.simplecity.amp_library.R.drawable.ic_folder_closed_white;
import static com.simplecity.amp_library.R.drawable.ic_folder_open_white;
import static com.simplecity.amp_library.R.drawable.ic_headphones_white;
import static com.simplecity.amp_library.R.id;
import static com.simplecity.amp_library.R.id.btn_overflow;
import static com.simplecity.amp_library.R.id.checkbox;
import static com.simplecity.amp_library.R.id.image;
import static com.simplecity.amp_library.R.id.line_four;
import static com.simplecity.amp_library.R.id.line_one;
import static com.simplecity.amp_library.R.id.line_three;
import static com.simplecity.amp_library.R.id.line_two;
import static com.simplecity.amp_library.R.string.parent_folder;
import static com.simplecity.amp_library.interfaces.FileType.FILE;
import static com.simplecity.amp_library.interfaces.FileType.PARENT;
import static com.simplecity.amp_library.ui.adapters.ViewType.FOLDER;
import static com.simplecity.amp_library.utils.SettingsManager.getInstance;
import static com.simplecity.amp_library.utils.StringUtils.makeSubfoldersLabel;
import static java.lang.String.format;

public class FolderView extends BaseViewModel<FolderView.ViewHolder> {

    public interface ClickListener {

        void onFileObjectClick(BaseFileObject fileObject);

        void onFileObjectOverflowClick(View v, BaseFileObject fileObject);
    }

    public BaseFileObject baseFileObject;

    @Nullable
    private ClickListener listener;

    private boolean showCheckboxes;

    private boolean isChecked;

    public FolderView(BaseFileObject baseFileObject) {
        this.baseFileObject = baseFileObject;
    }

    public void setClickListener(@Nullable ClickListener listener) {
        this.listener = listener;
    }

    public void setShowCheckboxes(boolean show) {
        showCheckboxes = show;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    private void onClick() {
        if (listener != null) {
            listener.onFileObjectClick(baseFileObject);
        }
    }

    private void onOverflowClick(View v) {
        if (listener != null) {
            listener.onFileObjectOverflowClick(v, baseFileObject);
        }
    }

    @Override
    public int getViewType() {
        return FOLDER;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_folder;
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        if (baseFileObject instanceof FileObject && getInstance().getFolderBrowserShowFileNames()) {
            holder.lineFour.setText(format("%s.%s", ((FileObject) baseFileObject).name, ((FileObject) baseFileObject).extension));
            holder.lineFour.setVisibility(VISIBLE);
            holder.textContainer.setVisibility(GONE);
        } else {
            holder.lineFour.setVisibility(GONE);
            holder.textContainer.setVisibility(VISIBLE);
        }

        holder.lineThree.setText(null);

        switch (baseFileObject.fileType) {
            case PARENT:
                holder.imageView.setImageDrawable(holder.parentFolderDrawable);
                holder.lineTwo.setText(holder.itemView.getContext().getString(parent_folder));
                holder.overflow.setVisibility(GONE);
                holder.lineThree.setVisibility(GONE);
                holder.lineOne.setText(baseFileObject.name);
                break;
            case FileType.FOLDER:
                holder.overflow.setVisibility(VISIBLE);
                holder.imageView.setImageDrawable(holder.folderDrawable);
                holder.lineTwo.setText(makeSubfoldersLabel(holder.itemView.getContext(), ((FolderObject) baseFileObject).folderCount, ((FolderObject) baseFileObject).fileCount));
                holder.lineThree.setVisibility(GONE);
                holder.lineOne.setText(baseFileObject.name);
                break;
            case FILE:
                holder.overflow.setVisibility(VISIBLE);
                holder.imageView.setImageDrawable(holder.fileDrawable);
                holder.lineThree.setVisibility(VISIBLE);
                holder.lineOne.setText(((FileObject) baseFileObject).tagInfo.trackName);
                holder.lineTwo.setText(format("%s - %s", ((FileObject) baseFileObject).tagInfo.artistName, ((FileObject) baseFileObject).tagInfo.albumName));
                DurationTask durationTask = new DurationTask(holder.lineThree, (FileObject) baseFileObject);
                durationTask.execute();
                break;
        }

        if (showCheckboxes && baseFileObject.fileType == FileType.FOLDER) {
            holder.checkBox.setVisibility(VISIBLE);
            holder.imageView.setVisibility(GONE);
        } else {
            holder.checkBox.setVisibility(GONE);
            holder.imageView.setVisibility(VISIBLE);
        }

        holder.checkBox.setChecked(isChecked);
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder<FolderView> {

        @BindView(line_one)
        public TextView lineOne;

        @BindView(line_two)
        public TextView lineTwo;

        @BindView(line_three)
        public TextView lineThree;

        @BindView(line_four)
        public TextView lineFour;

        @BindView(id.textContainer)
        public View textContainer;

        @BindView(image)
        public CircleImageView imageView;

        @BindView(btn_overflow)
        public ImageButton overflow;

        @BindView(checkbox)
        public CheckBox checkBox;

        Drawable folderDrawable;
        Drawable parentFolderDrawable;
        Drawable fileDrawable;

        public ViewHolder(final View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(v -> viewModel.onClick());
            overflow.setOnClickListener(v -> viewModel.onOverflowClick(v));

            int colorPrimary = Aesthetic.get().colorPrimary().blockingFirst();

            folderDrawable = itemView.getContext().getResources().getDrawable(ic_folder_closed_white);
            parentFolderDrawable = itemView.getContext().getResources().getDrawable(ic_folder_open_white);
            fileDrawable = itemView.getContext().getResources().getDrawable(ic_headphones_white);

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

        FolderView that = (FolderView) o;

        return baseFileObject != null ? baseFileObject.equals(that.baseFileObject) : that.baseFileObject == null;
    }

    @Override
    public int hashCode() {
        return baseFileObject != null ? baseFileObject.hashCode() : 0;
    }
}