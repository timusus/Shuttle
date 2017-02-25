package com.simplecity.amp_library.ui.modelviews;

import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
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
import com.simplecity.amp_library.utils.ColorUtils;

import java.io.File;
import java.io.InputStream;

public class ArtworkView extends BaseAdaptableItem<ArtworkModel, ArtworkView.ViewHolder> {

    public interface GlideListener {
        void onArtworkLoadFailed(ArtworkView artworkView);
    }

    @ArtworkProvider.Type
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
        return ViewType.ARTWORK;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_artwork;
    }

    @Override
    public void bindView(ViewHolder holder) {

        long time = System.currentTimeMillis();

        holder.textContainer.setBackground(null);
        holder.progressBar.setVisibility(View.VISIBLE);
        holder.lineTwo.setText(null);

        Glide.with(holder.itemView.getContext())
                .using(new TypeLoader(type, file), InputStream.class)
                .from(ArtworkProvider.class)
                .as(BitmapAndSize.class)
                .sourceEncoder(new StreamEncoder())
                .decoder(new BitmapAndSizeDecoder(holder.itemView.getContext()))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .load(artworkProvider)
                .listener(new RequestListener<ArtworkProvider, BitmapAndSize>() {
                    @Override
                    public boolean onException(Exception e, ArtworkProvider model, Target<BitmapAndSize> target, boolean isFirstResource) {
                        if (listener != null) {
                            if (holder.itemView.getHandler() != null) {
                                holder.itemView.getHandler().postDelayed(() ->
                                        listener.onArtworkLoadFailed(ArtworkView.this), System.currentTimeMillis() + 1000 - time);
                            }
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(BitmapAndSize resource, ArtworkProvider model, Target<BitmapAndSize> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(new ImageViewTarget<BitmapAndSize>(((ArtworkView.ViewHolder) holder).imageView) {
                    @Override
                    protected void setResource(BitmapAndSize resource) {
                        holder.textContainer.setBackgroundResource(R.drawable.text_protection_scrim_reversed);
                        holder.progressBar.setVisibility(View.GONE);

                        holder.imageView.setImageBitmap(resource.bitmap);
                        holder.lineTwo.setText(String.format("%sx%spx", resource.size.width, resource.size.height));
                    }
                });

        holder.lineOne.setText(ArtworkModel.getTypeString(type));

        if (type == ArtworkProvider.Type.FOLDER && file != null) {
            holder.lineOne.setText(file.getName());
        }

        if (isCustom && file != null && file.getPath().contains("custom_artwork")) {
            holder.lineOne.setText(holder.itemView.getContext().getString(R.string.artwork_type_custom));
        }

        holder.checkView.setVisibility(isSelected() ? View.VISIBLE : View.GONE);
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    @Override
    public ArtworkModel getItem() {
        return new ArtworkModel(type, file);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageView;
        public TextView lineOne;
        public TextView lineTwo;
        public View checkView;
        public View textContainer;
        public ProgressBar progressBar;

        public ViewHolder(View itemView) {
            super(itemView);

            imageView = (ImageView) itemView.findViewById(R.id.imageView);
            lineOne = (TextView) itemView.findViewById(R.id.line_one);
            lineTwo = (TextView) itemView.findViewById(R.id.line_two);
            checkView = itemView.findViewById(R.id.checkView);
            textContainer = itemView.findViewById(R.id.textContainer);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);

            DrawableCompat.setTint(DrawableCompat.wrap(checkView.getBackground()), ColorUtils.getAccentColor());
        }

        @Override
        public String toString() {
            return "ArtworkView.ViewHolder";
        }
    }
}