package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
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
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;
import java.io.File;
import java.io.InputStream;
import java.util.List;

public class ArtworkView extends BaseViewModel<ArtworkView.ViewHolder> {

    public interface GlideListener {
        void onArtworkLoadFailed(ArtworkView artworkView);
    }

    public interface ClickListener {
        void onClick(ArtworkView artworkView);
    }

    @ArtworkProvider.Type
    private int type;

    private ArtworkProvider artworkProvider;

    GlideListener glideListener;

    public File file;

    private boolean selected;

    private boolean isCustom;

    @Nullable
    private ClickListener listener;

    public ArtworkView(int type, ArtworkProvider artworkProvider, GlideListener glideListener) {
        this(type, artworkProvider, glideListener, null, false);
    }

    public ArtworkView(int type, ArtworkProvider artworkProvider, GlideListener glideListener, File file, boolean isCustom) {
        this.type = type;
        this.artworkProvider = artworkProvider;
        this.glideListener = glideListener;
        this.file = file;
        this.isCustom = isCustom;
    }

    public ArtworkModel getItem() {
        return new ArtworkModel(type, file);
    }

    public void setListener(@Nullable ClickListener listener) {
        this.listener = listener;
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

    void onClick() {
        if (listener != null) {
            listener.onClick(this);
        }
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        long time = System.currentTimeMillis();

        holder.textContainer.setBackground(null);
        holder.progressBar.setVisibility(View.VISIBLE);
        holder.lineTwo.setText(null);

        Glide.with(holder.itemView.getContext())
                .using(new TypeLoader(holder.itemView.getContext(), type, file), InputStream.class)
                .from(ArtworkProvider.class)
                .as(BitmapAndSize.class)
                .sourceEncoder(new StreamEncoder())
                .decoder(new BitmapAndSizeDecoder(holder.itemView.getContext()))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .load(artworkProvider)
                .listener(new RequestListener<ArtworkProvider, BitmapAndSize>() {
                    @Override
                    public boolean onException(Exception e, ArtworkProvider model, Target<BitmapAndSize> target, boolean isFirstResource) {
                        if (glideListener != null) {
                            if (holder.itemView.getHandler() != null) {
                                holder.itemView.getHandler().postDelayed(() ->
                                        glideListener.onArtworkLoadFailed(ArtworkView.this), System.currentTimeMillis() + 1000 - time);
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
                        holder.textContainer.setBackgroundResource(R.drawable.text_protection_scrim_reversed);
                        holder.progressBar.setVisibility(View.GONE);

                        holder.imageView.setImageBitmap(resource.bitmap);
                        holder.lineTwo.setText(String.format("%sx%spx", resource.size.width, resource.size.height));
                    }
                });

        holder.lineOne.setText(ArtworkModel.getTypeString(holder.itemView.getContext(), type));

        if (type == ArtworkProvider.Type.FOLDER && file != null) {
            holder.lineOne.setText(file.getName());
        }

        if (isCustom && file != null && file.getPath().contains("custom_artwork")) {
            holder.lineOne.setText(holder.itemView.getContext().getString(R.string.artwork_type_custom));
        }

        holder.checkView.setVisibility(isSelected() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void bindView(ViewHolder holder, int position, List payloads) {
        super.bindView(holder, position, payloads);

        holder.checkView.setVisibility(isSelected() ? View.VISIBLE : View.GONE);
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder<ArtworkView> {

        public ImageView imageView;
        public TextView lineOne;
        public TextView lineTwo;
        public View checkView;
        public View textContainer;
        public ProgressBar progressBar;

        public ViewHolder(View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.imageView);
            lineOne = itemView.findViewById(R.id.line_one);
            lineTwo = itemView.findViewById(R.id.line_two);
            checkView = itemView.findViewById(R.id.checkView);
            textContainer = itemView.findViewById(R.id.textContainer);
            progressBar = itemView.findViewById(R.id.progressBar);

            itemView.setOnClickListener(v -> viewModel.onClick());
        }

        @Override
        public String toString() {
            return "ArtworkView.ViewHolder";
        }
    }
}
