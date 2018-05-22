package com.hal9000.musicplayerapp;

import android.media.MediaMetadataRetriever;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

public class SongListRecAdapter extends RecyclerView.Adapter<SongListRecAdapter.SongViewHolder>{
    private ArrayList<String> mDataset;
    private MediaMetadataRetriever mmr = new MediaMetadataRetriever();
    private ClickListener listener;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class SongViewHolder extends RecyclerView.ViewHolder{

        private TextView songTitleTV, songAuthorTV, songDurationTV;
        private ImageButton playIconButton;

        public SongViewHolder(View v) {
            super(v);
            songTitleTV = v.findViewById(R.id.textView_song_title);
            songAuthorTV = v.findViewById(R.id.textView_author);
            songDurationTV = v.findViewById(R.id.textView_song_duration);
            playIconButton = v.findViewById(R.id.movie_play_button);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public SongListRecAdapter(ArrayList<String> myDataset, ClickListener listener) {
        mDataset = myDataset;
        this.listener = listener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup parent,
                                              int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.music_list_row, parent, false);
        //...
        SongViewHolder vh = new SongViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(SongViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        mmr.setDataSource(mDataset.get(position));

        holder.songTitleTV.setText(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        holder.songAuthorTV.setText(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
        int songDuration = Integer.valueOf(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))/1000;
        holder.songDurationTV.setText(songDuration/60 + ":" + String.format("%02d", songDuration%60));

        holder.playIconButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onItemPlayButtonClick(position);
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void removeItem(int position) {
        mDataset.remove(position);
        notifyItemRemoved(position);
    }


    interface ClickListener {
        void onItemPlayButtonClick(int position);
    }
}

