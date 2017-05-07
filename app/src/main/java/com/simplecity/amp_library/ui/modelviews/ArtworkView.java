package com.simplecity.amp_library.ui.modelviews;

import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.glide.loader.TypeLoader;
import com.simplecity.amp_library.glide.utils.BitmapAndSize;
import com.simplecity.amp_library.glide.utils.BitmapAndSizeDecoder;
import com.simplecity.amp_library.model.ArtworkModel;
import com.simplecity.amp_library.model.ArtworkProvider;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import java.io.File;
import java.io.InputStream;

import static android.support.v4.graphics.drawable.DrawableCompat.setTint;
import static android.support.v4.graphics.drawable.DrawableCompat.wrap;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.bumptech.glide.Glide.with;
import static com.bumptech.glide.load.engine.DiskCacheStrategy.NONE;
import static com.simplecity.amp_library.R.drawable;
import static com.simplecity.amp_library.R.drawable.text_protection_scrim_reversed;
import static com.simplecity.amp_library.R.id;
import static com.simplecity.amp_library.R.id.line_one;
import static com.simplecity.amp_library.R.id.line_two;
import static com.simplecity.amp_library.R.layout;
import static com.simplecity.amp_library.R.layout.list_item_artwork;
import static com.simplecity.amp_library.R.string;
import static com.simplecity.amp_library.R.string.artwork_type_custom;
import static com.simplecity.amp_library.model.ArtworkModel.getTypeString;
import static com.simplecity.amp_library.model.ArtworkProvider.Type;
import static com.simplecity.amp_library.model.ArtworkProvider.Type.FOLDER;
import static com.simplecity.amp_library.ui.adapters.ViewType.ARTWORK;
import static com.simplecity.amp_library.ui.modelviews.ArtworkView.ViewHolder;
import static com.simplecity.amp_library.utils.ColorUtils.getAccentColor;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

public class ArtworkView extends BaseViewModel<ViewHolder> {

    public interface GlideListener {
        void onArtworkLoadFailed(ArtworkView artworkView);
    }

    @Type
    private int type;

    private ArtworkProvider artworkProvider;

    private GlideListener listener;

    public File file;

    private boolean selected;

    private boolean isCustom;

    public ArtworkView(int type, ArtworkProvider artworkProvider, GlideListener listener) {
        this(type, artworkProvider, listener, null, false);
    }

    public ArtworkView(int type, ArtworkProvider artworkProvider, GlideListener listener, File file, boolean isCustom) {
        this.type = type;
        this.artworkProvider = artworkProvider;
        this.listener = listener;
        this.file = file;
        this.isCustom = isCustom;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public int getViewType() {
        return ARTWORK;
    }

    @Override
    public int getLayoutResId() {
        return list_item_artwork;
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        long time = currentTimeMillis();

        holder.textContainer.setBackground(null);
        holder.progressBar.setVisibility(VISIBLE);
        holder.lineTwo.setText(null);

        with(holder.itemView.getContext())
                .using(new TypeLoader(type, file), InputStream.class)
                .from(ArtworkProvider.class)
                .as(BitmapAndSize.class)
                .sourceEncoder(new StreamEncoder())
                .decoder(new BitmapAndSizeDecoder(holder.itemView.getContext()))
                .diskCacheStrategy(NONE)
                .load(artworkProvider)
                .listener(new RequestListener<ArtworkProvider, BitmapAndSize>() {
                    @Override
                    public boolean onException(Exception e, ArtworkProvider model, Target<BitmapAndSize> target, boolean isFirstResource) {
                        if (listener != null) {
                            if (holder.itemView.getHandler() != null) {
                                holder.itemView.getHandler().postDelayed(() ->
                                        listener.onArtworkLoadFailed(ArtworkView.this), currentTimeMillis() + 1000 - time);
                            }
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(BitmapAndSize resource, ArtworkProvider model, Target<BitmapAndSize> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(new ImageViewTarget<BitmapAndSize>(((ViewHolder) holder).imageView) {
                    @Override
                    protected void setResource(BitmapAndSize resource) {
                        holder.textContainer.setBackgroundResource(text_protection_scrim_reversed);
                        holder.progressBar.setVisibility(GONE);

                        holder.imageView.setImageBitmap(resource.bitmap);
                        holder.lineTwo.setText(format("%sx%spx", resource.size.width, resource.size.height));
                    }
                });

        holder.lineOne.setText(getTypeString(type));

        if (type == FOLDER && file != null) {
            holder.lineOne.setText(file.getName());
        }

        if (isCustom && file != null && file.getPath().contains("custom_artwork")) {
            holder.lineOne.setText(holder.itemView.getContext().getString(artwork_type_custom));
        }

        holder.checkView.setVisibility(isSelected() ? VISIBLE : GONE);
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder {

        public ImageView imageView;
        public TextView lineOne;
        public TextView lineTwo;
        public View checkView;
        public View textContainer;
        public ProgressBar progressBar;

        public ViewHolder(View itemView) {
            super(itemView);

            imageView = (ImageView) itemView.findViewById(id.imageView);
            lineOne = (TextView) itemView.findViewById(line_one);
            lineTwo = (TextView) itemView.findViewById(line_two);
            checkView = itemView.findViewById(id.checkView);
            textContainer = itemView.findViewById(id.textContainer);
            progressBar = (ProgressBar) itemView.findViewById(id.progressBar);

            setTint(wrap(checkView.getBackground()), getAccentColor());
        }

        @Override
        public String toString() {
            return "ArtworkView.ViewHolder";
        }
    }
}